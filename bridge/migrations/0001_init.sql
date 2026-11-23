CREATE TABLE IF NOT EXISTS bridges (
 id TEXT PRIMARY KEY,
 provider TEXT NOT NULL CHECK(provider IN ('chatgpt','claude','mistral','deepseek','perplexity','other')),
 pair_hash TEXT NOT NULL,
 pair_expires INTEGER NOT NULL,
 device_token_hash TEXT,
 provider_token_hash TEXT NOT NULL,
 created INTEGER NOT NULL,
 revoked INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_bridge_pair ON bridges(pair_hash);
CREATE TABLE IF NOT EXISTS messages (
 id TEXT PRIMARY KEY,
 bridge_id TEXT NOT NULL REFERENCES bridges(id),
 direction TEXT NOT NULL CHECK(direction IN ('provider_to_phone','phone_to_provider')),
 body TEXT NOT NULL,
 created INTEGER NOT NULL,
 expires INTEGER NOT NULL,
 status TEXT NOT NULL CHECK(status IN ('queued','accepted','rejected','replied','delivered')),
 reply TEXT,
 FOREIGN KEY(bridge_id) REFERENCES bridges(id)
);
CREATE INDEX IF NOT EXISTS idx_bridge_messages ON messages(bridge_id, direction, created);
