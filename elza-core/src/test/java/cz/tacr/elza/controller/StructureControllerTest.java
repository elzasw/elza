package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.RulStructureTypeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemIntVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ItemGroupVO;
import cz.tacr.elza.domain.ArrStructureData;
import org.junit.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;


/**
 * Test pro {@link StructureController}.
 */
public class StructureControllerTest extends AbstractControllerTest {

    private final String NAME_AS = "Test AS1";
    private final String CODE_AS = "TST1";
    private final String STRUCTURE_TYPE_CODE = "ZP2015_OBAL";

    private final Integer NUMBER_VALUE_1 = 1;
    private final Integer NUMBER_VALUE_2 = 2;

    private final String PREFIX_VALUE = "AA_";
    private final String POSTFIX_VALUE = "r";

    @Test
    public void structureTest() {
        ArrFundVO fund = createFund(NAME_AS, CODE_AS);
        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        structureTypesAndExtensions(fundVersion);
        structureDataTest(fundVersion);
        structureItemTest(fundVersion);
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

    private void structureTypesAndExtensions(final ArrFundVersionVO fundVersion) {
        List<RulStructureTypeVO> structureTypes = findStructureTypes(fundVersion.getId());
        assertNotNull(structureTypes);
        assertEquals(1, structureTypes.size());

        RulStructureTypeVO structureType = structureTypes.get(0);
        assertTrue(STRUCTURE_TYPE_CODE.equals(structureType.code));
        assertNotNull(structureType.id);
        assertNotNull(structureType.name);

        List<StructureExtensionFundVO> fundStructureExtension = findFundStructureExtension(fundVersion.getId());
        assertNotNull(fundStructureExtension);
        assertEquals(1, fundStructureExtension.size());

        StructureExtensionFundVO structureExtensionFund = fundStructureExtension.get(0);
        assertNotNull(structureExtensionFund.id);
        assertNotNull(structureExtensionFund.name);
        assertNotNull(structureExtensionFund.code);
        assertFalse(structureExtensionFund.active);

        addFundStructureExtension(fundVersion.getId(), structureExtensionFund.code);
        fundStructureExtension = findFundStructureExtension(fundVersion.getId());
        assertNotNull(fundStructureExtension);
        assertEquals(1, fundStructureExtension.size());

        structureExtensionFund = fundStructureExtension.get(0);
        assertTrue(structureExtensionFund.active);

        deleteFundStructureExtension(fundVersion.getId(), structureExtensionFund.code);

        fundStructureExtension = findFundStructureExtension(fundVersion.getId());
        assertNotNull(fundStructureExtension);
        assertEquals(1, fundStructureExtension.size());

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

        FilteredResultVO<ArrStructureDataVO> structureDataResult1 = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, null, null, null);
        assertEquals(1, structureDataResult1.getCount());
        assertEquals(1, structureDataResult1.getRows().size());

        FilteredResultVO<ArrStructureDataVO> structureDataResult2 = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, false, null, null);
        assertEquals(0, structureDataResult2.getCount());
        assertEquals(0, structureDataResult2.getRows().size());

        ArrStructureDataVO structureDataDeleted = deleteStructureData(fundVersion.getId(), structureData.id);
        assertNotNull(structureDataDeleted);

        FilteredResultVO<ArrStructureDataVO> structureDataResult3 = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, null, null, null);
        assertEquals(0, structureDataResult3.getCount());
        assertEquals(0, structureDataResult3.getRows().size());
    }

    private ArrStructureDataVO createStructureData(final ArrFundVersionVO fundVersion) {
        return createStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId());
    }

}
