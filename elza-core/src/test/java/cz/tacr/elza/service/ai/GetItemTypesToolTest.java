package cz.tacr.elza.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.DataType;
import cz.tacr.elza.aiprovider.client.vo.ItemTypeDictionary;
import cz.tacr.elza.aiprovider.client.vo.ItemTypeInfo;
import cz.tacr.elza.core.data.PackageTexts;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.service.RuleService;

/**
 * Unit test for {@link GetItemTypesTool}: the dictionary resolves each item type's data
 * type by id, never through the {@code RulDataType} association of the cached entity copy.
 */
class GetItemTypesToolTest {

    private MockedStatic<cz.tacr.elza.core.data.DataType> dataTypes;
    private RuleService ruleService;
    private GetItemTypesTool tool;

    @BeforeEach
    void setUp() {
        // The id cache of the domain DataType is filled at application startup;
        // here fromId resolves the enum ordinal used by dataTypeRef.
        dataTypes = mockStatic(cz.tacr.elza.core.data.DataType.class, CALLS_REAL_METHODS);
        dataTypes.when(() -> cz.tacr.elza.core.data.DataType.fromId(anyInt()))
                .thenAnswer(inv -> cz.tacr.elza.core.data.DataType.values()[inv.<Integer>getArgument(0)]);

        ruleService = mock(RuleService.class);

        PackageTexts packageTexts = mock(PackageTexts.class);
        when(packageTexts.name(any(RulItemType.class))).thenAnswer(inv -> inv.<RulItemType>getArgument(0).getName());
        when(packageTexts.name(any(RulItemSpec.class))).thenAnswer(inv -> inv.<RulItemSpec>getArgument(0).getName());

        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any()))
                .thenAnswer(inv -> inv.<TransactionCallback<?>>getArgument(0).doInTransaction(null));

        tool = new GetItemTypesTool();
        ReflectionTestUtils.setField(tool, "ruleService", ruleService);
        ReflectionTestUtils.setField(tool, "packageTexts", packageTexts);
        ReflectionTestUtils.setField(tool, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(tool, "transactionTemplate", transactionTemplate);
    }

    @AfterEach
    void tearDown() {
        dataTypes.close();
    }

    @Test
    void dataTypeIsResolvedById() {
        RulItemTypeExt unitDate = new RulItemTypeExt(itemType(7, "ZP2015_UNIT_DATE", "Datace", "UNITDATE"), List.of());
        when(ruleService.getDescriptionItemTypesByRuleSet("ZP2015")).thenReturn(List.of(unitDate));

        ItemTypeDictionary out = (ItemTypeDictionary) tool.execute(Map.of("ruleSetCode", "ZP2015"), null);

        assertThat(out.getRuleSetCode()).isEqualTo("ZP2015");
        assertThat(out.getItemTypes()).singleElement().satisfies(info -> {
            assertThat(info.getCode()).isEqualTo("ZP2015_UNIT_DATE");
            assertThat(info.getName()).isEqualTo("Datace");
            assertThat(info.getDataType()).isEqualTo(DataType.UNITDATE);
        });
    }

    @Test
    void specsAreListedForEnumeratedTypes() {
        RulItemSpec spec = new RulItemSpec();
        spec.setCode("ZP2015_LEVEL_FOLDER");
        spec.setName("spis");
        RulItemTypeExt levelType = new RulItemTypeExt(itemType(8, "ZP2015_LEVEL_TYPE", "Úroveň popisu", "ENUM"),
                List.of(spec));
        when(ruleService.getDescriptionItemTypesByRuleSet("ZP2015")).thenReturn(List.of(levelType));

        ItemTypeDictionary out = (ItemTypeDictionary) tool.execute(Map.of("ruleSetCode", "ZP2015"), null);

        ItemTypeInfo info = out.getItemTypes().get(0);
        assertThat(info.getDataType()).isEqualTo(DataType.ENUM);
        assertThat(info.getSpecs()).singleElement().satisfies(s -> {
            assertThat(s.getCode()).isEqualTo("ZP2015_LEVEL_FOLDER");
            assertThat(s.getName()).isEqualTo("spis");
        });
    }

    @Test
    void missingRuleSetCodeIsRejected() {
        assertThatThrownBy(() -> tool.execute(Map.of(), null)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Item type whose data type reference carries only the id: reading the code
     * through the association instead of resolving the id fails.
     */
    private static RulItemType itemType(int id, String code, String name, String dataTypeCode) {
        RulDataType dataType = new RulDataType();
        dataType.setDataTypeId(cz.tacr.elza.core.data.DataType.valueOf(dataTypeCode).ordinal());
        RulItemType base = new RulItemType();
        base.setItemTypeId(id);
        base.setCode(code);
        base.setName(name);
        base.setDataType(dataType);
        return base;
    }
}
