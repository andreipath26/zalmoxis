#!/usr/bin/env node
/**
 * Schema and immutable-baseline guard for the Zalmoxis progress registry.
 * Reads local repository files only. No packages or network access required.
 */
import { readFileSync } from "node:fs";
import assert from "node:assert/strict";

const read = path => JSON.parse(readFileSync(new URL("../" + path, import.meta.url), "utf8"));
const ledger = read("docs/progress/roadmap-ledger.json");
const lock = read("docs/progress/baseline-lock.json");

assert.equal(ledger.schemaVersion, 1);
assert.equal(lock.schemaVersion, 1);
assert.equal(ledger.baselineId, lock.baselineId);
assert.equal(ledger.baselineDate, lock.baselineDate);
assert.equal(ledger.methodology.totalImplementationBaselinePercent, lock.primaryImplementationPercent);
assert.equal(ledger.methodology.totalReleaseReadinessBaselinePercent, lock.primaryReleaseReadinessPercent);

const percent = (value, label) => {
  assert.ok(Number.isInteger(value) && value >= 0 && value <= 100,
    label + " must be an integer between 0 and 100");
};
const baseline = new Map();
for (const group of lock.groups) {
  assert.ok(!baseline.has(group.id), "Duplicate locked group: " + group.id);
  if (group.baselinePercent !== null) percent(group.baselinePercent, group.id);
  const entries = new Map();
  for (const item of group.items) {
    assert.ok(!entries.has(item.id), "Duplicate locked item: " + item.id);
    percent(item.baselinePercent, item.id);
    entries.set(item.id, item.baselinePercent);
  }
  baseline.set(group.id, { percentage: group.baselinePercent, entries });
}
const seen = new Set();
let itemCount = 0;
for (const group of ledger.sections) {
  assert.ok(!seen.has(group.id), "Duplicate ledger group: " + group.id);
  seen.add(group.id);
  const fixed = baseline.get(group.id);
  assert.ok(fixed, "Unknown group (add a baseline before new groups): " + group.id);
  assert.equal(group.estimatedPercent, fixed.percentage, "Original group percentage changed: " + group.id);
  if (group.currentPercent !== undefined && group.currentPercent !== null)
    percent(group.currentPercent, group.id + ".currentPercent");
  const localSeen = new Set();
  for (const item of group.items) {
    assert.ok(!localSeen.has(item.id), "Duplicate item: " + item.id);
    localSeen.add(item.id);
    assert.equal(item.baselinePercent, fixed.entries.get(item.id),
      "Changed baseline/unknown item: " + item.id);
    percent(item.currentPercent, item.id + ".currentPercent");
    assert.equal(typeof item.name, "string");
    assert.ok(item.name.length > 0, "Missing item label: " + item.id);
    assert.ok(Array.isArray(item.evidence), "Missing evidence array: " + item.id);
    assert.ok(Array.isArray(item.blockers), "Missing blocker array: " + item.id);
    if (item.currentPercent !== item.baselinePercent) {
      assert.ok(item.evidence.length > 0, "Changed item has no evidence: " + item.id);
      assert.ok(item.lastReviewed >= ledger.baselineDate, "Invalid update date: " + item.id);
    }
    itemCount++;
  }
  assert.equal(localSeen.size, fixed.entries.size, "Incomplete baseline group: " + group.id);
}
assert.equal(seen.size, baseline.size, "Missing baseline group");
console.log(`Roadmap ledger valid: ${seen.size} groups, ${itemCount} requirements, baseline unchanged.`);
