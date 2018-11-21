package cz.tacr.elza.controller;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import cz.tacr.elza.controller.vo.*;
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

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.factory.WfFactory;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.WfComment;
import cz.tacr.elza.domain.WfIssue;
import cz.tacr.elza.domain.WfIssueList;
import cz.tacr.elza.domain.WfIssueState;
import cz.tacr.elza.domain.WfIssueType;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.ArrangementService;
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

    private final UserService userService;

    // --- fields ---

    private final ClientFactoryVO factoryVo;

    private final WfFactory factory;

    // --- constructor ---

    @Autowired
    public IssueController(ArrangementService arrangementService, IssueService issueService, UserService userService, ClientFactoryVO factoryVo, WfFactory factory) {
        this.arrangementService = arrangementService;
        this.issueService = issueService;
        this.userService = userService;
        this.factoryVo = factoryVo;
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
        return factoryVo.createIssueTypes(issueTypeList);
    }

    /**
     * Získání stavů připomínek.
     *
     * @returns seznam stavů připomínek
     */
    @RequestMapping(value = "/issue_states", method = RequestMethod.GET)
    public List<WfIssueStateVO> findAllIssueStates() {
        List<WfIssueState> issueStateList = issueService.findAllIssueStates();
        return factoryVo.createIssueStates(issueStateList);
    }

    /**
     * Vyhledá protokoly k danému archivní souboru - řazeno nejprve otevřené a pak uzavřené
     *
     * @param fundId identifikátor AS
     * @return seznam protokolů
     */
    @RequestMapping(value = "/funds/{fundId}/issue_lists", method = RequestMethod.GET)
    @Transactional
    public List<WfIssueListVO> findIssueListByFund(@PathVariable Integer fundId) {

        // kontrola existence
        ArrFund fund = arrangementService.getFund(fundId);

        UserDetail userDetail = userService.getLoggedUserDetail();

        List<WfIssueList> issueListList = issueService.findIssueListByFund(fund, userDetail);

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
                                                @RequestParam(name = "issue_state_id", required = false) Integer issueStateId,
                                                @RequestParam(name = "issue_type_id", required = false) Integer issueTypeId) {

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

        // kontrola existence a opravneni
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
        Validate.notNull(user, "Uzivatel není prihlášený");

        WfIssue issue = issueService.addIssue(issueList, node, issueType, issueVO.getDescription(), user);

        return factory.createIssueVO(issue);
    }

    /**
     * Změna stavu připomínky
     *
     * @param issueStateId identifikátor stavu připomínky
     */
    @RequestMapping(value = "/issues/{issueId}/setState", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public void setIssueState(@PathVariable Integer issueId, @RequestParam Integer issueStateId) {
        // kontrola existence
        WfIssue issue = issueService.getIssue(issueId);
        WfIssueState issueState = issueService.getIssueState(issueStateId);
        issueService.setIssueState(issue, issueState);
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
        Validate.notNull(user, "Uzivatel není prihlášený");

        WfComment comment = issueService.addComment(issue, commentVO.getComment(), newIssueState, user);

        return factory.createCommentVO(comment);
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
