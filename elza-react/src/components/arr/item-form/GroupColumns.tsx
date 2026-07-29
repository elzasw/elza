import React, { useEffect, useRef, useState } from "react";
import { DataType } from "elza-api";
import { ViewDescItemGroupsLocal } from "./types";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { GRID_BREAKPOINTS, getGridCols } from "./gridBreakpoints";

const TEXTAREA_HEIGHT_MULTIPLIER = 3;
const MIN_COLUMN_WIDTH = 350;

function estimateGroupHeight(item: ViewDescItemGroupsLocal, groupWidth: number, dataTypesMap: Record<number, { code: string }>): number {
    const gridCols = getGridCols(GRID_BREAKPOINTS, groupWidth);
    const height = item.descItemTypes.reduce((sum, t) => {
        const colSpan = t.typeWidth === 0 ? gridCols : Math.min(t.typeWidth || 1, gridCols);
        const dataTypeCode = dataTypesMap[t.typeRef.dataTypeId]?.code;
        const isTextarea = dataTypeCode === DataType.Text || dataTypeCode === DataType.FormattedText;
        const heightMultiplier = isTextarea ? TEXTAREA_HEIGHT_MULTIPLIER : 1;
        const itemRows = Math.ceil(colSpan / gridCols) * Math.max(t.descItems.length, 1) * heightMultiplier;
        return sum + itemRows;
    }, 0);
    return Math.max(height, 1);
}

// Linear partition: split items into k contiguous columns minimising the maximum column sum.
// Uses DP: dp[i][j] = min possible maximum sum when partitioning first i items into j columns.
function partitionOrdered<T>(items: T[], heights: number[], k: number): T[][] {
    const n = items.length;
    if (n === 0) return Array.from({ length: k }, () => []);
    const cols = Math.min(k, n);

    const prefix = [0];
    for (const h of heights) prefix.push(prefix[prefix.length - 1] + h);
    const rangeSum = (i: number, j: number) => prefix[j + 1] - prefix[i];

    const INF = Infinity;
    const dp: number[][] = Array.from({ length: n }, () => new Array(cols).fill(INF));
    const split: number[][] = Array.from({ length: n }, () => new Array(cols).fill(0));

    for (let i = 0; i < n; i++) dp[i][0] = rangeSum(0, i);
    for (let j = 1; j < cols; j++) {
        for (let i = j; i < n; i++) {
            for (let s = j - 1; s < i; s++) {
                const cost = Math.max(dp[s][j - 1], rangeSum(s + 1, i));
                if (cost < dp[i][j]) {
                    dp[i][j] = cost;
                    split[i][j] = s;
                }
            }
        }
    }

    const result: T[][] = [];
    let end = n - 1;
    for (let j = cols - 1; j >= 0; j--) {
        const start = j === 0 ? 0 : split[end][j] + 1;
        result.unshift(items.slice(start, end + 1));
        end = start - 1;
    }

    // pad with empty columns if cols < k
    while (result.length < k) result.push([]);
    return result;
}

interface Props {
    groups: ViewDescItemGroupsLocal[];
    columnCount: number;
    children: (item: ViewDescItemGroupsLocal) => React.ReactNode;
}

export function GroupColumns({ groups, columnCount, children }: Props) {
    const containerRef = useRef<HTMLDivElement>(null);
    const [containerWidth, setContainerWidth] = useState<number>(0);
    const dataTypesMap = useAppSelector(({ refTables }) => refTables.rulDataTypes.itemsMap);

    useEffect(() => {
        const el = containerRef.current;
        if (!el) return;
        const observer = new ResizeObserver(([entry]) => setContainerWidth(entry.contentRect.width));
        observer.observe(el);
        return () => observer.disconnect();
    }, []);

    const maxColumns = containerWidth > 0 ? Math.max(1, Math.floor(containerWidth / MIN_COLUMN_WIDTH)) : 1;
    const effectiveColumnCount = Math.min(columnCount, maxColumns);

    const groupWidth = effectiveColumnCount > 0 ? containerWidth / effectiveColumnCount : containerWidth;

    const heights = groups.map(item => estimateGroupHeight(item, groupWidth, dataTypesMap));
    console.log("#column", heights)
    const columns = partitionOrdered(groups, heights, effectiveColumnCount);

    return (
        <div ref={containerRef} style={{ display: "flex", alignItems: "flex-start" }}>
            {columns.map((columnGroups, columnIndex) => (
                <div key={columnIndex} style={{ flex: 1, minWidth: 0 }}>
                    {columnGroups.map(children)}
                </div>
            ))}
        </div>
    );
}
