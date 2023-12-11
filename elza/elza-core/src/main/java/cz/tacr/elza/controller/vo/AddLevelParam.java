package cz.tacr.elza.controller.vo;

import cz.tacr.elza.controller.ArrangementController;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.service.FundLevelService;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Vstupní parametry pro přidání uzlu.
 */
public class AddLevelParam extends ArrangementController.NodeParam {
    /**
     * Směr přidávání uzlu (před, za, pod)
     */
    private FundLevelService.AddLevelDirection direction;
    /**
     * Název scénáře, ze kterého se mají převzít výchozí hodnoty atributů.
     */
    @Nullable
    private String scenarioName;

    @Nullable
    private List<ArrItemVO> createItems;

    /**
     * Seznam id typů atributů, které budou zkopírovány z uzlu přímo nadřazeným nad přidaným uzlem (jeho mladší sourozenec).
     */
    @Nullable
    private Set<Integer> descItemCopyTypes;

    /**
     * Počet zakládaných položek
     */
    @Nullable
    private Integer count;

    public FundLevelService.AddLevelDirection getDirection() {
        return direction;
    }

    public void setDirection(final FundLevelService.AddLevelDirection direction) {
        this.direction = direction;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(final String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public List<ArrItemVO> getCreateItems() {
        return createItems;
    }

    public void setCreateItems(final List<ArrItemVO> createItems) {
        this.createItems = createItems;
    }

    public Set<Integer> getDescItemCopyTypes() {
        return descItemCopyTypes;
    }

    public void setDescItemCopyTypes(final Set<Integer> descItemCopyTypes) {
        this.descItemCopyTypes = descItemCopyTypes;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
