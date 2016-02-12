package cz.tacr.elza.controller.vo;

import java.util.List;

import cz.tacr.elza.controller.vo.nodes.descitems.DescItemGroupVO;


/**
 * Hromadné akce
 *
 * @author Petr Compel[petr.compel@marbes.cz]
 * @since 29. 1. 2016
 */
public class ScenarioOfNewLevelVO {

    private String name;

    List<DescItemGroupVO> groups;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DescItemGroupVO> getGroups() {
        return groups;
    }

    public void setGroups(final List<DescItemGroupVO> groups) {
        this.groups = groups;
    }
}
