package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.RulStructureTypeVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeDescItemsLiteVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemIntVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ItemGroupVO;
import cz.tacr.elza.domain.ArrStructureData;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;


/**
 * Test pro {@link StructureController}.
 */
public class StructureControllerTest extends AbstractControllerTest {

    private static final int BATCH_COUNT = 100;
    private static final String NAME_AS = "Test AS1";
    private static final String CODE_AS = "TST1";
    private static final String STRUCTURE_TYPE_CODE = "ZP2015_OBAL";
    private static final String STRUCTURE_EXTENSION_CODE = "ZP2015_ObalMZABrno";

    private static final Integer NUMBER_VALUE_1 = 1;
    private static final Integer NUMBER_VALUE_2 = 2;

    private static final String PREFIX_VALUE = "AA_";
    private static final String POSTFIX_VALUE = "r";

    @Test
    public void structureTest() {
        ArrFundVO fund = createFund(NAME_AS, CODE_AS);
        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        structureTypesAndExtensions(fundVersion);
        structureDataTest(fundVersion);
        structureItemTest(fundVersion);
    }

    @Test
    public void structureBatchTest() {
        ArrFundVO fund = createFund(NAME_AS, CODE_AS);
        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        ArrStructureDataVO structureData = createStructureData(fundVersion);

        // vytvoření hodnot
        createStructureItemPacketNumber(fundVersion, structureData);
        createStructureItemPacketPrefix(fundVersion, structureData);
        createStructureItemPacketPostfix(fundVersion, structureData);
        createStructureItemPacketType(fundVersion, structureData);

        RulDescItemTypeExtVO typePostfix = findDescItemTypeByCode("ZP2015_PACKET_POSTFIX");
        RulDescItemTypeExtVO typeNumber = findDescItemTypeByCode("ZP2015_PACKET_NUMBER");
        List<Integer> itemTypeIds = Collections.singletonList(typeNumber.getId());

        duplicateStructureDataBatch(fundVersion.getId(), structureData.id, BATCH_COUNT, itemTypeIds);

        FilteredResultVO<ArrStructureDataVO> structureDataResult = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, null, null, null);
        assertEquals(BATCH_COUNT, structureDataResult.getCount());
        assertEquals(BATCH_COUNT, structureDataResult.getRows().size());

        StructureController.StructureDataFormDataVO structureDataForm = getFormStructureItems(fundVersion.getId(), structureData.id);

        ItemGroupVO group = structureDataForm.getGroups().get(0);
        List<ItemTypeDescItemsLiteVO> itemTypes = group.getTypes();
        Map<Integer, List<ArrItemVO>> items = itemTypes.stream()
                .peek(it -> {
                    if (it.getId().equals(typeNumber.getId())) {
                        it.getDescItems().forEach(i -> ((ArrItemIntVO) i).setValue(BATCH_COUNT + 1));
                    }
                })
                .filter(it -> !it.getId().equals(typePostfix.getId()))
                .collect(Collectors.toMap(ItemTypeDescItemsLiteVO::getId, ItemTypeDescItemsLiteVO::getDescItems));

        StructureController.StructureDataBatchUpdate data = new StructureController.StructureDataBatchUpdate();
        data.setStructureDataIds(structureDataResult.getRows().stream().map(sd -> sd.id).collect(Collectors.toList()));
        data.setDeleteItemTypeIds(Collections.singletonList(typePostfix.getId()));
        data.setItems(items);
        data.setAutoincrementItemTypeIds(Collections.singletonList(typeNumber.getId()));
        updateStructureDataBatch(fundVersion.getId(), STRUCTURE_TYPE_CODE, data);
    }

    private void structureItemTest(final ArrFundVersionVO fundVersion) {
        ArrStructureDataVO structureData = createStructureData(fundVersion);

        // vytvoření hodnoty
        RulDescItemTypeExtVO typeNumber = findDescItemTypeByCode("ZP2015_PACKET_NUMBER");
        ArrItemVO itemNumber = buildDescItem(typeNumber.getCode(), null, NUMBER_VALUE_1, null, null);
        StructureController.StructureItemResult siNumberCreated = createStructureItem(itemNumber, fundVersion.getId(), typeNumber.getId(), structureData.id);

        // aktualizace hodnoty
        ArrItemIntVO itemIntNumber = (ArrItemIntVO) siNumberCreated.getItem();
        itemIntNumber.setValue(NUMBER_VALUE_2);
        StructureController.StructureItemResult structureItemUpdated = updateStructureItem(itemIntNumber, fundVersion.getId(), true);
        ArrItemIntVO itemIntNumber2 = (ArrItemIntVO) structureItemUpdated.getItem();
        assertEquals(NUMBER_VALUE_2, itemIntNumber2.getValue());

        // vytvoření hodnoty
        RulDescItemTypeExtVO typePrefix = findDescItemTypeByCode("ZP2015_PACKET_PREFIX");
        ArrItemVO itemPrefix = buildDescItem(typePrefix.getCode(), null, PREFIX_VALUE, null, null);
        StructureController.StructureItemResult siPrefixCreated = createStructureItem(itemPrefix, fundVersion.getId(), typePrefix.getId(), structureData.id);
        ArrItemStringVO itemStringPrefix = (ArrItemStringVO) siPrefixCreated.getItem();
        assertEquals(PREFIX_VALUE, itemStringPrefix.getValue());

        // vytvoření hodnoty
        RulDescItemTypeExtVO typePostfix = findDescItemTypeByCode("ZP2015_PACKET_POSTFIX");
        ArrItemVO itemPostfix = buildDescItem(typePostfix.getCode(), null, POSTFIX_VALUE, null, null);
        StructureController.StructureItemResult siPostfixCreated = createStructureItem(itemPostfix, fundVersion.getId(), typePostfix.getId(), structureData.id);
        ArrItemStringVO itemStringPostfix = (ArrItemStringVO) siPostfixCreated.getItem();
        assertEquals(POSTFIX_VALUE, itemStringPostfix.getValue());

        // vytvoření hodnoty
        RulDescItemTypeExtVO typePacketType = findDescItemTypeByCode("ZP2015_PACKET_TYPE");
        ArrItemVO itemPacketType = buildDescItem(typePacketType.getCode(), "ZP2015_PACKET_TYPE_BOX", null, null, null);
        StructureController.StructureItemResult siPacketTypeCreated = createStructureItem(itemPacketType, fundVersion.getId(), typePacketType.getId(), structureData.id);

        StructureController.StructureDataFormDataVO formStructureItems = getFormStructureItems(fundVersion.getId(), structureData.id);

        List<ItemGroupVO> groups = formStructureItems.getGroups();
        assertEquals(groups.size(), 1);
        ItemGroupVO itemGroup = groups.get(0);
        assertEquals(itemGroup.getTypes().size(), 4);

        StructureController.StructureItemResult deleteStructureItem = deleteStructureItem(itemIntNumber2, fundVersion.getId());
        assertNotNull(deleteStructureItem);
        assertNotNull(deleteStructureItem.getItem());

        deleteStructureItemsByType(fundVersion.getId(), structureData.id, typePrefix.getId());
        deleteStructureItemsByType(fundVersion.getId(), structureData.id, typePostfix.getId());
        deleteStructureItemsByType(fundVersion.getId(), structureData.id, typePacketType.getId());

        formStructureItems = getFormStructureItems(fundVersion.getId(), structureData.id);

        groups = formStructureItems.getGroups();
        assertEquals(groups.size(), 0);

    }

    private StructureController.StructureItemResult createStructureItemPacketType(final ArrFundVersionVO fundVersion, final ArrStructureDataVO structureData) {
        RulDescItemTypeExtVO typePacketType = findDescItemTypeByCode("ZP2015_PACKET_TYPE");
        ArrItemVO itemPacketType = buildDescItem(typePacketType.getCode(), "ZP2015_PACKET_TYPE_BOX", null, null, null);
        return createStructureItem(itemPacketType, fundVersion.getId(), typePacketType.getId(), structureData.id);
    }

    private StructureController.StructureItemResult createStructureItemPacketPostfix(final ArrFundVersionVO fundVersion, final ArrStructureDataVO structureData) {
        RulDescItemTypeExtVO typePostfix = findDescItemTypeByCode("ZP2015_PACKET_POSTFIX");
        ArrItemVO itemPostfix = buildDescItem(typePostfix.getCode(), null, POSTFIX_VALUE, null, null);
        StructureController.StructureItemResult siPostfixCreated = createStructureItem(itemPostfix, fundVersion.getId(), typePostfix.getId(), structureData.id);
        ArrItemStringVO itemStringPostfix = (ArrItemStringVO) siPostfixCreated.getItem();
        assertEquals(POSTFIX_VALUE, itemStringPostfix.getValue());
        return siPostfixCreated;
    }

    private StructureController.StructureItemResult createStructureItemPacketPrefix(final ArrFundVersionVO fundVersion, final ArrStructureDataVO structureData) {
        RulDescItemTypeExtVO typePrefix = findDescItemTypeByCode("ZP2015_PACKET_PREFIX");
        ArrItemVO itemPrefix = buildDescItem(typePrefix.getCode(), null, PREFIX_VALUE, null, null);
        StructureController.StructureItemResult siPrefixCreated = createStructureItem(itemPrefix, fundVersion.getId(), typePrefix.getId(), structureData.id);
        ArrItemStringVO itemStringPrefix = (ArrItemStringVO) siPrefixCreated.getItem();
        assertEquals(PREFIX_VALUE, itemStringPrefix.getValue());
        return siPrefixCreated;
    }

    private StructureController.StructureItemResult createStructureItemPacketNumber(final ArrFundVersionVO fundVersion, final ArrStructureDataVO structureData) {
        RulDescItemTypeExtVO typeNumber = findDescItemTypeByCode("ZP2015_PACKET_NUMBER");
        ArrItemVO itemNumber = buildDescItem(typeNumber.getCode(), null, NUMBER_VALUE_1, null, null);
        StructureController.StructureItemResult structureItem = createStructureItem(itemNumber, fundVersion.getId(), typeNumber.getId(), structureData.id);
        ArrItemIntVO itemIntPrefix = (ArrItemIntVO) structureItem.getItem();
        assertEquals(NUMBER_VALUE_1, itemIntPrefix.getValue());
        return structureItem;
    }

    private void structureTypesAndExtensions(final ArrFundVersionVO fundVersion) {
        List<RulStructureTypeVO> structureTypes = findStructureTypes(fundVersion.getId());
        assertNotNull(structureTypes);
        assertEquals(1, structureTypes.size());

        RulStructureTypeVO structureType = structureTypes.get(0);
        assertTrue(STRUCTURE_TYPE_CODE.equals(structureType.code));
        assertNotNull(structureType.id);
        assertNotNull(structureType.name);

        List<StructureExtensionFundVO> fundStructureExtension = findFundStructureExtension(fundVersion.getId(), STRUCTURE_TYPE_CODE);
        assertNotNull(fundStructureExtension);
        assertEquals(1, fundStructureExtension.size());

        StructureExtensionFundVO structureExtensionFund = fundStructureExtension.get(0);
        assertNotNull(structureExtensionFund.id);
        assertNotNull(structureExtensionFund.name);
        assertNotNull(structureExtensionFund.code);
        assertFalse(structureExtensionFund.active);

        setFundStructureExtensions(fundVersion.getId(), STRUCTURE_TYPE_CODE, Collections.singletonList(STRUCTURE_EXTENSION_CODE));
        fundStructureExtension = findFundStructureExtension(fundVersion.getId(), STRUCTURE_TYPE_CODE);
        structureExtensionFund = fundStructureExtension.get(0);
        assertTrue(structureExtensionFund.active);

        setFundStructureExtensions(fundVersion.getId(), STRUCTURE_TYPE_CODE, Collections.emptyList());
        fundStructureExtension = findFundStructureExtension(fundVersion.getId(), STRUCTURE_TYPE_CODE);
        structureExtensionFund = fundStructureExtension.get(0);
        assertFalse(structureExtensionFund.active);
    }

    private void structureDataTest(final ArrFundVersionVO fundVersion) {
        ArrStructureDataVO structureData = createStructureData(fundVersion);
        assertNotNull(structureData);
        assertNotNull(structureData.id);
        assertNotNull(structureData.assignable);
        assertTrue(structureData.state == ArrStructureData.State.TEMP);

        ArrStructureDataVO structureDataConfirmed = confirmStructureData(fundVersion.getId(), structureData.id);
        assertTrue(Objects.equals(structureDataConfirmed.id, structureDataConfirmed.id));
        assertTrue(structureDataConfirmed.state == ArrStructureData.State.ERROR);
        assertNotNull(structureDataConfirmed.value);
        assertNotNull(structureDataConfirmed.errorDescription);

        ArrStructureDataVO structureDataGet = getStructureData(fundVersion.getId(), structureData.id);
        assertEquals(structureDataConfirmed, structureDataGet);

        FilteredResultVO<ArrStructureDataVO> structureDataResult1 = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, null, null, null);
        assertEquals(1, structureDataResult1.getCount());
        assertEquals(1, structureDataResult1.getRows().size());

        setAssignableStructureData(fundVersion.getId(), false, Collections.singletonList(structureData.id));

        FilteredResultVO<ArrStructureDataVO> structureDataResult2 = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, false, null, null);
        assertEquals(1, structureDataResult2.getCount());
        assertEquals(1, structureDataResult2.getRows().size());

        FilteredResultVO<ArrStructureDataVO> structureDataResult3 = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, true, null, null);
        assertEquals(0, structureDataResult3.getCount());
        assertEquals(0, structureDataResult3.getRows().size());

        ArrStructureDataVO structureDataDeleted = deleteStructureData(fundVersion.getId(), structureData.id);
        assertNotNull(structureDataDeleted);

        FilteredResultVO<ArrStructureDataVO> structureDataResult4 = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, null, null, null);
        assertEquals(0, structureDataResult4.getCount());
        assertEquals(0, structureDataResult4.getRows().size());
    }

    private ArrStructureDataVO createStructureData(final ArrFundVersionVO fundVersion) {
        return createStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId());
    }

}
