package cz.tacr.elza.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import io.restassured.RestAssured;

import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.controller.ArrangementController;
import cz.tacr.elza.controller.ArrangementController.DescFormDataNewVO;
import cz.tacr.elza.controller.vo.ArrDaoLinkVO;
import cz.tacr.elza.controller.vo.ArrDaoVO;
import cz.tacr.elza.controller.vo.ArrDigitalRepositoryVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.ws.core.v1.DaoService;
import cz.tacr.elza.ws.types.v1.Dao;
import cz.tacr.elza.ws.types.v1.DaoImport;
import cz.tacr.elza.ws.types.v1.DaoLinks;
import cz.tacr.elza.ws.types.v1.DaoPackage;
import cz.tacr.elza.ws.types.v1.DaoPackages;
import cz.tacr.elza.ws.types.v1.DaoType;
import cz.tacr.elza.ws.types.v1.Daoset;
import cz.tacr.elza.ws.types.v1.ObjectFactory;

public class DaoCoreServiceTest extends AbstractControllerTest {

    static final String DIGIT_REPO_CODE = "DigitRepo1";
    static final String DIGIT_REPO_NAME = "Digit repo 1";
    static final String FUND_NAME = "TestFund";
    static final String FUND_CODE = "TestFundCode";
    static final String PACKAGE_ID1 = "PackageId1";
    static final String TEXT_VALUE_XY = "value xy";

    private ObjectFactory objFactory = new ObjectFactory();

    private void createDigitalRepo() {
        ArrDigitalRepositoryVO digitalRepositoryVO = new ArrDigitalRepositoryVO();
        digitalRepositoryVO.setCode(DIGIT_REPO_CODE);
        digitalRepositoryVO.setName(DIGIT_REPO_NAME);
        digitalRepositoryVO.setSendNotification(false);

        SysExternalSystemVO digitalRepositoryCreatedVO = createExternalSystem(digitalRepositoryVO);
        assertNotNull(digitalRepositoryCreatedVO.getId());

    }

    @Test
    public void importTest() {
        createDigitalRepo();

        String address = RestAssured.baseURI + ":" + RestAssured.port + "/services"
                + WebServiceConfig.DAO_CORE_SERVICE_URL;
        DaoService daoServiceClient = DaoServiceClientFactory.createDaoService(address, "admin", "admin");

        // create AS
        ArrFundVO fundInfo = this.createFund(FUND_NAME, FUND_CODE);
        ArrFundVersionVO fundVersion = getOpenVersion(fundInfo);
        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);
        // Musí existovat root node
        assertNotNull(treeData.getNodes());
        // Musí existovat pouze root node
        assertEquals(1, treeData.getNodes().size());
        TreeNodeVO rootTreeNodeClient = treeData.getNodes().iterator().next();
        ArrNodeVO rootNode = convertTreeNode(rootTreeNodeClient);

        // import DAO
        DaoImport daoImport = objFactory.createDaoImport();
        DaoLinks daoLinks = objFactory.createDaoLinks();
        daoImport.setDaoLinks(daoLinks);
        DaoPackages daoPackages = objFactory.createDaoPackages();

        // prepare dao package
        DaoPackage daoPackage = createDaoPackage(FUND_CODE, DIGIT_REPO_CODE, PACKAGE_ID1,
                                                 DaoType.LEVEL,
                                                 "Testovaci DAO");
        daoPackages.getDaoPackage().add(daoPackage);
        daoImport.setDaoPackages(daoPackages);
        daoServiceClient._import(daoImport);

        List<ArrDaoVO> daos = this.findDaos(fundVersion.getId());
        Assert.assertEquals(1, daos.size());
        ArrDaoVO daoVo = daos.get(0);

        // connect
        ArrDaoLinkVO linkVo = createDaoLink(fundVersion.getId(), daoVo.getId(), rootNode.getId());
        assertNotNull(linkVo);
        assertNotNull(linkVo.getId());
        Integer newNodeId = linkVo.getTreeNodeClient().getId();
        assertNotNull(linkVo);

        // check form data - if extracted from dao
        DescFormDataNewVO formData = getNodeFormData(newNodeId, fundVersion.getId());
        assertNotNull(formData);
        List<ArrItemVO> descItems = formData.getDescItems();
        assertEquals(1, descItems.size());
        ArrItemVO descItem = descItems.get(0);
        assertTrue(descItem.getReadOnly());
        assertTrue(descItem instanceof ArrItemTextVO);
        ArrItemTextVO descItemTextVO = (ArrItemTextVO) descItem;
        assertEquals(TEXT_VALUE_XY, descItemTextVO.getValue());

        // ověření napojeni - neexistence volnych dao
        List<ArrDaoVO> daosConnected = findDaos(fundVersion.getId());
        assertEquals(0, daosConnected.size());

        helperTestService.waitForWorkers();
        // disconnect
        deleteDaoLink(fundVersion.getId(), linkVo.getId());

        // ověření vztahu
        List<ArrDaoVO> daosConnected2 = findDaos(fundVersion.getId());
        assertEquals(1, daosConnected2.size());
        ArrDaoVO daoConnected2 = daosConnected2.get(0);
        ArrDaoLinkVO daoLinkVo2 = daoConnected2.getDaoLink();
        assertNull(daoLinkVo2);
    }

    private DaoPackage createDaoPackage(String fundCode, String digitRepoCode, String packageId, DaoType daoType,
                                        String label) {
        DaoPackage daoPackage = objFactory.createDaoPackage();
        daoPackage.setFundIdentifier(fundCode);
        daoPackage.setRepositoryIdentifier(digitRepoCode);
        daoPackage.setIdentifier(packageId);
        Daoset daoset = objFactory.createDaoset();
        Dao dao = objFactory.createDao();
        dao.setDaoType(daoType);
        dao.setIdentifier(packageId);
        dao.setLabel(label);

        cz.tacr.elza.ws.types.v1.Items items = objFactory.createItems();
        cz.tacr.elza.ws.types.v1.ItemString its = objFactory.createItemString();
        its.setType("SRD_TITLE");
        its.setValue(TEXT_VALUE_XY);
        its.setReadOnly(true);
        items.getStrOrLongOrEnm().add(its);

        dao.setItems(items);
        daoset.getDao().add(dao);
        daoPackage.setDaos(daoset);
        return daoPackage;
    }

}
