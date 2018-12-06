package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer settingsId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId", nullable = true)
    private UsrUser user;

    @Column(insertable = false, updatable = false, nullable = true)
    private Integer userId;

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

    @Column(insertable = false, updatable = false, nullable = true)
    private Integer packageId;

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
        this.userId = user != null ? user.getUserId() : null;
    }

    public Integer getUserId() {
        return userId;
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
        this.packageId = rulPackage != null ? rulPackage.getPackageId() : null;
    }

    public Integer getPackageId() {
        return packageId;
    }

    /**
     * Check if other settings is same.
     */
    public boolean isSameSettings(UISettings other) {
        return settingsType == other.settingsType && entityType == other.entityType && Objects.equals(entityId, other.entityId);
    }

    /**
     * Typ entity.
     */
    public enum EntityType {
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

        FUND_READ_MODE(true, EntityType.FUND),
        FUND_RIGHT_PANEL(true, EntityType.FUND),
        FUND_CENTER_PANEL(true, EntityType.FUND),

        /**
         * nastavení strictního módu pro uživatele (přepíše nastavení pravidel)
         */
        FUND_STRICT_MODE(true, EntityType.FUND),

        /**
         * uživatelské šablony JP
         */
        FUND_TEMPLATES(true, EntityType.FUND),

        /**
         * oblíbené specifikace u typu atributu
         */
        FAVORITE_ITEM_SPECS(false, EntityType.ITEM_TYPE),

        /**
         * Připnutí sekcí osob
         */
        PARTY_PIN,

        /**
         * Zobrazení popisků archivních souborů.
         */
        FUND_VIEW(false, EntityType.RULE),

        /**
         * Zobrazení skupin typů atributů v archivním souboru.
         */
        TYPE_GROUPS(false, EntityType.RULE),

        /**
         * Zobrazení skupin typů atributů v archivním souboru.
         */
        STRUCTURE_TYPES(false, EntityType.RULE),

        /**
         * Výchozí nastavení pro rejstříky.
         */
        RECORD,

        /**
         * Nastavení sloupců / atributů pro zobrazení v gridu
         */
        GRID_VIEW(false, EntityType.RULE);

        /**
         * If settings can be global or has to be defined on some entity.
         */
        private final boolean global;

        /**
         * Typ oprávnění
         */
        private final EntityType entityType;

        SettingsType() {
            this(true, null);
        }

        SettingsType(boolean global, EntityType entityType) {
            this.global = global;
            this.entityType = entityType;
        }

        public boolean isSupportedEntityType(EntityType entityType) {
            if (entityType == null) {
                return global;
            }
            return entityType.equals(this.entityType);
        }

        public static List<SettingsType> findByType(final EntityType ...types) {
            List<SettingsType> results = new ArrayList<>();
            for (SettingsType value : SettingsType.values()) {
                if (types == null) {
                    if (value.isSupportedEntityType(null)) {
                        results.add(value);
                    }
                } else {
                    for (EntityType t : types) {
                        if (value.isSupportedEntityType(t)) {
                            results.add(value);
                            break;
                        }
                    }
                }
            }
            return results;
        }
    }
}
