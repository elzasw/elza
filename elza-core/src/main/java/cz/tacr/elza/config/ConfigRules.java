package cz.tacr.elza.config;


import com.google.common.eventbus.Subscribe;
import cz.tacr.elza.EventBusListener;
import cz.tacr.elza.config.rules.ViewConfiguration;
import cz.tacr.elza.config.rules.RuleConfiguration;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.packageimport.xml.SettingTypeGroups;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.SettingsRepository;
import cz.tacr.elza.service.event.CacheInvalidateEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Nastavení strategií pravidel.
 *
 */
@Component
@EventBusListener
public class ConfigRules {

	/**
	 * Group code used when explicit configuration for given item is not available
	 */
	private final static String defaultGroupConfigurationCode="DEFAULT";
	public String getDefaultGroupConfigurationCode(){
		return defaultGroupConfigurationCode;
	}

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private RuleSetRepository ruleSetRepository;

    /**
     * Map of configurations for rules
     */
    private Map<String, RuleConfiguration> ruleConfigs;

    @Subscribe
    public synchronized void invalidateCache(final CacheInvalidateEvent cacheInvalidateEvent) {
        if (cacheInvalidateEvent.contains(CacheInvalidateEvent.Type.RULE)) {
        	ruleConfigs = null;
        }
    }

    private void initRuleConfigs()
    {
    	ruleConfigs = new HashMap<>();

    	// read configuration from DB
        List<UISettings> uiSettingsList = settingsRepository.findByUserAndSettingsTypeAndEntityType(null, UISettings.SettingsType.TYPE_GROUPS.toString(), UISettings.EntityType.RULE);

        // prepare objects
        if (uiSettingsList!=null) {
            uiSettingsList.forEach(uiSettings -> {
                RulRuleSet ruleSet = ruleSetRepository.findOne(uiSettings.getEntityId());
                SettingTypeGroups ruleSettings = SettingTypeGroups.newInstance(uiSettings);
                RuleConfiguration ruleConfig = new RuleConfiguration(ruleSettings);
                ruleConfigs.put(ruleSet.getCode(), ruleConfig);
            });
        }
    }

	private synchronized Map<String, RuleConfiguration> getTypeGroups() {
        if (ruleConfigs == null) {
        	initRuleConfigs();
        }
        return ruleConfigs;
    }

	/**
	 * Return configuration for rules
	 * @param ruleCode
	 * @return
	 */
	private RuleConfiguration getRuleConfiguration(final String ruleCode) {
		Map<String, RuleConfiguration> configs = getTypeGroups();
		return configs.get(ruleCode);
	}


    /**
     * Return view configuration
     */
    public ViewConfiguration getViewConfiguration(final String ruleCode, final Integer fundId) {

        RuleConfiguration ruleConfig = getRuleConfiguration(ruleCode);
        if(ruleConfig==null) {
        	throw new ObjectNotFoundException("Rules not found, code: "+ruleCode, BaseCode.ID_NOT_EXIST);
        }
        return ruleConfig.getViewConfiguration(fundId);
    }

}
