/**
 * Baseline handling for the stricter TypeScript check.
 *
 * Errors are identified by file + code + message, deliberately without the line and
 * column. Editing the top of a file shifts every line below it; keying on position
 * would turn unrelated edits into "new" errors and the ratchet would be useless.
 */

export type Baseline = Record<string, string[]>;

export type TsError = {
    file: string;
    signature: string;
};

const ERROR_LINE = /^(?<file>[^(]+)\((?<line>\d+),(?<col>\d+)\): (?<code>error TS\d+): (?<message>.*)$/;

/**
 * Parses `tsc --noEmit` output. Lines that are not diagnostics (progress, blank) are ignored.
 */
export function parseErrors(output: string): TsError[] {
    const errors: TsError[] = [];
    for (const line of output.split(/\r?\n/)) {
        const match = ERROR_LINE.exec(line.trim());
        if (!match?.groups) {
            continue;
        }
        const file = match.groups.file.replace(/\\/g, "/");
        errors.push({ file, signature: `${match.groups.code}: ${match.groups.message}` });
    }
    return errors;
}

export function toBaseline(errors: TsError[]): Baseline {
    const baseline: Baseline = {};
    for (const { file, signature } of errors) {
        (baseline[file] ??= []).push(signature);
    }
    for (const file of Object.keys(baseline)) {
        baseline[file].sort();
    }
    return Object.fromEntries(Object.keys(baseline).sort().map(f => [f, baseline[f]]));
}

export type Comparison = {
    /** Errors present now but not in the baseline - these fail the build. */
    added: TsError[];
    /** Baseline entries no longer reported - the baseline can be shrunk. */
    fixed: number;
    /** Total errors currently reported. */
    total: number;
};

/**
 * Compares as multisets, so a second identical error in the same file is still caught.
 */
export function compare(errors: TsError[], baseline: Baseline): Comparison {
    const remaining = new Map<string, string[]>();
    for (const [file, signatures] of Object.entries(baseline)) {
        remaining.set(file, [...signatures]);
    }

    const added: TsError[] = [];
    for (const error of errors) {
        const pool = remaining.get(error.file);
        const at = pool?.indexOf(error.signature) ?? -1;
        if (pool && at >= 0) {
            pool.splice(at, 1);
        } else {
            added.push(error);
        }
    }

    let fixed = 0;
    for (const pool of remaining.values()) {
        fixed += pool.length;
    }

    return { added, fixed, total: errors.length };
}

export function countBaseline(baseline: Baseline): number {
    return Object.values(baseline).reduce((sum, signatures) => sum + signatures.length, 0);
}
