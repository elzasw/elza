interface GridBreakpoint {
    minWidth: number;
    cols: number;
}

export const GRID_BREAKPOINTS: GridBreakpoint[] = [
    { minWidth: 0,    cols: 1 },
    { minWidth: 500,  cols: 2 },
    { minWidth: 900,  cols: 4 },
    { minWidth: 1400, cols: 6 },
];

export function getGridCols(breakpoints: GridBreakpoint[], groupWidth: number): number {
    return [...breakpoints].reverse().find(b => groupWidth > b.minWidth)?.cols ?? 1;
}

export function gridBreakpointsToStyles(breakpoints: GridBreakpoint[]): Record<string, { gridTemplateColumns: string }> {
    return Object.fromEntries(
        breakpoints
            .filter(b => b.minWidth > 0)
            .map(b => [
                `@container group-container (width > ${b.minWidth}px)`,
                { gridTemplateColumns: `repeat(${b.cols} ,1fr)` },
            ])
    );
}
