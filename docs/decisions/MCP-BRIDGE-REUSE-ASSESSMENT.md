# Zalmoxis MCP/A2A Bridge — Build-vs-Reuse Decision
Date: 2026-11-23
Status: **ACCEPTED FOR ARCHITECTURE; CODE NOT YET MIGRATED OR DEPLOYED**

## Decision
**Stop expanding the home-grown MCP JSON-RPC and authorization implementation in `bridge/src/index.js`.** Preserve its pairing/message queue as a prototype reference only. Migrate the public connector endpoint to the official MCP SDK / Cloudflare Agents MCP handler with standards-compliant remote transport and a maintained OAuth 2.1 resource server. Use A2A semantics for agent tasks and replies without claiming consumer AI apps automatically initiate messages.

## Primary adopted dependencies
1. **Official MCP SDK** — https://github.com/modelcontextprotocol/typescript-sdk — current v2 2026-07-28 protocol implementation. Owns tools, schema validation, Streamable HTTP, transport/session rules, request/response messages, notifications, spec negotiation and interoperability. Never hand-code another ad-hoc MCP implementation.
2. **Cloudflare Agents MCP runtime** — https://github.com/cloudflare/agents — remote MCP Worker integration, `createMcpHandler` and testing. Production auth via OAuth resource-server integration. Current docs: https://developers.cloudflare.com/agents/model-context-protocol/guides/remote-mcp-server/
3. **Cloudflare Workers OAuth Provider** — https://github.com/cloudflare/workers-oauth-provider — OAuth 2.1 authorization server/resource server, bearer validation, discovery, client registration, PKCE and resource/audience binding. See https://developers.cloudflare.com/agents/model-context-protocol/guides/securing-mcp-server/ . Never replace these with a static provider bearer token in a production connector.
4. **A2A official protocol/SDKs** — https://github.com/a2aproject/A2A and https://github.com/a2aproject/a2a-js — use task/message/artifact/status/correlation/reply semantics in the provider-independent message service; implement an A2A endpoint only when compatibility with agent clients is demonstrated. MCP exposes tools to provider agents; A2A enables agent-to-agent task exchange and is not itself a hosted model.

## Evaluated, deferred
| Project | Links | Use case | Reason not to ship initially |
| --- | --- | --- | --- |
| agentgateway | https://github.com/agentgateway/agentgateway | MCP/A2A gateway, OAuth, authentication, policy, observability | Excellent scale-out; extra infrastructure for one Android device |
| IBM ContextForge | https://github.com/IBM/mcp-context-forge | MCP/A2A proxy, discovery, governance, registry | Useful multi-tenant deployment, heavier Python/container operations |
| Cloudflare MCP portals | https://developers.cloudflare.com/agents/model-context-protocol/cloudflare/mcp-portal/ | Federated remote servers | Optional managed aggregation later |
| Supergateway | https://github.com/supercorp-ai/supergateway | stdio <-> HTTP bridge for existing MCP clients | Transport conversion only, not phone pairing or auth |
| mcp-proxy | https://github.com/sparfenyuk/mcp-proxy | stdio/HTTP transport bridging | Useful for desktop DeepSeek harness compatibility; not a trust boundary |
| mcpo | https://github.com/open-webui/mcpo | MCP to OpenAPI proxy | Only if a particular provider requires OpenAPI |
| Pipelock | https://github.com/pipelab-org/pipelock | MCP/A2A traffic/prompt-injection firewall | Defense in depth, not a replacement for app policy |
| DeepSeek Harness MCP | https://github.com/deepseek-ai/deepseek-harness/blob/master/packages/mcp/mcp-client/README.md | Existing client to Zalmoxis hosted MCP | Required provider interoperability test |
| DeepSeek MCP Manager | https://github.com/hyqhyq3/dsh-mcp-manager | Browser OAuth/PKCE and MCP UI | Community integration; audit, version compatibility and third-party trust required before bundling |

## Recommended deployment architecture
```
ChatGPT / Claude / Mistral / DeepSeek Harness
        |
  OAuth-authenticated remote MCP endpoint
  Cloudflare Agents MCP + Workers OAuth Provider
        |
  Authorized bridge task service
  (user/provider/device IDs, delivery IDs, expiration, audit)
        |
  Cloudflare D1 / Durable Object or equivalent durable queue
        |
  Paired Zalmoxis Android device (outbound HTTPS, encrypted local secret)
        |
  Explicit user approval -> local inference -> reply
```

Keep `bridge/src/index.js` as a non-production spike until replaced. Its current `/mcp` implementation is not guaranteed to satisfy MCP transport or OAuth requirements. A manually issued bearer token to a server URL is not a general replacement for provider OAuth authorization. No automatic access from a consumer ChatGPT or DeepSeek session is implied.

## DeepSeek no-API-key specifics
Official DeepSeek Harness supports a DeepSeek-account model route and an MCP client. This may be connected to the OAuth-protected Zalmoxis MCP endpoint if Harness and client authentication are compatible. A community MCP manager offers OAuth/PKCE if the built-in client lacks it. **Do not extract browser cookies, bypass authentication or embed model API keys.** A Harness host, account authorization, task worker and observed bidirectional round trips are needed before claiming autonomous account-backed DeepSeek responses.

## Smallest next implementation
- [ ] Remove/disable the bespoke `/mcp` production route; keep D1 task service and Android side behind a development feature flag.
- [ ] Bootstrap Cloudflare's authenticated remote MCP Worker template, using official libraries, not copied protocol JSON.
- [ ] Reuse the existing queue schema with migration: scoped device/provider grants, expiring bearer/OAuth tokens, enrollment one-time code with rate limiting, replay detection, retention TTL and durable permission records.
- [ ] Register `bridge_status`, `send_task`, `list_replies`, `list_phone_requests`, `reply_to_phone` through MCP SDK tool registration with typed schemas; never accept arbitrary execution.
- [ ] Add MCP Inspector/conformance tests, OAuth flow + malicious-client tests, Android pairing tests and provider-specific DeepSeek Harness test.
- [ ] Record and verify at least 10 round trips in both directions, including offline/reconnect and revocation; report latency, failures and data handling.

## User experience and operational constraints
No model API key required for MCP connectivity, but provider account sign-in, OAuth client registration, hosted HTTPS relay, and device pairing may still be required. A disconnected phone cannot be reached directly. Outbound mobile polling or push requires explicit opt-in and power/battery controls. Never silently copy private Knowledge content. The existing Android Sharesheet remains the safe offline/keyless fallback.

**Decision quality:** upstream first-party documentation was consulted on 2026-11-23. This is a technical recommendation pending build, integration, security review and account-capability tests.
