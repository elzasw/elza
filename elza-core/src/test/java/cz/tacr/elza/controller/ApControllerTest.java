package cz.tacr.elza.controller;

import java.util.List;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Petr Compel
 * @since 18.2.2016
 */
public class ApControllerTest extends AbstractControllerTest {

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
