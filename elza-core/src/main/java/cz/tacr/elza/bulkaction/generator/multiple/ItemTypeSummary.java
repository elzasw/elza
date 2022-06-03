package cz.tacr.elza.bulkaction.generator.multiple;

import cz.tacr.elza.bulkaction.generator.result.DateRangeActionResult;

public class ItemTypeSummary {

    private Integer count = 0;

    private DateRangeAction dateRangeAction;

    public void addCount(Integer add) {
        count += add;
    }

    public String getTextResut() {
        DateRangeActionResult result = (DateRangeActionResult) dateRangeAction.getResult();
        return result.getText();
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public void setDateCounter(DateRangeAction dateRangeAction) {
        this.dateRangeAction = dateRangeAction;        
    }

    public DateRangeAction getDateCounter() {
        return dateRangeAction;
    }
}
