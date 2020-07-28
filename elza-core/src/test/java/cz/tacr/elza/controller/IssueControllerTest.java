package cz.tacr.elza.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.UsrAuthentication;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;

import static cz.tacr.elza.domain.UsrPermission.Permission;
import static org.junit.Assert.*;

public class IssueControllerTest extends AbstractControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(IssueControllerTest.class);

    private static final String PASSWORD = "passwd";

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
    public void issueTest1() throws Exception {

        loginAsAdmin();

        ArrFundVO fund = createFund();

        List<ApAccessPointVO> records = findRecord(null, null, null, null, null, null);
        ApAccessPointVO ap = records.get(0);

        UsrUserVO adminUserVO = createAdmin("admin1", fund.getId(), ap);

        UsrUserVO userVO1 = createUser("user1", ap);
        UsrUserVO userVO2 = createUser("user2", ap);
        UsrUserVO userVO3 = createUser("user3", ap);
        UsrUserVO userVO4 = createUser("user4", ap);

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
                    assertNotNull(comment, commentVO2.getComment());
                    assertNotNull(commentVO2.getTimeCreated());

                    commentId = commentVO2.getId();
                }

                {
                    // --- get comment

                    WfCommentVO commentVO3 = getIssueComment(commentId);

                    assertNotNull(commentVO3);
                    assertNotNull(commentVO3.getId());
                    assertNotNull(comment, commentVO3.getComment());
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
     * Zalozeni testovaciho AS
     */
    private ArrFundVO createFund() {
        ArrFundVO fund = createFund("Test issue", "TST1");
        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode("SRD_TITLE");
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

        addUserPermission(adminUserVO.getId(), Arrays.asList(permissionVO));

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
