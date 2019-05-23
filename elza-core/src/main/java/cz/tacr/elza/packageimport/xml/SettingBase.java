package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UISettings.EntityType;


/**
 * VO PolicyType.
 *
 * @author Martin Šlapa
 * @since 22.3.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "setting-base")
public class SettingBase extends Setting {

    @XmlValue
    private String value;
    
    public SettingBase()
    {
    	
    }

    public SettingBase(String settingsType, EntityType entityType,
                       final String value) {
		super(settingsType, entityType);
		this.value = value;
	}

	public static SettingBase newInstance(UISettings uiSettings) {
		SettingBase result = new SettingBase(uiSettings.getSettingsType(), uiSettings.getEntityType(), 
		                                     uiSettings.getValue());
		return result;
	}

    @Override
    void store(UISettings uiSettings) {
        uiSettings.setValue(value);
        
    }

}
