package cz.tacr.elza.api;

/**
 * Uživatelské nastavení.
 *
 * @author Martin Šlapa
 * @since 22.3.2016
 */
public interface UISettings<U extends UsrUser> {


    Integer getSettingsId();

    void setSettingsId(Integer settingsId);

    U getUser();

    void setUser(U user);

    SettingsType getSettingsType();

    void setSettingsType(SettingsType settingsType);

    EntityType getEntityType();

    void setEntityType(EntityType entityType);

    Integer getEntityId();

    void setEntityId(Integer entityId);

    String getValue();

    void setValue(String value);

    /**
     * Typ entity.
     */
    enum EntityType {

        /**
         * Bez vazby na entitu.
         */
        NONE,

        /**
         * Vazba na archivní fond.
         */
        FUND

    }

    /**
     * Typ nastavení.
     */
    enum SettingsType {

        FUND_RIGHT_PANEL(EntityType.FUND),
        FUND_CENTER_PANEL(EntityType.FUND);

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
