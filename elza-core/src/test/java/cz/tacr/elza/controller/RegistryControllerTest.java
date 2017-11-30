package cz.tacr.elza.controller;

import java.util.Collections;
import java.util.List;

import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrNodeRegisterVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;
import org.junit.Assert;
import org.junit.Test;

import cz.tacr.elza.controller.vo.RegCoordinatesVO;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Petr Compel
 * @since 18.2.2016
 */
public class RegistryControllerTest extends AbstractControllerTest {

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
        RegScopeVO scopeVO = new RegScopeVO();
        scopeVO.setName("Testing");
        scopeVO.setCode("ABCD");
        scopeVO = createScopeTest(scopeVO);
        scopeVO.setName("Testing2");
        scopeVO = updateScopeTest(scopeVO);
        deleteScopeTest(scopeVO.getId());
    }

    /**
     * Vložení nové třídy.
     *
     * @param scopeVO objekt třídy
     */
    private RegScopeVO createScopeTest(final RegScopeVO scopeVO) {
        return createScope(scopeVO);
    }

    /**
     * Aktualizace třídy.
     *
     * @param scope id třídy
     */
    private RegScopeVO updateScopeTest(final RegScopeVO scope) {
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
     * Scénář
     * ----------
     * - vytvořit nové rejstříkové heslo A do libovolného hierarchického typu rejstříku, který nemá vazbu na typ osoby
     * - vytvořit nové rejstříkové heslo B do libovolného nehierarchického typu rejstříku, který nemá vazbu na typ osoby
     * - získat seznam hesel, ověřit výsledek (2 hesla, A,B)
     * - přidat k hierarchickému heslu A 2 variantní hesla
     * - získat detail hesla A, ověřit výsledek (A s 2 variantními hesly)
     * - smazat hierarchickému heslu A 1 variantní heslo
     * - upravit 2 variantní heslo
     * - upravit heslo A (jiné charakteristika)
     * - získat detail hesla A, ověřit výsledek (A s 1 variantním heslem, upravená charakteristika)
     * - vytvořit nová rejstříková hesla C a D pod heslo A
     * - získat seznam hesel pod heslem A, ověřit výsledek (2 hesla, C,D)
     * - smazat heslo D
     * - získat seznam všech hesel, ověřit výsledek (3 hesla, A,B,C)
     * - získat seznam hesel s nějakým filtrem, ověřit výsledek
     */
    @Test
    public void scenarioTest() {

        /* Smazání tabulek (kvůli XML importu pro zakládání archivních fondů) **/
    	helperTestService.deleteTables();

        RegRecordVO recordA = new RegRecordVO();
        RegRecordVO recordB = new RegRecordVO();
        List<RegRegisterTypeVO> types = getRecordTypes();
        List<RegScopeVO> scopes = getAllScopes();

        Assert.assertTrue(scopes != null && scopes.size() > 0);
        Integer scopeId = scopes.iterator().next().getId();

        RegRegisterTypeVO hierarchal = getHierarchicalRegRegisterType(types, null, false);
        RegRegisterTypeVO nonHierarchal = getNonHierarchicalRegRegisterType(types, false);

        Assert.assertNotNull("Nebyl nalezen hirearchický typ rejstříku", hierarchal);
        Assert.assertNotNull("Nebyl nalezen nehirearchický typ rejstříku", nonHierarchal);

        recordA.setRegisterTypeId(hierarchal.getId());
        recordB.setRegisterTypeId(nonHierarchal.getId());

        recordA.setCharacteristics("Ja jsem regRecordA");
        recordB.setCharacteristics("Ja jsem regRecordB");

        recordA.setRecord("RegRecordA");
        recordB.setRecord("RegRecordB");

        recordA.setScopeId(scopeId);
        recordB.setScopeId(scopeId);

        recordA.setHierarchical(true);
        recordA.setAddRecord(true);

        recordA = createRecord(recordA);
        recordB = createRecord(recordB);
        List<RegRecordVO> list = findRecord(null, 0, 10, null, null, null);
        Assert.assertTrue(list.size() == 2);

        RegVariantRecordVO variant1 = new RegVariantRecordVO();
        RegVariantRecordVO variant2 = new RegVariantRecordVO();

        variant1.setRegRecordId(recordA.getId());
        variant2.setRegRecordId(recordA.getId());

        variant1 = createVariantRecord(variant1);
        variant2 = createVariantRecord(variant2);

        RegCoordinatesVO coordinates = new RegCoordinatesVO();
        coordinates.setRegRecordId(recordA.getId());
        coordinates.setValue("POINT(11 10)");
        coordinates.setDescription("Karlův most");

        coordinates = createRegCoordinates(coordinates);

        recordA = getRecord(recordA.getId());
        Assert.assertTrue("Ocekavame 2 variantni hesla pro heslo A", recordA.getVariantRecords().size() == 2);
        Assert.assertTrue("Ocekavame 1 souradnici", recordA.getCoordinates().size() == 1);

        String changedCharacter = "Ja nechci byt regRecordA.";
        recordA.setCharacteristics(changedCharacter);
        updateRecord(recordA);

        String changedRecord = "Upraveny record";
        variant2.setRecord(changedRecord);
        variant2 = updateVariantRecord(variant2);
        Assert.assertTrue("Ocekavame upraveny record", variant2.getRecord().equals(changedRecord));

        String cordNewPoint = "POINT (1 1)";
        String cordDesc = "Karlův most odkud kam";
        coordinates.setValue(cordNewPoint);
        coordinates.setDescription(cordDesc);

        coordinates = updateRegCoordinates(coordinates);
        Assert.assertTrue("Ocekavame upravene souradnice", coordinates.getValue().equals(cordNewPoint) && coordinates.getDescription().equals(cordDesc));

        deleteVariantRecord(variant2.getId());
        recordA = getRecord(recordA.getId());
        Assert.assertTrue("Ocekavame 1 variantni heslo pro heslo A", recordA.getVariantRecords().size() == 1);
        Assert.assertTrue("Ocekavame charakteristiku \"" + changedCharacter + "\"", recordA.getCharacteristics().equals(changedCharacter));

        deleteRegCoordinates(coordinates.getId());
        recordA = getRecord(recordA.getId());
        Assert.assertTrue("Ocekavame 0 souradnic pro heslo A", recordA.getCoordinates().isEmpty());

        RegRecordVO recordC = new RegRecordVO();
        RegRecordVO recordD = new RegRecordVO();
        recordC.setParentRecordId(recordA.getId());
        recordD.setParentRecordId(recordA.getId());

        recordC.setRegisterTypeId(hierarchal.getId());
        recordD.setRegisterTypeId(hierarchal.getId());

        recordC.setCharacteristics("Ja jsem regRecordC");
        recordD.setCharacteristics("Ja jsem regRecordD");

        recordC.setRecord("RegRecordC");
        recordD.setRecord("RegRecordD");

        recordC.setScopeId(scopeId);
        recordD.setScopeId(scopeId);

        recordC = createRecord(recordC);
        recordD = createRecord(recordD);

        recordA = getRecord(recordA.getId());
        Assert.assertTrue("Ocekavame 2 potomky recordu A", recordA.getChilds().size() == 2);

        /* změna parent record type id */
        RegRegisterTypeVO newHierarchicalType = getHierarchicalRegRegisterType(types, Collections.singletonList(hierarchal), false);
        recordA.setRegisterTypeId(newHierarchicalType.getId());
        recordA = updateRecord(recordA);

        deleteRecord(recordD.getId());

        list = findRecord(null, 0, 10, null, null, null);
        Assert.assertTrue(list.size() == 3);

        list = findRecord("RegRecordA", 0, 10, null, null, null);
        Assert.assertTrue(list.size() == 1);

        list = findRecord("RegRecordB", 0, 10, null, null, null);
        Assert.assertTrue(list.size() == 1);

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

        List<RegRegisterTypeVO> types = getRecordTypes();
        List<RegScopeVO> scopes = getAllScopes();
        Integer scopeId = scopes.iterator().next().getId();

        // Vytvoření replace
        RegRecordVO replacedRecord = new RegRecordVO();

        replacedRecord.setRegisterTypeId(getHierarchicalRegRegisterType(types, null, false).getId());

        replacedRecord.setCharacteristics("Ja jsem regRecordA");

        replacedRecord.setRecord("RegRecordA");

        replacedRecord.setScopeId(scopeId);

        replacedRecord.setHierarchical(true);
        replacedRecord.setAddRecord(true);

        replacedRecord = createRecord(replacedRecord);
        Assert.assertNotNull(replacedRecord.getId());


        // Vytvoření node register
        ArrNodeRegisterVO nodeRegister = new ArrNodeRegisterVO();

        nodeRegister.setValue(replacedRecord.getId());
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
        RegRecordVO replacementRecord = new RegRecordVO();

        replacementRecord.setRegisterTypeId(getHierarchicalRegRegisterType(types, null, false).getId());

        replacementRecord.setCharacteristics("Ja jsem regRecordB");

        replacementRecord.setRecord("RegRecordB");

        replacementRecord.setScopeId(scopeId);

        replacementRecord.setHierarchical(true);
        replacementRecord.setAddRecord(true);

        replacementRecord = createRecord(replacementRecord);
        Assert.assertNotNull(replacementRecord.getId());

        // Dohledání usages
        RecordUsageVO usage = usagesRecord(replacedRecord.getId());
        Assert.assertNotNull(usage.funds);

        // Replace
        replaceRecord(replacedRecord.getId(), replacementRecord.getId());
        RecordUsageVO usageAfterReplace = usagesRecord(replacedRecord.getId());
        Assert.assertTrue(usageAfterReplace.funds == null || usageAfterReplace.funds.isEmpty());
    }

    private RegRegisterTypeVO getNonHierarchicalRegRegisterType(final List<RegRegisterTypeVO> list, final boolean hasPartyType) {
        for (RegRegisterTypeVO type : list) {
            if (!type.getHierarchical() && type.getAddRecord() && ((!hasPartyType && type.getPartyTypeId() == null) || (hasPartyType && type.getPartyTypeId() != null))) {
                return type;
            }
        }

        for (RegRegisterTypeVO type : list) {
            if (type.getChildren() != null) {
                RegRegisterTypeVO res = getNonHierarchicalRegRegisterType(type.getChildren(), hasPartyType);
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

}
