/**
 * Android-side HTTPS API for the OAuth-backed MCP bridge.
 *
 * The user pairs via one-time 64-bit code exposed by an authenticated MCP tool.
 * A 256-bit device credential is returned once; Android encrypts it in Keystore.
 * Provider accounts never receive the phone credential.
 */
export interface PhoneEnv { DB: D1Database }
const json = (value: unknown, status = 200) =>
  new Response(JSON.stringify(value), {
    status,
    headers: { "Content-Type": "application/json", "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff", "Referrer-Policy": "no-referrer" },
  });
const error = (message: string, status = 400) => json({ error: message }, status);
const now = () => Math.floor(Date.now() / 1000);
const uuid = () => crypto.randomUUID();
const hex = (bytes: Uint8Array) =>
  Array.from(bytes, b => b.toString(16).padStart(2, "0")).join("");
const newSecret = () => hex(crypto.getRandomValues(new Uint8Array(32)));
const sha = async (input: string) => hex(new Uint8Array(
  await crypto.subtle.digest("SHA-256", new TextEncoder().encode(input))
));
const validId = (value: unknown): value is string =>
  typeof value === "string" &&
  /^[0-9a-f]{8}-[0-9a-f-]{27,36}$/i.test(value);
const validText = (value: unknown): value is string =>
  typeof value === "string" && value.trim().length > 0 && value.length <= 12000;
async function parseBody(req: Request): Promise<Record<string, unknown>> {
  if (!req.headers.get("content-type")?.includes("application/json"))
    throw new Error("JSON content type required.");
  const data = await req.text();
  if (data.length > 16000) throw new Error("Request exceeds size limit.");
  const value: unknown = JSON.parse(data);
  if (value === null || typeof value !== "object" || Array.isArray(value))
    throw new Error("Invalid object.");
  return value as Record<string, unknown>;
}
async function authorize(req: Request, env: PhoneEnv): Promise<string | null> {
  const bearer = req.headers.get("authorization")?.match(/^Bearer ([a-f0-9]{64})$/i)?.[1];
  if (!bearer) return null;
  const row = await env.DB.prepare(
    "SELECT id FROM bridges WHERE device_token_hash=? AND revoked=0 AND device_token_hash IS NOT NULL"
  ).bind(await sha(bearer)).first<{id:string}>();
  return row?.id ?? null;
}
async function pair(req: Request, env: PhoneEnv): Promise<Response> {
  const actor = (req.headers.get("cf-connecting-ip") || "unknown").slice(0,64);
  // Only count attempts after a bounded request is received.
  await env.DB.prepare("DELETE FROM pairing_attempts WHERE attempted_at<?")
    .bind(now()-900).run();
  const limit = await env.DB.prepare(
    "SELECT COUNT(*) AS count FROM pairing_attempts WHERE actor=? AND attempted_at>?"
  ).bind(actor,now()-900).first<{count:number}>();
  if ((limit?.count ?? 0) >= 10) return error("Too many pairing attempts; try later.",429);
  await env.DB.prepare("INSERT INTO pairing_attempts(actor,attempted_at) VALUES(?,?)")
    .bind(actor,now()).run();
  const data = await parseBody(req);
  const code = typeof data.pairing_code === "string" ? data.pairing_code.trim().toUpperCase() : "";
  if (!/^[A-F0-9]{16}$/.test(code)) return error("Invalid or expired pairing code.",403);
  const row = await env.DB.prepare(
    "SELECT id,provider FROM bridges WHERE pair_hash=? AND pair_expires>? AND revoked=0 AND device_token_hash IS NULL"
  ).bind(await sha(code),now()).first<{id:string;provider:string}>();
  if (!row) return error("Invalid or expired pairing code.",403);
  const deviceToken = newSecret();
  const update = await env.DB.prepare(
    "UPDATE bridges SET device_token_hash=?, pair_hash='',pair_expires=0 WHERE id=? AND device_token_hash IS NULL AND pair_expires>?"
  ).bind(await sha(deviceToken),row.id,now()).run();
  if (update.meta.changes !== 1) return error("Pairing code already used or expired.",409);
  return json({ device_token:deviceToken, provider:row.provider, bridge_id:row.id },201);
}
export async function phoneFetch(req: Request, env: PhoneEnv): Promise<Response> {
  const path = new URL(req.url).pathname;
  try {
    if(path==="/v1/pair" && req.method==="POST") return await pair(req,env);
    if(!path.startsWith("/v1/device/")) return error("Not found.",404);
    const bridge = await authorize(req,env);
    if(!bridge) return error("Unauthorized or revoked.",401);
    if(req.method==="GET" && path==="/v1/device/status")
      return json({ paired:true, bridge_id:bridge });
    if(req.method==="GET" && path==="/v1/device/inbox") {
      const rows=await env.DB.prepare(
        "SELECT id,body,created FROM messages WHERE bridge_id=? AND direction='provider_to_phone' AND status='queued' AND expires>? ORDER BY created ASC LIMIT 25"
      ).bind(bridge,now()).all();
      return json({messages:rows.results});
    }
    if(req.method==="GET" && path==="/v1/device/outbox") {
      const rows=await env.DB.prepare(
        "SELECT id,reply,status,created FROM messages WHERE bridge_id=? AND direction='phone_to_provider' AND expires>? ORDER BY created DESC LIMIT 25"
      ).bind(bridge,now()).all();
      return json({messages:rows.results});
    }
    if(req.method==="POST" && path==="/v1/device/send") {
      const body=await parseBody(req);
      if(!validText(body.message)) return error("Invalid message.");
      const messageId=uuid();
      await env.DB.prepare(
        "INSERT INTO messages(id,bridge_id,direction,body,created,expires,status) VALUES(?,?,?,?,?,?,?)"
      ).bind(messageId,bridge,"phone_to_provider",body.message.trim(),now(),now()+86400,"queued").run();
      return json({message_id:messageId,status:"queued"},201);
    }
    if(req.method==="POST" && path==="/v1/device/answer") {
      const body=await parseBody(req);
      if(!validId(body.message_id)) return error("Invalid message ID.");
      const reject=body.reject===true;
      const reply=reject?"Rejected by the Zalmoxis user.":body.reply;
      if(!validText(reply)) return error("Invalid reply.");
      const result=await env.DB.prepare(
        "UPDATE messages SET status=?,reply=? WHERE id=? AND bridge_id=? AND direction='provider_to_phone' AND status='queued' AND expires>?"
      ).bind(reject?"rejected":"replied",reply,body.message_id,bridge,now()).run();
      if(result.meta.changes!==1) return error("Message already handled or expired.",409);
      return json({status:reject?"rejected":"replied"});
    }
    if(req.method==="POST" && path==="/v1/device/disconnect") {
      await env.DB.prepare("UPDATE bridges SET revoked=1 WHERE id=?").bind(bridge).run();
      await env.DB.prepare("DELETE FROM messages WHERE bridge_id=?").bind(bridge).run();
      return json({disconnected:true});
    }
    return error("Not found.",404);
  }catch(e) {
    return error(e instanceof SyntaxError?"Invalid JSON.":e instanceof Error?e.message:"Request failed.",400);
  }
}
