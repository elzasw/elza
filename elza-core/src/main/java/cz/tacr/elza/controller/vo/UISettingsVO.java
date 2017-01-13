package cz.tacr.elza.controller.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.UISettings.EntityType;
import cz.tacr.elza.domain.UISettings.SettingsType;

/**
 * VO uživatelského nastavení.
 *
 * @author Martin Šlapa
 * @since 19.07.2016
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UISettingsVO {

    private Integer id;

    private SettingsType settingsType;

    private EntityType entityType;

    private Integer entityId;

    private String value;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public SettingsType getSettingsType() {
        return settingsType;
    }

    public void setSettingsType(final SettingsType settingsType) {
        this.settingsType = settingsType;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(final EntityType entityType) {
        this.entityType = entityType;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(final Integer entityId) {
        this.entityId = entityId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}
