package cz.tacr.elza.config.rules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.packageimport.xml.SettingTypeGroups;
import cz.tacr.elza.packageimport.xml.SettingTypeGroups.Item;

/**
 * Configuration of named rule collection
 *
 */
public class RuleConfiguration {

	/**
	 * Default configuration
	 */
	ViewConfiguration defaultConfiguration;
	
	/**
	 * Map of fundId and group configuration
	 */
	Map<Integer, ViewConfiguration> fundConfigurations = new HashMap<>();

	/**
	 * Initialize configuration for single rules from XML
	 * @param ruleSettings
	 */
	public RuleConfiguration(SettingTypeGroups ruleSettings) {
        List<SettingTypeGroups.Item> items = ruleSettings.getItems();
        for (SettingTypeGroups.Item item : items) {
        	loadConfig(item);
        }
        
        // Prepare default configuration
        if(defaultConfiguration==null) {
        	defaultConfiguration = new ViewConfiguration();
        }
	}
	
    private void loadConfig(Item item) {
    	List<SettingTypeGroups.Group> groups = item.getGroups();
    	
    	ViewConfiguration viewConfig = new ViewConfiguration(groups); 
    	
        if (item instanceof SettingTypeGroups.Default) {
        	if(defaultConfiguration!=null) {
        		// multiple default configurations?
        		throw new IllegalStateException("Multiple default configurations");
        	}
        	defaultConfiguration = viewConfig;
        } else if (item instanceof SettingTypeGroups.Fund) {
            Integer fundId = ((SettingTypeGroups.Fund) item).getFundId();
            fundConfigurations.put(fundId, viewConfig);
        } else {
            throw new IllegalStateException("Nedefinovaný stav pro třídu:" + item.getClass().getSimpleName());
        }		
	}

	public ViewConfiguration getViewConfiguration(Integer fundId) {
		ViewConfiguration viewConfig = fundConfigurations.get(fundId);
		if(viewConfig==null) {
			viewConfig = this.defaultConfiguration;
		}
		return viewConfig;
	}

}
