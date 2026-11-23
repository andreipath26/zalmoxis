# DeepSeek Harness ↔ Zalmoxis MCP (no model API key)

**Status: integration configuration prepared, not connected or tested.**

## Recommended upstream stack

Use DeepSeek's official [DeepSeek Harness](https://github.com/deepseek-ai/deepseek-harness) and its [MCP Manager](https://github.com/deepseek-ai/deepseek-harness/blob/master/packages/mcp/mcp-manager/README.md). Do not fork the Harness or copy its protocol implementation. The official manager supports Streamable HTTP connections, credential-managed OAuth grants, enable/disable, diagnostics and probe/restart. The upstream client uses the official MCP SDK.

### Configure the MCP server

After deploying the Zalmoxis auth and MCP workers, add an MCP server in **DeepSeek Harness → Settings → MCP** using these values:

- **Name:** `zalmoxis`
- **Transport:** `streamable-http`
- **URL:** `https://YOUR-MCP.WORKERS.DEV/mcp`
- **Authentication:** OAuth via MCP Manager (browser login), not a static token.
- **Tools:** `create_pairing_code`, `bridge_status`, `send_task`, `list_phone_requests`, `list_replies`, `reply_to_phone`.
- **Account model:** select the official DeepSeek-account route offered by your compatible Harness installation; it requires DeepSeek sign-in, **not a DeepSeek API key**. Account limits still apply.

For a YAML-managed profile, use this **illustrative configuration** (adapt to the installed Harness version):

```yaml
- id: mcp
  name: "@deepseek-ai/dsh-mcp-manager"
  inject: [credentials]
  config:
    servers:
      zalmoxis:
        transport: streamable-http
        url: https://YOUR-MCP.WORKERS.DEV/mcp
        enabled: true
```

Start OAuth in the MCP Manager UI. Do not hardcode bearer tokens or import browser cookies. If the currently installed official MCP client does not support OAuth itself, use the official manager's credential authorization owner or evaluate a vetted OAuth-capable manager compatible with your Harness version. Never bypass authentication to make connectivity appear to work.

### Phone pairing

1. After authenticating to the MCP service, instruct DeepSeek Harness to call `create_pairing_code` for provider `deepseek`.
2. Open Zalmoxis Android → Workspace → Agent Bridge, enter the **MCP Worker origin** (without `/mcp`) and the one-time 16-character pairing code.
3. Zalmoxis receives a device-specific 256-bit bearer token, encrypted with Android Keystore; it never receives the DeepSeek account credential.
4. Ask DeepSeek to call `send_task`. Zalmoxis should display the task, let you reject or type a reply, and the Harness can read it via `list_replies`.
5. From Zalmoxis, use Send to agent. The Harness must be running, invoke `list_phone_requests`, and reply via `reply_to_phone`. This **does not** wake a dormant consumer DeepSeek session by itself.

### Acceptance evidence

Record at least 10 real round trips in each direction, signed-in account model availability with **no `DEEPSEEK_API_KEY` variable**, proper OAuth scopes, network interruption, expiry, revoke, malformed messages, and device approval denial. Only then mark the DeepSeek integration deployed.

Do not claim this connects to the browser-based consumer DeepSeek chat automatically. This is a DeepSeek **Harness account-backed agent** integration.
