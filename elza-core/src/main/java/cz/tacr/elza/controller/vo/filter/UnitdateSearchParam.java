package cz.tacr.elza.controller.vo.filter;

import org.springframework.util.Assert;

/**
 * Hledání podle datace.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 16. 1. 2017
 */
public class UnitdateSearchParam extends SearchParam {

    private UnitdateCondition condition;

    public UnitdateSearchParam() {
    }

    protected UnitdateSearchParam(final String value, final UnitdateCondition condition) {
        super(SearchParamType.UNITDATE, value);

        Assert.notNull(condition, "Podmínka musí být vyplněna");

        this.condition = condition;
    }

    public UnitdateCondition getCondition() {
        return condition;
    }

    public void setCondition(final UnitdateCondition condition) {
        this.condition = condition;
    }
}
