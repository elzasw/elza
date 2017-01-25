package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Uživatelské nastavení.
 *
 * @author Martin Šlapa
 * @since
 */
@Entity(name = "ui_settings")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class UISettings {

    @Id
    @GeneratedValue
    private Integer settingsId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId", nullable = true)
    private UsrUser user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SettingsType settingsType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = true)
    private EntityType entityType;

    @Column(nullable = true)
    private Integer entityId;

    @Column(nullable = true)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId")
    private RulPackage rulPackage;

    public Integer getSettingsId() {
        return settingsId;
    }

    public void setSettingsId(final Integer settingsId) {
        this.settingsId = settingsId;
    }

    public UsrUser getUser() {
        return user;
    }

    public void setUser(final UsrUser user) {
        this.user = user;
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

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    /**
     * Typ entity.
     */
    public enum EntityType {

        /**
         * Bez vazby na entitu.
         */
        NONE,

        /**
         * Vazba na archivní fond.
         */
        FUND,

        /**
         * Vazba na typ atributu.
         */
        ITEM_TYPE,

        /**
         * Vazba na pravidla.
         */
        RULE
    }

    /**
     * Typ nastavení.
     */
    public enum SettingsType {

        FUND_READ_MODE(EntityType.FUND),
        FUND_RIGHT_PANEL(EntityType.FUND),
        FUND_CENTER_PANEL(EntityType.FUND),

        /**
         * nastavení strictního módu pro uživatele (přepíše nastavení pravidel)
         */
        FUND_STRICT_MODE(EntityType.FUND),

        /**
         * oblíbené specifikace u typu atributu
         */
        FAVORITE_ITEM_SPECS(EntityType.ITEM_TYPE),

        /**
         * Připnutí sekcí osob
         */
        PARTY_PIN(EntityType.NONE),

        /**
         * Zobrazení popisků archivních souborů.
         */
        FUND_VIEW(EntityType.RULE),

        /**
         * Zobrazení skupin typů atributů v archivním souboru.
         */
        TYPE_GROUPS(EntityType.RULE),

        /**
         * Výchozí nastavení pro rejstříky.
         */
        RECORD,

        /**
         * Nastavení sloupců / atributů pro zobrazení v gridu
         */
        GRID_VIEW(EntityType.RULE);

        /**
         * Typ oprávnění
         */
        private EntityType type;

        SettingsType() {
            this.type = EntityType.NONE;
        }

        SettingsType(final EntityType type) {
            this.type = type;
        }

        public EntityType getType() {
            return type;
        }
    }
}
