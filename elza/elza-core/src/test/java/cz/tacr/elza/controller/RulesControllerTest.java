package cz.tacr.elza.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import cz.tacr.elza.test.controller.vo.DataType;
import cz.tacr.elza.test.controller.vo.IntViewDefinition;
import cz.tacr.elza.test.controller.vo.ItemDisplayType;
import cz.tacr.elza.test.controller.vo.ItemType;
import cz.tacr.elza.test.controller.vo.ItemTypeList;
import cz.tacr.elza.test.controller.vo.ItemTypeSpec;
import cz.tacr.elza.test.controller.vo.ItemViewDefinition;
import cz.tacr.elza.test.controller.vo.JsonTableViewDefinition;

/**
 * Integration test for the new public {@code /api/v1/rules/itemTypes}
 * endpoint. Asserts against the {@code rules-simple-dev} fixture loaded by
 * {@link AbstractControllerTest}.
 *
 * Uses per-class lifecycle: setUp/tearDown run once for all test methods.
 * Tests are read-only.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RulesControllerTest extends AbstractControllerTest {

    @BeforeAll
    public void initOnce() throws Exception {
        super.setUp();
    }

    @AfterAll
    public void cleanupOnce() {
        super.tearDown();
    }

    @Override
    @BeforeEach
    public void setUp() {
        // no-op: setup is done once in @BeforeAll initOnce()
    }

    @Override
    @AfterEach
    public void tearDown() {
        // no-op: cleanup is done once in @AfterAll cleanupOnce()
    }

    @Test
    public void listItemTypes_returnsAllRegisteredTypes() {
        ItemTypeList response = rulesApi.rulesListItemTypes(null, null);

        assertNotNull(response);
        List<ItemType> items = response.getItemTypes();
        assertThat(items).isNotEmpty();
        // Required scalar fields populated on every entry.
        for (ItemType it : items) {
            assertNotNull(it.getId(), "id must be set");
            assertNotNull(it.getCode(), "code must be set");
            assertNotNull(it.getName(), "name must be set");
            assertNotNull(it.getShortcut(), "shortcut must be set");
            assertNotNull(it.getDataType(), "dataType must be set");
            assertNotNull(it.getSortable(), "sortable must be set");
        }
    }

    @Test
    public void listItemTypes_intDurationDiscriminatorDeserialized() {
        ItemType movieLength = findByCode("SRD_MOVIE_LENGTH");
        assertEquals(DataType.INT, movieLength.getDataType());

        ItemViewDefinition vd = movieLength.getViewDefinition();
        assertNotNull(vd, "SRD_MOVIE_LENGTH should carry a viewDefinition (DURATION)");
        assertThat(vd).isInstanceOf(IntViewDefinition.class);
        assertEquals(ItemDisplayType.DURATION, ((IntViewDefinition) vd).getDisplayType());
    }

    @Test
    public void listItemTypes_jsonTableDiscriminatorDeserialized() {
        ItemType statistics = findByCode("SRD_STATISTICS");
        assertEquals(DataType.JSON_TABLE, statistics.getDataType());

        ItemViewDefinition vd = statistics.getViewDefinition();
        assertNotNull(vd, "SRD_STATISTICS should carry a viewDefinition (table columns)");
        assertThat(vd).isInstanceOf(JsonTableViewDefinition.class);
        JsonTableViewDefinition jt = (JsonTableViewDefinition) vd;
        assertThat(jt.getTableColumns()).isNotEmpty();
        assertThat(jt.getTableColumns()).allSatisfy(col -> {
            assertNotNull(col.getCode());
            assertNotNull(col.getName());
            assertNotNull(col.getDataType());
        });
    }

    @Test
    public void listItemTypes_structureTypeCodePopulatedForStructured() {
        ItemType storageId = findByCode("SRD_STORAGE_ID");
        assertEquals(DataType.STRUCTURED, storageId.getDataType());
        assertEquals("SRD_PACKET", storageId.getStructureTypeCode());
    }

    @Test
    public void listItemTypes_structureTypeCodeAbsentForNonStructured() {
        ItemType title = findByCode("SRD_TITLE");
        assertThat(title.getDataType()).isNotEqualTo(DataType.STRUCTURED);
        assertThat(title.getStructureTypeCode()).isNull();
    }

    @Test
    public void listItemTypes_specsLoadedForItemTypeWithSpecs() {
        ItemType levelType = findByCode("SRD_LEVEL_TYPE");
        assertNotNull(levelType.getSpecs(), "SRD_LEVEL_TYPE should expose specs");
        assertThat(levelType.getSpecs()).isNotEmpty();
        for (ItemTypeSpec spec : levelType.getSpecs()) {
            assertNotNull(spec.getId());
            assertNotNull(spec.getCode());
            assertNotNull(spec.getName());
            assertNotNull(spec.getShortcut());
        }
        // SRD_LEVEL_ROOT is one of the six level specs in rules-simple-dev.
        assertTrue(levelType.getSpecs().stream().anyMatch(s -> "SRD_LEVEL_ROOT".equals(s.getCode())),
                "Expected SRD_LEVEL_ROOT among SRD_LEVEL_TYPE specs");
    }

    @Test
    public void listItemTypes_specsOmittedForItemTypeWithoutSpecs() {
        // SRD_FORMAL_TITLE is STRING with no use-specification.
        ItemType formalTitle = findByCode("SRD_FORMAL_TITLE");
        assertThat(formalTitle.getSpecs()).isNullOrEmpty();
    }

    @Test
    public void listItemTypes_sortableReflectsCanBeOrdered() {
        // SRD_UNIT_COUNT is canBeOrdered=true in rules-simple-dev.
        ItemType unitCount = findByCode("SRD_UNIT_COUNT");
        assertEquals(Boolean.TRUE, unitCount.getSortable());

        // SRD_MOVIE_LENGTH has canBeOrdered=false (default).
        ItemType movieLength = findByCode("SRD_MOVIE_LENGTH");
        assertEquals(Boolean.FALSE, movieLength.getSortable());
    }

    @Test
    public void listItemTypes_acceptLanguageHeaderAccepted() {
        // Until the translation layer lands, Accept-Language is a no-op but
        // must not cause the request to fail.
        ItemTypeList response = rulesApi.rulesListItemTypes(null, "cs");
        assertThat(response.getItemTypes()).isNotEmpty();
    }

    @Test
    public void listItemTypes_unknownRuleSetCode_returnsEmpty() {
        // An unknown rule-set filter yields no item types (and never an error).
        ItemTypeList response = rulesApi.rulesListItemTypes("__no_such_rule_set__", null);
        assertThat(response.getItemTypes()).isEmpty();
    }

    private ItemType findByCode(String code) {
        ItemTypeList response = rulesApi.rulesListItemTypes(null, null);
        Optional<ItemType> found = response.getItemTypes().stream()
                .filter(it -> code.equals(it.getCode()))
                .findFirst();
        assertTrue(found.isPresent(), "Expected item type " + code + " in rules-simple-dev fixture");
        return found.get();
    }
}
