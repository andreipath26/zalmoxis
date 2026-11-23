# Zalmoxis Bridge — official MCP, OAuth and Android pairing

**Source implementation exists; not deployed or device-validated.**

The project replaces the obsolete custom `bridge/src/index.js` protocol with:
- `auth-server/`: Cloudflare Workers OAuth Provider v1, with Access JWT validation and consent. No model API key.
- `mcp-server/`: official MCP TypeScript SDK v2 + Cloudflare Agents Streamable HTTP; OAuth resource-server middleware; D1 queues and limited Android pairing.
- `migrations/`: message and pairing storage.
- `deepseek/`: DeepSeek Harness account-authenticated MCP instructions.
- Android `AgentBridgeClient.kt` and `AgentBridgeWorkspace.kt`: user-controlled pairing, Keystore-encrypted device credential, inbox, reply/reject, outgoing messages, revocation and manual fallback.

## Security and architecture

```
ChatGPT / Claude / DeepSeek Harness / Mistral (provider initiates tools)
       | OAuth 2.1, MFA/email sign-in and user consent
       v
[Cloudflare Access] -> [OAuth authorization Worker] <--- private Service Binding
                                  |
                        verified OAuth bearer
                                  v
[Official MCP resource Worker /mcp] ---- Cloudflare D1 task queue
                                  ^
                           device HTTPS bearer
                                  |
                      [Zalmoxis Android phone]
```

**Only** the MCP Worker is used for phone pairing, status and message transport. The older bespoke `bridge/src/index.js` is disabled by default and **must not be deployed**.

The public OAuth login handler requires an Access JWT whose signature, issuer and audience pass validation; its page requires an explicit consent POST. Restrict the Cloudflare Access application to the account(s) allowed to pair Zalmoxis, and protect the **/authorize path**. Do **not** protect the OAuth token endpoint, discovery or dynamic registration endpoints behind Access as that breaks standards-based clients; they are protected by OAuth protocol checks.

MCP read scope permits status and reading the account's bridge messages; `mcp:write` is required to create pairing codes, send tasks or reply. A connected AI provider never receives phone/device credentials, shell, filesystem, contacts, Accessibility, microphone or Knowledge write access. Incoming agent tasks are **untrusted**, displayed for explicit reply/reject. Phone messages are encrypted in transit via HTTPS, but **D1 stores message bodies in plaintext**; do not send sensitive information before adding end-to-end encryption and retention review. Metadata and reply history expire after 24 hours. Scheduled cleanup runs daily. No background phone service is enabled: inbox polling runs only while Agent Bridge UI is open (15 seconds).

## Deploy entirely from an Android phone using GitHub Actions

The repository includes [Deploy secure agent bridge](../.github/workflows/bridge-deploy.yml), a **manual** workflow. No local terminal or laptop is required once the Cloudflare account is prepared. The deployment job checks the Workers, applies the D1 schema as needed, and deploys OAuth followed by MCP. It never deploys from an ordinary source push.

From your phone's GitHub web interface:

1. In your Cloudflare account create one KV namespace, one D1 database, and a Cloudflare Access application restricted to your own account for the OAuth **/authorize** route. Keep Cloudflare Access enabled; a valid JWT audience is required.
2. In GitHub repository **Settings → Secrets and variables → Actions**, create **secrets** `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID`. They are **Cloudflare infrastructure credentials, not AI model API keys**. Limit the token to the permissions needed for Workers, KV and D1.
3. Add **variables** `ZALMOXIS_AUTH_URL`, `ZALMOXIS_MCP_URL`, `ZALMOXIS_ACCESS_TEAM_DOMAIN`, `ZALMOXIS_ACCESS_AUD`, `ZALMOXIS_OAUTH_KV_ID`, `ZALMOXIS_BRIDGE_D1_ID`. OAuth/MCP URLs must use your real `workers.dev` hostname, and MCP URL must end in `/mcp`.
4. Optionally protect the GitHub Actions environment `zalmoxis-bridge` with required approval.
5. Open **Actions → Deploy secure agent bridge → Run workflow → DEPLOY**. If any configuration value is missing, the workflow fails closed instead of publishing an unauthenticated server.
6. After a successful run, verify OAuth 401 challenge, Cloudflare Access sign-in, MCP consent, and single-use phone pairing. Only connect DeepSeek or ChatGPT when those checks pass.

The cloud identity configuration and initial KV/D1 resource creation are external account actions not performed by the source-code commits. The GitHub workflow itself has not yet been executed or verified.

## Deployment prerequisites

One-time Cloudflare setup is required, using your own Cloudflare account (free tiers may suffice):
- Cloudflare account and a Workers deployment-capable login.
- Cloudflare Access application with an allowed user, email PIN or supported identity provider.
- The Access application audience `ACCESS_AUD`, team domain `ACCESS_TEAM_DOMAIN`, and **Access JWT assertion** forwarded to the authorization Worker on its /authorize route.
- One KV namespace for OAuth state and one D1 database for bridge state.
- Your authenticated account must be permitted to deploy Workers/Access. This is infrastructure authentication, **not an OpenAI, Claude or DeepSeek model API key**.

### 1. Create storage

On a trusted development environment with Node.js 22+, `npm`, `wrangler`:

```bash
npx wrangler login
npx wrangler kv namespace create OAUTH_KV
npx wrangler d1 create zalmoxis-bridge
```

Write the returned KV ID into `bridge/auth-server/wrangler.jsonc` and D1 ID into `bridge/mcp-server/wrangler.jsonc`. Copy the checked-in `wrangler.example.jsonc` files to `wrangler.jsonc`; do not publish live account IDs or credentials. Configure the exact HTTPS auth/MCP URLs (MCP URL **ends in /mcp**) and the Access team domain/audience. Create a Cloudflare Access policy protecting the authorization route for your account and verify the Access JWT is available to the Worker. Authorization fails closed without a valid Access JWT.

### 2. Apply all database migrations

```bash
npx wrangler d1 migrations apply zalmoxis-bridge --remote --config bridge/mcp-server/wrangler.jsonc
```

Cloudflare Wrangler migrations require the configured `migrations_dir` to point to `../migrations`; set this explicitly in the MCP Worker configuration, or use the equivalent `wrangler d1 execute` command for each migration in numeric order:

```bash
npx wrangler d1 execute zalmoxis-bridge --remote --file=bridge/migrations/0001_init.sql --config bridge/mcp-server/wrangler.jsonc
npx wrangler d1 execute zalmoxis-bridge --remote --file=bridge/migrations/0002_oauth_pairing.sql --config bridge/mcp-server/wrangler.jsonc
npx wrangler d1 execute zalmoxis-bridge --remote --file=bridge/migrations/0003_client_isolation.sql --config bridge/mcp-server/wrangler.jsonc
```

Apply these once in order. Do not re-run the non-idempotent `ALTER TABLE` migration against an already-migrated database.

### 3. Deploy OAuth authorization Worker first

```bash
cd bridge/auth-server
npm install
npm run typecheck
npx wrangler deploy --config wrangler.jsonc
```

Confirm the OAuth issuer and exact resource URL. OAuth tokens issued to a *different* resource are invalid for this bridge.

### 4. Deploy MCP Worker and verify protection

```bash
cd bridge/mcp-server
npm install
npm run typecheck
npx wrangler deploy --config wrangler.jsonc
```

Before any phone pairing, an unauthenticated MCP request to `/mcp` must receive a standards-compliant **401 OAuth challenge**, never a tools list. `/health` is a non-sensitive readiness endpoint.

### 5. Connect and pair an agent

Use a supported provider's custom MCP connection flow to connect to `https://YOUR-MCP.WORKERS.DEV/mcp`. Approve OAuth in the browser. After consent, call `create_pairing_code(provider)` and enter the one-time code in **Zalmoxis → Workspace → Agent Bridge** along with the MCP Worker origin. The code is valid for 15 minutes, single-use and throttled by IP at 10 attempts per 15 minutes. The phone stores the issued token using Android Keystore AES-GCM.

Each OAuth client ID has its own bridge channel and cannot read another client’s queue. Provider-initiated `send_task` queues a message. The phone may accept it, type a reply or reject. Phone-originated `send` queues a message that a running agent can retrieve with `list_phone_requests` and respond to using `reply_to_phone`. An idle consumer AI chat is not automatically awakened by MCP.

### Provider-specific setup

- **DeepSeek (mandatory):** [DeepSeek Harness guide](deepseek/README.md), official account-sign-in model route plus MCP Manager OAuth.
- **ChatGPT:** connect only if the user's ChatGPT plan/workspace supports custom MCP tools; existing conversations are not remotely readable.
- **Claude/Mistral:** connect a custom remote MCP server where account feature access permits OAuth.
- **No model API keys:** MCP pairing uses account OAuth and the phone's bridge credential. Some account/hosting prerequisites remain mandatory.

## Release acceptance gates

- [ ] Both Workers type-check and bundle via GitHub Actions Bridge SDK checks.
- [ ] OAuth sign-in requires real Access identity and explicit user consent; token discovery and PKCE pass MCP Inspector.
- [ ] Scope isolation, owner isolation, TLS/Origin protection and negative authorization tests pass.
- [ ] D1 migrations apply and revoke/offline/TTL cleanup pass.
- [ ] Android APK compiles and installs on the user's device.
- [ ] DeepSeek Harness uses account-backed inference with **no model API key**, and handles 10+ real round trips in each direction.
- [ ] Other connected providers separately meet the same acceptance gates.
- [ ] Private data/logging, relay rate limits and retention are reviewed; add end-to-end encryption before sensitive tasks.

The current version is a **development implementation**. It is not production-ready and cannot be claimed connected until those deployment and runtime tests pass.
