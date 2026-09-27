import { useEffect } from "react";
import { useSelector } from "react-redux";
import FundTreeDaos from "../../FundTreeDaos";
import { fundTreeNodeExpand, fundTreeSelectNode } from "actions/arr/fundTree";
import { FUND_TREE_AREA_AIP } from "actions/constants/ActionTypes";
import { AppState } from "typings/store";
import { useThunkDispatch } from "utils/hooks";

/** Uzel stromu archivního souboru - jen to, co připojování čte. */
export type TargetNode = { id: number; name: string };

/** Strom archivního souboru v oblasti připojování balíčků, jak ho drží store. */
type AipFundTree = { nodes: TargetNode[]; selectedId: number | null; expandedIds: Record<number, boolean> };

/** Aktivní archivní soubor - jen to, co připojování čte. */
type ActiveFund = { id: number; versionId: number; fundTreeAip: AipFundTree };

/**
 * Aktivní archivní soubor a cíl připojení - jednotka popisu vybraná ve stromu připojování
 * balíčků. Strom má vlastní oblast, takže výběr v něm neovlivní strom pořádání.
 */
export function useAipTarget() {
    const fund = useSelector((state: AppState) =>
        state.arrRegion?.funds?.[state.arrRegion.activeIndex ?? -1]) as unknown as ActiveFund | undefined;
    const tree = fund?.fundTreeAip;
    const target = tree?.nodes.find(n => n.id === tree.selectedId);
    return { fund, tree, target };
}

/** Rozbalí cíl, aby byly vidět jednotky popisu nově vytvořené pod ním. */
export const expandTarget = (node: TargetNode) => fundTreeNodeExpand(FUND_TREE_AREA_AIP, node);

/**
 * Strom archivního souboru jako cíl připojení balíčků: líné načítání, hledání a kontextové menu
 * s otevřením v pořádání. Na začátku je vybrán kořen - nejčastěji se připojuje k němu.
 */
export function AipTargetTree() {
    const dispatch = useThunkDispatch();
    const { fund, tree } = useAipTarget();

    const root = tree?.nodes[0];
    useEffect(() => {
        if (fund && root && tree?.selectedId == null) {
            dispatch(fundTreeSelectNode(FUND_TREE_AREA_AIP, fund.versionId, root.id, false, false) as never);
        }
    }, [root?.id]);

    return fund && tree
        ? <FundTreeDaos fund={fund} versionId={fund.versionId} area={FUND_TREE_AREA_AIP} {...tree} />
        : null;
}
