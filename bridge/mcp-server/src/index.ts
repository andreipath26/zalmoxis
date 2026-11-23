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
import { env as cloudflareEnv } from "cloudflare:workers";
import { z } from "zod";
import { phoneFetch } from "./phone";

type AuthProps = { userId: string; bridgeId: string };
interface Env {
  DB: D1Database;
  AUTH_SERVER: AuthorizationServerBinding<AuthProps>;
  AUTH_SERVER_URL: string;
  MCP_SERVER_URL: string;
}

const env = cloudflareEnv as unknown as Env;

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

async function authorizedBridge(clientId: string | undefined): Promise<string | null> {
  const userId = currentOwner();
  if (!userId || !clientId || clientId.length > 256) return null;
  const row = await env.DB.prepare(
    "SELECT id FROM bridges WHERE owner_id=? AND client_id=? AND revoked=0 AND device_token_hash IS NOT NULL ORDER BY created DESC LIMIT 1"
  ).bind(userId, clientId).first<{ id: string }>();
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
    const clientId = context.http?.authInfo?.clientId;
    if (!owner || !clientId) return fail("OAuth identity/client ID missing; pairing denied.");
    await env.DB.prepare(
      "UPDATE bridges SET revoked=1 WHERE owner_id=? AND client_id=? AND device_token_hash IS NULL AND pair_expires<?"
    ).bind(owner,clientId,current()).run();
    const existing = await env.DB.prepare(
      "SELECT id FROM bridges WHERE owner_id=? AND client_id=? AND revoked=0 LIMIT 1"
    ).bind(owner,clientId).first();
    if (existing) return fail("A phone is already paired or a code is pending. Disconnect, revoke, or allow the code to expire.");
    const code = secret(8).toUpperCase();
    const id = uuid();
    await env.DB.prepare(
      "INSERT INTO bridges(id,provider,pair_hash,pair_expires,provider_token_hash,owner_id,client_id,created) VALUES(?,?,?,?,?,?,?,?)"
    ).bind(id, provider, await hash(code), current() + 900, await hash(secret()), owner, clientId, current()).run();
    return text({ code, provider, expires_in_seconds: 900, bridge_id: id,
      instruction: "Enter this code under Zalmoxis → Workspace → Agent Bridge; do not publish it." });
  });

  server.registerTool("revoke_bridge", {
    description: "Revoke this OAuth client's Zalmoxis phone pairing and delete its queued messages. This cannot be undone.",
    inputSchema: { confirm: z.literal(true) },
  }, async ({ confirm }, context) => {
    if (!confirm || !context.http?.authInfo?.scopes?.includes("mcp:write")) {
      return fail("Explicit confirmation and OAuth mcp:write scope required.");
    }
    const owner = currentOwner();
    const clientId = context.http?.authInfo?.clientId;
    if (!owner || !clientId) return fail("Identity or OAuth client missing.");
    const matches = await env.DB.prepare(
      "SELECT id FROM bridges WHERE owner_id=? AND client_id=? AND revoked=0"
    ).bind(owner,clientId).all<{id:string}>();
    for (const row of matches.results) {
      await env.DB.prepare("DELETE FROM messages WHERE bridge_id=?").bind(row.id).run();
      await env.DB.prepare("UPDATE bridges SET revoked=1 WHERE id=?").bind(row.id).run();
    }
    return text({ revoked: matches.results.length, messages_deleted: true,
      note: "Any previously paired phone must re-pair before using this channel." });
  });

  server.registerTool("bridge_status", {
    description: "Read the user's Zalmoxis phone bridge pairing status. No actions are executed.",
    inputSchema: {},
  }, async (_args, context) => {
    const id = await authorizedBridge(context.http?.authInfo?.clientId);
    return text({ connected: !!id, bridge_id: id, phone_reachability: "not measured" });
  });

  server.registerTool("send_task", {
    description: "Queue one message for explicit review in the paired Zalmoxis phone; cannot execute commands or change Knowledge.",
    inputSchema: { message: z.string().trim().min(1).max(12000) },
  }, async ({ message }, context) => {
    if (!context.http?.authInfo?.scopes?.includes("mcp:write")) return fail("OAuth mcp:write scope required.");
    const id = await authorizedBridge(context.http?.authInfo?.clientId);
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
  }, async (_args, context) => {
    const id = await authorizedBridge(context.http?.authInfo?.clientId);
    if (!id) return fail("Not paired.");
    const result = await env.DB.prepare(
      "SELECT id,status,reply,created FROM messages WHERE bridge_id=? AND direction='provider_to_phone' AND status IN ('replied','rejected') AND expires>? ORDER BY created DESC LIMIT 30"
    ).bind(id, current()).all();
    return text({ messages: result.results });
  });

  server.registerTool("list_phone_requests", {
    description: "Read messages explicitly sent by the user from Zalmoxis for this connected provider.",
    inputSchema: {},
  }, async (_args, context) => {
    const id = await authorizedBridge(context.http?.authInfo?.clientId);
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
    const id = await authorizedBridge(context.http?.authInfo?.clientId);
    if (!id) return fail("Not paired.");
    const result = await env.DB.prepare(
      "UPDATE messages SET reply=?,status='replied' WHERE id=? AND bridge_id=? AND direction='phone_to_provider' AND status='queued' AND expires>?"
    ).bind(reply, message_id, id, current()).run();
    return text({ accepted: result.meta.changes === 1 });
  });
  return server;
}

const mcpHandler = createMcpHandler(createServer);

const oauthMcp = new OAuthResourceServer<Env, AuthProps>({
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

/** The /mcp route uses standards-compliant OAuth; phone routes use a separately
 * paired 256-bit credential. Neither accepts the other's credential. */
export default {
  fetch(request: Request, workerEnv: Env, ctx: ExecutionContext): Promise<Response> | Response {
    const pathname = new URL(request.url).pathname;
    if (pathname === "/v1/pair" || pathname.startsWith("/v1/device/")) {
      return phoneFetch(request, workerEnv);
    }
    if (pathname === "/health" && request.method === "GET") {
      return new Response(JSON.stringify({ service: "zalmoxis-mcp", status: "ready" }), {
        headers: { "Content-Type": "application/json", "Cache-Control": "no-store" },
      });
    }
    return oauthMcp.fetch(request, workerEnv, ctx);
  },
  async scheduled(_event: ScheduledController, workerEnv: Env) {
    const unix = current();
    await workerEnv.DB.batch([
      workerEnv.DB.prepare("DELETE FROM messages WHERE expires<?").bind(unix),
      workerEnv.DB.prepare("DELETE FROM pairing_attempts WHERE attempted_at<?").bind(unix-900),
      workerEnv.DB.prepare(
        "UPDATE bridges SET revoked=1 WHERE device_token_hash IS NULL AND pair_expires<?"
      ).bind(unix),
    ]);
  },
};
