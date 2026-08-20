#!/usr/bin/env node
/**
 * Type-check with stricter settings than the build uses, and fail only on errors that
 * are not already recorded in the baseline.
 *
 * This is an opt-out ratchet rather than an opt-in list: every file is checked, and the
 * known offenders are enumerated in ts-strict-baseline.json. New code is therefore
 * covered automatically - a file added tomorrow has no baseline entries, so any strict
 * error in it fails. Existing debt stays parked until someone fixes it.
 *
 * The baseline may only shrink. `--update` rewrites it; the check refuses to record more
 * errors than it already holds, so the ratchet cannot be loosened by accident.
 *
 * Usage:
 *   node scripts/ts-strict-check.ts             verify against the baseline
 *   node scripts/ts-strict-check.ts --update    refresh the baseline after fixing errors
 */
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { compare, countBaseline, parseErrors, toBaseline, type Baseline } from "./ts-strict-core.ts";

const BASELINE_PATH = "ts-strict-baseline.json";
const CONFIG = "tsconfig.strict.json";

const update = process.argv.includes("--update");

const TSC = "node_modules/typescript/bin/tsc";

/**
 * Runs tsc through node directly. Invoking the `npx` wrapper would need a shell on
 * Windows, and a shell that fails to start looks exactly like a clean run - which would
 * make this check pass silently whenever the toolchain breaks.
 */
function runTsc(): string {
    if (!existsSync(TSC)) {
        console.error(`✗ ${TSC} not found - run npm install first.`);
        process.exit(1);
    }
    try {
        execFileSync(process.execPath, [TSC, "--noEmit", "-p", CONFIG], { encoding: "utf8" });
        return "";
    } catch (e: any) {
        // tsc exits non-zero when it reports diagnostics; the output is what we want.
        // A failure to spawn has no exit status at all - that is a broken toolchain, not a clean run.
        if (typeof e.status !== "number") {
            console.error(`✗ could not run tsc: ${e.message}`);
            process.exit(1);
        }
        return `${e.stdout ?? ""}${e.stderr ?? ""}`;
    }
}

const output = runTsc();
const errors = parseErrors(output);

// tsc reporting a non-zero exit but nothing we could parse means the output format changed
// or the run failed for another reason; treating that as "no errors" would be unsafe.
if (errors.length === 0 && output.trim() !== "") {
    console.error("✗ tsc produced output that contained no recognisable diagnostics:\n");
    console.error(output.trim().split(/\r?\n/).slice(0, 10).join("\n"));
    process.exit(1);
}

if (update) {
    const next = toBaseline(errors);
    const previous: Baseline = existsSync(BASELINE_PATH)
        ? JSON.parse(readFileSync(BASELINE_PATH, "utf8"))
        : {};
    const before = countBaseline(previous);
    const after = countBaseline(next);
    if (existsSync(BASELINE_PATH) && after > before) {
        console.error(`✗ refusing to grow the baseline: ${before} -> ${after}.`);
        console.error("  Fix the new errors instead, or remove the offending file from tsconfig.strict.json.");
        process.exit(1);
    }
    writeFileSync(BASELINE_PATH, JSON.stringify(next, null, 2) + "\n", "utf8");
    console.log(`✓ baseline updated: ${before} -> ${after} known errors in ${Object.keys(next).length} files.`);
    process.exit(0);
}

if (!existsSync(BASELINE_PATH)) {
    console.error(`✗ ${BASELINE_PATH} is missing. Create it with: npm run ts:strict-baseline`);
    process.exit(1);
}

const baseline: Baseline = JSON.parse(readFileSync(BASELINE_PATH, "utf8"));
const { added, fixed, total } = compare(errors, baseline);

if (added.length > 0) {
    console.error(`✗ ${added.length} strict type error(s) not in the baseline:\n`);
    for (const error of added) {
        console.error(`  ${error.file}\n    ${error.signature}`);
    }
    console.error("\nFix them, or - if the code is intentionally left loose - record them with:");
    console.error("  npm run ts:strict-baseline");
    process.exit(1);
}

if (fixed > 0) {
    console.log(`✓ no new strict errors. ${fixed} baseline entr${fixed === 1 ? "y is" : "ies are"} now fixed —`);
    console.log("  shrink the baseline with: npm run ts:strict-baseline");
} else {
    console.log(`✓ no new strict errors (${total} known, all in the baseline).`);
}
