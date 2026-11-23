-- OAuth principal owns bridge; phone bearer is independent from MCP OAuth grant.
ALTER TABLE bridges ADD COLUMN owner_id TEXT;
CREATE INDEX IF NOT EXISTS idx_bridges_owner ON bridges(owner_id, revoked, created);
CREATE TABLE IF NOT EXISTS pairing_attempts (
    actor TEXT NOT NULL,
    attempted_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_pairing_attempts ON pairing_attempts(actor, attempted_at);
