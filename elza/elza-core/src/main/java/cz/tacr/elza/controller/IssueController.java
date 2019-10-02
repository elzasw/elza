package cz.tacr.elza.controller;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.factory.WfFactory;
import cz.tacr.elza.controller.vo.IssueNodeItem;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.controller.vo.WfCommentVO;
import cz.tacr.elza.controller.vo.WfIssueListVO;
import cz.tacr.elza.controller.vo.WfIssueStateVO;
import cz.tacr.elza.controller.vo.WfIssueTypeVO;
import cz.tacr.elza.controller.vo.WfIssueVO;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.WfComment;
import cz.tacr.elza.domain.WfIssue;
import cz.tacr.elza.domain.WfIssueList;
import cz.tacr.elza.domain.WfIssueState;
import cz.tacr.elza.domain.WfIssueType;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.IssueDataService;
import cz.tacr.elza.service.IssueService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.utils.CsvUtils;

@RestController
@RequestMapping("/api/issue")
public class IssueController {

    private static final Logger logger = LoggerFactory.getLogger(IssueController.class);

    // --- services ---

    private final ArrangementService arrangementService;

    private final IssueService issueService;
    private final IssueDataService issueDataService;

    private final UserService userService;

    // --- fields ---

    private final WfFactory factory;

    // --- constructor ---

    @Autowired
    public IssueController(final ArrangementService arrangementService,
                           final IssueService issueService,
                           final IssueDataService issueDataService,
                           final UserService userService,
                           final WfFactory factory) {
        this.arrangementService = arrangementService;
        this.issueService = issueService;
        this.issueDataService = issueDataService;
        this.userService = userService;
        this.factory = factory;
    }

    // --- methods ---

    /**
     * Získání druhů připomínek.
     *
     * @returns seznam druhů připomínek
     */
    @RequestMapping(value = "/issue_types", method = RequestMethod.GET)
    public List<WfIssueTypeVO> findAllIssueTypes() {
        List<WfIssueType> issueTypeList = issueService.findAllIssueTypes();
        return factory.createIssueTypes(issueTypeList);
    }

    /**
     * Získání stavů připomínek.
     *
     * @returns seznam stavů připomínek
     */
    @RequestMapping(value = "/issue_states", method = RequestMethod.GET)
    public List<WfIssueStateVO> findAllIssueStates() {
        List<WfIssueState> issueStateList = issueService.findAllIssueStates();
        return factory.createIssueStates(issueStateList);
    }

    /**
     * Vyhledá protokoly k danému archivní souboru - řazeno nejprve otevřené a pak uzavřené
     *
     * @param fundId identifikátor AS
     * @param open filtr pro stav (otevřený/uzavřený)
     * @return seznam protokolů
     */
    @RequestMapping(value = "/funds/{fundId}/issue_lists", method = RequestMethod.GET)
    @Transactional
    public List<WfIssueListVO> findIssueListByFund(@PathVariable Integer fundId, @RequestParam(name = "open", required = false) Boolean open) {

        // kontrola existence
        ArrFund fund = arrangementService.getFund(fundId);

        UserDetail userDetail = userService.getLoggedUserDetail();

        List<WfIssueList> issueListList = issueDataService.findIssueListByFund(fund, open, userDetail);

        return issueListList.stream().map(issueList -> factory.createIssueListVO(issueList, false)).collect(Collectors.toList());
    }

    /**
     * Získání detailu protokolu.
     *
     * @param issueListId identifikátor protokolu
     * @return protokol
     */
    @RequestMapping(value = "/issue_lists/{issueListId}", method = RequestMethod.GET)
    @Transactional
    public WfIssueListVO getIssueList(@PathVariable Integer issueListId) {
        WfIssueList issueList = issueService.getIssueList(issueListId);
        return factory.createIssueListVO(issueList, true);
    }

    /**
     * Vyhledá připomínky k danému protokolu - řazeno vzestupně podle čísla připomínky
     *
     * @param issueListId identifikátor protokolu
     * @param issueStateId identifikátor stavu připomínky, dle kterého filtrujeme
     * @param issueTypeId identifikátor druhu připomínky, dle kterého filtrujeme
     */
    @RequestMapping(value = "/issue_lists/{issueListId}/issues", method = RequestMethod.GET)
    @Transactional
    public List<WfIssueVO> findIssueByIssueList(@PathVariable Integer issueListId,
                                                @RequestParam(name = "issueStateId", required = false) Integer issueStateId,
                                                @RequestParam(name = "issueTypeId", required = false) Integer issueTypeId) {

        // kontrola existence a opravneni
        WfIssueList issueList = issueService.getIssueList(issueListId);

        WfIssueState state = issueStateId != null ? issueService.getIssueState(issueStateId) : null;
        WfIssueType type = issueTypeId != null ? issueService.getIssueType(issueTypeId) : null;

        List<WfIssue> issues = issueService.findIssueByIssueListId(issueList, state, type);

        return factory.createIssueVO(issues);
    }

    /**
     * Založí nový protokol k danému AS
     *
     * @param issueListVO data pro založení protokolu
     * @return detail založeného protokolu
     */
    @RequestMapping(value = "/issue_lists", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public WfIssueListVO addIssueList(@RequestBody WfIssueListVO issueListVO) {

        Validate.isTrue(issueListVO.getId() == null, "Neplatný identifikátor protokolu [id=" + issueListVO.getId() + "]");
        Validate.notNull(issueListVO.getFundId(), "Chybí identifikátor AS [fundId]");
        Validate.notBlank(issueListVO.getName(), "Chybí název protokolu [name]");
        Validate.notNull(issueListVO.getOpen(), "Chybí příznak stavu protokolu [open]");

        // kontrola existence
        ArrFund fund = arrangementService.getFund(issueListVO.getFundId());

        // validace uzivatelu
        Collection<UsrUser> rdUsers = findUsers(issueListVO.getRdUsers(), "rdUsers");
        Collection<UsrUser> wrUsers = findUsers(issueListVO.getWrUsers(), "wrUsers");

        WfIssueList issueList = issueService.addIssueList(fund, issueListVO.getName(), issueListVO.getOpen());

        userService.updateIssueListPermissions(issueList, rdUsers, wrUsers);

        return factory.createIssueListVO(issueList, true);
    }

    /**
     * Úprava vlastností existujícího protokolu
     *
     * @param issueListId identifikátor protokolu
     * @param issueListVO data pro uložení protokolu
     * @return detail uloženého protokolu
     */
    @RequestMapping(value = "/issue_lists/{issueListId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public WfIssueListVO updateIssueList(@PathVariable Integer issueListId, @RequestBody WfIssueListVO issueListVO) {

        Validate.isTrue(issueListId.equals(issueListVO.getId()), "Neplatný identifikátor protokolu [id=" + issueListVO.getId() + "]");

        // kontrola existence a opravneni
        WfIssueList issueList = issueService.getIssueList(issueListId);

        // validace uzivatelu
        Collection<UsrUser> rdUsers = findUsers(issueListVO.getRdUsers(), "rdUsers");
        Collection<UsrUser> wrUsers = findUsers(issueListVO.getWrUsers(), "wrUsers");

        issueService.updateIssueList(issueList, issueListVO.getName(), issueListVO.getOpen());

        userService.updateIssueListPermissions(issueList, rdUsers, wrUsers);

        return factory.createIssueListVO(issueList, true);
    }

    /**
     * Export protokolu ve formátu CSV.
     *
     * @param issueListId identifikátor protokolu
     */
    @RequestMapping(value = "/issue_lists/{issueListId}/export", method = RequestMethod.GET, produces = "text/csv")
    @Transactional
    public void exportIssueList(HttpServletResponse response, @PathVariable(value = "issueListId") Integer issueListId) throws IOException {

        // kontrola existence a opravneni
        WfIssueList issueList = issueService.getIssueList(issueListId);

        List<WfIssue> issues = issueService.findIssueByIssueListId(issueList, null, null);

        MediaType mediaType = new MediaType("text", "csv", CsvUtils.CSV_EXCEL_CHARSET);
        response.setHeader(HttpHeaders.CONTENT_TYPE, mediaType.toString());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=issue-list-" + issueListId + ".csv");

        issueService.exportIssueList(issueList, response.getOutputStream());
    }

    /**
     * Získání detailu připomínky.
     *
     * @param issueId identifikátor připomínky
     * @return připomínka
     */
    @RequestMapping(value = "/issues/{issueId}", method = RequestMethod.GET)
    @Transactional
    public WfIssueVO getIssue(@PathVariable Integer issueId) {
        WfIssue issue = issueService.getIssue(issueId);
        return factory.createIssueVO(issue);
    }

    /**
     * Založení nové připomínky k danému protokolu
     *
     * @param issueVO data pro založení připomínky
     * @return detail založené připomínky
     */
    @RequestMapping(value = "/issues", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public WfIssueVO addIssue(@RequestBody WfIssueVO issueVO) {

        Validate.isTrue(issueVO.getId() == null, "Neplatný identifikátor připomínky [id=" + issueVO.getId() + "]");
        Validate.notNull(issueVO.getIssueListId(), "Chybí identifikátor protokolu [issueListId]");
        Validate.notNull(issueVO.getIssueTypeId(), "Chybí identifikátor druhu připomínky [issueTypeId]");
        Validate.notBlank(issueVO.getDescription(), "Chybí popis připomínky [description]");

        // validace na existenci
        WfIssueList issueList = issueService.getIssueList(issueVO.getIssueListId());
        WfIssueType issueType = issueService.getIssueType(issueVO.getIssueTypeId());
        ArrNode node = issueVO.getNodeId() != null ? arrangementService.getNode(issueVO.getNodeId()) : null;

        UsrUser user = userService.getLoggedUser();
        Validate.notNull(user, "Uživatel není přihlášený");

        Validate.isTrue(issueList.getOpen(), "Neplatný stav protokolu - uzavřený");

        WfIssue issue = issueService.addIssue(issueList, node, issueType, issueVO.getDescription(), user);

        return factory.createIssueVO(issue);
    }

    /**
     * Úprava připomínky
     *
     * @param issueId identifikátor připomínky
     * @param issueVO data pro uložení připomínky
     * @return detail založené připomínky
     */
    @RequestMapping(value = "/issues/{issueId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public WfIssueVO updateIssue(@PathVariable Integer issueId, @RequestBody WfIssueVO issueVO) {

        Validate.isTrue(issueId.equals(issueVO.getId()), "Neplatný identifikátor připomínky [id=" + issueVO.getId() + "]");
        Validate.notNull(issueVO.getIssueTypeId(), "Chybí identifikátor druhu připomínky [issueTypeId]");
        Validate.notNull(issueVO.getIssueStateId(), "Chybí identifikátor stavu připomínky [issueStateId]");
        Validate.notBlank(issueVO.getDescription(), "Chybí popis připomínky [description]");

        // validace na existenci
        WfIssue issue = issueService.getIssue(issueId);
        WfIssueType issueType = issueService.getIssueType(issueVO.getIssueTypeId());
        WfIssueState issueState = issueService.getIssueState(issueVO.getIssueStateId());
        ArrNode node = issueVO.getNodeId() != null ? arrangementService.getNode(issueVO.getNodeId()) : null;

        UsrUser user = userService.getLoggedUser();
        Validate.notNull(user, "Uživatel není přihlášený");

        Validate.isTrue(issue.getUserCreate().getUserId().equals(user.getUserId()), "Uživatel není vlastník [userId=" + user.getUserId() + "]");

        WfComment lastComment = issueDataService.getLastComment(issue);

        Validate.isTrue(lastComment == null, "K připomínce již existují komentáře [issueId=" + issue.getIssueId() + "]");

        issue = issueService.updateIssue(issue, node, issueType, issueState, issueVO.getDescription());

        return factory.createIssueVO(issue);
    }

    /**
     * Změna druhu připomínky
     *
     * @param issueTypeId identifikátor stavu připomínky
     */
    @RequestMapping(value = "/issues/{issueId}/type", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public void setIssueType(@PathVariable Integer issueId, @RequestParam(name = "issueTypeId") Integer issueTypeId) {
        // kontrola existence a opravneni
        WfIssue issue = issueService.getIssue(issueId);
        WfIssueType issueType = issueService.getIssueType(issueTypeId);
        issueService.setIssueType(issue, issueType);
    }

    /**
     * Vyhledá komentáře k dané připomínce - řazeno vzestupně podle času
     *
     * @param issueId identifikátor připomínky
     * @return seznam komentářů
     */
    @RequestMapping(value = "/issues/{issueId}/comments", method = RequestMethod.GET)
    @Transactional
    public List<WfCommentVO> findIssueCommentByIssue(@PathVariable Integer issueId) {
        // kontrola existence a opravneni
        WfIssue issue = issueService.getIssue(issueId);
        List<WfComment> commentList = issueService.findCommentByIssueId(issue);
        return commentList.stream().map(comment -> factory.createCommentVO(comment)).collect(Collectors.toList());
    }

    /**
     * Získání detailu komentáře.
     *
     * @param commentId identifikátor komentáře
     * @returns komentář
     */
    @RequestMapping(value = "/comments/{commentId}", method = RequestMethod.GET)
    @Transactional
    public WfCommentVO getIssueComment(@PathVariable Integer commentId) {
        // kontrola existence a opravneni
        WfComment comment = issueService.getComment(commentId);
        return factory.createCommentVO(comment);
    }

    /**
     * Založení nového komentáře k dané připomínce
     *
     * @param commentVO data pro založení protokolu
     * @return detail založeného komentáře
     */
    @RequestMapping(value = "/comments", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public WfCommentVO addIssueComment(@RequestBody WfCommentVO commentVO) {

        Validate.isTrue(commentVO.getId() == null, "Neplatný identifikátor komentáře [id=" + commentVO.getId() + "");
        Validate.notNull(commentVO.getIssueId(), "Chybí identifikátor připomínky [issueId]");
        Validate.notBlank(commentVO.getComment(), "Chybí text připomínky [comment]");

        // validace na existenci issue
        WfIssue issue = issueService.getIssue(commentVO.getIssueId());

        WfIssueState newIssueState = commentVO.getNextStateId() != null
                ? issueService.getIssueState(commentVO.getNextStateId())
                : null;

        UsrUser user = userService.getLoggedUser();
        Validate.notNull(user, "Uživatel není přihlášený");

        WfComment comment = issueService.addComment(issue, commentVO.getComment(), newIssueState, user);

        return factory.createCommentVO(comment);
    }

    /**
     * Úprava komentáře
     *
     * @param commentId identifikátor komentáře
     * @param commentVO data pro založení protokolu
     * @return detail založeného komentáře
     */
    @RequestMapping(value = "/comments/{commentId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public WfCommentVO updateIssueComment(@PathVariable Integer commentId, @RequestBody WfCommentVO commentVO) {

        Validate.isTrue(commentId.equals(commentVO.getId()), "Neplatný identifikátor komentáře [id=" + commentVO.getId() + "");
        Validate.notBlank(commentVO.getComment(), "Chybí text připomínky [comment]");

        // kontrola existence a opravneni
        WfComment comment = issueService.getComment(commentId);

        WfIssueState newIssueState = commentVO.getNextStateId() != null
                ? issueService.getIssueState(commentVO.getNextStateId())
                : null;

        UsrUser user = userService.getLoggedUser();
        Validate.notNull(user, "Uživatel není přihlášený");

        Validate.isTrue(comment.getUser().getUserId().equals(user.getUserId()), "Uživatel není vlastník [userId=" + user.getUserId() + "]");

        WfComment lastComment = issueDataService.getLastComment(comment.getIssue());

        Validate.isTrue(commentId.equals(lastComment.getCommentId()), "K připomínce existuje novější komentář [issueId=" + comment.getIssue().getIssueId() + "]");

        comment = issueService.updateComment(comment, commentVO.getComment(), newIssueState);

        return factory.createCommentVO(comment);
    }

    /**
     * Vyhledá další uzel s otevřenou připomínkou.
     *
     * @param fundVersionId verze AS
     * @param currentNodeId výchozí uzel (default root)
     * @param direction krok (default 1)
     * @return uzel s připomínkou
     */
    @RequestMapping(value = "/funds/{fundVersionId}/issues/nextNode", method = RequestMethod.GET)
    @Transactional
    public IssueNodeItem nextIssueByFundVersion(@PathVariable(value = "fundVersionId") Integer fundVersionId,
                                                @RequestParam(value = "nodeId", required = false) Integer currentNodeId,
                                                @RequestParam(value = "direction", required = false, defaultValue = "1") int direction) {

        // kontrola existence
        ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);

        UserDetail userDetail = userService.getLoggedUserDetail();

        return issueService.nextIssueNode(fundVersion, currentNodeId, direction, userDetail);
    }

    protected Collection<UsrUser> findUsers(List<UsrUserVO> users, String fieldName) {
        if (users != null) {
            Map<Integer, UsrUser> userMap = userService.findUserMap(users.stream().map(UsrUserVO::getId).collect(Collectors.toList()));
            Validate.isTrue(userMap.size() == users.size(), "Neplatný uživatel [" + fieldName + "]");
            return userMap.values();
        }
        return null;
    }

}
