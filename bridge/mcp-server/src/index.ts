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

function bridgeIdFromAuth(): string | null {
  const auth = getMcpAuthContext();
  const bridgeId = (auth?.props as Partial<AuthProps> | undefined)?.bridgeId;
  return typeof bridgeId === "string" &&
    /^[0-9a-f]{8}-[0-9a-f-]{27,36}$/i.test(bridgeId)
    ? bridgeId : null;
}

async function authorizedBridge(): Promise<string | null> {
  const bridgeId = bridgeIdFromAuth();
  if (!bridgeId) return null;
  const row = await env.DB.prepare(
    "SELECT id FROM bridges WHERE id=? AND revoked=0 AND device_token_hash IS NOT NULL"
  ).bind(bridgeId).first();
  return row ? bridgeId : null;
}

function createServer() {
  const server = new McpServer({ name: "zalmoxis-agent-bridge", version: "0.2.0" });
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
  }, async ({ message }) => {
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
  }, async ({ message_id, reply }) => {
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
      // Read-only status/list tools and message writes are distinguished by
      // mcp:write; the tool handlers check grants before mutations.
      return mcpHandler(request, e, ctx);
    },
  },
});
