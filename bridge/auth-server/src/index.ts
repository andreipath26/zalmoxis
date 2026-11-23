/**
 * OAuth 2.1 authorization service for the Zalmoxis remote MCP endpoint.
 *
 * Cloudflare Access performs account login (including email PIN / SSO).
 * This Worker RE-verifies Access JWT signatures, audience and issuer before
 * showing a click-through authorization screen. There is no anonymous grant.
 * The maintained Cloudflare OAuth library handles PKCE, token issuance,
 * discovery, client registration, resource binding and validation.
 */
import {
  AuthorizationError,
  OAuthAuthorizationServer,
  type AuthRequest,
} from "@cloudflare/workers-oauth-provider";
import { env, WorkerEntrypoint } from "cloudflare:workers";
import { createRemoteJWKSet, jwtVerify } from "jose";

interface Env {
  OAUTH_KV: KVNamespace;
  ISSUER: string;
  MCP_SERVER_URL: string;
  ACCESS_TEAM_DOMAIN: string;
  ACCESS_AUD: string;
}
const SCOPES = ["mcp:read", "mcp:write"];
const oauthServer = new OAuthAuthorizationServer<Env>({
  issuer: env.ISSUER,
  resources: [env.MCP_SERVER_URL],
  scopesSupported: SCOPES,
  clientIdMetadataDocumentEnabled: true,
  clientRegistrationEndpoint: "/oauth/register",
});
const escapeHtml = (s: string) =>
  s.replaceAll("&","&amp;").replaceAll("<","&lt;")
    .replaceAll(">","&gt;").replaceAll('"',"&quot;").replaceAll("'","&#39;");

async function identity(request: Request, e: Env): Promise<string | null> {
  const assertion = request.headers.get("cf-access-jwt-assertion");
  if (!assertion || assertion.length > 16000) return null;
  const domain = e.ACCESS_TEAM_DOMAIN;
  if (!/^[a-z0-9-]+\.cloudflareaccess\.com$/i.test(domain)) return null;
  const issuer = `https://${domain}`;
  const keys = createRemoteJWKSet(new URL(`${issuer}/cdn-cgi/access/certs`));
  try {
    const { payload } = await jwtVerify(assertion, keys, {
      issuer,
      audience: e.ACCESS_AUD,
      algorithms: ["RS256"],
    });
    return typeof payload.sub === "string" && payload.sub.length >= 3
      ? payload.sub.slice(0, 128) : null;
  } catch {
    return null;
  }
}

async function authorize(request: Request, e: Env): Promise<Response> {
  const userId = await identity(request, e);
  if (!userId) return new Response("Cloudflare Access authentication required.", {
    status: 401, headers: { "Cache-Control": "no-store" },
  });
  const oauth = oauthServer.getOAuthApi(e);
  let requestDetails: AuthRequest;
  try {
    requestDetails = await oauth.parseAuthRequest(request);
  } catch (error) {
    if (error instanceof AuthorizationError) {
      if (error.redirectTo) return Response.redirect(error.redirectTo, 302);
      return new Response(error.description, { status: 400 });
    }
    throw error;
  }

  const scopes = requestDetails.scope.filter(s => SCOPES.includes(s));
  const headers = {
    "Cache-Control": "no-store",
    "Content-Security-Policy": "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'",
    "X-Frame-Options": "DENY",
    "X-Content-Type-Options": "nosniff",
  };

  if (request.method === "GET") {
    const nonce = crypto.randomUUID();
    const requestedScopes = escapeHtml(scopes.join(", ") || "mcp:read");
    const html = `<!doctype html><html lang="en"><head><meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Authorize Zalmoxis MCP</title>
    <style>body{font:16px system-ui;background:#0c110e;color:#f2f5ed;max-width:600px;
    margin:45px auto;padding:20px}button{padding:14px 22px;border:0;border-radius:12px;
    background:#c2fa59;font-weight:700}p{line-height:1.6;color:#bac6bc}</style></head>
    <body><h1>Connect your AI agent to Zalmoxis?</h1>
    <p>The provider requests: <strong>${requestedScopes}</strong></p>
    <p>The connection can send messages and read your responses only if those scopes
    are granted. It cannot run device tools or read your Knowledge database.</p>
    <form method="post"><input type="hidden" name="csrf" value="${nonce}">
    <button type="submit">Approve connection</button></form>
    <p>To reject, close this page.</p></body></html>`;
    return new Response(html, {
      headers: {
        ...headers,
        "Content-Type": "text/html; charset=utf-8",
        "Set-Cookie": `zalmoxis_csrf=${nonce}; Max-Age=300; Path=/authorize; Secure; HttpOnly; SameSite=Strict`,
      },
    });
  }

  if (request.method !== "POST") return new Response("Method not allowed", { status: 405 });
  const origin = request.headers.get("origin");
  if (origin !== e.ISSUER) return new Response("Request origin rejected.", { status: 403 });
  const post = await request.formData();
  const csrf = post.get("csrf");
  const cookie = request.headers.get("cookie")?.match(/(?:^|; )zalmoxis_csrf=([0-9a-f-]{36})(?:;|$)/)?.[1];
  if (typeof csrf !== "string" || !cookie || csrf !== cookie)
    return new Response("Authorization expired; restart sign-in.", { status: 403 });
  const { redirectTo } = await oauth.completeAuthorization({
    request: requestDetails,
    userId,
    metadata: {},
    scope: scopes,
    props: { userId, bridgeId: "" },
  });
  return new Response(null, {
    status: 302,
    headers: {
      "Location": redirectTo,
      "Cache-Control": "no-store",
      "Set-Cookie": "zalmoxis_csrf=; Max-Age=0; Path=/authorize; Secure; HttpOnly; SameSite=Strict",
    },
  });
}

export default class ZalmoxisAuthServer extends WorkerEntrypoint<Env> {
  fetch(request: Request) {
    if (new URL(request.url).pathname === "/authorize") {
      return authorize(request, this.env);
    }
    return oauthServer.fetch(request, this.env, this.ctx);
  }
  validateToken(resource: string, token: string) {
    return oauthServer.validateToken(resource, token, this.env);
  }
}
