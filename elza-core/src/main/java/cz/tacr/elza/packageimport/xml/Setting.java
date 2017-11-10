package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.UISettings;


/**
 * VO Setting.
 *
 * @author Martin Šlapa
 * @since 22.3.2016
 */
@XmlTransient
public abstract class Setting {

    @XmlAttribute(name = "settings-type", required = true)
    @JsonIgnore
    private UISettings.SettingsType settingsType;

    @XmlAttribute(name = "entity-type")
    @JsonIgnore
    private UISettings.EntityType entityType;

    @JsonIgnore
    public abstract String getValue();

    public abstract void setValue(String value);

    public UISettings.SettingsType getSettingsType() {
        return settingsType;
    }

    public void setSettingsType(final UISettings.SettingsType settingsType) {
        this.settingsType = settingsType;
    }

    public UISettings.EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(final UISettings.EntityType entityType) {
        this.entityType = entityType;
    }

    public UISettings createUISettings(RulPackage rulPackage) {
        UISettings uiSettings = new UISettings();
        uiSettings.setRulPackage(Validate.notNull(rulPackage));
        uiSettings.setSettingsType(settingsType);
        uiSettings.setEntityType(entityType);
        uiSettings.setValue(getValue());
        return uiSettings;
    }
}
