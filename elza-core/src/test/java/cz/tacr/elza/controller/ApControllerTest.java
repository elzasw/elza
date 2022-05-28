package cz.tacr.elza.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import cz.tacr.elza.controller.vo.ApAccessPointCreateVO;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ApPartVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.ApScopeWithConnectedVO;
import cz.tacr.elza.controller.vo.ApStateChangeVO;
import cz.tacr.elza.controller.vo.ApTypeVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.RulPartTypeVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.ap.ApStateVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemAccessPointRefVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemStringVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;
import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.domain.RevStateApproval;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.Fund;


/**
 * Test method na APController
 * 
 */
public class ApControllerTest extends AbstractControllerTest {

    public static final String STRUCT_AP_TYPE = "PERSON_BEING_STRUCT";
    public static final String PT_NAME = "PT_NAME";
    public static final String PT_BODY = "PT_BODY";
    public static final String PT_REL = "PT_REL";
    public static final String NM_MAIN = "NM_MAIN";
    public static final String NM_SUP_GEN = "NM_SUP_GEN";
    public static final String GEO_ADMIN_CLASS = "GEO_ADMIN_CLASS";
    public static final String BRIEF_DESC = "BRIEF_DESC";
    public static final String REL_ENTITY = "REL_ENTITY";
    public static final String RT_BROTHER = "RT_BROTHER";
    private RulPartTypeVO ptName;
    private RulPartTypeVO ptRel;
    private RulPartTypeVO ptBody;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        preparePartType();
    }

    private void preparePartType() {
        Map<String, RulPartTypeVO> partTypes = findPartTypesMap();

        ptName = partTypes.get(PT_NAME);
        assertNotNull(ptName);
        ptRel = partTypes.get(PT_REL);
        assertNotNull(ptRel);
        ptBody = partTypes.get(PT_BODY);
        assertNotNull(ptBody);
    }

    @Test
    public void getRecordTypesTest() {
        getRecordTypes();
    }

    /**
     * Vrací všechny třídy rejstříků z databáze.
     */
    @Test
    public void getAllScopesTest() {
        getAllScopes();
    }

    /**
     * Pokud je nastavená verze, vrací třídy napojené na verzi, jinak vrací třídy nastavené v konfiguraci elzy (YAML).
     *
     * @param versionId id verze nebo null
     */
    public void getScopeIdsByVersionTest(final Integer versionId) {
        getScopeIdsByVersion(null);
        getScopeIdsByVersion(versionId);
    }

    @Test
    public void findAccessPoint() {
        List<ApAccessPointVO> records = findRecord(null, 0, 10, null, null, SearchType.FULLTEXT);
        assertNotNull(records);
    }


    /**
     * Testování vytvoření, upravení a smazání scope
     */
    @Test
    public void createUpdateDeleteScopesTest() {
        ApScopeVO scopeVO = createScopeTest();
        getScopeWithConnectedTest(scopeVO.getId());
        scopeVO.setName("Testing");
        scopeVO.setCode("ABCD");
        scopeVO = updateScopeTest(scopeVO);
        deleteScopeTest(scopeVO.getId());
    }

    /**
     * Testování provázání a zrušení provázání tříd.
     */
    @Test
    public void connectDisconnectScopesTest() {
        ApScopeVO scopeVO = createScopeTest();
        ApScopeVO scopeVO2 = createScopeTest();
        connectScopeTest(scopeVO.getId(), scopeVO2.getId());
        disconnectScopeTest(scopeVO.getId(), scopeVO2.getId());
        deleteScopeTest(scopeVO.getId());
        deleteScopeTest(scopeVO2.getId());
    }

    @Test
    public void testExternalIdTypes() {
        //TODO definovat externi typy, bylo jenom INTERPI
       /* Map<String, ApEidTypeVO> types = getAllExternalIdTypes();
        ApEidTypeVO eidType = types.get("DEFAULT");
        Assert.assertNotNull(eidType);
        Assert.assertNotNull(eidType.getId());
        Assert.assertNotNull(eidType.getCode());
        Assert.assertNotNull(eidType.getName());*/
    }

    @Test//(timeout = 60000)
    public void testAccessPoint() throws InterruptedException {
        ApTypeVO type = getApType(STRUCT_AP_TYPE);
        assertNotNull(type);

        List<ApScopeVO> scopes = getAllScopes();
        Integer scopeId = scopes.iterator().next().getId();

        List<ApItemVO> items = new ArrayList<>();
        RulItemType nmMainItemType = itemTypeRepository.findOneByCode(NM_MAIN);
        RulItemType nmSupGenItemType = itemTypeRepository.findOneByCode(NM_SUP_GEN);

        items.add(buildApItem(nmMainItemType.getCode(), null, "TEST", null, null));
        items.add(buildApItem(nmSupGenItemType.getCode(), null, "AP", null, null));

        ApAccessPointCreateVO ap = new ApAccessPointCreateVO();
        ap.setTypeId(type.getId());
        ap.setScopeId(scopeId);
        ap.setPartForm(createPartFormVO(null, ptName.getCode(), null, items));

        ApAccessPointVO accessPoint = createAccessPoint(ap);
        assertNotNull(accessPoint);
        List<ApPartVO> parts = accessPoint.getParts();
        assertNotNull(parts);
        ApPartVO preferredPart = findPreferredPart(accessPoint);
        assertNotNull(preferredPart);

        // add new part Karel IV
        items = new ArrayList<>();
        items.add(buildApItem(nmMainItemType.getCode(), null, "Karel", null, null));
        items.add(buildApItem(nmSupGenItemType.getCode(), null, "IV", null, null));

        ApPartFormVO partFormVO = createPartFormVO(null, ptName.getCode(), null, items);
        createPart(accessPoint.getId(), partFormVO);

        // check existence of two parts
        accessPoint = getAccessPoint(accessPoint.getId());
        Assert.assertEquals(2, accessPoint.getParts().size());

        // modify preferred part to Karel X
        items = new ArrayList<>(preferredPart.getItems());
        ApItemStringVO itemNmMain = (ApItemStringVO) items.get(0);
        itemNmMain.setValue("Karel");

        ApItemStringVO itemNmSupGen = (ApItemStringVO) items.get(1);
        itemNmSupGen.setValue("X");

        partFormVO = createPartFormVO(preferredPart.getId(), ptName.getCode(), null, items);
        updatePart(accessPoint.getId(), preferredPart.getId(), partFormVO);

        // check modified preferred part
        do {
            accessPoint = getAccessPoint(accessPoint.getId());
            Assert.assertNotNull(accessPoint);
            if (StringUtils.equals("Karel (X)", accessPoint.getName())) {
                break;
            }
            counter("Čekání na validaci ap kvůli změně položek hlavního jména");
            Thread.sleep(100);
        } while (true);

        // get non preferred part and set as preferred
        Integer nextPredPartId = null, oldPrefName = accessPoint.getPreferredPart();
        for (ApPartVO part : accessPoint.getParts()) {
            if (!part.getId().equals(accessPoint.getPreferredPart())) {
                nextPredPartId = part.getId();
                break;
            }
        }
        setPreferName(accessPoint.getId(), nextPredPartId);

        do {
            accessPoint = getAccessPoint(accessPoint.getId());
            Assert.assertNotNull(accessPoint);
            if (StringUtils.equals("Karel (IV)", accessPoint.getName())) {
                break;
            }
            counter("Čekání na validaci ap kvůli změně položek hlavního jména");
            Thread.sleep(100);
        } while (true);

        deletePart(accessPoint.getId(), oldPrefName);
        accessPoint = getAccessPoint(accessPoint.getId());
        Assert.assertEquals(1, accessPoint.getParts().size());
    }

    @Test//(timeout = 60000)
    public void testAccessPointRev() throws InterruptedException, ApiException {
        ApTypeVO type = getApType(STRUCT_AP_TYPE);
        assertNotNull(type);

        List<ApScopeVO> scopes = getAllScopes();
        Integer scopeId = scopes.iterator().next().getId();

        List<ApItemVO> items = new ArrayList<>();
        RulItemType nmMainItemType = itemTypeRepository.findOneByCode(NM_MAIN);
        RulItemType nmSupGenItemType = itemTypeRepository.findOneByCode(NM_SUP_GEN);

        items.add(buildApItem(nmMainItemType.getCode(), null, "TEST", null, null));
        items.add(buildApItem(nmSupGenItemType.getCode(), null, "AP", null, null));

        ApAccessPointCreateVO ap = new ApAccessPointCreateVO();
        ap.setTypeId(type.getId());
        ap.setScopeId(scopeId);
        ap.setPartForm(createPartFormVO(null, ptName.getCode(), null, items));

        ApAccessPointVO accessPoint = createAccessPoint(ap);
        assertNotNull(accessPoint);
        List<ApPartVO> parts = accessPoint.getParts();
        assertNotNull(parts);
        ApPartVO preferredPart = findPreferredPart(accessPoint);
        assertNotNull(preferredPart);

        // AP created
        // prepare revision
        accesspointsApi.createRevision(accessPoint.getId());
        accessPoint = getAccessPoint(accessPoint.getId());
        assertEquals(accessPoint.getRevStateApproval(), RevStateApproval.ACTIVE);

        // add new part Karel IV
        items = new ArrayList<>();
        items.add(buildApItem(nmMainItemType.getCode(), null, "Karel", null, null));
        items.add(buildApItem(nmSupGenItemType.getCode(), null, "IV", null, null));

        ApPartFormVO partFormVO = ApControllerTest.createPartFormVO(null, ptName.getCode(), null, items);

        Integer revPartId = createPart(accessPoint.getId(), partFormVO);
        assertNotNull(revPartId);

        // drop revision
        accesspointsApi.deleteRevision(accessPoint.getId());
        accessPoint = getAccessPoint(accessPoint.getId());
        assertNull(accessPoint.getRevStateApproval());

        // prepare revision 2
        accesspointsApi.createRevision(accessPoint.getId());
        accessPoint = getAccessPoint(accessPoint.getId());
        assertEquals(accessPoint.getRevStateApproval(), RevStateApproval.ACTIVE);

        revPartId = createPart(accessPoint.getId(), partFormVO);
        assertNotNull(revPartId);
        // check revPart
        accessPoint = getAccessPoint(accessPoint.getId());
        assertTrue(accessPoint.getRevParts().size() == 1);

        // modify existing part
        ApPartVO modPart = accessPoint.getParts().get(0);
        List<ApItemVO> modItems = new ArrayList<>();
        for (ApItemVO modItem : modPart.getItems()) {
            if (modItem.getTypeId().equals(nmMainItemType.getItemTypeId())) {
                // modify main name
                assertNull(modItem.getSpecId());
                ApItemStringVO stringVo = (ApItemStringVO) modItem;
                stringVo.setValue("TEST2");
                modItems.add(modItem);
            } else if (modItem.getTypeId().equals(nmSupGenItemType.getItemTypeId())) {
                // delete doplnek
                assertNull(modItem.getSpecId());
                ApItemStringVO stringVo = (ApItemStringVO) modItem;
                assertEquals(stringVo.getValue(), "AP");
            } else {
                fail("Unexpected item");
            }
        }
        ApPartFormVO modFormPartVO = ApControllerTest.createPartFormVO(modPart.getId(),
                                                                       ptName.getCode(),
                                                                       null, modItems);
        updatePart(accessPoint.getId(), modPart.getId(), modFormPartVO);

        accessPoint = getAccessPoint(accessPoint.getId());
        assertTrue(accessPoint.getRevParts().size() == 2);

        mergeRevision(accessPoint.getId(), null);

        accessPoint = getAccessPoint(accessPoint.getId());
        assertTrue(accessPoint.getRevParts().size() == 0);
        assertTrue(accessPoint.getParts().size() == 2);
        
        // Kontrola obsahu partu
        for (ApPartVO part : accessPoint.getParts()) {
            if (part.getId().equals(preferredPart.getId())) {
                // prvni part
                assertEquals(part.getItems().size(), 1);
                ApItemVO mainItem = part.getItems().get(0);
                ApItemStringVO stringVo = (ApItemStringVO) mainItem;
                assertEquals(stringVo.getValue(), "TEST2");
            } else {
                // druhy part
                assertEquals(part.getItems().size(), 2);
            }
        }
    }

    private ApPartVO findPreferredPart(final ApAccessPointVO accessPoint) {
        if (CollectionUtils.isNotEmpty(accessPoint.getParts())) {
            for (ApPartVO part : accessPoint.getParts()) {
                if (part.getId().equals(accessPoint.getPreferredPart())) {
                    return part;
                }
            }
        }
        return null;
    }

    static public ApPartFormVO createPartFormVO(final Integer partId, final String partTypeCode,
                                                final Integer parentPartId, final List<ApItemVO> items) {
        ApPartFormVO apPartFormVO = new ApPartFormVO();
        apPartFormVO.setPartId(partId);
        apPartFormVO.setPartTypeCode(partTypeCode);
        apPartFormVO.setParentPartId(parentPartId);
        apPartFormVO.setItems(items);
        return apPartFormVO;
    }

    private ApTypeVO getApType(final String structApType) {
        List<ApTypeVO> recordTypes = getRecordTypes();
        return findApTypeRecursive(structApType, recordTypes);
    }

    private ApTypeVO findApTypeRecursive(final String structApType, final List<ApTypeVO> recordTypes) {
        for (ApTypeVO recordType : recordTypes) {
            if (recordType.getCode().equalsIgnoreCase(structApType)) {
                return recordType;
            } else {
                List<ApTypeVO> children = recordType.getChildren();
                if (children != null) {
                    ApTypeVO foundType = findApTypeRecursive(structApType, children);
                    if (foundType != null) {
                        return foundType;
                    }
                }
            }
        }
        return null;
    }

    @Test
    public void testPart() {
        List<ApTypeVO> types = getRecordTypes();
        List<ApScopeVO> scopes = getAllScopes();
        Integer scopeId = scopes.iterator().next().getId();
        RulItemType nmMainItemType = itemTypeRepository.findOneByCode(NM_MAIN);
        RulItemType nmSupGenItemType = itemTypeRepository.findOneByCode(NM_SUP_GEN);

        List<ApItemVO> apItems = new ArrayList<>();
        apItems.add(buildApItem(nmMainItemType.getCode(), null, "Petr Novák", null, null));
        apItems.add(buildApItem(nmSupGenItemType.getCode(), null, "1920-1986", null, null));

        ApAccessPointCreateVO ap = new ApAccessPointCreateVO();
        ap.setTypeId(getApType(types, "PERSON_BEING").getId());
        ap.setPartForm(createPartFormVO(null, ptName.getCode(), null, apItems));
        ap.setScopeId(scopeId);
        ApAccessPointVO accessPoint = createAccessPoint(ap);
        Assert.assertNotNull(accessPoint.getId());

        List<ApItemVO> items = new ArrayList<>();
        RulDescItemTypeExtVO vztahTypType = findDescItemTypeByCode("VZTAH_TYP");
        RulDescItemSpecExtVO vztahTypSpec = findDescItemSpecByCode("VZTAH_TYP_PRIMATOR", vztahTypType);
        RulDescItemTypeExtVO vztahEntitaType = findDescItemTypeByCode("VZTAH_ENTITA");

        items.add(buildApItem(vztahTypType.getCode(), vztahTypSpec.getCode(), null, null, null));
        items.add(buildApItem(vztahEntitaType.getCode(), null, accessPoint, null, null));

        createPart(accessPoint.getId(), createPartFormVO(null, ptRel.getCode(), null, items));
        accessPoint = getAccessPoint(accessPoint.getId());
        Assert.assertEquals(2, accessPoint.getParts().size());

        ApPartVO partVO = accessPoint.getParts().get(1);
        items = new ArrayList<>(partVO.getItems());
        RulDescItemTypeExtVO nadType = findDescItemTypeByCode("SRD_NAD");
        items.add(buildApItem(nadType.getCode(), null, 1, null, null));
        items.add(buildApItem(nadType.getCode(), null, 2, null, null));
        items.add(buildApItem(nadType.getCode(), null, 3, 1, null));
        items.add(buildApItem(nadType.getCode(), null, 4, 8, null));
        items.add(buildApItem(nadType.getCode(), null, 5, 2, null));

        updatePart(accessPoint.getId(), partVO.getId(), createPartFormVO(partVO.getId(), ptRel.getCode(), null, items));
        accessPoint = getAccessPoint(accessPoint.getId());
        partVO = accessPoint.getParts().get(1);
        Assert.assertEquals(7, partVO.getItems().size());
    }

    /**
     * Načtení třídy včetně navázaných tříd.
     *
     * @param id ID třídy
     */
    private ApScopeWithConnectedVO getScopeWithConnectedTest(final int id) {
        return getScopeWithConnected(id);
    }

    /**
     * Vložení nové třídy.
     */
    private ApScopeVO createScopeTest() {
        return createScope();
    }

    /**
     * Aktualizace třídy.
     *
     * @param scope id třídy
     */
    private ApScopeVO updateScopeTest(final ApScopeVO scope) {
        return updateScope(scope);
    }

    /**
     * Smazání třídy. Třída nesmí být napojena na rejstříkové heslo.
     *
     * @param id id třídy.
     */
    private void deleteScopeTest(final int id) {
        deleteScope(id);
    }

    /**
     * Propojení tříd.
     *
     * @param id ID třídy ke které se bude navazovat
     * @param id2 ID navazované třídy
     */
    private void connectScopeTest(final int id, final int id2) {
        connectScope(id, id2);
    }

    /**
     * Zrušení propojení tříd.
     *
     * @param id ID třídy na které se bude rušit propojení
     * @param id2 ID navázané třídy
     */
    private void disconnectScopeTest(final int id, final int id2) {
        disconnectScope(id, id2);
    }

    @Test
    public void registerReplaceTest() throws ApiException {
        // Vytvoření fund
        Fund fund = createFund("RegisterLinks Test AP", "IC3");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);

        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());
        ArrNodeVO rootNode = nodes.get(0);

        List<ApTypeVO> types = getRecordTypes();
        List<ApScopeVO> scopes = getAllScopes();
        Integer scopeId = scopes.iterator().next().getId();

        RulItemType nmMainItemType = itemTypeRepository.findOneByCode(NM_MAIN);
        RulItemType nmSupGenItemType = itemTypeRepository.findOneByCode(NM_SUP_GEN);
        RulItemType briefDscType = itemTypeRepository.findOneByCode(BRIEF_DESC);
        RulItemType relEntityType = itemTypeRepository.findOneByCode(REL_ENTITY);

        // AP A: Vytvoření replace
        List<ApItemVO> itemReplace = new ArrayList<>();
        itemReplace.add(buildApItem(NM_MAIN, null, "ApRecordA name", null, null));
        itemReplace.add(buildApItem(NM_SUP_GEN, null, "ApRecordA complement", null, null));

        ApAccessPointCreateVO replacedRecord = new ApAccessPointCreateVO();
        replacedRecord.setTypeId(getApType(types, "PERSON_BEING").getId());
        replacedRecord.setPartForm(createPartFormVO(null, ptName.getCode(), null, itemReplace));
        replacedRecord.setScopeId(scopeId);
        ApAccessPointVO replacedRecordCreated = createAccessPoint(replacedRecord);
        assertNotNull(replacedRecordCreated.getId());


        // AP B: Vytvoření replacement
        List<ApItemVO> itemReplacement = new ArrayList<>();
        itemReplacement.add(buildApItem(NM_MAIN, null, "ApRecordB name", null, null));
        itemReplacement.add(buildApItem(NM_SUP_GEN, null, "ApRecordB complement", null, null));

        ApAccessPointCreateVO replacementRecord = new ApAccessPointCreateVO();
        replacementRecord.setTypeId(getApType(types, "PERSON_BEING").getId());
        replacementRecord.setPartForm(createPartFormVO(null, ptName.getCode(), null, itemReplacement));
        replacementRecord.setScopeId(scopeId);

        ApAccessPointVO replacementRecordCreated = createAccessPoint(replacementRecord);
        Assert.assertNotNull(replacementRecordCreated.getId());

        List<ApItemVO> bodyItemsB = new ArrayList<>();
        bodyItemsB.add(buildApItem(BRIEF_DESC, null, "ApRecordB desc", null, null));
        Integer partBodyBId = createPart(replacementRecordCreated.getId(),
                                         createPartFormVO(null, ptBody.getCode(), null, bodyItemsB));
        assertNotNull(partBodyBId);

        // AP C: Vytvoření AP s vazbou na replaced
        List<ApItemVO> nameItemsC = new ArrayList<>();
        nameItemsC.add(buildApItem(NM_MAIN, null, "ApRecordC name", null, null));
        nameItemsC.add(buildApItem(NM_SUP_GEN, null, "ApRecordC complement", null, null));

        ApAccessPointCreateVO recordC = new ApAccessPointCreateVO();
        recordC.setTypeId(getApType(types, "PERSON_BEING").getId());
        recordC.setPartForm(createPartFormVO(null, ptName.getCode(), null, nameItemsC));
        recordC.setScopeId(scopeId);

        ApAccessPointVO recordCCreated = createAccessPoint(recordC);
        assertNotNull(recordCCreated.getId());

        List<ApItemVO> bodyItemsC = new ArrayList<>();
        bodyItemsC.add(buildApItem(BRIEF_DESC, null, "ApRecordC desc", null, null));
        Integer partBodyCId = createPart(recordCCreated.getId(), createPartFormVO(null, ptBody.getCode(), null,
                                                                                  bodyItemsC));
        assertNotNull(partBodyCId);

        List<ApItemVO> relItemsC = new ArrayList<>();
        relItemsC.add(buildApItem(REL_ENTITY, RT_BROTHER, replacedRecordCreated, null, null));
        Integer partRelCId = createPart(recordCCreated.getId(), createPartFormVO(null, ptRel.getCode(), null,
                                                                                 relItemsC));
        assertNotNull(partRelCId);

        // AP D: Vytvoření schváleného AP s vazbou na replaced
        List<ApItemVO> itemsD = new ArrayList<>();
        itemsD.add(buildApItem(NM_MAIN, null, "ApRecordD name", null, null));
        itemsD.add(buildApItem(NM_SUP_GEN, null, "ApRecordD complement", null, null));

        ApAccessPointCreateVO recordD = new ApAccessPointCreateVO();
        recordD.setTypeId(getApType(types, "PERSON_BEING").getId());
        recordD.setPartForm(createPartFormVO(null, ptName.getCode(), null, itemsD));
        recordD.setScopeId(scopeId);

        ApAccessPointVO recordDCreated = createAccessPoint(recordD);
        Assert.assertNotNull(recordDCreated.getId());

        List<ApItemVO> bodyItemsD = new ArrayList<>();
        bodyItemsD.add(buildApItem(BRIEF_DESC, null, "ApRecordD desc", null, null));
        Integer partBodyDId = createPart(recordDCreated.getId(), createPartFormVO(null, ptBody.getCode(), null,
                                                                                  bodyItemsD));
        assertNotNull(partBodyDId);

        List<ApItemVO> relItemsD = new ArrayList<>();
        relItemsD.add(buildApItem(REL_ENTITY, RT_BROTHER, replacedRecordCreated, null, null));
        Integer partRelDId = createPart(recordDCreated.getId(), createPartFormVO(null, ptRel.getCode(), null,
                                                                                 relItemsD));
        assertNotNull(partRelDId);

        // mark as approved
        ApStateVO stateD = recordDCreated.getState();
        ApStateChangeVO stateChangeD = new ApStateChangeVO();
        stateChangeD.setState(StateApproval.APPROVED);
        this.changeState(recordDCreated.getId(), stateChangeD);

        // Dohledání usages
        RecordUsageVO usage = usagesRecord(replacedRecordCreated.getId());
        assertNotNull(usage.getFunds());

        // Replace
        replaceRecord(replacedRecordCreated.getId(), replacementRecordCreated.getId());
        RecordUsageVO usageAfterReplace = usagesRecord(replacedRecordCreated.getId());
        Assert.assertTrue(usageAfterReplace.getFunds() == null || usageAfterReplace.getFunds().isEmpty());

        // ověření změny v AP - C
        recordCCreated = getAccessPoint(recordCCreated.getId());
        assertNull(recordCCreated.getRevStateApproval()); // has no revision
        assertEquals(3, recordCCreated.getParts().size()); // has no revision
        ApPartVO relCPart = getFirstPart(recordCCreated.getParts(), ptRel);
        assertEquals(1, relCPart.getItems().size()); // 1 item as on the beginning
        ApItemVO relEntityItem = getFirstItem(relCPart, relEntityType);
        assertNotNull(relEntityItem);
        ApItemAccessPointRefVO relEntityItemApRef = (ApItemAccessPointRefVO) relEntityItem;
        assertEquals(relEntityItemApRef.getAccessPoint().getId(), replacementRecordCreated.getId());

        // ověření změny v AP - D
        recordDCreated = getAccessPoint(recordDCreated.getId());
        assertEquals(recordDCreated.getRevStateApproval(), RevStateApproval.ACTIVE); // has revision
        assertEquals(3, recordDCreated.getParts().size());
        assertEquals(1, recordDCreated.getRevParts().size());
        ApPartVO mainDPart = recordDCreated.getRevParts().get(0);
        assertEquals(1, mainDPart.getItems().size());
        ApItemVO relEntityDItem = getFirstItem(mainDPart, relEntityType);
        assertNotNull(relEntityDItem);
        assertNull(relEntityDItem.getObjectId());
        assertNotNull(relEntityDItem.getOrigObjectId());
        ApItemAccessPointRefVO relEntityDItemApRef = (ApItemAccessPointRefVO) relEntityDItem;
        assertEquals(relEntityDItemApRef.getAccessPoint().getId(), replacementRecordCreated.getId());
    }

    private ApItemVO getFirstItem(ApPartVO part, RulItemType itemType) {
        if (part.getItems() == null) {
            return null;
        }
        for (ApItemVO item : part.getItems()) {
            if (item.getTypeId().equals(itemType.getItemTypeId())) {
                return item;
            }
        }
        return null;
    }

    private ApPartVO getFirstPart(List<ApPartVO> parts, RulPartTypeVO partType) {
        for (ApPartVO p : parts) {
            if (p.getTypeId().equals(partType.getId())) {
                return p;
            }
        }
        return null;
    }

    private ApTypeVO getApType(final List<ApTypeVO> list, final String typeCode) {
        for (ApTypeVO t : list) {
            if (t.getCode().equals(typeCode)) {
                return t;
            }
            if (t.getChildren() != null) {
                ApTypeVO child = getApType(t.getChildren(), typeCode);
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }

}
