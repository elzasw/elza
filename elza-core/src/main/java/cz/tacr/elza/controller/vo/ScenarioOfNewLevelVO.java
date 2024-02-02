package cz.tacr.elza.controller.vo;

import java.util.List;

import cz.tacr.elza.controller.vo.nodes.descitems.ItemGroupVO;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;


/**
 * Hromadné akce
 *
 * @author Petr Compel[petr.compel@marbes.cz]
 * @since 29. 1. 2016
 */
public class ScenarioOfNewLevelVO {

    private String name;

    List<ItemGroupVO> groups;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ItemGroupVO> getGroups() {
        return groups;
    }

    public void setGroups(final List<ItemGroupVO> groups) {
        this.groups = groups;
    }

    public static ScenarioOfNewLevelVO newInstance(final ScenarioOfNewLevel scenarioOfNewLevel) {
    	ScenarioOfNewLevelVO result = new ScenarioOfNewLevelVO();
    	result.setName(scenarioOfNewLevel.getName());
    	return result;
    }
}
