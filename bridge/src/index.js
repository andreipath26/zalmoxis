// DEVELOPMENT PROTOTYPE ONLY — NOT A PRODUCTION MCP/OAUTH SERVER.
// This hand-written JSON-RPC transport is superseded by the upstream-first decision:
// docs/decisions/MCP-BRIDGE-REUSE-ASSESSMENT.md
// Do not deploy publicly as an authenticated provider MCP endpoint.
// Production will use the official MCP SDK / Cloudflare Agents and Workers OAuth Provider.
// Zalmoxis Bridge: minimal remote MCP + phone JSON relay.
// Deployed behind HTTPS on Cloudflare Workers; no model API key.
const headers = {"content-type":"application/json","cache-control":"no-store","x-content-type-options":"nosniff"};
const now = () => Math.floor(Date.now()/1000);
const uuid = () => crypto.randomUUID();
const token = () => Array.from(crypto.getRandomValues(new Uint8Array(32)),b=>b.toString(16).padStart(2,"0")).join("");
async function sha(s){const a=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(s));return Array.from(new Uint8Array(a),b=>b.toString(16).padStart(2,"0")).join("")}
const json=(obj,status=200)=>new Response(JSON.stringify(obj),{status,headers});
const err=(msg,status=400)=>json({error:msg},status);
const auth=req=>{const a=req.headers.get("authorization")||"";return a.startsWith("Bearer ")?a.slice(7):null};
const ID=/^[0-9a-f]{8}-[0-9a-f-]{27,36}$/i;
const providers=new Set(["chatgpt","claude","mistral","deepseek","perplexity","other"]);
async function body(req,max=16000){
 const raw=await req.text();
 if(raw.length>max)throw new Error("Request exceeds size limit");
 return JSON.parse(raw);
}
const cleanText=x=>typeof x==="string"&&x.trim().length>0&&x.length<=12000?x.trim():null;
async function lookup(env,req,role){
 const key=auth(req);if(!key)return null;
 const hash=await sha(key);
 const column=role==="device"?"device_token_hash":"provider_token_hash";
 return env.DB.prepare(`SELECT * FROM bridges WHERE ${column}=? AND revoked=0`).bind(hash).first();
}
async function purge(env){await env.DB.prepare("DELETE FROM messages WHERE expires<?").bind(now()).run()}
function mcpResult(id,data){return json({jsonrpc:"2.0",id,result:data})}
function mcpError(id,msg,code=-32602){return json({jsonrpc:"2.0",id,error:{code,message:msg}})}
const tools=[
 {name:"bridge_status",description:"Check whether the paired Zalmoxis phone is registered.",inputSchema:{type:"object",properties:{},additionalProperties:false}},
 {name:"send_task",description:"Queue a bounded message for review by the paired Zalmoxis user. Cannot execute tools or write Knowledge.",inputSchema:{type:"object",properties:{message:{type:"string",maxLength:12000}},required:["message"],additionalProperties:false}},
 {name:"list_replies",description:"Read phone responses to tasks this connector sent.",inputSchema:{type:"object",properties:{},additionalProperties:false}},
 {name:"list_phone_requests",description:"Read messages submitted by the Zalmoxis phone for this provider.",inputSchema:{type:"object",properties:{},additionalProperties:false}},
 {name:"reply_to_phone",description:"Send a reply to a specific user-initiated phone message.",inputSchema:{type:"object",properties:{message_id:{type:"string"},reply:{type:"string",maxLength:12000}},required:["message_id","reply"],additionalProperties:false}},
];
async function mcp(req,env){
 const bridge=await lookup(env,req,"provider");
 if(!bridge)return err("Unauthorized",401);
 if(req.method==="GET")return err("SSE subscription not implemented; use POST tools.",405);
 if(req.method!=="POST")return err("Method not allowed",405);
 let packet;try{packet=await body(req)}catch{return err("Invalid JSON",400)}
 const id=packet.id??null;
 if(packet.method==="initialize")return mcpResult(id,{protocolVersion:"2025-11-25",capabilities:{tools:{listChanged:false}},serverInfo:{name:"zalmoxis-bridge",version:"0.1.0"}});
 if(packet.method==="notifications/initialized")return new Response(null,{status:202});
 if(packet.method==="ping")return mcpResult(id,{});
 if(packet.method==="tools/list")return mcpResult(id,{tools});
 if(packet.method!=="tools/call")return mcpError(id,"Unknown method",-32601);
 const name=packet.params?.name,args=packet.params?.arguments??{};
 if(!tools.some(x=>x.name===name))return mcpError(id,"Unknown tool",-32601);
 let result;
 if(name==="bridge_status")result={paired:!!bridge.device_token_hash,provider:bridge.provider,bridge_id:bridge.id};
 if(name==="send_task"){
   const message=cleanText(args.message);if(!message)return mcpError(id,"Invalid message");
   if(!bridge.device_token_hash)return mcpError(id,"Phone not paired");
   const mid=uuid();
   await env.DB.prepare("INSERT INTO messages(id,bridge_id,direction,body,created,expires,status) VALUES(?,?,?,?,?,?,?)")
     .bind(mid,bridge.id,"provider_to_phone",message,now(),now()+86400,"queued").run();
   result={message_id:mid,status:"queued",note:"User approval required; no automatic execution."};
 }
 if(name==="list_replies"){
   const rows=await env.DB.prepare("SELECT id,reply,status,created FROM messages WHERE bridge_id=? AND direction='provider_to_phone' AND status IN ('replied','rejected') ORDER BY created DESC LIMIT 30").bind(bridge.id).all();
   result={messages:rows.results};
 }
 if(name==="list_phone_requests"){
   const rows=await env.DB.prepare("SELECT id,body,created,status FROM messages WHERE bridge_id=? AND direction='phone_to_provider' AND status IN ('queued','replied') ORDER BY created DESC LIMIT 30").bind(bridge.id).all();
   result={messages:rows.results};
 }
 if(name==="reply_to_phone"){
   const reply=cleanText(args.reply);
   if(!reply||!ID.test(args.message_id||""))return mcpError(id,"Invalid reply or message ID");
   const r=await env.DB.prepare("UPDATE messages SET status='replied',reply=? WHERE id=? AND bridge_id=? AND direction='phone_to_provider' AND status='queued'")
     .bind(reply,args.message_id,bridge.id).run();
   result={updated:r.meta.changes===1};
 }
 return mcpResult(id,{content:[{type:"text",text:JSON.stringify(result)}],structuredContent:result,isError:false});
}
export default {
 async fetch(req,env){
  const url=new URL(req.url);
  try{
   if(url.pathname==="/health"&&req.method==="GET")return json({service:"zalmoxis-bridge",status:"ready",version:1});
   // Disabled until standards-compliant MCP SDK and OAuth 2.1 migration is completed.
   if(url.pathname==="/mcp")return err("MCP prototype disabled; use the supported SDK/OAuth implementation.",503);
   if(req.method==="POST"&&url.pathname==="/v1/admin/provision"){
    if(!env.ADMIN_TOKEN||auth(req)!==env.ADMIN_TOKEN)return err("Unauthorized",401);
    const b=await body(req,512),provider=b.provider;
    if(!providers.has(provider))return err("Invalid provider");
    const pairing=Array.from(crypto.getRandomValues(new Uint8Array(8)),x=>x.toString(16).padStart(2,"0")).join("").toUpperCase();
    const ptoken=token(),id=uuid();
    await env.DB.prepare("INSERT INTO bridges(id,provider,pair_hash,pair_expires,provider_token_hash,created) VALUES(?,?,?,?,?,?)")
      .bind(id,provider,await sha(pairing),now()+900,await sha(ptoken),now()).run();
    return json({bridge_id:id,pairing_code:pairing,pairing_expires_in_seconds:900,provider_token:ptoken,provider,warning:"Treat provider token like a password."},201);
   }
   if(req.method==="POST"&&url.pathname==="/v1/pair"){
    const data=await body(req,512),pairing=typeof data.pairing_code==="string"?data.pairing_code.trim().toUpperCase():"";
    if(!/^[A-F0-9]{16}$/.test(pairing))return err("Invalid or expired pairing code",403);
    const hash=await sha(pairing),row=await env.DB.prepare("SELECT id,provider FROM bridges WHERE pair_hash=? AND pair_expires>? AND revoked=0 AND device_token_hash IS NULL").bind(hash,now()).first();
    if(!row)return err("Invalid or expired pairing code",403);
    const deviceToken=token();
    const r=await env.DB.prepare("UPDATE bridges SET device_token_hash=?,pair_hash='',pair_expires=0 WHERE id=? AND device_token_hash IS NULL")
      .bind(await sha(deviceToken),row.id).run();
    if(r.meta.changes!==1)return err("Pairing code already used",409);
    return json({device_token:deviceToken,bridge_id:row.id,provider:row.provider},201);
   }
   if(url.pathname.startsWith("/v1/device/")){
    const bridge=await lookup(env,req,"device");
    if(!bridge)return err("Unauthorized",401);
    if(req.method==="GET"&&url.pathname==="/v1/device/status")
      return json({bridge_id:bridge.id,provider:bridge.provider,paired:true});
    if(req.method==="GET"&&url.pathname==="/v1/device/inbox"){
      const rows=await env.DB.prepare("SELECT id,body,created FROM messages WHERE bridge_id=? AND direction='provider_to_phone' AND status='queued' AND expires>? ORDER BY created ASC LIMIT 25")
        .bind(bridge.id,now()).all();return json({messages:rows.results});
    }
    if(req.method==="GET"&&url.pathname==="/v1/device/outbox"){
      const rows=await env.DB.prepare("SELECT id,reply,status,created FROM messages WHERE bridge_id=? AND direction='phone_to_provider' ORDER BY created DESC LIMIT 25")
        .bind(bridge.id).all();return json({messages:rows.results});
    }
    if(req.method==="POST"&&url.pathname==="/v1/device/send"){
      const b=await body(req),msg=cleanText(b.message);if(!msg)return err("Invalid message");
      const id=uuid();await env.DB.prepare("INSERT INTO messages(id,bridge_id,direction,body,created,expires,status) VALUES(?,?,?,?,?,?,?)")
        .bind(id,bridge.id,"phone_to_provider",msg,now(),now()+86400,"queued").run();
      return json({message_id:id,status:"queued"},201);
    }
    if(req.method==="POST"&&url.pathname==="/v1/device/answer"){
      const b=await body(req);if(!ID.test(b.message_id||""))return err("Invalid message ID");
      const answer=b.reject?"Rejected by the Zalmoxis user.":cleanText(b.reply);
      if(!answer)return err("Invalid reply");
      const r=await env.DB.prepare("UPDATE messages SET status=?,reply=? WHERE id=? AND bridge_id=? AND direction='provider_to_phone' AND status='queued'")
        .bind(b.reject?"rejected":"replied",answer,b.message_id,bridge.id).run();
      if(r.meta.changes!==1)return err("Message already answered or expired",409);
      return json({status:b.reject?"rejected":"replied"});
    }
    if(req.method==="POST"&&url.pathname==="/v1/device/disconnect"){
      await env.DB.prepare("UPDATE bridges SET revoked=1 WHERE id=?").bind(bridge.id).run();
      await env.DB.prepare("DELETE FROM messages WHERE bridge_id=?").bind(bridge.id).run();
      return json({disconnected:true});
    }
   }
   return err("Not found",404);
  }catch(e){return err(e instanceof SyntaxError?"Invalid JSON":e.message||"Request failed",400)}
 }
};
