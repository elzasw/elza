package cz.tacr.elza.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.controller.vo.ArrFundBaseVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.controller.vo.WfCommentVO;
import cz.tacr.elza.controller.vo.WfIssueListVO;
import cz.tacr.elza.controller.vo.WfIssueStateVO;
import cz.tacr.elza.controller.vo.WfIssueTypeVO;
import cz.tacr.elza.controller.vo.WfIssueVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;

import static cz.tacr.elza.domain.UsrPermission.Permission;
import static org.junit.Assert.*;

public class IssueControllerTest extends AbstractControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(IssueControllerTest.class);

    private static final String PASSWORD = "passwd";

    @Test
    public void findAllIssueTypesTest() {
        List<WfIssueTypeVO> issueTypes = findAllIssueTypes();
        assertNotNull(issueTypes);
        assertFalse(issueTypes.isEmpty());
    }

    @Test
    public void findAllIssueStatesTest() {
        List<WfIssueStateVO> issueStates = findAllIssueStates();
        assertNotNull(issueStates);
        assertFalse(issueStates.isEmpty());
    }

    @Test
    public void issueTest1() throws Exception {

        loginAsAdmin();

        ArrFundVO fund = createFund();

        UsrUserVO adminUserVO = createAdmin("admin1", fund.getId());

        UsrUserVO userVO1 = createUser("user1");
        UsrUserVO userVO2 = createUser("user2");
        UsrUserVO userVO3 = createUser("user3");
        UsrUserVO userVO4 = createUser("user4");

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
                    issueListVO1.setRdUserIds(Arrays.asList(userVO1.getId(), userVO2.getId()));
                    issueListVO1.setWrUserIds(Arrays.asList(userVO1.getId()));

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
                    issueListVO.setRdUserIds(Arrays.asList(userVO1.getId(), userVO3.getId(), userVO4.getId()));
                    issueListVO.setWrUserIds(Arrays.asList(userVO1.getId(), userVO3.getId()));

                    WfIssueListVO issueListVO4 = updateIssueList(issueListId, issueListVO);

                    assertEquals(name2, issueListVO4.getName());
                    assertFalse(issueListVO4.getOpen());

                    issueListVO = issueListVO4;
                }

                return issueListId;
            });

            runAsUser(userVO1.getUsername(), () -> {

                String description = "description";

                WfIssueTypeVO issueTypeVO1 = findAllIssueTypes().get(0);

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

                WfIssueStateVO issueStateVO2 = null;
                {
                    // najit jeden final state
                    for (WfIssueStateVO issueStateVO : issueStateVOMap.values()) {
                        if (issueStateVO.isFinalState()) {
                            issueStateVO2 = issueStateVO;
                            break;
                        }
                    }
                    assertNotNull(issueStateVO2);
                }

                {
                    // --- set issue state

                    setIssueState(issueVO.getId(), issueStateVO2.getId());

                    WfIssueVO issueVO4 = getIssue(issueId);

                    assertNotNull(issueVO4);
                    assertEquals(issueStateVO2.getId(), issueVO4.getIssueStateId());

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

                    List<WfIssueListVO> issueListVOs = findIssueListByFund(fund.getId());

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
            deleteUserFundAllPermission(userVO1.getId());
            deleteUserFundAllPermission(userVO2.getId());
            deleteUserFundAllPermission(userVO3.getId());
            deleteUserFundAllPermission(userVO4.getId());
            deleteFund(fund.getId());
        }
    }

    protected <T> T runAsUser(String username, Callable<T> callback) throws Exception {
        login(username, PASSWORD);
        try {
            return callback.call();
        } finally {
            loginAsAdmin();
        }
    }

    private UsrUserVO createAdmin(String username, Integer fundId) {

        UsrUserVO adminUserVO = createUser(username);

        UsrPermissionVO permissionVO = new UsrPermissionVO();
        ArrFundBaseVO fund1 = new ArrFundBaseVO();
        fund1.setId(fundId);
        permissionVO.setFund(fund1);
        permissionVO.setPermission(Permission.FUND_ISSUE_ADMIN);

        addUserPermission(adminUserVO.getId(), Arrays.asList(permissionVO));

        return adminUserVO;
    }

    private UsrUserVO createUser(String username) {
        List<ParPartyVO> party = findParty(null, 0, 1, null, null);
        return createUser(username, PASSWORD, party.get(0).getId());
    }

    private ArrFundVO createFund() {
        ArrFundVO fund = createFund("Test issue", "TST1");
        RulDescItemTypeExtVO typeVo = findDescItemTypeByCode("SRD_TITLE");
        return fund;
    }

    // ---

    /**
     * Získání druhů připomínek.
     *
     * @returns seznam druhů připomínek
     */
    protected List<WfIssueTypeVO> findAllIssueTypes() {
        return Arrays.asList(get(ALL_ISSUE_TYPES).getBody().as(WfIssueTypeVO[].class));
    }

    /**
     * Získání stavů připomínek.
     *
     * @returns seznam stavů připomínek
     */
    protected List<WfIssueStateVO> findAllIssueStates() {
        return Arrays.asList(get(ALL_ISSUE_STATES).getBody().as(WfIssueStateVO[].class));
    }

    /**
     * Získání detailu protokolu.
     *
     * @param issueListId identifikátor protokolu
     * @return protokol
     */
    protected WfIssueListVO getIssueList(Integer issueListId) {
        return get(spec -> spec.pathParameter("issueListId", issueListId), GET_ISSUE_LIST).as(WfIssueListVO.class);
    }

    /**
     * Získání detailu připomínky.
     *
     * @param issueId identifikátor připomínky
     * @return připomínka
     */
    protected WfIssueVO getIssue(Integer issueId) {
        return get(spec -> spec.pathParameter("issueId", issueId), GET_ISSUE).as(WfIssueVO.class);
    }

    /**
     * Získání detailu komentáře.
     *
     * @param commentId identifikátor komentáře
     * @returns komentář
     */
    protected WfCommentVO getIssueComment(Integer commentId) {
        return get(spec -> spec.pathParameter("commentId", commentId), GET_COMMENT).as(WfCommentVO.class);
    }

    /**
     * Založí nový protokol k danému AS
     *
     * @param issueListVO data pro založení protokolu
     * @return detail založeného protokolu
     */
    protected WfIssueListVO addIssueList(WfIssueListVO issueListVO) {
        return post(spec -> spec.body(issueListVO), CREATE_ISSUE_LIST).getBody().as(WfIssueListVO.class);
    }

    /**
     * Založení nové připomínky k danému protokolu
     *
     * @param issueVO data pro založení připomínky
     * @return detail založené připomínky
     */
    protected WfIssueVO addIssue(WfIssueVO issueVO) {
        return post(spec -> spec.body(issueVO), CREATE_ISSUE).getBody().as(WfIssueVO.class);
    }

    /**
     * Založení nového komentáře k dané připomínce
     *
     * @param commentVO data pro založení protokolu
     * @return detail založeného komentáře
     */
    protected WfCommentVO addIssueComment(WfCommentVO commentVO) {
        return post(spec -> spec.body(commentVO), CREATE_COMMENT).getBody().as(WfCommentVO.class);
    }

    /**
     * Vyhledá protokoly k danému archivní souboru - řazeno nejprve otevřené a pak uzavřené
     *
     * @param fundId identifikátor AS
     * @return seznam protokolů
     */
    protected List<WfIssueListVO> findIssueListByFund(Integer fundId) {
        return Arrays.asList(get(spec -> spec.pathParameter("fundId", fundId), FIND_ISSUE_LISTS).as(WfIssueListVO[].class));
    }

    /**
     * Vyhledá připomínky k danému protokolu - řazeno vzestupně podle čísla připomínky
     *
     * @param issueListId identifikátor protokolu
     * @param issueStateId identifikátor stavu připomínky, dle kterého filtrujeme
     * @param issueTypeId identifikátor druhu připomínky, dle kterého filtrujeme
     */
    protected List<WfIssueVO> findIssueByIssueList(Integer issueListId, Integer issueStateId, Integer issueTypeId) {
        return Arrays.asList(get(spec -> {
            spec.pathParameter("issueListId", issueListId);
            if (issueStateId != null) {
                spec.queryParameter("issue_state_id", issueStateId);
            }
            if (issueTypeId != null) {
                spec.queryParameter("issue_type_id", issueTypeId);
            }
            return spec;
        }, FIND_ISSUES).as(WfIssueVO[].class));
    }

    /**
     * Vyhledá komentáře k dané připomínce - řazeno vzestupně podle času
     *
     * @param issueId identifikátor připomínky
     * @return seznam komentářů
     */
    protected List<WfCommentVO> findIssueCommentByIssue(Integer issueId) {
        return Arrays.asList(get(spec -> spec.pathParameter("issueId", issueId), FIND_COMMENTS).as(WfCommentVO[].class));
    }

    /**
     * Úprava vlastností existujícího protokolu
     *
     * @param issueListId identifikátor protokolu
     * @param issueListVO data pro uložení protokolu
     * @return detail uloženého protokolu
     */
    protected WfIssueListVO updateIssueList(Integer issueListId, WfIssueListVO issueListVO) {
        return put(spec -> spec
                .pathParameter("issueListId", issueListId)
                .body(issueListVO), UPDATE_ISSUE_LIST)
                .getBody().as(WfIssueListVO.class);
    }

    /**
     * Změna stavu připomínky
     *
     * @param issueStateId identifikátor stavu připomínky
     */
    protected void setIssueState(Integer issueId, Integer issueStateId) {
        post(spec -> spec
                .pathParameter("issueId", issueId)
                .queryParameter("issueStateId", issueStateId), SET_ISSUE_STATE);
    }

}
