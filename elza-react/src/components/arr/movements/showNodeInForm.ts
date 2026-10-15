import type { AnyAction } from 'redux';
import type { ThunkAction } from 'redux-thunk';
import { fundSelectSubNode } from 'actions/arr/node';
import type { AppState, Fund } from 'typings/store';
import type { TreeNodeVO } from 'typings/Outputs';
import { createFundRoot, getParentNode } from '../ArrUtils';

/** Opens a node of a movements tree in the node form of Pořádání. */
export function showNodeInForm(
    versionId: number,
    node: TreeNodeVO,
    treeNodes: TreeNodeVO[],
    fund: Fund
): ThunkAction<void, AppState, void, AnyAction> {
    return (dispatch) => {
        const parentNode = getParentNode(node, treeNodes) ?? createFundRoot(fund);
        dispatch(fundSelectSubNode(versionId, node.id, parentNode, false, null, true, undefined, undefined, true));
    };
}
