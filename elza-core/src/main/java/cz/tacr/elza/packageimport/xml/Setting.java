package cz.tacr.elza.packageimport.xml;

import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.UISettings;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;


/**
 * VO Setting.
 *
 */
@XmlTransient
public abstract class Setting {

    @XmlAttribute(name = "settings-type", required = true)
    @JsonIgnore
    private String settingsType;

    @XmlAttribute(name = "entity-type")
    @JsonIgnore
    private UISettings.EntityType entityType;
    
    protected Setting() {
    	
    }
    
    protected Setting(final String settingsType,
    				  final UISettings.EntityType entityType) {
    	this.settingsType = settingsType;
    	this.entityType = entityType;
    }

    public String getSettingsType() {
        return settingsType;
    }

    public void setSettingsType(final String settingsType) {
        this.settingsType = settingsType;
    }

    public UISettings.EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(final UISettings.EntityType entityType) {
        this.entityType = entityType;
    }

    /**
     * Store value of object in the settings
     * @param uiSettings
     */
    abstract void store(UISettings uiSettings);
    
    public UISettings createUISettings(RulPackage rulPackage) {
        UISettings uiSettings = new UISettings();
        uiSettings.setRulPackage(Validate.notNull(rulPackage));
        uiSettings.setSettingsType(settingsType);
        uiSettings.setEntityType(entityType);
        store(uiSettings);
        return uiSettings;
    }
}
