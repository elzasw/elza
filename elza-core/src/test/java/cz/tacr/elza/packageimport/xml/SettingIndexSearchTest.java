package cz.tacr.elza.packageimport.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.packageimport.xml.SettingIndexSearch.Field;

/**
 * Fields of the search configuration the text search uses in each area: the core defaults, the
 * CZ_BASE name fields and an item field of a package's own rules.
 */
class SettingIndexSearchTest {

    private static Field field(String name, String partType) {
        Field field = Field.of(name, 1.0f, null, null, null, true);
        field.setPartType(partType);
        return field;
    }

    private static SettingIndexSearch config() {
        SettingIndexSearch base = new SettingIndexSearch();
        base.setFields(List.of(
                field("pt_name_index", null),
                field("pref_nm_main", null),
                field("nm_main", "PT_NAME"),
                field("nm_minor", "PT_NAME")));
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
                "entity id and index fields are searched separately");
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
}
