import {fundsSelectFund} from "../actions/fund/fund";
import {createFundRoot, getFundFromFundAndVersion} from "../components/arr/ArrUtils";
import {selectFundTab} from "../actions/arr/fund";
import {routerNavigate} from "../actions/router";
import {fundSelectSubNode} from "../actions/arr/node";

export const processNodeNavigation = (data, versionId) => 
    (dispatch, getState) => {
        const fund = data.fund;
        dispatch(fundsSelectFund(fund.id));
        const fundVersion = fund.versions.find(v => versionId != null ? v.id === versionId : !v.lockDate);
        const fundObj = getFundFromFundAndVersion(fund, fundVersion);
        dispatch(selectFundTab(fundObj));
        let { arrRegion } = getState();

        waitForLoadAS(() => {
            dispatch((dispatch, getState) => {
                arrRegion = getState().arrRegion; // aktuální stav ve store
            });

            const selectFund = arrRegion.funds[arrRegion.activeIndex];

            if (selectFund.fundTree.fetched) {
                // čekáme na načtení stromu, potom můžeme vybrat JP
                const nodeWithParent = data.nodeWithParent;
                const node = nodeWithParent.node;
                let parentNode = nodeWithParent.parentNode;
                if (parentNode == null) {
                    // root
                    parentNode = createFundRoot(selectFund);
                }
                dispatch(fundSelectSubNode(fundVersion.id, node.id, parentNode, false, null, false));
                return false;
            } else {
                return true;
                }
        });
    }

export const waitForLoadAS = fce => {
    const next = fce();
    if (next) {
        setTimeout(() => {
            waitForLoadAS(fce);
        }, 50);
    }
};
