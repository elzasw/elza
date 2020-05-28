package cz.tacr.elza.ws;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;

import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.controller.ArrangementController;
import cz.tacr.elza.controller.vo.ArrDaoLinkVO;
import cz.tacr.elza.controller.vo.ArrDaoVO;
import cz.tacr.elza.controller.vo.ArrDigitalRepositoryVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.ws.core.v1.DaoCoreServiceImpl;
import cz.tacr.elza.ws.core.v1.DaoService;
import cz.tacr.elza.ws.core.v1.items.ItemString;
import cz.tacr.elza.ws.core.v1.items.Items;
import cz.tacr.elza.ws.types.v1.Attribute;
import cz.tacr.elza.ws.types.v1.Attributes;
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
    @Ignore
    public void importTest() throws JsonProcessingException {
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
        assertTrue(treeData.getNodes().size() == 1);
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
        Assert.assertTrue(daos.size()==1);
        ArrDaoVO daoVo = daos.get(0);

        // connect
        ArrDaoLinkVO linkVo = createDaoLink(fundVersion.getId(), daoVo.getId(), rootNode.getId());
        assertNotNull(linkVo);
        assertNotNull(linkVo.getId());

        // disconnect
        deleteDaoLink(fundVersion.getId(), linkVo.getId());
    }

    private DaoPackage createDaoPackage(String fundCode, String digitRepoCode, String packageId, DaoType daoType,
                                        String label) throws JsonProcessingException {
        DaoPackage daoPackage = objFactory.createDaoPackage();
        daoPackage.setFundIdentifier(fundCode);
        daoPackage.setRepositoryIdentifier(digitRepoCode);
        daoPackage.setIdentifier(packageId);
        Daoset daoset = objFactory.createDaoset();
        Dao dao = objFactory.createDao();
        dao.setDaoType(daoType);
        dao.setIdentifier(packageId);
        dao.setLabel(label);
        Attributes attrs = objFactory.createAttributes();
        List<Attribute> attrList = attrs.getAttribute();
        Attribute attr = objFactory.createAttribute();

        Items items = new Items();
        ItemString its = new ItemString();
        its.setItemType("SRD_TITLE");
        its.setValue("value xy");
        its.setReadOnly(true);
        items.getItems().add(its);
        String itemValues = new ObjectMapper().writeValueAsString(items);

        attr.setName(DaoCoreServiceImpl.ITEMS);
        attr.setValue(itemValues);
        attrList.add(attr);

        dao.setAttributes(attrs);
        daoset.getDao().add(dao);
        daoPackage.setDaos(daoset);
        return daoPackage;
    }

}
