package cz.tacr.elza.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Objects;

import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.controller.ArrangementController;
import cz.tacr.elza.controller.ArrangementController.DescFormDataNewVO;
import cz.tacr.elza.controller.ArrangementController.DescItemResult;
import cz.tacr.elza.controller.vo.ArrDaoLinkVO;
import cz.tacr.elza.controller.vo.ArrDaoVO;
import cz.tacr.elza.controller.vo.ArrDigitalRepositoryVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.service.DaoSyncService;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.ws.core.v1.DaoService;
import cz.tacr.elza.ws.core.v1.FundService;
import cz.tacr.elza.ws.types.v1.Dao;
import cz.tacr.elza.ws.types.v1.DaoImport;
import cz.tacr.elza.ws.types.v1.DaoLinks;
import cz.tacr.elza.ws.types.v1.DaoPackage;
import cz.tacr.elza.ws.types.v1.DaoPackages;
import cz.tacr.elza.ws.types.v1.DaoType;
import cz.tacr.elza.ws.types.v1.Daoset;
import cz.tacr.elza.ws.types.v1.FundIdentifiers;
import cz.tacr.elza.ws.types.v1.ItemEnum;
import cz.tacr.elza.ws.types.v1.Items;
import cz.tacr.elza.ws.types.v1.ObjectFactory;
import io.restassured.RestAssured;

public class DaoCoreServiceTest extends AbstractControllerTest {

    static final String DIGIT_REPO_CODE = "DigitRepo1";
    static final String DIGIT_REPO_NAME = "Digit repo 1";
    static final String FUND_NAME = "TestFund";
    static final String FUND_CODE = "TestFundCode";
    static final String PACKAGE_ID1 = "PackageId1";
    static final String TEXT_VALUE_XY = "value xy";
    static final String TEXT_VALUE_YZ = "value yz";

    private ObjectFactory objFactory = new ObjectFactory();
    private DaoService daoServiceClient;
    private Fund fundInfo;
    private ArrFundVersionVO fundVersion;

    private void createDigitalRepo() {
        ArrDigitalRepositoryVO digitalRepositoryVO = new ArrDigitalRepositoryVO();
        digitalRepositoryVO.setCode(DIGIT_REPO_CODE);
        digitalRepositoryVO.setName(DIGIT_REPO_NAME);
        digitalRepositoryVO.setSendNotification(false);

        SysExternalSystemVO digitalRepositoryCreatedVO = createExternalSystem(digitalRepositoryVO);
        assertNotNull(digitalRepositoryCreatedVO.getId());

    }

    private DaoService createDaoServiceClient() {
        String address = RestAssured.baseURI + ":" + RestAssured.port + "/services"
                + WebServiceConfig.DAO_CORE_SERVICE_URL;
        return WebServiceClientFactory.createDaoService(address, "admin", "admin");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        createDigitalRepo();
        daoServiceClient = createDaoServiceClient();
    }

    @After
    public void tearDown() {
        daoServiceClient = null;

        super.tearDown();
    }

    @Test
    public void importTestAttach() throws ApiException {
        // create AS
        fundInfo = createFund(FUND_NAME, FUND_CODE);
        fundVersion = getOpenVersion(fundInfo);

        // prepare dao package
        DaoPackage daoPackage = createDaoPackage(FUND_CODE, DIGIT_REPO_CODE, PACKAGE_ID1,
                                                 DaoType.ATTACHMENT,
                                                 "Testovaci DAO", null);
        DaoImport daoImport = createDaoImport(daoPackage);

        daoServiceClient._import(daoImport);

        List<ArrDaoVO> daos = this.findDaos(fundVersion.getId());
        Assert.assertEquals(1, daos.size());
        ArrDaoVO daoVo = daos.get(0);
        assertNull(daoVo.getDaoLink());

        // connect
        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);
        // Musí existovat root node
        assertNotNull(treeData.getNodes());
        // Musí existovat pouze root node
        assertEquals(1, treeData.getNodes().size());
        TreeNodeVO rootTreeNodeClient = treeData.getNodes().iterator().next();
        ArrNodeVO rootNode = convertTreeNode(rootTreeNodeClient);

        ArrDaoLinkVO linkVo = createDaoLink(fundVersion.getId(), daoVo.getId(), rootNode.getId());
        assertNotNull(linkVo);
        assertNotNull(linkVo.getId());
        Integer newNodeId = linkVo.getTreeNodeClient().getId();
        assertNotNull(linkVo);

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

    @Test
    public void importTestLevel() throws ApiException {
        // create AS
        fundInfo = createFund(FUND_NAME, FUND_CODE);
        fundVersion = getOpenVersion(fundInfo);

        // import DAO
        Items daoItems = createDaoItems(TEXT_VALUE_XY);
        // prepare dao package
        DaoPackage daoPackage = createDaoPackage(FUND_CODE, DIGIT_REPO_CODE, PACKAGE_ID1,
                                                 DaoType.LEVEL,
                                                 "Testovaci DAO", daoItems);
        DaoImport daoImport = createDaoImport(daoPackage);

        daoServiceClient._import(daoImport);

        helperTestService.waitForWorkers();

        // Check not free DAOS exists
        List<ArrDaoVO> daos = this.findDaos(fundVersion.getId());
        Assert.assertEquals(0, daos.size());

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);
        // Musí existovat root node
        assertNotNull(treeData.getNodes());
        // Musí existovat root node + serie pro import
        assertEquals(2, treeData.getNodes().size());
        Iterator<TreeNodeVO> treeNodeIter = treeData.getNodes().iterator();
        TreeNodeVO rootTreeNodeClient = treeNodeIter.next();
        assertEquals(rootTreeNodeClient.getDepth(), Integer.valueOf(1));
        //ArrNodeVO rootNode = convertTreeNode(rootTreeNodeClient);
        TreeNodeVO serieTreeNodeClient = treeNodeIter.next();
        assertEquals(serieTreeNodeClient.getDepth(), Integer.valueOf(2));
        // druhe vyzadani vcetne rozbalene serie
        Set<Integer> expandedIds = Stream.of(rootTreeNodeClient.getId(), serieTreeNodeClient.getId()).collect(Collectors
                .toCollection(HashSet::new));
        input.setExpandedIds(expandedIds);
        treeData = getFundTree(input);
        assertEquals(3, treeData.getNodes().size());

        treeNodeIter = treeData.getNodes().iterator();
        TreeNodeVO levelNode = null;
        while (treeNodeIter.hasNext()) {
            TreeNodeVO nv = treeNodeIter.next();
            if (!expandedIds.contains(nv.getId())) {
                levelNode = nv;
                break;
            }
        }
        assertNotNull(levelNode);
        assertEquals(levelNode.getDepth(), Integer.valueOf(3));

        // check form data - if extracted from dao
        DescFormDataNewVO formData = getNodeFormData(levelNode.getId(), fundVersion.getId());
        assertNotNull(formData);
        List<ArrItemVO> descItems = formData.getDescItems();
        assertEquals(1, descItems.size());
        ArrItemTextVO descItemTextVO = checkExistsTextVO(descItems, "SRD_TITLE", TEXT_VALUE_XY);
        assertTrue(descItemTextVO.getReadOnly());

        // opakovaný import dao/update
        Items daoItems2 = createDaoItems(TEXT_VALUE_YZ);
        // prepare dao package
        DaoPackage daoPackage2 = createDaoPackage(FUND_CODE, DIGIT_REPO_CODE, PACKAGE_ID1,
                                                  DaoType.LEVEL,
                                                  "Testovaci DAO", daoItems2);
        DaoImport daoImport2 = createDaoImport(daoPackage2);

        daoServiceClient._import(daoImport2);

        // refresh tree
        treeData = getFundTree(input);
        assertNotNull(treeData.getNodes());
        // Musí existovat root node + serie pro import + uzel, tj. nevznikne dalsi serie
        assertEquals(3, treeData.getNodes().size());

        // check form data - if extracted from dao
        formData = getNodeFormData(levelNode.getId(), fundVersion.getId());
        assertNotNull(formData);
        descItems = formData.getDescItems();
        assertEquals(1, descItems.size());
        descItemTextVO = checkExistsTextVO(descItems, "SRD_TITLE", TEXT_VALUE_YZ);
        assertTrue(descItemTextVO.getReadOnly());

        helperTestService.waitForWorkers();
    }

    @Test
    public void importTestLevelWithScenarios() throws ApiException {
        // create AS
        fundInfo = createFund(FUND_NAME, FUND_CODE);
        fundVersion = getOpenVersion(fundInfo);

        // import DAO
        Items daoItems = createDaoScenarios(TEXT_VALUE_XY);
        // prepare dao package
        DaoPackage daoPackage = createDaoPackage(FUND_CODE, DIGIT_REPO_CODE, PACKAGE_ID1,
                                                 DaoType.LEVEL,
                                                 "Testovaci DAO", daoItems);
        DaoImport daoImport = createDaoImport(daoPackage);

        daoServiceClient._import(daoImport);

        helperTestService.waitForWorkers();

        // Check not free DAOS exists
        List<ArrDaoVO> daos = this.findDaos(fundVersion.getId());
        Assert.assertEquals(0, daos.size());

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);
        // Musí existovat root node
        assertNotNull(treeData.getNodes());
        // Musí existovat root node + serie pro import
        assertEquals(2, treeData.getNodes().size());
        Iterator<TreeNodeVO> treeNodeIter = treeData.getNodes().iterator();
        TreeNodeVO rootTreeNodeClient = treeNodeIter.next();
        assertEquals(rootTreeNodeClient.getDepth(), Integer.valueOf(1));
        //ArrNodeVO rootNode = convertTreeNode(rootTreeNodeClient);
        TreeNodeVO serieTreeNodeClient = treeNodeIter.next();
        assertEquals(serieTreeNodeClient.getDepth(), Integer.valueOf(2));
        // druhe vyzadani vcetne rozbalene serie
        Set<Integer> expandedIds = Stream.of(rootTreeNodeClient.getId(), serieTreeNodeClient.getId()).collect(Collectors
                .toCollection(HashSet::new));
        input.setExpandedIds(expandedIds);
        treeData = getFundTree(input);
        assertEquals(3, treeData.getNodes().size());

        treeNodeIter = treeData.getNodes().iterator();
        TreeNodeVO levelNode = null;
        while (treeNodeIter.hasNext()) {
            TreeNodeVO nv = treeNodeIter.next();
            if (!expandedIds.contains(nv.getId())) {
                levelNode = nv;
                break;
            }
        }
        assertNotNull(levelNode);
        assertEquals(levelNode.getDepth(), Integer.valueOf(3));

        // check form data - if extracted from dao
        DescFormDataNewVO formData = getNodeFormData(levelNode.getId(), fundVersion.getId());
        assertNotNull(formData);
        List<ArrItemVO> descItems = formData.getDescItems();
        assertEquals(2, descItems.size());
        ArrItemTextVO descItemTextVO = checkExistsTextVO(descItems, "SRD_TITLE", TEXT_VALUE_XY);
        assertTrue(descItemTextVO.getReadOnly() == null || !descItemTextVO.getReadOnly());

        // prepnuti na sc2
        List<ArrDaoVO> daoVos = this.findDaos(fundVersion.getId(), levelNode.getId());
        assertTrue(daoVos.size() == 1);
        ArrDaoVO daoVo = daoVos.get(0);
        ArrDaoLinkVO daoLinkVO = daoVo.getDaoLink();
        assertNotNull(daoLinkVO);
        assertEquals(daoLinkVO.getScenario(), "sc1");

        // aktualizace hodnoty
        helperTestService.waitForWorkers();
        descItemTextVO.setValue("update value");
        ArrNodeVO nodeVO = convertTreeNode(levelNode);
        DescItemResult descItemResult = updateDescItem(descItemTextVO, fundVersion, nodeVO, true);
        helperTestService.waitForWorkers();

        this.daosApi.changeLinkScenario(daoVo.getId(), "sc2");
        daoVos = this.findDaos(fundVersion.getId(), levelNode.getId());
        assertTrue(daoVos.size() == 1);
        daoVo = daoVos.get(0);
        daoLinkVO = daoVo.getDaoLink();
        assertNotNull(daoLinkVO);
        assertEquals("sc2", daoLinkVO.getScenario());

        // opakovaný import dao/update
        Items daoItems2 = createDaoScenarios(TEXT_VALUE_YZ);
        // prepare dao package
        DaoPackage daoPackage2 = createDaoPackage(FUND_CODE, DIGIT_REPO_CODE, PACKAGE_ID1,
                                                  DaoType.LEVEL,
                                                  "Testovaci DAO", daoItems2);
        DaoImport daoImport2 = createDaoImport(daoPackage2);

        daoServiceClient._import(daoImport2);

        // refresh tree
        treeData = getFundTree(input);
        assertNotNull(treeData.getNodes());
        // Musí existovat root node + serie pro import + uzel, tj. nevznikne dalsi serie
        assertEquals(3, treeData.getNodes().size());

        daoVos = this.findDaos(fundVersion.getId(), levelNode.getId());
        assertTrue(daoVos.size() == 1);
        daoVo = daoVos.get(0);
        daoLinkVO = daoVo.getDaoLink();
        assertNotNull(daoLinkVO);
        assertEquals("sc2", daoLinkVO.getScenario());

        // check form data - if extracted from dao
        formData = getNodeFormData(levelNode.getId(), fundVersion.getId());
        assertNotNull(formData);
        descItems = formData.getDescItems();
        assertEquals(2, descItems.size());
        descItemTextVO = checkExistsTextVO(descItems, "SRD_TITLE", "update value");
        assertTrue(descItemTextVO.getReadOnly() == null || !descItemTextVO.getReadOnly());

        helperTestService.waitForWorkers();
    }

    // shodne s vyse, jen maximalni interakce pres WS
    @Test
    public void importTestLevelWithScenariosWS() throws ApiException {
        String address = RestAssured.baseURI + ":" + RestAssured.port + "/services"
                + WebServiceConfig.FUND_SERVICE_URL;
        FundService fundServiceClient = WebServiceClientFactory.createFundService(address, "admin", "admin");

        cz.tacr.elza.ws.types.v1.Fund fundCreate = new cz.tacr.elza.ws.types.v1.Fund();
        fundCreate.setFundName("Test fund XYZ");
        fundCreate.setRulesetCode("SIMPLE-DEV");
        fundCreate.setInstitutionIdentifier("in1");
        fundCreate.setDateRange("date range");
        fundCreate.setInternalCode(FUND_CODE);
        fundCreate.setFundNumber("100");

        FundIdentifiers fundCreated = fundServiceClient.createFund(fundCreate);

        Assert.assertNotNull(fundCreated);
        assertTrue(Integer.valueOf(fundCreated.getId()) >= 1);
        Assert.assertNotNull(UUID.fromString(fundCreated.getUuid()));

        // import DAO
        Items daoItems = createDaoScenarios(TEXT_VALUE_XY);
        // prepare dao package
        DaoPackage daoPackage = createDaoPackage(FUND_CODE, DIGIT_REPO_CODE, PACKAGE_ID1,
                                                 DaoType.LEVEL,
                                                 "Testovaci DAO", daoItems);
        DaoImport daoImport = createDaoImport(daoPackage);

        daoServiceClient._import(daoImport);

        helperTestService.waitForWorkers();

        ArrFundVersionVO fundVersion = getOpenVersion(Integer.valueOf(fundCreated.getId()));

        // read data from levelcache
        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);
        // result has to contain to nodes
        assertEquals(treeData.getNodes().size(), 2);
        // first node has no mark
        Iterator<TreeNodeVO> nodeIt = treeData.getNodes().iterator();
        TreeNodeVO firstNode = nodeIt.next();
        String[] refMark = firstNode.getReferenceMark();
        assertTrue(refMark == null || refMark.length == 0);
        TreeNodeVO secondNode = nodeIt.next();
        String[] refMark2 = secondNode.getReferenceMark();
        assertEquals(refMark2.length, 1);
        assertEquals(refMark2[0], "1");
    }

    private ArrItemTextVO checkExistsTextVO(List<ArrItemVO> descItems, String itemTypeCode, String textValue) {
        RulDescItemTypeExtVO itemType = findDescItemTypeByCode(itemTypeCode);
        assertNotNull("ItemType not found: " + itemTypeCode, itemType);

        for (ArrItemVO descItem : descItems) {
            Integer itemTypeId = descItem.getItemTypeId();
            if (!Objects.equal(itemType.getId(), itemTypeId)) {
                continue;
            }
            assertTrue(descItem instanceof ArrItemTextVO);
            ArrItemTextVO textVo = (ArrItemTextVO) descItem;
            if (Objects.equal(textValue, textVo.getValue())) {
                return textVo;
            }
        }
        fail("Item type not found: " + itemTypeCode);
        return null;
    }

    private DaoImport createDaoImport(DaoPackage daoPackage) {
        DaoImport daoImport = objFactory.createDaoImport();
        DaoLinks daoLinks = objFactory.createDaoLinks();
        daoImport.setDaoLinks(daoLinks);
        DaoPackages daoPackages = objFactory.createDaoPackages();
        daoPackages.getDaoPackage().add(daoPackage);
        daoImport.setDaoPackages(daoPackages);
        return daoImport;
    }

    private Items createDaoScenarios(String textValue) {
        cz.tacr.elza.ws.types.v1.Items items = objFactory.createItems();
        cz.tacr.elza.ws.types.v1.ItemString its = objFactory.createItemString();
        its.setType(DaoSyncService.ELZA_SCENARIO);
        its.setValue("sc1");
        items.getStrOrLongOrEnm().add(its);
        // add level type as readonly
        ItemEnum ite = objFactory.createItemEnum();
        ite.setType("SRD_LEVEL_TYPE");
        ite.setSpec("SRD_LEVEL_FOLDER");
        ite.setReadOnly(true);
        items.getStrOrLongOrEnm().add(ite);

        its = objFactory.createItemString();
        its.setType("SRD_TITLE");
        its.setValue(textValue);
        items.getStrOrLongOrEnm().add(its);

        // scenario 2
        its = objFactory.createItemString();
        its.setType(DaoSyncService.ELZA_SCENARIO);
        its.setValue("sc2");
        items.getStrOrLongOrEnm().add(its);
        // add level type as readonly
        ite = objFactory.createItemEnum();
        ite.setType("SRD_LEVEL_TYPE");
        ite.setSpec("SRD_LEVEL_ITEM");
        ite.setReadOnly(true);
        items.getStrOrLongOrEnm().add(ite);

        its = objFactory.createItemString();
        its.setType("SRD_TITLE");
        its.setValue(textValue);
        items.getStrOrLongOrEnm().add(its);
        return items;
    }

    private Items createDaoItems(String textValue) {
        cz.tacr.elza.ws.types.v1.Items items = objFactory.createItems();
        cz.tacr.elza.ws.types.v1.ItemString its = objFactory.createItemString();
        its.setType("SRD_TITLE");
        its.setValue(textValue);
        its.setReadOnly(true);
        items.getStrOrLongOrEnm().add(its);
        return items;
    }

    private DaoPackage createDaoPackage(String fundCode, String digitRepoCode, String packageId, DaoType daoType,
                                        String label, Items items) {
        DaoPackage daoPackage = objFactory.createDaoPackage();
        daoPackage.setFundIdentifier(fundCode);
        daoPackage.setRepositoryIdentifier(digitRepoCode);
        daoPackage.setIdentifier(packageId);
        Daoset daoset = objFactory.createDaoset();
        Dao dao = objFactory.createDao();
        dao.setDaoType(daoType);
        dao.setIdentifier(packageId);
        dao.setLabel(label);

        dao.setItems(items);
        daoset.getDao().add(dao);
        daoPackage.setDaos(daoset);
        return daoPackage;
    }

}
