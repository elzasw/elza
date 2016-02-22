package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;


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
        scopeVO = updateScopeTest(scopeVO.getId(), scopeVO);
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
     * @param id id třídy
     */
    private RegScopeVO updateScopeTest(final int id, final RegScopeVO scope) {
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
     * Vrací výchozí třídy rejstříků z databáze.
     */
    @Test
    public void getDefaultScopesTest() {
        getDefaultScopes();
    }

    private static final String NON_HIERARCHIC_REGISTER_TYPE_CODE = "ARTWORK_CONSTR";
    private static final String HIERARCHIC_REGISTER_TYPE_CODE = "GEO_SPACE";

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
        RegRecordVO recordA = new RegRecordVO();
        RegRecordVO recordB = new RegRecordVO();
        List<RegRegisterTypeVO> types = getRecordTypes();
        List<RegScopeVO> scopes = getAllScopes();

        Assert.assertTrue(scopes != null && scopes.size() > 0);
        Integer scopeId = scopes.iterator().next().getId();

        RegRegisterTypeVO hierarchal = getHierarchicalRegRegisterType(types);
        RegRegisterTypeVO nonHierarchal = getNonHierarchicalRegRegisterType(types);

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

        variant1.setRegRecordId(recordA.getRecordId());
        variant2.setRegRecordId(recordA.getRecordId());

        variant1 = createVariantRecord(variant1);
        variant2 = createVariantRecord(variant2);

        recordA = getRecord(recordA.getRecordId());
        Assert.assertTrue("Ocekavame 2 variantni hesla pro heslo A", recordA.getVariantRecords().size() == 2);

        String changedCharacter = "Ja nechci byt regRecordA.";
        recordA.setCharacteristics(changedCharacter);
        updateRecord(recordA);

        String changedRecord = "Upraveny record";
        variant2.setRecord(changedRecord);
        variant2 = updateVariantRecord(variant2);
        Assert.assertTrue("Ocekavame upravny record", variant2.getRecord().equals(changedRecord));

        deleteVariantRecord(variant2.getVariantRecordId());
        recordA = getRecord(recordA.getRecordId());
        Assert.assertTrue("Ocekavame 1 variantni heslo pro heslo A", recordA.getVariantRecords().size() == 1);
        Assert.assertTrue("Ocekavame charakteristiku \"" + changedCharacter + "\"", recordA.getCharacteristics().equals(changedCharacter));

        RegRecordVO recordC = new RegRecordVO();
        RegRecordVO recordD = new RegRecordVO();
        recordC.setParentRecordId(recordA.getRecordId());
        recordD.setParentRecordId(recordA.getRecordId());

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

        recordA = getRecord(recordA.getRecordId());
        Assert.assertTrue("Ocekavame 2 potomky recordu A", recordA.getChilds().size() == 2);

        deleteRecord(recordD.getRecordId());

        list = findRecord(null, 0, 10, null, null, null);
        Assert.assertTrue(list.size() == 3);

        list = findRecord("RegRecordA", 0, 10, null, null, null);
        Assert.assertTrue(list.size() == 1);

        list = findRecord("RegRecordB", 0, 10, null, null, null);
        Assert.assertTrue(list.size() == 1);

    }

    private RegRegisterTypeVO getNonHierarchicalRegRegisterType(List<RegRegisterTypeVO> list) {
        for (RegRegisterTypeVO type : list) {
            if (!type.getHierarchical() && type.getAddRecord()) {
                return type;
            }
        }

        for (RegRegisterTypeVO type : list) {
            if (type.getChildren() != null) {
                RegRegisterTypeVO res = getNonHierarchicalRegRegisterType(type.getChildren());
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

}
