package cz.tacr.elza.controller.vo;

import java.util.List;

import cz.tacr.elza.interpi.service.vo.ConditionVO;

/**
 * Parametry pro vyhledávání v INTERPI.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 5. 12. 2016
 */
public class InterpiSearchVO {

    private boolean isParty;

    private List<ConditionVO> conditions;

    private Integer count;

    private Integer systemId;

    public boolean isParty() {
        return isParty;
    }
    public void setParty(final boolean isParty) {
        this.isParty = isParty;
    }
    public boolean getIsParty() {
        return isParty;
    }
    public void setIsParty(final boolean isParty) {
        this.isParty = isParty;
    }
    public List<ConditionVO> getConditions() {
        return conditions;
    }
    public void setConditions(final List<ConditionVO> conditions) {
        this.conditions = conditions;
    }
    public Integer getCount() {
        return count;
    }
    public void setCount(final Integer count) {
        this.count = count;
    }
    public Integer getSystemId() {
        return systemId;
    }
    public void setSystemId(final Integer systemId) {
        this.systemId = systemId;
    }
}
