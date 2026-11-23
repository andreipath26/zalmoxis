-- Provider-specific MCP OAuth client segregation. A user can pair more than one
-- provider, but no OAuth client may read or write another client's queue.
ALTER TABLE bridges ADD COLUMN client_id TEXT;
CREATE INDEX IF NOT EXISTS idx_bridges_oauth_client
    ON bridges(owner_id, client_id, revoked, created);
