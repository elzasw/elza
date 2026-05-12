package cz.tacr.elza.controller.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cz.tacr.elza.controller.vo.DataType;
import cz.tacr.elza.controller.vo.IntViewDefinition;
import cz.tacr.elza.controller.vo.ItemDisplayType;
import cz.tacr.elza.controller.vo.ItemType;
import cz.tacr.elza.controller.vo.ItemTypeList;
import cz.tacr.elza.controller.vo.ItemTypeSpec;
import cz.tacr.elza.controller.vo.JsonTableViewDefinition;
import cz.tacr.elza.controller.vo.StringViewDefinition;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.integer.DisplayType;
import cz.tacr.elza.domain.table.ElzaColumn;

/**
 * Pure unit test for {@link RulesMapper}. Builds domain objects in-memory
 * and stubs only {@link StaticDataService} for structure-type lookups.
 *
 * Deliberately avoids any Spring context so the test stays fast and the
 * mapper's contract is asserted directly.
 */
class RulesMapperTest {

    private StaticDataService staticDataService;
    private StaticDataProvider staticDataProvider;
    private RulesMapper mapper;

    @BeforeEach
    void setUp() {
        staticDataService = mock(StaticDataService.class);
        staticDataProvider = mock(StaticDataProvider.class);
        lenient().when(staticDataService.getData()).thenReturn(staticDataProvider);
        mapper = new RulesMapper(staticDataService);
    }

    // ---------- toItemTypeList -------------------------------------------------

    @Test
    void toItemTypeList_wrapsAllEntries_inOrder() {
        RulItemTypeExt a = stringItemType(1, "A_CODE", null);
        RulItemTypeExt b = stringItemType(2, "B_CODE", null);

        ItemTypeList out = mapper.toItemTypeList(List.of(a, b));

        assertThat(out.getItemTypes()).extracting(ItemType::getCode).containsExactly("A_CODE", "B_CODE");
    }

    // ---------- viewDefinition: INT --------------------------------------------

    @Test
    void viewDefinition_intDuration_mappedToIntViewDefinition() {
        RulItemTypeExt src = intItemType("INT_DURATION", DisplayType.DURATION);

        ItemType out = mapper.toItemType(src);

        assertInstanceOf(IntViewDefinition.class, out.getViewDefinition());
        IntViewDefinition vd = (IntViewDefinition) out.getViewDefinition();
        assertEquals(ItemDisplayType.DURATION, vd.getDisplayType());
        assertEquals(DataType.INT, vd.getDataType());
    }

    @Test
    void viewDefinition_intNumber_omitted() {
        RulItemTypeExt src = intItemType("INT_NUMBER", DisplayType.NUMBER);

        assertNull(mapper.toItemType(src).getViewDefinition());
    }

    @Test
    void viewDefinition_intNullRaw_omitted() {
        // No viewDefinition set on the source.
        RulItemTypeExt src = intItemType("INT_NULL", null);

        assertNull(mapper.toItemType(src).getViewDefinition());
    }

    // ---------- viewDefinition: STRING -----------------------------------------

    @Test
    void viewDefinition_stringMask_mappedToStringViewDefinition() {
        cz.tacr.elza.domain.viewDefinition.StringViewDefinition raw =
                new cz.tacr.elza.domain.viewDefinition.StringViewDefinition();
        raw.setMask("###-##");
        RulItemTypeExt src = stringItemType(3, "STR_MASK", raw);

        ItemType out = mapper.toItemType(src);

        assertInstanceOf(StringViewDefinition.class, out.getViewDefinition());
        StringViewDefinition vd = (StringViewDefinition) out.getViewDefinition();
        assertEquals("###-##", vd.getMask());
        assertEquals(DataType.STRING, vd.getDataType());
    }

    @Test
    void viewDefinition_stringNullRaw_omitted() {
        RulItemTypeExt src = stringItemType(4, "STR_PLAIN", null);

        assertNull(mapper.toItemType(src).getViewDefinition());
    }

    @Test
    void viewDefinition_stringEmptyMask_omitted() {
        cz.tacr.elza.domain.viewDefinition.StringViewDefinition raw =
                new cz.tacr.elza.domain.viewDefinition.StringViewDefinition();
        raw.setMask("");
        RulItemTypeExt src = stringItemType(5, "STR_EMPTY_MASK", raw);

        assertNull(mapper.toItemType(src).getViewDefinition());
    }

    // ---------- viewDefinition: JSON_TABLE --------------------------------------

    @Test
    void viewDefinition_jsonTable_mappedToJsonTableViewDefinition() {
        ElzaColumn col = new ElzaColumn();
        col.setCode("KEY");
        col.setName("Klíč");
        col.setDataType(ElzaColumn.DataType.TEXT);
        col.setWidth(120);
        RulItemTypeExt src = jsonTableItemType("JT", List.of(col));

        ItemType out = mapper.toItemType(src);

        assertInstanceOf(JsonTableViewDefinition.class, out.getViewDefinition());
        JsonTableViewDefinition vd = (JsonTableViewDefinition) out.getViewDefinition();
        assertEquals(DataType.JSON_TABLE, vd.getDataType());
        assertThat(vd.getTableColumns()).hasSize(1);
        assertEquals("KEY", vd.getTableColumns().get(0).getCode());
        assertEquals("Klíč", vd.getTableColumns().get(0).getName());
        assertEquals("TEXT", vd.getTableColumns().get(0).getDataType());
        assertEquals(120, vd.getTableColumns().get(0).getWidth());
    }

    @Test
    void viewDefinition_jsonTableEmptyColumns_omitted() {
        RulItemTypeExt src = jsonTableItemType("JT_EMPTY", Collections.emptyList());

        assertNull(mapper.toItemType(src).getViewDefinition());
    }

    // ---------- viewDefinition: other data types --------------------------------

    @Test
    void viewDefinition_otherDataType_omitted() {
        RulItemTypeExt src = bareItemType(6, "T_TEXT", "TEXT");

        assertNull(mapper.toItemType(src).getViewDefinition());
    }

    // ---------- structureTypeCode ----------------------------------------------

    @Test
    void structureTypeCode_setForStructured() {
        StructType st = mock(StructType.class);
        when(st.getCode()).thenReturn("SRD_PACKET");
        when(staticDataProvider.getStructuredTypeById(42)).thenReturn(st);

        RulItemTypeExt src = bareItemType(7, "STORAGE", "STRUCTURED");
        src.setStructuredType(rulStructuredType(42));

        assertEquals("SRD_PACKET", mapper.toItemType(src).getStructureTypeCode());
    }

    @Test
    void structureTypeCode_nullForNonStructured() {
        RulItemTypeExt src = bareItemType(8, "PLAIN", "STRING");

        assertNull(mapper.toItemType(src).getStructureTypeCode());
    }

    @Test
    void structureTypeCode_nullWhenLookupReturnsNull() {
        when(staticDataProvider.getStructuredTypeById(99)).thenReturn(null);
        RulItemTypeExt src = bareItemType(9, "UNKNOWN_STRUCT", "STRUCTURED");
        src.setStructuredType(rulStructuredType(99));

        assertNull(mapper.toItemType(src).getStructureTypeCode());
    }

    // ---------- toItemType: basic fields ---------------------------------------

    @Test
    void toItemType_copiesAllScalarFields() {
        RulItemTypeExt src = bareItemType(10, "FULL", "STRING");
        src.setName("Full name");
        src.setShortcut("Short");
        src.setDescription("Long description");
        src.setCanBeOrdered(Boolean.TRUE);

        ItemType out = mapper.toItemType(src);

        assertEquals(10, out.getId());
        assertEquals("FULL", out.getCode());
        assertEquals("Full name", out.getName());
        assertEquals("Short", out.getShortcut());
        assertEquals("Long description", out.getDescription());
        assertEquals(DataType.STRING, out.getDataType());
        assertEquals(Boolean.TRUE, out.getSortable());
    }

    @Test
    void toItemType_sortableFalseWhenCanBeOrderedIsNullOrFalse() {
        RulItemTypeExt nullCase = bareItemType(11, "NULL_ORD", "STRING");
        nullCase.setCanBeOrdered(null);
        RulItemTypeExt falseCase = bareItemType(12, "FALSE_ORD", "STRING");
        falseCase.setCanBeOrdered(Boolean.FALSE);

        assertEquals(Boolean.FALSE, mapper.toItemType(nullCase).getSortable());
        assertEquals(Boolean.FALSE, mapper.toItemType(falseCase).getSortable());
    }

    // ---------- specs ----------------------------------------------------------

    @Test
    void specs_emptyWhenSourceHasNone() {
        RulItemTypeExt src = bareItemType(13, "NO_SPECS", "ENUM");
        // The mapper does not call setSpecs; the generated DTO initializes
        // specs to an empty list by default, so consumers see [] either way.
        assertThat(mapper.toItemType(src).getSpecs()).isEmpty();
    }

    @Test
    void toItemTypeSpec_copiesAllFields() {
        RulItemSpec src = new RulItemSpec();
        src.setItemSpecId(101);
        src.setCode("SPEC_A");
        src.setName("Spec A");
        src.setShortcut("SA");
        src.setDescription("Spec A description");

        ItemTypeSpec out = mapper.toItemTypeSpec(src);

        assertEquals(101, out.getId());
        assertEquals("SPEC_A", out.getCode());
        assertEquals("Spec A", out.getName());
        assertEquals("SA", out.getShortcut());
        assertEquals("Spec A description", out.getDescription());
    }

    // ---------- dataType mapping -----------------------------------------------

    /**
     * Exhaustive check that every value of the domain enum maps cleanly to
     * the public {@link DataType} enum. Adding a value to one side without
     * the other will break this test.
     */
    @Test
    void dataType_allDomainValuesMapToPublicEnum() {
        Set<String> publicNames = new LinkedHashSet<>();
        for (DataType d : DataType.values()) {
            publicNames.add(d.name());
        }
        Set<String> domainNames = new LinkedHashSet<>();
        for (cz.tacr.elza.core.data.DataType d : cz.tacr.elza.core.data.DataType.values()) {
            domainNames.add(d.name());
        }
        assertEquals(domainNames, publicNames,
                "Public DataType (OpenAPI) and domain DataType enums must declare the same values.");
    }

    @Test
    void dataType_unknownCodeThrowsIllegalState() {
        try {
            RulesMapper.toDataType("NOT_A_REAL_TYPE");
            org.junit.jupiter.api.Assertions.fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessageContaining("NOT_A_REAL_TYPE");
        }
    }

    // ---------- helpers --------------------------------------------------------

    private static RulItemTypeExt bareItemType(int id, String code, String dataTypeCode) {
        RulItemType base = new RulItemType();
        base.setItemTypeId(id);
        base.setCode(code);
        base.setName(code);
        base.setShortcut(code);
        base.setDescription("");
        base.setDataType(rulDataType(dataTypeCode));
        base.setCanBeOrdered(Boolean.FALSE);
        // empty spec list
        return new RulItemTypeExt(base, new ArrayList<>());
    }

    private static RulItemTypeExt stringItemType(int id, String code,
            cz.tacr.elza.domain.viewDefinition.StringViewDefinition viewDef) {
        RulItemTypeExt out = bareItemType(id, code, "STRING");
        if (viewDef != null) {
            out.setViewDefinition(viewDef);
        }
        return out;
    }

    private static RulItemTypeExt intItemType(String code, DisplayType displayType) {
        RulItemTypeExt out = bareItemType(100, code, "INT");
        if (displayType != null) {
            out.setViewDefinition(displayType);
        }
        return out;
    }

    private static RulItemTypeExt jsonTableItemType(String code, List<ElzaColumn> columns) {
        RulItemTypeExt out = bareItemType(101, code, "JSON_TABLE");
        out.setViewDefinition(columns);
        return out;
    }

    private static RulDataType rulDataType(String code) {
        RulDataType dt = new RulDataType();
        dt.setCode(code);
        return dt;
    }

    private static cz.tacr.elza.domain.RulStructuredType rulStructuredType(int id) {
        cz.tacr.elza.domain.RulStructuredType st = new cz.tacr.elza.domain.RulStructuredType();
        st.setStructuredTypeId(id);
        return st;
    }
}
