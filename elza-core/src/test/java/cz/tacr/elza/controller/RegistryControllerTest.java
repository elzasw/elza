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

        replacedRecord.setRegisterTypeId(getNonHierarchicalRegRegisterType(types, false).getId());

        replacedRecord.setCharacteristics("Ja jsem regRecordA");

        replacedRecord.setRecord("RegRecordA");

        replacedRecord.setScopeId(scopeId);

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

        replacementRecord.setRegisterTypeId(getNonHierarchicalRegRegisterType(types, false).getId());

        replacementRecord.setCharacteristics("Ja jsem regRecordB");

        replacementRecord.setRecord("RegRecordB");

        replacementRecord.setScopeId(scopeId);

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
            if (type.getAddRecord() && ((!hasPartyType && type.getPartyTypeId() == null) || (hasPartyType && type.getPartyTypeId() != null))) {
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
