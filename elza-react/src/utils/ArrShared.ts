import type {NodeInfo} from "elza-api";
import {fundsSelectFund} from "../actions/fund/fund";
import {selectFundTab} from "../actions/arr/fund";
import {createFundRoot, getFundFromFundAndVersion} from "../components/arr/ArrUtils";
import {fundSelectSubNode} from "../actions/arr/node";
import {WebApi} from "../actions/WebApi";
import {Api} from "../api";

export {fetchNodeInfo} from "./fetchNodeInfo";

/**
 * Drive node selection after a node identifier has been resolved to NodeInfo.
 *
 * The node-data request below must be addressed to a version of the node's
 * own fund. The currently active tab may belong to a different fund (e.g.
 * navigation from the cross-fund search dialog) or to a different version
 * than the URL requests — in that case the fund detail is fetched and its
 * tab selected first, the same resolution ArrParentPage.resolveUrlsRaw
 * performs on a full page load. Only then is the parent chain fetched via
 * getNodeData and the node selected, always with the tab's own versionId.
 *
 * @param nodeInfo resolved node identity (id + fundId)
 * @param versionId fund version requested by the URL; null selects the open version
 */
export const processNodeNavigation = (nodeInfo: NodeInfo, versionId: number | null = null) =>
    async (dispatch, getState) => {
        dispatch(fundsSelectFund(nodeInfo.fundId));

        const {arrRegion} = getState();
        const activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;
        const tabMatches = activeFund != null
            && activeFund.id === nodeInfo.fundId
            && (versionId == null || activeFund.versionId === versionId);

        if (!tabMatches) {
            let fund;
            try {
                fund = await WebApi.getFundDetail(nodeInfo.fundId);
            } catch (e) {
                console.error("Failed to load fund detail", nodeInfo, e);
                return;
            }
            const version = versionId != null
                ? fund.versions.find((v) => v.id === versionId)
                : fund.versions.find((v) => !v.lockDate) ?? fund.versions[0];
            if (!version) {
                console.error("Fund version not found", nodeInfo, versionId);
                return;
            }
            dispatch(selectFundTab(getFundFromFundAndVersion(fund, version)));
        }

        waitForLoadAS(() => {
            const { arrRegion } = getState();
            const selectFund = arrRegion.funds[arrRegion.activeIndex];

            if (selectFund?.id !== nodeInfo.fundId || !selectFund?.fundTree?.fetched) {
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
