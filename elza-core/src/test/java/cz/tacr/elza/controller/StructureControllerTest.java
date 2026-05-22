package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.repository.SobjVrequestRepository;
import cz.tacr.elza.test.controller.vo.DataInteger;
import cz.tacr.elza.test.controller.vo.DataString;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.SdoBatchUpdateParam;
import cz.tacr.elza.test.controller.vo.SdoCopyObjectParam;
import cz.tacr.elza.test.controller.vo.SdoExtensionFund;
import cz.tacr.elza.test.controller.vo.SdoFindResult;
import cz.tacr.elza.test.controller.vo.SdoItemResult;
import cz.tacr.elza.test.controller.vo.SdoType;
import cz.tacr.elza.test.controller.vo.StructuredObject;
import cz.tacr.elza.test.controller.vo.StructuredObject.StateEnum;
import cz.tacr.elza.test.controller.vo.StructuredObjectItem;
import cz.tacr.elza.test.controller.vo.StructuredObjectItems;

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

        structureTypesAndExtensions(fund);
        structureDataTest(fundVersion, fund);
        structureItemTest(fund);

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

        // create object
        StructuredObject structureData = structureApi.sdoCreateObject(fund.getId(), STRUCTURE_TYPE_CODE, null);

        // vytvoření hodnot
        createStructureItemPacketNumber(fund, structureData);
        createStructureItemPacketPrefix(fund, structureData);
        createStructureItemPacketPostfix(fund, structureData);
        createStructureItemPacketType(fund, structureData);

        RulDescItemTypeExtVO typePostfix = findDescItemTypeByCode("SRD_PACKET_POSTFIX");
        RulDescItemTypeExtVO typeNumber = findDescItemTypeByCode("SRD_PACKET_NUMBER");
        List<Integer> itemTypeIds = Collections.singletonList(typeNumber.getId());

        SdoCopyObjectParam copyObjectParam = new SdoCopyObjectParam();
        copyObjectParam.setCount(BATCH_COUNT);
        copyObjectParam.setIncrementedTypeIds(itemTypeIds);

        structureApi.sdoCopyObject(fund.getId(), structureData.getId(), copyObjectParam);

        SdoFindResult structureDataResult = structureApi.sdoFindStructObj(fund.getId(), STRUCTURE_TYPE_CODE, null, null, null, null, null);
        assertEquals(BATCH_COUNT, structureDataResult.getCount());
        assertEquals(BATCH_COUNT, structureDataResult.getRows().size());

        StructuredObjectItems structureDataForm = structureApi.sdoGetFormStructureItems(fund.getId(), structureData.getId(), null);
        structureDataForm.getItems().forEach(it -> {
            if (it.getItemTypeId().equals(typeNumber.getId())) {
                ((DataInteger) it.getData()).setIntegerValue(BATCH_COUNT + 1);
            }
        });

        SdoBatchUpdateParam data = new SdoBatchUpdateParam();
        data.setIds(structureDataResult.getRows().stream().map(sd -> sd.getId()).collect(Collectors.toList()));
        data.setDeleteItemTypeIds(Collections.singletonList(typePostfix.getId()));
        data.setItems(structureDataForm.getItems());
        data.setAutoincrementItemTypeIds(Collections.singletonList(typeNumber.getId()));
        structureApi.sdoUpdateObjects(fund.getId(), STRUCTURE_TYPE_CODE, data);

        // wait to process whole queue
        while (sobjVrequestRepository.count() > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }

    private void structureItemTest(final Fund fund) {
        StructuredObject structureObject = structureApi.sdoCreateObject(fund.getId(), STRUCTURE_TYPE_CODE, null);

        // vytvoření hodnoty
        SdoItemResult siNumberCreated = createStructureItemPacketNumber(fund, structureObject);
        StructuredObjectItem createdNumber = siNumberCreated.getItem();

        // aktualizace hodnoty
        ((DataInteger) createdNumber.getData()).setIntegerValue(NUMBER_VALUE_2);
        SdoItemResult siNumberUpdated = structureApi.sdoUpdateItem(fund.getId(), structureObject.getId(), true, createdNumber);
        StructuredObjectItem updatedNumber = siNumberUpdated.getItem();
        assertEquals(NUMBER_VALUE_2, ((DataInteger) updatedNumber.getData()).getIntegerValue());

        // vytvoření hodnoty
        SdoItemResult siPrefixCreated = createStructureItemPacketPrefix(fund, structureObject);
        StructuredObjectItem typePrefix = siPrefixCreated.getItem();

        // vytvoření hodnoty
        SdoItemResult siPostfixCreated = createStructureItemPacketPostfix(fund, structureObject);
        StructuredObjectItem typePostfix = siPostfixCreated.getItem();

        // vytvoření hodnoty
        SdoItemResult siPackedTypeCreated = createStructureItemPacketType(fund, structureObject);
        StructuredObjectItem typePacketType = siPackedTypeCreated.getItem();

        StructuredObjectItems structuteObjectItems = structureApi.sdoGetFormStructureItems(fund.getId(), structureObject.getId(), null);
        assertEquals(5, structuteObjectItems.getItemTypes().size());

        // smazání hodnoty
        structureApi.sdoDeleteItem(fund.getId(), structureObject.getId(), updatedNumber.getItemObjectId());

        structureApi.sdoDeleteItemsByType(fund.getId(), structureObject.getId(), typePrefix.getId());
        structureApi.sdoDeleteItemsByType(fund.getId(), structureObject.getId(), typePostfix.getId());
        structureApi.sdoDeleteItemsByType(fund.getId(), structureObject.getId(), typePacketType.getId());
    }

    // SRD_PACKET_NUMBER
    private SdoItemResult createStructureItemPacketNumber(final Fund fund, final StructuredObject structureData) {
    	RulDescItemTypeExtVO typeNumber = findDescItemTypeByCode("SRD_PACKET_NUMBER");
        StructuredObjectItem itemNumber = buildStructuredObjectItem(typeNumber.getCode(), null, NUMBER_VALUE_1, null, null, null);
        SdoItemResult createdNumber = structureApi.sdoCreateItem(fund.getId(), 
        		                                                 structureData.getId(),
      		                                                     itemNumber);
        assertNotNull(createdNumber);
    	assertEquals(NUMBER_VALUE_1, ((DataInteger) createdNumber.getItem().getData()).getIntegerValue());
    	return createdNumber;
    }

    // SRD_PACKET_PREFIX
    private SdoItemResult createStructureItemPacketPrefix(final Fund fund, final StructuredObject structureData) {
    	RulDescItemTypeExtVO typePrefix = findDescItemTypeByCode("SRD_PACKET_PREFIX");
    	StructuredObjectItem itemPrefix = buildStructuredObjectItem(typePrefix.getCode(), null, PREFIX_VALUE, null, null, null);
    	SdoItemResult siPrefixCreated = structureApi.sdoCreateItem(fund.getId(),
                                                                   structureData.getId(),
                                                                   itemPrefix);
    	assertEquals(PREFIX_VALUE, ((DataString) siPrefixCreated.getItem().getData()).getStringValue());
    	return siPrefixCreated;
    }

    // SRD_PACKET_POSTFIX
    private SdoItemResult createStructureItemPacketPostfix(final Fund fund, final StructuredObject structureData) {
    	RulDescItemTypeExtVO typePostfix = findDescItemTypeByCode("SRD_PACKET_POSTFIX");
    	StructuredObjectItem itemPostfix = buildStructuredObjectItem(typePostfix.getCode(), null, POSTFIX_VALUE, null, null, null);
    	SdoItemResult siPostfixCreated = structureApi.sdoCreateItem(fund.getId(),
                                                                    structureData.getId(),
                                                                    itemPostfix);
    	assertEquals(POSTFIX_VALUE, ((DataString) siPostfixCreated.getItem().getData()).getStringValue());
    	return siPostfixCreated;
    }

    // SRD_PACKET_TYPE
    private SdoItemResult createStructureItemPacketType(final Fund fund, final StructuredObject structureData) {
        RulDescItemTypeExtVO typePacketType = findDescItemTypeByCode("SRD_PACKET_TYPE");
        StructuredObjectItem itemPacketType = buildStructuredObjectItem(typePacketType.getCode(), "SRD_PACKET_TYPE_BOX", (Object) null, null, null, null);
        return structureApi.sdoCreateItem(fund.getId(), structureData.getId(), itemPacketType);
    }

    /**
     * Check existence of structure type and extensions
     *
     * @param fundVersion
     */
    private void structureTypesAndExtensions(final Fund fund) {
        // find structure types
        List<SdoType> structureTypes = structureApi.sdoFindStructureTypes(null);
        assertNotNull(structureTypes);
        assertEquals(11, structureTypes.size()); // SRD_PACKET, STAT_ZASTUPCE, SRD_*

        // check name and id
        SdoType structureType = structureTypes.stream()
                .filter(st -> st.getCode().equals(STRUCTURE_TYPE_CODE))
                .findFirst()
                .get();
        assertEquals(STRUCTURE_TYPE_CODE, structureType.getCode());
        assertNotNull(structureType.getId());
        assertNotNull(structureType.getName());

        // check extensions
        List<SdoExtensionFund> fundStructureExtension = structureApi.sdoFindFundStructureExtension(fund.getId(), STRUCTURE_TYPE_CODE, null);
        assertNotNull(fundStructureExtension);
        assertEquals(1, fundStructureExtension.size());

        SdoExtensionFund structureExtensionFund = fundStructureExtension.get(0);
        assertNotNull(structureExtensionFund.getId());
        assertNotNull(structureExtensionFund.getName());
        assertNotNull(structureExtensionFund.getCode());
        assertFalse(structureExtensionFund.getActive());

        structureApi.sdoSetFundStructureExtensions(fund.getId(), STRUCTURE_TYPE_CODE, Collections.singletonList(STRUCTURE_EXTENSION_CODE));
        fundStructureExtension = structureApi.sdoFindFundStructureExtension(fund.getId(), STRUCTURE_TYPE_CODE, null);
        structureExtensionFund = fundStructureExtension.get(0);
        assertTrue(structureExtensionFund.getActive());

        structureApi.sdoSetFundStructureExtensions(fund.getId(), STRUCTURE_TYPE_CODE, Collections.emptyList());
        fundStructureExtension = structureApi.sdoFindFundStructureExtension(fund.getId(), STRUCTURE_TYPE_CODE, null);
        structureExtensionFund = fundStructureExtension.get(0);
        assertFalse(structureExtensionFund.getActive());
    }

    private void structureDataTest(final ArrFundVersionVO fundVersion, final Fund fund) {
        // create data type
        StructuredObject structureObject = structureApi.sdoCreateObject(fund.getId(), STRUCTURE_TYPE_CODE, null);
        assertNotNull(structureObject);
        assertNotNull(structureObject.getId());
        assertNotNull(structureObject.getAssignable());
        assertSame(structureObject.getState(), StateEnum.TEMP);

        // add item
        createStructureItemPacketNumber(fund, structureObject);

        StructuredObject confirmedStructureObject = structureApi.sdoConfirm(fund.getId(), structureObject.getId());
        // check returned object
        assertEquals(structureObject.getId(), confirmedStructureObject.getId());
        assertEquals(confirmedStructureObject.getState(), StateEnum.OK);
        assertTrue(StringUtils.isEmpty(confirmedStructureObject.getErrorDescription()));

        // wait to process whole queue
        while (sobjVrequestRepository.count() > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
        structureObject = structureApi.sdoGetObject(fund.getId(), structureObject.getId(), null);
        assertSame(structureObject.getState(), StateEnum.OK);
        assertTrue(StringUtils.isNotEmpty(structureObject.getValue()));

        SdoFindResult findResult1 = structureApi.sdoFindStructObj(fund.getId(), STRUCTURE_TYPE_CODE, null, null, null, null, null);
        assertEquals(1, findResult1.getCount());
        assertEquals(1, findResult1.getRows().size());

        structureApi.sdoSetDataAssignable(fund.getId(), false, Collections.singletonList(structureObject.getId()));

        SdoFindResult findResult2 = structureApi.sdoFindStructObj(fund.getId(), STRUCTURE_TYPE_CODE, null, false, null, null, null);
        assertEquals(1, findResult2.getCount());
        assertEquals(1, findResult2.getRows().size());

        SdoFindResult findResult3 = structureApi.sdoFindStructObj(fund.getId(), STRUCTURE_TYPE_CODE, null, true, null, null, null);
        assertEquals(0, findResult3.getCount());
        assertEquals(0, findResult3.getRows().size());

        // delete
        structureApi.sdoDeleteObjects(fund.getId(), List.of(structureObject.getId()), null);

        SdoFindResult findResult4 = structureApi.sdoFindStructObj(fund.getId(), STRUCTURE_TYPE_CODE, null, null, null, null, null);
        assertEquals(0, findResult4.getCount());
        assertEquals(0, findResult4.getRows().size());
    }
}
