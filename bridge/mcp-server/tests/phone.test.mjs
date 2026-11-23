import test from "node:test";
import assert from "node:assert/strict";
import { phoneFetch } from "../src/phone.ts";

const endpoint = "https://mcp.example.test";
const request = (path, method="GET", data, token) => new Request(endpoint + path, {
  method,
  headers: {
    ...(data ? { "Content-Type": "application/json" } : {}),
    ...(token ? { Authorization: "Bearer " + token } : {}),
    "cf-connecting-ip": "203.0.113.5",
  },
  body: data ? JSON.stringify(data) : undefined,
});
function memoryDb() {
  const attempts = [];
  const state = { attempts };
  const DB = {
    prepare(sql) {
      let args = [];
      const builder = {
        bind(...values) { args = values; return builder; },
        async first() {
          if (sql.includes("COUNT(*) AS count")) {
            return { count: attempts.filter(a => a[0] === args[0]).length };
          }
          return null;
        },
        async run() {
          if (sql.startsWith("INSERT INTO pairing_attempts")) attempts.push(args);
          if (sql.startsWith("DELETE FROM pairing_attempts")) attempts.splice(0,
            attempts.length, ...attempts.filter(a => a[1] >= args[0]));
          return { meta: { changes: 1 } };
        },
      };
      return builder;
    },
  };
  return { DB, state };
}

test("Unauthenticated phone operations fail closed", async () => {
  const env = memoryDb();
  for (const path of ["/v1/device/inbox", "/v1/device/outbox", "/v1/device/status"]) {
    const response = await phoneFetch(request(path), env);
    assert.equal(response.status, 401);
    assert.equal(response.headers.get("cache-control"), "no-store");
  }
  const sending = await phoneFetch(request("/v1/device/send", "POST", { message: "Hello" }), env);
  assert.equal(sending.status, 401);
  assert.equal(env.state.attempts.length, 0);
});

test("Invalid pairing codes cannot reach a device session", async () => {
  const env = memoryDb();
  const res = await phoneFetch(request("/v1/pair", "POST", {
    pairing_code: "wrong",
  }), env);
  assert.equal(res.status, 403);
  assert.equal(env.state.attempts.length, 1);
});

test("Pairing attempts are throttled per actor", async () => {
  const env = memoryDb();
  for (let i = 0; i < 10; i++) {
    const res = await phoneFetch(request("/v1/pair", "POST", {
      pairing_code: "FFFFFFFFFFFFFFFF",
    }), env);
    assert.equal(res.status, 403);
  }
  const blocked = await phoneFetch(request("/v1/pair", "POST", {
    pairing_code: "FFFFFFFFFFFFFFFF",
  }), env);
  assert.equal(blocked.status, 429);
});

test("Non-JSON pairing payloads and bad methods are rejected", async () => {
  const env = memoryDb();
  const res = await phoneFetch(new Request(endpoint+"/v1/pair", {
    method: "POST",
    headers: { "Content-Type": "text/plain" },
    body: "secret",
  }), env);
  assert.equal(res.status, 400);
  const invalidMethod = await phoneFetch(request("/v1/device/send"), env);
  assert.equal(invalidMethod.status, 401);
});

test("Unrecognized paths cannot list bridge secrets", async () => {
  const env = memoryDb();
  const res = await phoneFetch(request("/v1/admin/provision"), env);
  assert.equal(res.status, 404);
  assert.equal((await res.text()).includes("token"), false);
});
