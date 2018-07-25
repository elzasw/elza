package cz.tacr.elza.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.ap.ApFormVO;
import cz.tacr.elza.controller.vo.ap.ApFragmentTypeVO;
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
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


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


    /**
     * Testování vytvoření, upravení a smazání scope
     */
    @Test
    public void createUpdateDeleteScopesTest() {
        ApScopeVO scopeVO = new ApScopeVO();
        scopeVO.setName("Testing");
        scopeVO.setCode("ABCD");
        scopeVO = createScopeTest(scopeVO);
        scopeVO.setName("Testing2");
        scopeVO = updateScopeTest(scopeVO);
        deleteScopeTest(scopeVO.getId());
    }

    @Test
    public void testStructureAccessPoint() {

        ApTypeVO type = getApType(STRUCT_AP_TYPE);

        List<ApScopeVO> scopes = getAllScopes();
        Integer scopeId = scopes.iterator().next().getId();

        ApAccessPointCreateVO ap = new ApAccessPointCreateVO();
        ap.setTypeId(type.getId());
        ap.setScopeId(scopeId);

        ApAccessPointVO accessPoint = createStructuredAccessPoint(ap);
        Assert.assertNotNull(accessPoint);
        ApFormVO form = accessPoint.getForm();
        Assert.assertNotNull(form);
        List<ApAccessPointNameVO> names = new ArrayList<>(accessPoint.getNames());
        ApAccessPointNameVO accessPointName = names.get(0);

        accessPointName = getAccessPointName(accessPoint.getId(), accessPointName.getObjectId());

        List<ApUpdateItemVO> items = new ArrayList<>();
        RulDescItemTypeExtVO apNameType = findDescItemTypeByCode("AP_NAME");
        RulDescItemTypeExtVO apComplementType = findDescItemTypeByCode("AP_COMPLEMENT");

        items.add(buildApItem(UpdateOp.CREATE, apNameType.getCode(), null, "Karel", null, null));
        items.add(buildApItem(UpdateOp.CREATE, apComplementType.getCode(), null, "IV", null, null));
        changeNameItems(accessPoint.getId(), accessPointName.getObjectId(), items);

        confirmStructuredAccessPoint(accessPointName.getObjectId());

        accessPoint = getAccessPoint(accessPointName.getObjectId());
        names = new ArrayList<>(accessPoint.getNames());
        accessPointName = names.get(0);
        accessPointName = getAccessPointName(accessPoint.getId(), accessPointName.getObjectId());

        items = new ArrayList<>();
        form = accessPointName.getForm();
        ApItemStringVO item = (ApItemStringVO) form.getItems().get(0);
        item.setValue("KarelX");
        items.add(buildApItem(UpdateOp.UPDATE, item));
        changeNameItems(accessPoint.getId(), accessPointName.getObjectId(), items);

        accessPoint = getAccessPoint(accessPointName.getObjectId());
        names = new ArrayList<>(accessPoint.getNames());
        accessPointName = names.get(0);
        accessPointName = getAccessPointName(accessPoint.getId(), accessPointName.getObjectId());

        Assert.assertNotNull(accessPointName);
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
    public void testFragment() {
        Map<String, ApFragmentTypeVO> fragmentTypes = findFragmentTypesMap();

        ApFragmentTypeVO fragmentType = fragmentTypes.get(STAT_ZASTUPCE);
        Assert.assertNotNull(fragmentType);
        List<ApTypeVO> types = getRecordTypes();
        List<ApScopeVO> scopes = getAllScopes();
        Integer scopeId = scopes.iterator().next().getId();

        ApAccessPointCreateVO ap = new ApAccessPointCreateVO();
        ap.setTypeId(getNonHierarchicalApType(types, false).getId());
        ap.setName("Petr Novák");
        ap.setComplement("1920-1986");
        ap.setScopeId(scopeId);
        ApAccessPointVO accessPoint = createAccessPoint(ap);

        ApFragmentVO fragment = createFragment(fragmentType.getCode());

        List<ApUpdateItemVO> items = new ArrayList<>();
        RulDescItemTypeExtVO vztahTypType = findDescItemTypeByCode("VZTAH_TYP");
        RulDescItemSpecExtVO vztahTypSpec = findDescItemSpecByCode("VZTAH_TYP_PRIMATOR", vztahTypType);
        RulDescItemTypeExtVO vztahEntitaType = findDescItemTypeByCode("VZTAH_ENTITA");

        items.add(buildApItem(UpdateOp.CREATE, vztahTypType.getCode(), vztahTypSpec.getCode(), null, null, null));
        items.add(buildApItem(UpdateOp.CREATE, vztahEntitaType.getCode(), null, accessPoint, null, null));

        fragment = changeFragmentItems(fragment.getId(), items);
        Assert.assertEquals(vztahTypSpec.getName() + ": " + accessPoint.getRecord(), fragment.getValue());

        confirmFragment(fragment.getId());

        ApFragmentVO fragmentConfirmed = getFragment(fragment.getId());
        Assert.assertEquals(ApStateVO.OK, fragmentConfirmed.getState());

        items = new ArrayList<>();
        RulDescItemTypeExtVO nadType = findDescItemTypeByCode("ZP2015_NAD");
        items.add(buildApItem(UpdateOp.CREATE, nadType.getCode(), null, 1, null, null));
        items.add(buildApItem(UpdateOp.CREATE, nadType.getCode(), null, 2, null, null));
        items.add(buildApItem(UpdateOp.CREATE, nadType.getCode(), null, 3, 1, null));
        items.add(buildApItem(UpdateOp.CREATE, nadType.getCode(), null, 4, 8, null));
        items.add(buildApItem(UpdateOp.CREATE, nadType.getCode(), null, 5, 2, null));
        fragment = changeFragmentItems(fragment.getId(), items);

        ApFormVO fragmentForm = fragment.getForm();
        List<ApItemVO> fragmentItems = fragmentForm.getItems();
        ApItemVO itemVO = fragmentItems.get(2);

        items = new ArrayList<>();
        items.add(buildApItem(UpdateOp.DELETE, nadType.getCode(), null, null, null, itemVO.getObjectId()));
        fragment = changeFragmentItems(fragment.getId(), items);

        fragmentForm = fragment.getForm();
        fragmentItems = fragmentForm.getItems();
        itemVO = fragmentItems.get(5);

        itemVO.setPosition(2);
        items = new ArrayList<>();
        items.add(buildApItem(UpdateOp.UPDATE, itemVO));

        ApFragmentVO fragmentUpdated = changeFragmentItems(fragment.getId(), items);
        ApFormVO fragmentUpdatedForm = fragmentUpdated.getForm();
        List<ApItemVO> fragmentUpdatedItems = fragmentUpdatedForm.getItems();

        Assert.assertEquals(fragmentItems.get(2).getObjectId(), fragmentUpdatedItems.get(2).getObjectId());
        Assert.assertEquals(fragmentItems.get(3).getObjectId(), fragmentUpdatedItems.get(4).getObjectId());
        Assert.assertEquals(fragmentItems.get(4).getObjectId(), fragmentUpdatedItems.get(5).getObjectId());
        Assert.assertEquals(fragmentItems.get(5).getObjectId(), fragmentUpdatedItems.get(3).getObjectId());
    }

    @Test
    public void testTempFragment() {
        Map<String, ApFragmentTypeVO> fragmentTypes = findFragmentTypesMap();
        ApFragmentTypeVO fragmentType = fragmentTypes.get(STAT_ZASTUPCE);
        Assert.assertNotNull(fragmentType);

        ApFragmentVO fragment = createFragment(fragmentType.getCode());
        deleteFragment(fragment.getId());
    }

    private Map<String, ApFragmentTypeVO> findFragmentTypesMap() {
        return findFragmentTypes().stream().collect(Collectors.toMap(ApFragmentTypeVO::getCode, Function.identity()));
    }

    /**
     * Vložení nové třídy.
     *
     * @param scopeVO objekt třídy
     */
    private ApScopeVO createScopeTest(final ApScopeVO scopeVO) {
        return createScope(scopeVO);
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

        // Vytvoření replace
        ApAccessPointCreateVO replacedRecord = new ApAccessPointCreateVO();
        replacedRecord.setTypeId(getNonHierarchicalApType(types, false).getId());
        replacedRecord.setName("ApRecordA name");
        replacedRecord.setComplement("ApRecordA complement");
        replacedRecord.setScopeId(scopeId);
        ApAccessPointVO replacedRecordCreated = createAccessPoint(replacedRecord);
        Assert.assertNotNull(replacedRecordCreated.getId());

        // Vytvoření node register
        ArrNodeRegisterVO nodeRegister = new ArrNodeRegisterVO();

        nodeRegister.setValue(replacedRecordCreated.getId());
        nodeRegister.setNodeId(rootNode.getId());
        nodeRegister.setNode(rootNode);

        ArrNodeRegisterVO createdLink = createRegisterLinks(fundVersion.getId(), rootNode.getId(), nodeRegister);

        assertNotNull(createdLink);

        List<ArrNodeRegisterVO> registerLinks = findRegisterLinks(fundVersion.getId(), rootNode.getId());
        assertTrue(registerLinks.size()>0);

        ArrangementController.NodeRegisterDataVO registerLinksForm = findRegisterLinksForm(fundVersion.getId(),
                rootNode.getId());

        assertNotNull(registerLinksForm.getNode());
        assertTrue(registerLinksForm.getNodeRegisters().size()>0);

        ArrNodeRegisterVO updatedLink = updateRegisterLinks(fundVersion.getId(), rootNode.getId(), createdLink);

        assertTrue(!createdLink.getId().equals(updatedLink.getId()));


        // Vytvoření replacement
        ApAccessPointCreateVO replacementRecord = new ApAccessPointCreateVO();
        replacementRecord.setTypeId(getNonHierarchicalApType(types, false).getId());
        replacementRecord.setName("ApRecordB name");
        replacementRecord.setComplement("ApRecordB complement");
        replacementRecord.setScopeId(scopeId);

        ApAccessPointVO replacementRecordCreated = createAccessPoint(replacementRecord);
        Assert.assertNotNull(replacementRecordCreated.getId());

        // Dohledání usages
        RecordUsageVO usage = usagesRecord(replacedRecordCreated.getId());
        Assert.assertNotNull(usage.funds);

        // Replace
        replaceRecord(replacedRecordCreated.getId(), replacementRecordCreated.getId());
        RecordUsageVO usageAfterReplace = usagesRecord(replacedRecordCreated.getId());
        Assert.assertTrue(usageAfterReplace.funds == null || usageAfterReplace.funds.isEmpty());
    }

    private ApTypeVO getNonHierarchicalApType(final List<ApTypeVO> list, final boolean hasPartyType) {
        for (ApTypeVO type : list) {
            if (type.getAddRecord() && ((!hasPartyType && type.getPartyTypeId() == null) || (hasPartyType && type.getPartyTypeId() != null))) {
                return type;
            }
        }

        for (ApTypeVO type : list) {
            if (type.getChildren() != null) {
                ApTypeVO res = getNonHierarchicalApType(type.getChildren(), hasPartyType);
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

}
