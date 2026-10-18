package cz.tacr.elza.packageimport.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.packageimport.xml.SettingIndexSearch.Field;

/**
 * Fields of the search configuration the text search uses in each area: the core defaults, the
 * CZ_BASE name fields, an item field of a package's own rules and the fields of an index type.
 */
class SettingIndexSearchTest {

    private static Field field(String name, String partType) {
        Field field = Field.of(name, 1.0f, null, null, null, true);
        field.setPartType(partType);
        return field;
    }

    private static Field indexTypeField(String name, String indexType) {
        Field field = Field.of(name, null, 10.0f, 8.0f, 4.0f, null);
        field.setIndexType(indexType);
        return field;
    }

    private static SettingIndexSearch config() {
        SettingIndexSearch base = new SettingIndexSearch();
        base.setFields(List.of(
                field("pt_name_index", null),
                field("pref_nm_main", null),
                field("nm_main", "PT_NAME"),
                field("nm_minor", "PT_NAME"),
                indexTypeField("pref_real_name", "REAL_NAME"),
                indexTypeField("real_name", "REAL_NAME")));
        SettingIndexSearch addon = new SettingIndexSearch();
        addon.setFields(List.of(field("own_code", "PT_IDENT")));
        return SettingIndexSearch.merge(List.of(SettingIndexSearch.coreDefaults(), base, addon));
    }

    private static List<String> names(List<Field> fields) {
        return fields.stream().map(Field::getName).toList();
    }

    @Test
    void anyPartSearchesEveryItemField() {
        assertEquals(List.of("pref_nm_main", "nm_main", "nm_minor", "own_code"),
                names(config().searchedItemFields(null)),
                "entity id, index fields and fields of an index type are searched separately");
    }

    @Test
    void preferredPartSearchesThePreferredItemFields() {
        assertEquals(List.of("pref_nm_main"), names(config().searchedItemFields("pref")));
    }

    @Test
    void namesOfAPartTypeSearchThePreferredFieldsAndTheFieldsOfThePartType() {
        assertEquals(List.of("pref_nm_main", "nm_main", "nm_minor"),
                names(config().searchedItemFields("pt_name")));
        assertEquals(List.of("pref_nm_main", "own_code"), names(config().searchedItemFields("pt_ident")));
    }

    /** A field of an index type is matched as a whole in every area; the preferred names use the {@code pref_} fields only. */
    @Test
    void fieldsOfAnIndexTypeAreMatchedAsAWhole() {
        assertEquals(List.of("pref_real_name", "real_name"), names(config().indexTypeFields(null)));
        assertEquals(List.of("pref_real_name", "real_name"), names(config().indexTypeFields("pt_name")));
        assertEquals(List.of("pref_real_name"), names(config().indexTypeFields("pref")));
        assertTrue(config().findField("pref_real_name").preferredPartOnly());
        assertTrue(!config().findField("real_name").preferredPartOnly());
    }

    /** The stored form (JSON in ui_settings) keeps the index type. */
    @Test
    void indexTypeSurvivesTheStoredForm() {
        UISettings stored = new UISettings();
        config().store(stored);
        SettingIndexSearch read = SettingIndexSearch.newInstance(stored);
        assertEquals("REAL_NAME", read.findField("pref_real_name").getIndexType());
        assertEquals(10.0f, read.findField("real_name").getBoostExact());
        assertNull(read.findField("pref_nm_main").getIndexType());
        assertEquals(names(config().getFields()), names(read.getFields()));
    }
}
