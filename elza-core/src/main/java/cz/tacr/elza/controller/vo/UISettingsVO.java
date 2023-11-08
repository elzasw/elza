package cz.tacr.elza.controller.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UISettings.EntityType;
import cz.tacr.elza.domain.UISettings.SettingsType;

/**
 * VO uživatelského nastavení.
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UISettingsVO {

    private Integer id;

    private SettingsType settingsType;

    private EntityType entityType;

    private Integer entityId;

    private String value;

    // TODO: Check if still used, probably by some implicit conversion?
    public UISettingsVO() {

    }

    public UISettingsVO(UISettings uiSettings) {
        id = uiSettings.getSettingsId();
        settingsType = SettingsType.valueOf(uiSettings.getSettingsType());
        entityType = uiSettings.getEntityType();
        entityId = uiSettings.getEntityId();
        value = uiSettings.getValue();
    }

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

    public static UISettingsVO newInstance(final UISettings uiSettings) {
        return new UISettingsVO(uiSettings);
    }

    /**
     * Create corresponding empty DB object.
     * 
     * Object has no user and RulPackage.
     * 
     * @param vo
     * @return
     */
    public static UISettings createEntity(UISettingsVO vo) {
        UISettings entity = new UISettings();
        entity.setSettingsId(vo.getId());
        entity.setSettingsType(vo.getSettingsType().name());
        entity.setEntityId(vo.getEntityId());
        entity.setValue(vo.getValue());
        return entity;
    }
}
