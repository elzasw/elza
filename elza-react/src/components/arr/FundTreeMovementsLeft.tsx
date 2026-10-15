/**
 * Strom AS.
 */

import { useEffect } from 'react';
import type { MouseEvent } from 'react';
import FundTreeLazy from './FundTreeLazy';
import { FUND_TREE_AREA_MOVEMENTS_LEFT } from 'actions/constants/ActionTypes';
import {
    fundTreeCollapse,
    fundTreeFetchIfNeeded,
    fundTreeFocusNode,
    fundTreeFulltextChange,
    fundTreeFulltextNextItem,
    fundTreeFulltextPrevItem,
    fundTreeFulltextSearch,
    fundTreeNodeCollapse,
    fundTreeNodeExpand,
    fundTreeSelectNode,
} from 'actions/arr/fundTree';
import { contextMenuShow } from 'actions/global/contextMenu';
import type { Fund } from 'typings/store';
import type { TreeNodeVO } from 'typings/Outputs';
import { useAppThunkDispatch } from 'utils/hooks';

// Besides fund and versionId, the props are the tree store slice, forwarded to FundTreeLazy as a whole;
// only the fields read here are typed.
interface Props {
    fund: Fund;
    versionId: number;
    expandedIds: Record<number, boolean>;
    selectedId?: number | null;
    selectedIds: Record<number, boolean>;
    nodes: TreeNodeVO[];
    fetched: boolean;
    isFetching: boolean;
    dirty: boolean;
    fetchingIncludeIds: Record<number, boolean>;
    multipleSelection: boolean;
}

export type FundTreeMovementsLeftProps = Props;

function unFocus() {
    window.getSelection()?.removeAllRanges();
}

export function FundTreeMovementsLeft(props: Props) {
    const {
        fund,
        versionId,
        expandedIds,
        selectedId,
        selectedIds,
        nodes,
        fetched,
        isFetching,
        dirty,
        fetchingIncludeIds,
        multipleSelection,
    } = props;
    const dispatch = useAppThunkDispatch();

    // fundTreeFetchIfNeeded decides from these tree store values whether to load the tree,
    // so the check runs again whenever one of them changes.
    useEffect(() => {
        dispatch(fundTreeFetchIfNeeded(FUND_TREE_AREA_MOVEMENTS_LEFT, versionId, expandedIds, undefined));
    }, [
        dispatch,
        versionId,
        expandedIds,
        selectedId,
        selectedIds,
        nodes,
        fetched,
        isFetching,
        dirty,
        fetchingIncludeIds,
        multipleSelection,
    ]);

    function handleFulltextChange(value: string) {
        dispatch(fundTreeFulltextChange(FUND_TREE_AREA_MOVEMENTS_LEFT, versionId, value));
    }

    function handleFulltextSearch() {
        dispatch(fundTreeFulltextSearch(FUND_TREE_AREA_MOVEMENTS_LEFT, versionId));
    }

    function handleFulltextPrevItem() {
        dispatch(fundTreeFulltextPrevItem(FUND_TREE_AREA_MOVEMENTS_LEFT, versionId));
    }

    function handleFulltextNextItem() {
        dispatch(fundTreeFulltextNextItem(FUND_TREE_AREA_MOVEMENTS_LEFT, versionId));
    }

    function handleOpenCloseNode(node: TreeNodeVO, expand: boolean) {
        if (expand) {
            dispatch(fundTreeNodeExpand(FUND_TREE_AREA_MOVEMENTS_LEFT, node));
        } else {
            dispatch(fundTreeNodeCollapse(FUND_TREE_AREA_MOVEMENTS_LEFT, versionId, node));
        }
    }

    /**
     * Zobrazení kontextového menu pro daný uzel.
     */
    function handleContextMenu(node: TreeNodeVO, e: MouseEvent) {
        e.preventDefault();
        e.stopPropagation();

        const menu = <ul className="dropdown-menu"></ul>;

        dispatch(fundTreeFocusNode(FUND_TREE_AREA_MOVEMENTS_LEFT, versionId, node));
        dispatch(contextMenuShow(FundTreeMovementsLeft, menu, { x: e.clientX, y: e.clientY }));
    }

    /**
     * Klik na uzel.
     */
    function handleNodeClick(node: TreeNodeVO, ensureItemVisible: boolean, e: MouseEvent) {
        if (e.shiftKey) {
            unFocus();
        }
        dispatch(
            fundTreeSelectNode(
                FUND_TREE_AREA_MOVEMENTS_LEFT,
                versionId,
                node.id,
                e.ctrlKey,
                e.shiftKey,
                null,
                ensureItemVisible
            )
        );
    }

    /**
     * Zabalení stromu
     */
    function handleCollapse() {
        dispatch(fundTreeCollapse(FUND_TREE_AREA_MOVEMENTS_LEFT, versionId, fund));
    }

    return (
        <FundTreeLazy
            {...props}
            onOpenCloseNode={handleOpenCloseNode}
            onContextMenu={handleContextMenu}
            onNodeClick={handleNodeClick}
            onCollapse={handleCollapse}
            onFulltextChange={handleFulltextChange}
            onFulltextSearch={handleFulltextSearch}
            onFulltextPrevItem={handleFulltextPrevItem}
            onFulltextNextItem={handleFulltextNextItem}
        />
    );
}
