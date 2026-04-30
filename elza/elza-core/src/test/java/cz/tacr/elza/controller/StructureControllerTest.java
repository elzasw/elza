package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.RulStructureTypeVO;
import cz.tacr.elza.controller.vo.StructureExtensionFundVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemIntVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.repository.SobjVrequestRepository;
import cz.tacr.elza.test.controller.vo.DataInteger;
import cz.tacr.elza.test.controller.vo.DataString;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.NodeItem;
import cz.tacr.elza.test.controller.vo.StructureItemResult;

/**
 * Test pro {@link StructureOldController} &&  {@link StructureController}.
 */
public class StructureControllerTest extends AbstractControllerTest {

    private static final int BATCH_COUNT = 100;
    private static final String NAME_AS = "Test AS1";
    private static final String CODE_AS = "TST1";
    private static final String STRUCTURE_TYPE_CODE = "SRD_PACKET";
    private static final String STRUCTURE_EXTENSION_CODE = "SRD_PACKET_MZABrno";

    private static final Integer NUMBER_VALUE_1 = 1;
    private static final Integer NUMBER_VALUE_2 = 2;

    private static final String PREFIX_VALUE = "AA_";
    private static final String POSTFIX_VALUE = "r";

    @Autowired
    protected SobjVrequestRepository sobjVrequestRepository;

    @Test
    public void structureTest() {
        Fund fund = createFund(NAME_AS, CODE_AS);
        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        structureTypesAndExtensions(fundVersion);
        structureDataTest(fundVersion);
        structureItemTest(fundVersion);

        // wait to process whole queue
        while (sobjVrequestRepository.count() > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }

    @Test
    public void structureBatchTest() {
        Fund fund = createFund(NAME_AS, CODE_AS);
        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        ArrStructureDataVO structureData = createStructureData(fundVersion);

        // vytvoření hodnot
        createStructureItemPacketNumber(fundVersion, structureData);
        createStructureItemPacketPrefix(fundVersion, structureData);
        createStructureItemPacketPostfix(fundVersion, structureData);
        createStructureItemPacketType(fundVersion, structureData);

        RulDescItemTypeExtVO typePostfix = findDescItemTypeByCode("SRD_PACKET_POSTFIX");
        RulDescItemTypeExtVO typeNumber = findDescItemTypeByCode("SRD_PACKET_NUMBER");
        List<Integer> itemTypeIds = Collections.singletonList(typeNumber.getId());

        duplicateStructureDataBatch(fundVersion.getId(), structureData.getId(), BATCH_COUNT, itemTypeIds);

        FilteredResultVO<ArrStructureDataVO> structureDataResult = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, null, null, null);
        assertEquals(BATCH_COUNT, structureDataResult.getCount());
        assertEquals(BATCH_COUNT, structureDataResult.getRows().size());

        StructureOldController.StructureDataFormDataVO structureDataForm = getFormStructureItems(fundVersion.getId(),
                                                                                              structureData.getId());

        Map<Integer, List<ArrItemVO>> items = structureDataForm.getDescItems().stream().peek(it -> {
            if (it.getItemTypeId().equals(typeNumber.getId())) {
                ((ArrItemIntVO) it).setValue(BATCH_COUNT + 1);
            }
        }).collect(Collectors.groupingBy(ArrItemVO::getItemTypeId));

        StructureOldController.StructureDataBatchUpdate data = new StructureOldController.StructureDataBatchUpdate();
        data.setStructureDataIds(structureDataResult.getRows().stream().map(sd -> sd.getId())
                .collect(Collectors.toList()));
        data.setDeleteItemTypeIds(Collections.singletonList(typePostfix.getId()));
        data.setItems(items);
        data.setAutoincrementItemTypeIds(Collections.singletonList(typeNumber.getId()));
        updateStructureDataBatch(fundVersion.getId(), STRUCTURE_TYPE_CODE, data);

        // wait to process whole queue
        while (sobjVrequestRepository.count() > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }

    private void structureItemTest(final ArrFundVersionVO fundVersion) {
        ArrStructureDataVO structureData = createStructureData(fundVersion);

        // vytvoření hodnoty
        RulDescItemTypeExtVO typeNumber = findDescItemTypeByCode("SRD_PACKET_NUMBER");
        NodeItem itemNumber = buildNodeItem(typeNumber.getCode(), null, NUMBER_VALUE_1, null, null, null);
        StructureItemResult siNumberCreated = structureApi.structureCreateItem(fundVersion.getId(), 
        		                                                               structureData.getId(),
        		                                                               typeNumber.getId(),
        		                                                               itemNumber);
        NodeItem createdNumber = siNumberCreated.getItem();
        assertNotNull(createdNumber);
        assertEquals(NUMBER_VALUE_1, ((DataInteger) createdNumber.getData()).getIntegerValue());

        // aktualizace hodnoty
        ((DataInteger) createdNumber.getData()).setIntegerValue(NUMBER_VALUE_2);
        StructureItemResult siNumberUpdated = structureApi.structureUpdateItem(fundVersion.getId(), true, createdNumber);
        NodeItem updatedNumber = siNumberUpdated.getItem();
        assertEquals(NUMBER_VALUE_2, ((DataInteger) updatedNumber.getData()).getIntegerValue());

        // vytvoření hodnoty
        RulDescItemTypeExtVO typePrefix = findDescItemTypeByCode("SRD_PACKET_PREFIX");
        NodeItem itemPrefix = buildNodeItem(typePrefix.getCode(), null, PREFIX_VALUE, null, null, null);
        StructureItemResult siPrefixCreated = structureApi.structureCreateItem(fundVersion.getId(),
                                                                               structureData.getId(),
                                                                               typePrefix.getId(),
                                                                               itemPrefix);
        assertEquals(PREFIX_VALUE, ((DataString) siPrefixCreated.getItem().getData()).getStringValue());

        // vytvoření hodnoty
        RulDescItemTypeExtVO typePostfix = findDescItemTypeByCode("SRD_PACKET_POSTFIX");
        NodeItem itemPostfix = buildNodeItem(typePostfix.getCode(), null, POSTFIX_VALUE, null, null, null);
        StructureItemResult siPostfixCreated = structureApi.structureCreateItem(fundVersion.getId(),
                                                                                structureData.getId(),
                                                                                typePostfix.getId(),
                                                                                itemPostfix);
        assertEquals(POSTFIX_VALUE, ((DataString) siPostfixCreated.getItem().getData()).getStringValue());

        // vytvoření hodnoty
        RulDescItemTypeExtVO typePacketType = findDescItemTypeByCode("SRD_PACKET_TYPE");
        NodeItem itemPacketType = buildNodeItem(typePacketType.getCode(), "SRD_PACKET_TYPE_BOX", (Object) null, null, null, null);
        structureApi.structureCreateItem(fundVersion.getId(),
                                         structureData.getId(),
                                         typePacketType.getId(),
                                         itemPacketType);

        StructureOldController.StructureDataFormDataVO formStructureItems = getFormStructureItems(fundVersion.getId(), structureData.getId());
        assertEquals(5, formStructureItems.getItemTypes().size());

        // smazání hodnoty
        StructureItemResult siNumberDeleted = structureApi.structureDeleteItem(fundVersion.getId(), updatedNumber);
        assertNotNull(siNumberDeleted);
        assertNotNull(siNumberDeleted.getItem());

        structureApi.structureDeleteItemsByType(fundVersion.getId(), structureData.getId(), typePrefix.getId());
        structureApi.structureDeleteItemsByType(fundVersion.getId(), structureData.getId(), typePostfix.getId());
        structureApi.structureDeleteItemsByType(fundVersion.getId(), structureData.getId(), typePacketType.getId());

        getFormStructureItems(fundVersion.getId(), structureData.getId());

    }

    private StructureItemResult createStructureItemPacketType(final ArrFundVersionVO fundVersion, final ArrStructureDataVO structureData) {
        RulDescItemTypeExtVO typePacketType = findDescItemTypeByCode("SRD_PACKET_TYPE");
        NodeItem itemPacketType = buildNodeItem(typePacketType.getCode(), "SRD_PACKET_TYPE_BOX", (Object) null, null, null, null);
        return structureApi.structureCreateItem(fundVersion.getId(), structureData.getId(), typePacketType.getId(), itemPacketType);
    }

    private StructureItemResult createStructureItemPacketPostfix(final ArrFundVersionVO fundVersion, final ArrStructureDataVO structureData) {
    	RulDescItemTypeExtVO typePostfix = findDescItemTypeByCode("SRD_PACKET_POSTFIX");
    	NodeItem itemPostfix = buildNodeItem(typePostfix.getCode(), null, POSTFIX_VALUE, null, null, null);
    	StructureItemResult siPostfixCreated = structureApi.structureCreateItem(fundVersion.getId(),
                                                                                structureData.getId(),
                                                                                typePostfix.getId(),
                                                                                itemPostfix);
    	assertEquals(POSTFIX_VALUE, ((DataString) siPostfixCreated.getItem().getData()).getStringValue());
    	return siPostfixCreated;
    }

    private StructureItemResult createStructureItemPacketPrefix(final ArrFundVersionVO fundVersion, final ArrStructureDataVO structureData) {
    	RulDescItemTypeExtVO typePrefix = findDescItemTypeByCode("SRD_PACKET_PREFIX");
    	NodeItem itemPrefix = buildNodeItem(typePrefix.getCode(), null, PREFIX_VALUE, null, null, null);
    	StructureItemResult siPrefixCreated = structureApi.structureCreateItem(fundVersion.getId(),
                                                                               structureData.getId(),
                                                                               typePrefix.getId(),
                                                                               itemPrefix);
    	assertEquals(PREFIX_VALUE, ((DataString) siPrefixCreated.getItem().getData()).getStringValue());
    	return siPrefixCreated;
    }

    private StructureItemResult createStructureItemPacketNumber(final ArrFundVersionVO fundVersion, final ArrStructureDataVO structureData) {
    	RulDescItemTypeExtVO typeNumber = findDescItemTypeByCode("SRD_PACKET_NUMBER");
    	NodeItem itemNumber = buildNodeItem(typeNumber.getCode(), null, NUMBER_VALUE_1, null, null, null);
    	StructureItemResult siNumberCreated = structureApi.structureCreateItem(fundVersion.getId(),
                                                                               structureData.getId(),
                                                                               typeNumber.getId(),
                                                                               itemNumber);
    	assertEquals(NUMBER_VALUE_1, ((DataInteger) siNumberCreated.getItem().getData()).getIntegerValue());
    	return siNumberCreated;
    }

    /**
     * Check existence of structure type and extensions
     *
     * @param fundVersion
     */
    private void structureTypesAndExtensions(final ArrFundVersionVO fundVersion) {
        // find structure types
        List<RulStructureTypeVO> structureTypes = findStructureTypes();
        assertNotNull(structureTypes);
        assertEquals(11, structureTypes.size()); // SRD_PACKET, STAT_ZASTUPCE, SRD_*

        // check name and id
        RulStructureTypeVO structureType = structureTypes.stream()
                .filter(
                        st -> st.getCode().equals(STRUCTURE_TYPE_CODE))
                .findFirst().get();
        assertEquals(STRUCTURE_TYPE_CODE, structureType.getCode());
        assertNotNull(structureType.getId());
        assertNotNull(structureType.getName());

        // check extensions
        List<StructureExtensionFundVO> fundStructureExtension = findFundStructureExtension(fundVersion.getId(), STRUCTURE_TYPE_CODE);
        assertNotNull(fundStructureExtension);
        assertEquals(1, fundStructureExtension.size());

        StructureExtensionFundVO structureExtensionFund = fundStructureExtension.get(0);
        assertNotNull(structureExtensionFund.getId());
        assertNotNull(structureExtensionFund.getName());
        assertNotNull(structureExtensionFund.getCode());
        assertFalse(structureExtensionFund.getActive());

        setFundStructureExtensions(fundVersion.getId(), STRUCTURE_TYPE_CODE, Collections.singletonList(STRUCTURE_EXTENSION_CODE));
        fundStructureExtension = findFundStructureExtension(fundVersion.getId(), STRUCTURE_TYPE_CODE);
        structureExtensionFund = fundStructureExtension.get(0);
        assertTrue(structureExtensionFund.getActive());

        setFundStructureExtensions(fundVersion.getId(), STRUCTURE_TYPE_CODE, Collections.emptyList());
        fundStructureExtension = findFundStructureExtension(fundVersion.getId(), STRUCTURE_TYPE_CODE);
        structureExtensionFund = fundStructureExtension.get(0);
        assertFalse(structureExtensionFund.getActive());
    }

    private void structureDataTest(final ArrFundVersionVO fundVersion) {
        // create data type
        ArrStructureDataVO structureData = createStructureData(fundVersion);
        assertNotNull(structureData);
        assertNotNull(structureData.getId());
        assertNotNull(structureData.getAssignable());
        assertSame(structureData.getState(), ArrStructuredObject.State.TEMP);

        // add item
        createStructureItemPacketNumber(fundVersion, structureData);

        ArrStructureDataVO structureDataConfirmed = confirmStructureData(fundVersion.getId(), structureData.getId());
        // check id of returned type
        assertTrue(Objects.equals(structureDataConfirmed.getId(), structureDataConfirmed.getId()));

        // wait to process whole queue
        while (sobjVrequestRepository.count() > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
        ArrStructureDataVO structureDataGet = getStructureData(fundVersion.getId(), structureData.getId());
        assertSame(structureDataGet.getState(), ArrStructuredObject.State.OK);
        assertTrue(StringUtils.isNotEmpty(structureDataGet.getValue()));
        assertTrue(StringUtils.isEmpty(structureDataConfirmed.getErrorDescription()));

        FilteredResultVO<ArrStructureDataVO> structureDataResult1 = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, null, null, null);
        assertEquals(1, structureDataResult1.getCount());
        assertEquals(1, structureDataResult1.getRows().size());

        setAssignableStructureData(fundVersion.getId(), false, Collections.singletonList(structureData.getId()));

        FilteredResultVO<ArrStructureDataVO> structureDataResult2 = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, false, null, null);
        assertEquals(1, structureDataResult2.getCount());
        assertEquals(1, structureDataResult2.getRows().size());

        FilteredResultVO<ArrStructureDataVO> structureDataResult3 = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, true, null, null);
        assertEquals(0, structureDataResult3.getCount());
        assertEquals(0, structureDataResult3.getRows().size());

        List<Integer> structureDataDeletedIds = deleteStructureData(fundVersion.getId(), Collections.singletonList(structureData.getId()));
        assertNotNull(structureDataDeletedIds.size() == 1);

        FilteredResultVO<ArrStructureDataVO> structureDataResult4 = findStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId(), null, null, null, null);
        assertEquals(0, structureDataResult4.getCount());
        assertEquals(0, structureDataResult4.getRows().size());
    }

    private ArrStructureDataVO createStructureData(final ArrFundVersionVO fundVersion) {
        return createStructureData(STRUCTURE_TYPE_CODE, fundVersion.getId());
    }

}
