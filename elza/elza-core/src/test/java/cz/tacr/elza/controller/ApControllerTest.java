package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.domain.RulItemType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import cz.tacr.elza.controller.vo.ApAccessPointCreateVO;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApEidTypeVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.ApTypeVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.LanguageVO;
import cz.tacr.elza.controller.vo.RulStructureTypeVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.ap.ApFormVO;
import cz.tacr.elza.controller.vo.ap.ApFragmentVO;
import cz.tacr.elza.controller.vo.ap.ApStateVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemStringVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.controller.vo.ap.item.ApUpdateItemVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.UpdateOp;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;


/**
 * @author Petr Compel
 * @since 18.2.2016
 */
public class ApControllerTest extends AbstractControllerTest {

    /**
     * Kód typu fragmentu pro testování.
     */
    public static final String STAT_ZASTUPCE = "STAT_ZASTUPCE";
    public static final String STRUCT_AP_TYPE = "PERSON_BEING_STRUCT";
    public static final String LANG_CZE = "cze";
    public static final String PT_NAME = "PT_NAME";
    public static final String PT_REL = "PT_REL";
    public static final String NM_MAIN = "NM_MAIN";
    public static final String NM_SUP_GEN = "NM_SUP_GEN";

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
        Map<String, RulPartTypeVO> partTypes = findPartTypesMap();
        RulPartTypeVO ptName = partTypes.get(PT_NAME);

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

        items = new ArrayList<>();
        items.add(buildApItem(nmMainItemType.getCode(), null, "Karel", null, null));
        items.add(buildApItem(nmSupGenItemType.getCode(), null, "IV", null, null));

        ApPartFormVO partFormVO = createPartFormVO(null, ptName.getCode(), null, items);
        createPart(accessPoint.getId(), partFormVO);

        accessPoint = getAccessPoint(accessPoint.getId());
        Assert.assertEquals(2, accessPoint.getParts().size());

        items = new ArrayList<>(preferredPart.getItems());
        ApItemStringVO itemNmMain = (ApItemStringVO) items.get(0);
        itemNmMain.setValue("Karel");

        ApItemStringVO itemNmSupGen = (ApItemStringVO) items.get(1);
        itemNmSupGen.setValue("X");

        partFormVO = createPartFormVO(preferredPart.getId(), ptName.getCode(), null, items);
        updatePart(accessPoint.getId(), preferredPart.getId(), partFormVO);

        do {
            accessPoint = getAccessPoint(accessPoint.getId());
            Assert.assertNotNull(accessPoint);
            if (StringUtils.equals("Karel (X)", accessPoint.getName())) {
                break;
            }
            counter("Čekání na validaci ap kvůli změně položek hlavního jména");
            Thread.sleep(100);
        } while (true);

        setPreferName(accessPoint.getId(), accessPoint.getParts().get(0).getId());

        do {
            accessPoint = getAccessPoint(accessPoint.getId());
            Assert.assertNotNull(accessPoint);
            if (StringUtils.equals("Karel (IV)", accessPoint.getName())) {
                break;
            }
            counter("Čekání na validaci ap kvůli změně položek hlavního jména");
            Thread.sleep(100);
        } while (true);

        deletePart(accessPoint.getId(), accessPoint.getParts().get(1).getId());
        accessPoint = getAccessPoint(accessPoint.getId());
        Assert.assertEquals(1, accessPoint.getParts().size());
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

    private ApPartFormVO createPartFormVO(final Integer partId, final String partTypeCode, final Integer parentPartId, final List<ApItemVO> items) {
        ApPartFormVO apPartFormVO = new ApPartFormVO();
        apPartFormVO.setPartId(partId);
        apPartFormVO.setPartTypeCode(partTypeCode);
        apPartFormVO.setParentPartId(parentPartId);
        apPartFormVO.setItems(items);
        return apPartFormVO;
    }

    /*private ApAccessPointNameVO createName(final ApAccessPointVO accessPoint) {
        ApAccessPointNameVO accessPointName = createAccessPointStructuredName(accessPoint.getId());
        List<ApUpdateItemVO> items = new ArrayList<>();
        RulDescItemTypeExtVO apNameType = findDescItemTypeByCode("AP_NAME");
        RulDescItemTypeExtVO apComplementType = findDescItemTypeByCode("AP_COMPLEMENT");

        items.add(buildApItem(UpdateOp.CREATE, apNameType.getCode(), null, "Karel", null, null));
        items.add(buildApItem(UpdateOp.CREATE, apComplementType.getCode(), null, "IV", null, null));
        changeNameItems(accessPoint.getId(), accessPointName.getObjectId(), items);
        return accessPointName;
    }*/

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
        Map<String, RulPartTypeVO> partTypes = findPartTypesMap();

        RulPartTypeVO ptName = partTypes.get(PT_NAME);
        Assert.assertNotNull(ptName);
        RulPartTypeVO ptRel = partTypes.get(PT_REL);
        Assert.assertNotNull(ptRel);

        List<ApTypeVO> types = getRecordTypes();
        List<ApScopeVO> scopes = getAllScopes();
        Integer scopeId = scopes.iterator().next().getId();
        RulItemType nmMainItemType = itemTypeRepository.findOneByCode(NM_MAIN);
        RulItemType nmSupGenItemType = itemTypeRepository.findOneByCode(NM_SUP_GEN);

        List<ApItemVO> apItems = new ArrayList<>();
        apItems.add(buildApItem(nmMainItemType.getCode(), null, "Petr Novák", null, null));
        apItems.add(buildApItem(nmSupGenItemType.getCode(), null, "1920-1986", null, null));

        ApAccessPointCreateVO ap = new ApAccessPointCreateVO();
        ap.setTypeId(getNonHierarchicalApType(types).getId());
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

    private Map<String, RulStructureTypeVO> findFragmentTypesMap() {
        return findStructureTypes().stream().collect(Collectors.toMap(RulStructureTypeVO::getCode, Function.identity()));
    }

    private Map<String, RulPartTypeVO> findPartTypesMap() {
        return findPartTypes().stream().collect(Collectors.toMap(RulPartTypeVO::getCode, Function.identity()));
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
    public void registerReplaceTest() {
        // Vytvoření fund
        ArrFundVO fund = createFund("RegisterLinks Test AP", "IC3");
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
        Map<String, RulPartTypeVO> partTypes = findPartTypesMap();

        RulPartTypeVO ptName = partTypes.get(PT_NAME);
        Assert.assertNotNull(ptName);

        // Vytvoření replace
        List<ApItemVO> itemReplace = new ArrayList<>();
        itemReplace.add(buildApItem(nmMainItemType.getCode(), null,"ApRecordA name", null, null));
        itemReplace.add(buildApItem(nmSupGenItemType.getCode(), null, "ApRecordA complement", null, null));

        ApAccessPointCreateVO replacedRecord = new ApAccessPointCreateVO();
        replacedRecord.setTypeId(getNonHierarchicalApType(types).getId());
        replacedRecord.setPartForm(createPartFormVO(null, ptName.getCode(), null, itemReplace));
        replacedRecord.setScopeId(scopeId);
        ApAccessPointVO replacedRecordCreated = createAccessPoint(replacedRecord);
        Assert.assertNotNull(replacedRecordCreated.getId());

        // Vytvoření replacement
        List<ApItemVO> itemReplacement = new ArrayList<>();
        itemReplacement.add(buildApItem(nmMainItemType.getCode(), null , "ApRecordB name", null, null));
        itemReplacement.add(buildApItem(nmSupGenItemType.getCode(), null, "ApRecordB complement", null, null));

        ApAccessPointCreateVO replacementRecord = new ApAccessPointCreateVO();
        replacementRecord.setTypeId(getNonHierarchicalApType(types).getId());
        replacementRecord.setPartForm(createPartFormVO(null, ptName.getCode(), null, itemReplacement));
        replacementRecord.setScopeId(scopeId);

        ApAccessPointVO replacementRecordCreated = createAccessPoint(replacementRecord);
        Assert.assertNotNull(replacementRecordCreated.getId());

        // Dohledání usages
        RecordUsageVO usage = usagesRecord(replacedRecordCreated.getId());
        Assert.assertNotNull(usage.getFunds());

        // Replace
        replaceRecord(replacedRecordCreated.getId(), replacementRecordCreated.getId());
        RecordUsageVO usageAfterReplace = usagesRecord(replacedRecordCreated.getId());
        Assert.assertTrue(usageAfterReplace.getFunds() == null || usageAfterReplace.getFunds().isEmpty());
    }

    private ApTypeVO getNonHierarchicalApType(final List<ApTypeVO> list) {
        for (ApTypeVO type : list) {
            if (type.getAddRecord()) {
                return type;
            }
        }

        for (ApTypeVO type : list) {
            if (type.getChildren() != null) {
                ApTypeVO res = getNonHierarchicalApType(type.getChildren());
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

}
