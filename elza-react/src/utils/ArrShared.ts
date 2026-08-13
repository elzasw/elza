import type {NodeInfo} from "elza-api";
import {fundsSelectFund} from "../actions/fund/fund";
import {selectFundTab} from "../actions/arr/fund";
import {createFundRoot, getFundFromFundAndVersion} from "../components/arr/ArrUtils";
import {fundSelectSubNode} from "../actions/arr/node";
import {WebApi} from "../actions/WebApi";
import {Api} from "../api";

export {fetchNodeInfo} from "./fetchNodeInfo";

/**
 * Make the active arr tab show the given fund and version, taking the server
 * as the authority on which version is the current open one.
 *
 * The active tab may be restored from localStorage and stale — its persisted
 * lockDate cannot be trusted, because the version may have been approved
 * (closed) since the state was saved. The only case decided without a server
 * round-trip is a version pinned in the URL and already displayed: a closed
 * version is immutable. In every other case the fund detail is fetched and
 * the existing tab is kept only when the server confirms its version;
 * otherwise a tab with the resolved version is selected.
 *
 * @param dispatch redux dispatch
 * @param activeFund the currently active arr tab (or null)
 * @param fundId fund the tab must show
 * @param versionId fund version pinned by the URL; null resolves the open version
 * @returns the kept tab or the freshly fetched fund detail
 * @throws when the fund detail cannot be loaded or the version does not exist
 */
export const resolveFundTab = async (dispatch, activeFund, fundId: number, versionId: number | null = null) => {
    if (activeFund?.id === fundId && versionId != null && activeFund?.activeVersion?.id === versionId) {
        return activeFund;
    }

    const fund = await WebApi.getFundDetail(fundId);
    const version = versionId != null
        ? fund.versions.find((v) => v.id === versionId)
        : fund.versions.find((v) => !v.lockDate) ?? fund.versions[0];
    if (!version) {
        throw new Error(`Fund version not found, fundId=${fundId}, versionId=${versionId}`);
    }

    if (activeFund?.id === fundId && activeFund?.versionId === version.id) {
        return activeFund;
    }

    dispatch(selectFundTab(getFundFromFundAndVersion(fund, version)));
    return fund;
};

/**
 * Drive node selection after a node identifier has been resolved to NodeInfo.
 *
 * The node-data request below must be addressed to a version of the node's
 * own fund. The currently active tab may belong to a different fund (e.g.
 * navigation from the cross-fund search dialog) or to a different version
 * than the URL requests — in that case the tab is resolved via resolveFundTab
 * first. A tab already matching the fund and version is trusted without a
 * server round-trip: SPA navigation always follows a page mount that has
 * validated the tab through ArrParentPage.resolveUrlsRaw. Only then is the
 * parent chain fetched via getNodeData and the node selected, always with
 * the tab's own versionId.
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
            try {
                await resolveFundTab(dispatch, activeFund, nodeInfo.fundId, versionId);
            } catch (e) {
                console.error("Failed to load fund detail", nodeInfo, e);
                return;
            }
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
