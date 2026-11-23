/**
 * Zalmoxis production-direction remote MCP resource server.
 * Standards-based MCP 2026+ / 2025 compatibility: official SDK + Cloudflare Agents.
 * OAuth2.1 resource protection: Cloudflare Workers OAuth Provider.
 *
 * OAuth authorization server is a separately deployed, user-authenticating
 * Cloudflare service binding. This worker NEVER issues provider tokens itself.
 *
 * No direct device execution, no model API keys, no automatic knowledge writes.
 */
import { McpServer } from "@modelcontextprotocol/server";
import { createMcpHandler, getMcpAuthContext } from "agents/mcp/server";
import {
  OAuthResourceServer,
  insufficientScope,
  type AuthorizationServerBinding,
} from "@cloudflare/workers-oauth-provider";
import { env } from "cloudflare:workers";
import { z } from "zod";

type AuthProps = { userId: string; bridgeId: string };
interface Env {
  DB: D1Database;
  AUTH_SERVER: AuthorizationServerBinding<AuthProps>;
  AUTH_SERVER_URL: string;
  MCP_SERVER_URL: string;
}

const text = (value: unknown) => ({
  content: [{ type: "text" as const, text: JSON.stringify(value) }],
});
const fail = (reason: string) => ({
  isError: true,
  content: [{ type: "text" as const, text: reason }],
});
const uuid = () => crypto.randomUUID();
const current = () => Math.floor(Date.now() / 1000);

function currentOwner(): string | null {
  const auth = getMcpAuthContext();
  const userId = (auth?.props as Partial<AuthProps> | undefined)?.userId;
  return typeof userId === "string" && userId.length >= 3 && userId.length <= 128
    ? userId : null;
}

async function authorizedBridge(): Promise<string | null> {
  const userId = currentOwner();
  if (!userId) return null;
  const row = await env.DB.prepare(
    "SELECT id FROM bridges WHERE owner_id=? AND revoked=0 AND device_token_hash IS NOT NULL ORDER BY created DESC LIMIT 1"
  ).bind(userId).first<{ id: string }>();
  return row?.id ?? null;
}
const hex = (bytes: Uint8Array) =>
  Array.from(bytes, b => b.toString(16).padStart(2, "0")).join("");
const secret = (bytes = 32) => hex(crypto.getRandomValues(new Uint8Array(bytes)));
async function hash(input: string): Promise<string> {
  return hex(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(input))));
}

function createServer() {
  const server = new McpServer({ name: "zalmoxis-agent-bridge", version: "0.2.0" });
  server.registerTool("create_pairing_code", {
    description: "Create a one-time phone pairing code, valid for 15 minutes. Show it to the user for entry in Zalmoxis. Does not grant device tool permissions.",
    inputSchema: { provider: z.enum(["chatgpt","claude","mistral","deepseek","perplexity","other"]) },
  }, async ({ provider }, context) => {
    if (!context.http?.authInfo?.scopes?.includes("mcp:write")) return fail("OAuth mcp:write scope required.");
    const owner = currentOwner();
    if (!owner) return fail("OAuth user identity missing; pairing denied.");
    const existing = await authorizedBridge();
    if (existing) return fail("A phone is already paired. Disconnect or revoke before pairing a new one.");
    const code = secret(8).toUpperCase();
    const id = uuid();
    await env.DB.prepare(
      "INSERT INTO bridges(id,provider,pair_hash,pair_expires,provider_token_hash,owner_id,created) VALUES(?,?,?,?,?,?,?)"
    ).bind(id, provider, await hash(code), current() + 900, await hash(secret()), owner, current()).run();
    return text({ code, provider, expires_in_seconds: 900, bridge_id: id,
      instruction: "Enter this code under Zalmoxis → Workspace → Agent Bridge; do not publish it." });
  });

  server.registerTool("bridge_status", {
    description: "Read the user's Zalmoxis phone bridge pairing status. No actions are executed.",
    inputSchema: {},
  }, async () => {
    const id = await authorizedBridge();
    return text({ connected: !!id, bridge_id: id, phone_reachability: "not measured" });
  });

  server.registerTool("send_task", {
    description: "Queue one message for explicit review in the paired Zalmoxis phone; cannot execute commands or change Knowledge.",
    inputSchema: { message: z.string().trim().min(1).max(12000) },
  }, async ({ message }, context) => {
    if (!context.http?.authInfo?.scopes?.includes("mcp:write")) return fail("OAuth mcp:write scope required.");
    const id = await authorizedBridge();
    if (!id) return fail("Phone is not paired or access has been revoked.");
    const msgId = uuid();
    await env.DB.prepare(
      "INSERT INTO messages(id,bridge_id,direction,body,created,expires,status) VALUES(?,?,?,?,?,?,?)"
    ).bind(msgId, id, "provider_to_phone", message, current(), current() + 86400, "queued").run();
    return text({ id: msgId, status: "queued", user_approval_required: true });
  });

  server.registerTool("list_replies", {
    description: "Read replies/rejections for tasks this provider sent to Zalmoxis.",
    inputSchema: {},
  }, async () => {
    const id = await authorizedBridge();
    if (!id) return fail("Not paired.");
    const result = await env.DB.prepare(
      "SELECT id,status,reply,created FROM messages WHERE bridge_id=? AND direction='provider_to_phone' AND status IN ('replied','rejected') AND expires>? ORDER BY created DESC LIMIT 30"
    ).bind(id, current()).all();
    return text({ messages: result.results });
  });

  server.registerTool("list_phone_requests", {
    description: "Read messages explicitly sent by the user from Zalmoxis for this connected provider.",
    inputSchema: {},
  }, async () => {
    const id = await authorizedBridge();
    if (!id) return fail("Not paired.");
    const result = await env.DB.prepare(
      "SELECT id,body,status,created FROM messages WHERE bridge_id=? AND direction='phone_to_provider' AND status='queued' AND expires>? ORDER BY created DESC LIMIT 30"
    ).bind(id, current()).all();
    return text({ messages: result.results });
  });

  server.registerTool("reply_to_phone", {
    description: "Reply to a specific queued phone-originated message. No device actions are performed.",
    inputSchema: {
      message_id: z.string().uuid(),
      reply: z.string().trim().min(1).max(12000),
    },
  }, async ({ message_id, reply }, context) => {
    if (!context.http?.authInfo?.scopes?.includes("mcp:write")) return fail("OAuth mcp:write scope required.");
    const id = await authorizedBridge();
    if (!id) return fail("Not paired.");
    const result = await env.DB.prepare(
      "UPDATE messages SET reply=?,status='replied' WHERE id=? AND bridge_id=? AND direction='phone_to_provider' AND status='queued' AND expires>?"
    ).bind(reply, message_id, id, current()).run();
    return text({ accepted: result.meta.changes === 1 });
  });
  return server;
}

const mcpHandler = createMcpHandler(createServer);

export default new OAuthResourceServer<Env, AuthProps>({
  resourceMetadata: {
    resource: env.MCP_SERVER_URL,
    authorization_servers: [env.AUTH_SERVER_URL],
  },
  requiredScopes: ["mcp:read"],
  validateToken: (e) => e.AUTH_SERVER.validateToken,
  handler: {
    fetch(request, e, ctx) {
      const scopes = ctx.auth.scope;
      if (!scopes.includes("mcp:read")) {
        return insufficientScope(ctx.auth, ["mcp:read"]);
      }
      // Read-only status/list tools need mcp:read. Message mutations also
      // require mcp:write; enforced inside the registered tool handlers.
      return mcpHandler(request, e, ctx);
    },
  },
});
