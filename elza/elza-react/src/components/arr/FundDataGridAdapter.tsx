import { useCallback, useEffect, useRef, useState } from 'react';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import { useAppThunkDispatch } from 'utils/hooks/useThunkDispatch';
import { objectById } from 'stores/app/utils';
import {
    fundDataFulltextNextItem,
    fundDataFulltextPrevItem,
    fundDataGridResolveNodePage,
} from 'actions/arr/fundDataGrid';
import { DataGridViewProvider, useDataGridView } from 'contexts/dataGridView';
import { FundDataGridConnected } from './FundDataGrid';

interface Props {
    versionId: number;
    [key: string]: unknown;
}

export type FundDataGridProps = Props;

export function FundDataGrid(props: Props) {
    return (
        <DataGridViewProvider versionId={props.versionId}>
            <FundDataGridAdapter {...props} />
        </DataGridViewProvider>
    );
}

interface GridSlice {
    fetchedFilter?: boolean;
    itemsCount?: number;
    items?: Array<{ id: string | number; node?: { id: number } }>;
    searchedItems?: Array<{ index: number }>;
    searchedCurrentIndex?: number;
}

function useFundDataGridState(versionId: number): GridSlice | null {
    return useAppSelector(state => {
        const fund = objectById(state.arrRegion.funds, versionId, 'versionId') as { fundDataGrid?: GridSlice } | null;
        return fund?.fundDataGrid ?? null;
    });
}

function FundDataGridAdapter(props: Props) {
    const { versionId } = props;
    const dispatch = useAppThunkDispatch();
    const {
        view,
        setPageIndex,
        goToPage,
        setPageSize,
        setSelectedIds,
        setSelectedRowIndexes,
        setCellFocus,
        rememberRestoreNode,
        requestRestore,
        resolveRestoreColumn,
        restorePending,
        markRestoreDone,
    } = useDataGridView();

    const grid = useFundDataGridState(versionId);
    const fetchedFilter = grid?.fetchedFilter ?? false;
    const itemsCount = grid?.itemsCount ?? 0;
    const items = grid?.items ?? [];
    const searchedItems = grid?.searchedItems ?? [];
    const searchedCurrentIndex = grid?.searchedCurrentIndex ?? 0;

    // Restore the page from the target node once the filter is ready. Runs on the initial reload restore
    // and again whenever a new restore is requested (the "open in datagrid" deep link). The node whose
    // row still needs focusing is held in state so the focus effect re-runs even when the resolved page
    // equals the current page and the rows never change.
    const resolvingRef = useRef(false);
    const [focusNodeId, setFocusNodeId] = useState<number | null>(null);
    useEffect(() => {
        if (!restorePending) {
            resolvingRef.current = false;
            return;
        }
        if (resolvingRef.current || !fetchedFilter) {
            return;
        }
        resolvingRef.current = true;

        const nodeId = view.restoreNodeId;
        if (nodeId == null) {
            markRestoreDone();
            return;
        }
        (async () => {
            const page = await dispatch(fundDataGridResolveNodePage(versionId, nodeId, view.pageSize));
            if (page != null) {
                setFocusNodeId(nodeId);
                goToPage(page);
            } else {
                // Node not found - keep the saved page unchanged and finish the restore.
                markRestoreDone();
            }
        })();
    }, [restorePending, fetchedFilter, versionId, view.restoreNodeId, view.pageSize, dispatch, goToPage, markRestoreDone]);

    // Once the restored page's rows are present, focus the node's row and its saved column.
    useEffect(() => {
        if (focusNodeId == null) {
            return;
        }
        const row = items.findIndex(item => item.node?.id === focusNodeId);
        if (row < 0) {
            return;
        }
        setFocusNodeId(null);
        setSelectedRowIndexes([row]);
        setCellFocus({ row, col: view.cellFocus.col });
        markRestoreDone();
    }, [focusNodeId, items, view.cellFocus.col, setSelectedRowIndexes, setCellFocus, markRestoreDone]);

    // When filtering shrinks the record count, clamp the page to the last valid one.
    useEffect(() => {
        if (restorePending || !fetchedFilter) {
            return;
        }
        const lastPage = itemsCount === 0 ? 0 : Math.floor((itemsCount - 1) / view.pageSize);
        if (view.pageIndex > lastPage) {
            goToPage(lastPage);
        }
    }, [itemsCount, view.pageSize, view.pageIndex, restorePending, fetchedFilter, goToPage]);

    // Clamp the focused row when the current page has fewer rows than the focus points at (e.g. after a
    // filter shrinks the last page). Skipped during restore, which sets its own focus.
    useEffect(() => {
        if (restorePending || items.length === 0) {
            return;
        }
        if (view.cellFocus.row > items.length - 1) {
            setCellFocus({ row: items.length - 1, col: view.cellFocus.col });
        }
    }, [items.length, view.cellFocus.row, view.cellFocus.col, restorePending, setCellFocus]);

    // Selection is page-scoped: drop checked ids not present in the current page's rows (e.g. after a
    // server-side delete or filter). selectedIds keys are strings; item ids are numbers - compare as strings.
    useEffect(() => {
        if (view.selectedIds.length === 0) {
            return;
        }
        const presentIds = new Set(items.map(item => String(item.id)));
        const pruned = view.selectedIds.filter(id => presentIds.has(String(id)));
        if (pruned.length !== view.selectedIds.length) {
            setSelectedIds(pruned);
        }
    }, [items, view.selectedIds, setSelectedIds]);


    // Jump to a search hit - computes the page and row from the hit's absolute position.
    const gotoSearchHit = useCallback(
        (newIndex: number) => {
            const hit = searchedItems[newIndex];
            if (!hit) {
                return;
            }
            const page = Math.floor(hit.index / view.pageSize);
            const row = hit.index - page * view.pageSize;
            goToPage(page);
            setSelectedRowIndexes([row]);
            setCellFocus({ row, col: view.cellFocus.col });
        },
        [searchedItems, view.pageSize, view.cellFocus.col, goToPage, setSelectedRowIndexes, setCellFocus],
    );

    // A fresh search result jumps to the first hit (searchedCurrentIndex is reset to 0 by the reducer).
    const lastSearchedItemsRef = useRef(searchedItems);
    useEffect(() => {
        if (searchedItems !== lastSearchedItemsRef.current) {
            lastSearchedItemsRef.current = searchedItems;
            if (searchedItems.length > 0) {
                gotoSearchHit(0);
            }
        }
    }, [searchedItems, gotoSearchHit]);

    const onFulltextNextItem = useCallback(() => {
        if (searchedItems.length === 0) {
            return;
        }
        const newIndex = searchedCurrentIndex + 1;
        gotoSearchHit(newIndex);
        dispatch(fundDataFulltextNextItem(versionId));
    }, [searchedItems.length, searchedCurrentIndex, gotoSearchHit, dispatch, versionId]);

    const onFulltextPrevItem = useCallback(() => {
        if (searchedItems.length === 0 || searchedCurrentIndex <= 0) {
            return;
        }
        const newIndex = searchedCurrentIndex - 1;
        gotoSearchHit(newIndex);
        dispatch(fundDataFulltextPrevItem(versionId));
    }, [searchedItems.length, searchedCurrentIndex, gotoSearchHit, dispatch, versionId]);

    const onSetCellFocus = useCallback((row: number, col: number) => setCellFocus({ row, col }), [setCellFocus]);

    return (
        <FundDataGridConnected
            {...props}
            view={view}
            onSetPageIndex={setPageIndex}
            onSetPageSize={setPageSize}
            onSetSelectedIds={setSelectedIds}
            onSetRowIndexes={setSelectedRowIndexes}
            onSetCellFocus={onSetCellFocus}
            onFulltextNextItem={onFulltextNextItem}
            onFulltextPrevItem={onFulltextPrevItem}
            requestRestore={requestRestore}
            rememberRestoreNode={rememberRestoreNode}
            resolveRestoreColumn={resolveRestoreColumn}
            restoreDescItemTypeId={view.restoreDescItemTypeId}
        />
    );
}
