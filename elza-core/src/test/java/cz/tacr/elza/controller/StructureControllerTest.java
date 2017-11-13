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

    private final Integer CREATE_VALUE = 1;
    private final Integer UPDATE_VALUE = 2;

    private final String CREATE_VALUE2 = "popisek";

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
        RulDescItemTypeExtVO type = findDescItemTypeByCode("ZP2015_PACKET_NUMBER");
        ArrItemVO itemVO = buildDescItem(type.getCode(), null, CREATE_VALUE, null, null);
        StructureController.StructureItemResult structureItemCreated = createStructureItem(itemVO, fundVersion.getId(), type.getId(), structureData.id);

        ArrItemIntVO itemIntVO = (ArrItemIntVO) structureItemCreated.getItem();
        itemIntVO.setValue(UPDATE_VALUE);

        StructureController.StructureItemResult structureItemUpdated = updateStructureItem(itemIntVO, fundVersion.getId(), true);
        ArrItemIntVO itemIntVO2 = (ArrItemIntVO) structureItemUpdated.getItem();
        assertEquals(UPDATE_VALUE, itemIntVO2.getValue());

        // vytvoření hodnoty
        RulDescItemTypeExtVO type2 = findDescItemTypeByCode("ZP2015_PACKET_PREFIX");
        ArrItemVO itemVO2 = buildDescItem(type2.getCode(), null, CREATE_VALUE2, null, null);
        StructureController.StructureItemResult structureItemCreated2 = createStructureItem(itemVO2, fundVersion.getId(), type2.getId(), structureData.id);
        ArrItemStringVO itemStringVO = (ArrItemStringVO) structureItemCreated2.getItem();
        assertEquals(CREATE_VALUE2, itemStringVO.getValue());

        StructureController.StructureDataFormDataVO formStructureItems = getFormStructureItems(fundVersion.getId(), structureData.id);

        List<ItemGroupVO> groups = formStructureItems.getGroups();
        assertEquals(groups.size(), 1);
        ItemGroupVO itemGroup = groups.get(0);
        assertEquals(itemGroup.getTypes().size(), 2);

        StructureController.StructureItemResult deleteStructureItem = deleteStructureItem(itemIntVO, fundVersion.getId());
        assertNotNull(deleteStructureItem);
        assertNotNull(deleteStructureItem.getItem());

        deleteStructureItemsByType(fundVersion.getId(), structureData.id, type2.getId());

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
        assertTrue(structureDataConfirmed.state == ArrStructureData.State.OK);
        assertNotNull(structureDataConfirmed.value);
        assertNull(structureDataConfirmed.errorDescription);

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
