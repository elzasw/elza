package cz.tacr.elza.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.domain.UISettings.EntityType;
import cz.tacr.elza.domain.UISettings.SettingsType;

/**
 * Package import matches settings by key; the type read from XML and the one loaded from the
 * database are different String instances, which an identity comparison never matched.
 */
public class UISettingsTest {

    @Test
    void sameKeyMatchesAcrossStringInstances() {
        UISettings fromXml = settings(new String("FUND_VIEW"), EntityType.RULE, 1);
        UISettings fromDb = settings(new String("FUND_VIEW"), EntityType.RULE, 1);
        assertTrue(fromXml.isSameSettings(fromDb));
    }

    @Test
    void differentTypeEntityOrIdDoNotMatch() {
        UISettings base = settings("FUND_VIEW", EntityType.RULE, 1);
        assertFalse(base.isSameSettings(settings("GRID_VIEW", EntityType.RULE, 1)));
        assertFalse(base.isSameSettings(settings("FUND_VIEW", EntityType.FUND, 1)));
        assertFalse(base.isSameSettings(settings("FUND_VIEW", EntityType.RULE, 2)));
        assertTrue(settings("MENU", null, null).isSameSettings(settings(new String("MENU"), null, null)));
    }

    @Test
    void layeredTypes() {
        assertTrue(SettingsType.isLayered("OUTPUT_DEFAULTS"));
        // stated globally by ZP2015 and SIMPLE-DEV, which may be installed together
        assertTrue(SettingsType.isLayered("DAO_LEVEL_IMPORT"));
        assertFalse(SettingsType.isLayered("FUND_VIEW"));
        assertFalse(SettingsType.isLayered("STRUCT_TYPE_ZP2015_PACKET"));
    }

    private static UISettings settings(String type, EntityType entityType, Integer entityId) {
        UISettings s = new UISettings();
        s.setSettingsType(type);
        s.setEntityType(entityType);
        s.setEntityId(entityId);
        return s;
    }
}
