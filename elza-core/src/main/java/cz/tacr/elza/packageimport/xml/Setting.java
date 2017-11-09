package cz.tacr.elza.packageimport.xml;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    @XmlAttribute(name = "entity-id")
    @JsonIgnore
    private Integer entityId;

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

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(final Integer entityId) {
        this.entityId = entityId;
    }

    public boolean isSettingsFor(UISettings uiSetting) {
        return settingsType == uiSetting.getSettingsType()
                && entityType == uiSetting.getEntityType()
                && Objects.equals(entityId, uiSetting.getEntityId());
    }
}
