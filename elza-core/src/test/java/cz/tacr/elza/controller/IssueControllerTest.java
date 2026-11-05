package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ArrFundBaseVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.controller.vo.WfCommentVO;
import cz.tacr.elza.controller.vo.WfIssueListVO;
import cz.tacr.elza.controller.vo.WfIssueStateVO;
import cz.tacr.elza.controller.vo.WfIssueTypeVO;
import cz.tacr.elza.controller.vo.WfIssueVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.NodeBrief;
import cz.tacr.elza.test.controller.vo.NodeData;
import cz.tacr.elza.test.controller.vo.NodeDataParam;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IssueControllerTest extends AbstractControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(IssueControllerTest.class);

    private static final String PASSWORD = "passwd";

    @BeforeAll
    public void initOnce() throws Exception {
        super.setUp();
    }

    @AfterAll
    public void cleanupOnce() {
        super.tearDown();
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        // no-op: setup is done once in @BeforeAll initOnce()
    }

    @Override
    @AfterEach
    public void tearDown() {
        // no-op: cleanup is done once in @AfterAll cleanupOnce()
    }

    /**
     * Získání druhů připomínek.
     */
    @Test
    public void findAllIssueTypesTest() {
        List<WfIssueTypeVO> issueTypes = findAllIssueTypes();
        assertNotNull(issueTypes);
        assertFalse(issueTypes.isEmpty());
    }

    /**
     * Získání stavů připomínek.
     */
    @Test
    public void findAllIssueStatesTest() {
        List<WfIssueStateVO> issueStates = findAllIssueStates();
        assertNotNull(issueStates);
        assertFalse(issueStates.isEmpty());
    }

    /**
     * Pozitivni test na operace s protokoly, pripominkami a komentari - formou scenare.
     * Test zohlednuje i opravneni uzivatelu.
     */
    @Test
    public void issueTest() throws Exception {
        Fund fund = createFund();

        List<ApAccessPointVO> records = findRecord(null, null, null, null, null);
        ApAccessPointVO ap = records.get(0);

        UsrUserVO adminUserVO = createAdmin("admin1", fund.getId(), ap);

        UsrUserVO userVO1 = createUser("user1", ap);
        UsrUserVO userVO2 = createUser("user2", ap);
        UsrUserVO userVO3 = createUser("user3", ap);
        UsrUserVO userVO4 = createUser("user4", ap);

        // Pridani zakl. opravneni uzivatelum
        UsrPermissionVO usePermVO = new UsrPermissionVO();
        usePermVO.setPermission(Permission.FUND_RD_ALL);
        addUserPermission(userVO1.getId(), Arrays.asList(usePermVO));
        addUserPermission(userVO2.getId(), Arrays.asList(usePermVO));
        addUserPermission(userVO3.getId(), Arrays.asList(usePermVO));
        addUserPermission(userVO4.getId(), Arrays.asList(usePermVO));

        try {

            Integer issueListId1 = runAsUser(adminUserVO.getUsername(), () -> {

                String name1 = "Name 1";
                Integer issueListId;

                {
                    // add issue list

                    WfIssueListVO issueListVO1 = new WfIssueListVO();
                    issueListVO1.setFundId(fund.getId());
                    issueListVO1.setName(name1);
                    issueListVO1.setOpen(true);
                    issueListVO1.setRdUsers(Arrays.asList(userVO1, userVO2));
                    issueListVO1.setWrUsers(Arrays.asList(userVO1));

                    WfIssueListVO issueListVO2 = addIssueList(issueListVO1);

                    assertNotNull(issueListVO2);
                    assertNotNull(issueListVO2.getId());
                    assertEquals(name1, issueListVO2.getName());
                    assertTrue(issueListVO2.getOpen());

                    issueListId = issueListVO2.getId();
                }

                WfIssueListVO issueListVO;
                {
                    // --- get issue list

                    WfIssueListVO issueListVO3 = getIssueList(issueListId);

                    assertNotNull(issueListVO3);
                    assertNotNull(issueListVO3.getId());
                    assertEquals(name1, issueListVO3.getName());
                    assertTrue(issueListVO3.getOpen());

                    issueListVO = issueListVO3;
                }

                {
                    // --- update issue list

                    String name2 = "Name 2";

                    issueListVO.setName(name2);
                    issueListVO.setOpen(false);
                    issueListVO.setRdUsers(Arrays.asList(userVO1, userVO3, userVO4));
                    issueListVO.setWrUsers(Arrays.asList(userVO1, userVO3));

                    WfIssueListVO issueListVO4 = updateIssueList(issueListId, issueListVO);

                    assertEquals(name2, issueListVO4.getName());
                    assertFalse(issueListVO4.getOpen());

                    issueListVO.setOpen(true);
                    WfIssueListVO issueListVO5 = updateIssueList(issueListId, issueListVO);
                    assertTrue(issueListVO5.getOpen());

                    issueListVO = issueListVO5;
                }

                return issueListId;
            });

            runAsUser(userVO1.getUsername(), () -> {

                String description = "description";

                WfIssueTypeVO issueTypeVO1 = findAllIssueTypes().get(0);
                WfIssueTypeVO issueTypeVO2 = findAllIssueTypes().get(1);

                Integer issueId;
                {
                    // --- create issue

                    WfIssueVO issueVO1 = new WfIssueVO();
                    issueVO1.setIssueListId(issueListId1);
                    // issueVO1.setNodeId();
                    issueVO1.setIssueTypeId(issueTypeVO1.getId());
                    issueVO1.setDescription(description);

                    WfIssueVO issueVO2 = addIssue(issueVO1);

                    assertNotNull(issueVO2);
                    assertNotNull(issueVO2.getId());
                    assertEquals(Integer.valueOf(1), issueVO2.getNumber());
                    assertEquals(issueTypeVO1.getId(), issueVO2.getIssueTypeId());
                    assertEquals(description, issueVO2.getDescription());
                    assertNotNull(issueVO2.getTimeCreated());

                    issueId = issueVO2.getId();
                }

                Map<Integer, WfIssueStateVO> issueStateVOMap = findAllIssueStates().stream().collect(Collectors.toMap(state -> state.getId(), state -> state));

                WfIssueVO issueVO;
                Integer issueStateId1;
                {
                    // --- get issue

                    WfIssueVO issueVO3 = getIssue(issueId);

                    assertNotNull(issueVO3);
                    assertNotNull(issueVO3.getId());
                    assertEquals(Integer.valueOf(1), issueVO3.getNumber());
                    assertEquals(issueTypeVO1.getId(), issueVO3.getIssueTypeId());
                    assertEquals(description, issueVO3.getDescription());

                    issueVO = issueVO3;

                    WfIssueStateVO issueStateVO1 = issueStateVOMap.get(issueVO3.getIssueStateId());

                    assertNotNull(issueStateVO1);
                    assertTrue(issueStateVO1.isStartState());
                    assertFalse(issueStateVO1.isFinalState());

                    issueStateId1 = issueVO3.getIssueStateId();
                }

                {
                    // --- set issue type

                    setIssueType(issueVO.getId(), issueTypeVO2.getId());

                    WfIssueVO issueVO4 = getIssue(issueId);

                    assertNotNull(issueVO4);
                    assertEquals(issueTypeVO2.getId(), issueVO4.getIssueTypeId());

                    issueVO = issueVO4;
                }

                String comment = "comment";

                Integer commentId;
                {
                    // --- add comment

                    WfCommentVO commentVO1 = new WfCommentVO();
                    commentVO1.setIssueId(issueId);
                    commentVO1.setComment(comment);

                    WfCommentVO commentVO2 = addIssueComment(commentVO1);

                    assertNotNull(commentVO2);
                    assertNotNull(commentVO2.getId());
                    assertNotNull(commentVO2.getComment(), comment);
                    assertNotNull(commentVO2.getTimeCreated());

                    commentId = commentVO2.getId();
                }

                {
                    // --- get comment

                    WfCommentVO commentVO3 = getIssueComment(commentId);

                    assertNotNull(commentVO3);
                    assertNotNull(commentVO3.getId());
                    assertNotNull(commentVO3.getComment(), comment);
                    assertNotNull(commentVO3.getTimeCreated());
                }

                {
                    // --- find issue lists

                    List<WfIssueListVO> issueListVOs = findIssueListByFund(fund.getId(), null);

                    assertNotNull(issueListVOs);
                    assertFalse(issueListVOs.isEmpty());
                }

                {
                    // --- find issues

                    List<WfIssueVO> issueVOs = findIssueByIssueList(issueListId1, null, null);

                    assertNotNull(issueVOs);
                    assertFalse(issueVOs.isEmpty());
                }

                {
                    // --- find comments

                    List<WfCommentVO> commentVOs = findIssueCommentByIssue(issueId);

                    assertNotNull(commentVOs);
                    assertFalse(commentVOs.isEmpty());
                }

                return null;
            });

        } finally {
            deleteUserFundAllPermission(userVO4.getId());
            deleteUserFundAllPermission(userVO3.getId());
            deleteUserFundAllPermission(userVO2.getId());
            deleteUserFundAllPermission(userVO1.getId());
            deleteUserFundAllPermission(adminUserVO.getId());
            helperTestService.waitForWorkers();
            deleteFund(fund.getId());
        }
    }

    /**
     * An open issue on a node is visible to issue administrators and issue list members,
     * reading the fund is not enough.
     */
    @Test
    public void openIssueVisibilityTest() throws Exception {
        Fund fund = createFund("Test issue visibility", "TST2");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        Integer rootNodeId = getRootNodeId(fundVersion);

        ApAccessPointVO ap = findRecord(null, null, null, null, null).get(0);

        UsrUserVO issueAdminVO = createAdmin("issueAdmin", fund.getId(), ap);

        UsrUserVO issueAdminAllVO = createUser("issueAdminAll", ap);
        addUserPermission(issueAdminAllVO.getId(),
                Arrays.asList(createPermission(Permission.FUND_ISSUE_ADMIN_ALL), createPermission(Permission.FUND_RD_ALL)));

        UsrUserVO memberVO = createUser("issueListMember", ap);
        addUserPermission(memberVO.getId(), Arrays.asList(createPermission(Permission.FUND_RD_ALL)));

        UsrUserVO readerVO = createUser("fundReader", ap);
        addUserPermission(readerVO.getId(), Arrays.asList(createPermission(Permission.FUND_RD_ALL)));

        try {
            WfIssueListVO issueListVO = new WfIssueListVO();
            issueListVO.setFundId(fund.getId());
            issueListVO.setName("Visibility");
            issueListVO.setOpen(true);
            issueListVO.setRdUsers(Arrays.asList(memberVO));
            issueListVO.setWrUsers(Arrays.asList(memberVO));
            Integer issueListId = addIssueList(issueListVO).getId();

            WfIssueVO issueVO = new WfIssueVO();
            issueVO.setIssueListId(issueListId);
            issueVO.setNodeId(rootNodeId);
            issueVO.setIssueTypeId(findAllIssueTypes().get(0).getId());
            issueVO.setDescription("description");
            runAsUser(memberVO.getUsername(), () -> addIssue(issueVO));

            assertOpenIssueVisibility(issueAdminVO, fundVersion.getId(), rootNodeId, true);
            assertOpenIssueVisibility(issueAdminAllVO, fundVersion.getId(), rootNodeId, true);
            assertOpenIssueVisibility(memberVO, fundVersion.getId(), rootNodeId, true);
            assertOpenIssueVisibility(readerVO, fundVersion.getId(), rootNodeId, false);
        } finally {
            deleteUserFundAllPermission(readerVO.getId());
            deleteUserFundAllPermission(memberVO.getId());
            deleteUserFundAllPermission(issueAdminAllVO.getId());
            deleteUserFundAllPermission(issueAdminVO.getId());
            helperTestService.waitForWorkers();
            deleteFund(fund.getId());
        }
    }

    /**
     * Checks both places that show open issues of a node: the issue icon in the node header
     * (siblings in node data) and the previous/next node with an issue.
     */
    private void assertOpenIssueVisibility(UsrUserVO userVO, Integer fundVersionId, Integer nodeId, boolean visible)
            throws Exception {
        runAsUser(userVO.getUsername(), () -> {
            int expectedNodeCount = visible ? 1 : 0;
            assertEquals(expectedNodeCount, getIssueNodeCount(fundVersionId), userVO.getUsername());

            NodeDataParam nodeDataParam = new NodeDataParam();
            nodeDataParam.setFundVersionId(fundVersionId);
            nodeDataParam.setNodeId(nodeId);
            nodeDataParam.setFormData(false);
            nodeDataParam.setParents(false);
            nodeDataParam.setChildren(false);
            nodeDataParam.setSiblingsFrom(0);
            nodeDataParam.setSiblingsMaxCount(1);
            NodeData nodeData = nodeApi.nodeGetNodeData(nodeDataParam);

            NodeBrief nodeBrief = nodeData.getSiblings().get(0);
            boolean issueShown = !nodeBrief.getIssues().isEmpty();
            assertEquals(nodeId, nodeBrief.getId());
            assertEquals(visible, issueShown, userVO.getUsername());
            return null;
        });
    }

    private Integer getRootNodeId(ArrFundVersionVO fundVersion) {
        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);
        return treeData.getNodes().iterator().next().getId();
    }

    private UsrPermissionVO createPermission(Permission permission) {
        UsrPermissionVO permissionVO = new UsrPermissionVO();
        permissionVO.setPermission(permission);
        return permissionVO;
    }

    /**
     * Zalozeni testovaciho AS
     * @throws ApiException
     */
    private Fund createFund() {
        Fund fund = createFund("Test issue", "TST1");
        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode(SRD_TITLE);
        return fund;
    }

    /**
     * Zalozeni uzivatele s opravnenim na zakladani protokulu k danemu AS.
     */
    private UsrUserVO createAdmin(String username, Integer fundId, ApAccessPointVO ap) {

        UsrUserVO adminUserVO = createUser(username, ap);

        UsrPermissionVO permissionVO = new UsrPermissionVO();
        ArrFundBaseVO fund1 = new ArrFundBaseVO();
        fund1.setId(fundId);
        permissionVO.setFund(fund1);
        permissionVO.setPermission(Permission.FUND_ISSUE_ADMIN);

        UsrPermissionVO permissionVO2 = new UsrPermissionVO();
        fund1.setId(fundId);
        permissionVO2.setFund(fund1);
        permissionVO2.setPermission(Permission.FUND_RD);

        addUserPermission(adminUserVO.getId(), Arrays.asList(permissionVO, permissionVO2));

        return adminUserVO;
    }

    /**
     * Zalozeni testovaciho uzivatele bez jakychkoliv opravneni
     */
    private UsrUserVO createUser(String username, ApAccessPointVO ap) {
        Map<UsrAuthentication.AuthType, String> valueMap = new HashMap<>();
        valueMap.put(UsrAuthentication.AuthType.PASSWORD, PASSWORD);
        return createUser(username, valueMap, ap.getId());
    }

    /**
     * Provede akce v {@code callback} jako daný uživatel.
     *
     * @throws Exception výjimka z {@code callback}
     */
    private <T> T runAsUser(String username, Callable<T> callback) throws Exception {
        login(username, PASSWORD);
        try {
            return callback.call();
        } finally {
            loginAsAdmin();
        }
    }
}
