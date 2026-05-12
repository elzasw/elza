import {fundsSelectFund} from "../actions/fund/fund";
import {createFundRoot} from "../components/arr/ArrUtils";
import {fundSelectSubNode} from "../actions/arr/node";
import {Api} from "../api";

export {fetchNodeInfo} from "./fetchNodeInfo";

/**
 * Drive node selection after a node identifier has been resolved to NodeInfo.
 *
 * The caller is expected to have loaded the fund detail (e.g. via
 * ArrParentPage.resolveUrlsRaw) so that arrRegion.funds[active] is populated
 * with the right version. This function only handles selecting the node
 * inside that fund — it fetches the parent chain via getNodeData, picks the
 * immediate parent, and dispatches fundSelectSubNode.
 */
export const processNodeNavigation = (nodeInfo) =>
    (dispatch, getState) => {
        dispatch(fundsSelectFund(nodeInfo.fundId));

        waitForLoadAS(() => {
            const { arrRegion } = getState();
            const selectFund = arrRegion.funds[arrRegion.activeIndex];

            if (!selectFund?.fundTree?.fetched) {
                return true;
            }

            const targetVersionId = selectFund.versionId;

            void (async () => {
                try {
                    const { data: nodeData } = await Api.node.nodeGetNodeData({
                        fundVersionId: targetVersionId,
                        nodeId: nodeInfo.id,
                        formData: false,
                        parents: true,
                        children: false,
                        siblingsMaxCount: 0,
                        nodeStatus: false,
                    });
                    const parents = nodeData.parents ?? [];
                    const parentNode = parents.length > 0
                        ? parents[parents.length - 1]
                        : createFundRoot(selectFund);

                    dispatch(fundSelectSubNode(
                        targetVersionId, nodeInfo.id, parentNode,
                        false, null, false, undefined, undefined, true,
                    ));
                } catch (e) {
                    console.error("Failed to navigate to node", nodeInfo, e);
                }
            })();
            return false;
        });
    };

export const waitForLoadAS = fce => {
    const next = fce();
    if (next) {
        setTimeout(() => {
            waitForLoadAS(fce);
        }, 50);
    }
};
