package cz.tacr.elza.config.view;

import java.util.HashMap;
import java.util.Map;

/**
 * Collection of fund view configurations
 * 
 * 
 */
public class FundViewConfigs {

    Map<Integer, ViewTitles> fundViewsByRuleSets = new HashMap<>();

    // Not yet implemetned
    //Map<Integer, ViewTitles> fundViewsByFundId = new HashMap<>();

    public void addRuleSetFundView(Integer ruleSetId, ViewTitles fundView) {
        fundViewsByRuleSets.put(ruleSetId, fundView);

    }

    public ViewTitles getByRuleSetId(Integer ruleSetId) {
        return fundViewsByRuleSets.get(ruleSetId);
    }
}
