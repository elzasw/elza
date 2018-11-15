package cz.tacr.elza.controller;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.factory.WfFactory;
import cz.tacr.elza.controller.vo.WfCommentVO;
import cz.tacr.elza.controller.vo.WfIssueListVO;
import cz.tacr.elza.controller.vo.WfIssueStateVO;
import cz.tacr.elza.controller.vo.WfIssueTypeVO;
import cz.tacr.elza.controller.vo.WfIssueVO;
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

@RestController
@RequestMapping("/api/issue")
public class IssueController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

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

    @RequestMapping(value = "/issue_types", method = RequestMethod.GET)
    public List<WfIssueTypeVO> findAllIssueTypes() {
        List<WfIssueType> issueTypeList = issueService.findAllIssueTypes();
        return factoryVo.createIssueTypes(issueTypeList);
    }

    @RequestMapping(value = "/issue_states", method = RequestMethod.GET)
    public List<WfIssueStateVO> findAllIssueStates() {
        List<WfIssueState> issueStateList = issueService.findAllIssueStates();
        return factoryVo.createIssueStates(issueStateList);
    }

    @RequestMapping(value = "/funds/{fundId}/issue_lists", method = RequestMethod.GET)
    @Transactional
    public List<WfIssueListVO> findIssueListByFund(@PathVariable Integer fundId) {

        ArrFund fund = arrangementService.getFund(fundId);

        UserDetail userDetail = userService.getLoggedUserDetail();

        List<WfIssueList> issueListList = issueService.findIssueListByFund(fund, userDetail);

        return issueListList.stream().map(issueList -> factory.createIssueListVO(issueList, false)).collect(Collectors.toList());
    }

    @RequestMapping(value = "/issue_lists/{issueListId}", method = RequestMethod.GET)
    @Transactional
    public WfIssueListVO getIssueList(@PathVariable Integer issueListId) {
        WfIssueList issueList = issueService.getIssueList(issueListId);
        return factory.createIssueListVO(issueList, true);
    }

    @RequestMapping(value = "/issue_lists/{issueListId}/issues", method = RequestMethod.GET)
    @Transactional
    public List<WfIssueVO> findIssueByIssueList(@PathVariable Integer issueListId,
                                                @RequestParam(name = "issue_state_id", required = false) Integer stateId,
                                                @RequestParam(name = "issue_type_id", required = false) Integer typeId) {

        // kontrola existence a opravneni
        WfIssueList issueList = issueService.getIssueList(issueListId);

        WfIssueState state = stateId != null ? issueService.getIssueState(stateId) : null;
        WfIssueType type = typeId != null ? issueService.getIssueType(typeId) : null;

        List<WfIssue> issues = issueService.findIssueByIssueListId(issueList, state, type);

        return issues.stream().map(issue -> factory.createIssueVO(issue)).collect(Collectors.toList());
    }

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
        Collection<UsrUser> rdUsers = issueListVO.getRdUserIds() != null
                ? userService.getUsers(new HashSet<>(issueListVO.getRdUserIds()))
                : null;

        // validace uzivatelu
        Collection<UsrUser> wrUsers = issueListVO.getWrUserIds() != null
                ? userService.getUsers(new HashSet<>(issueListVO.getWrUserIds()))
                : null;

        WfIssueList issueList = issueService.addIssueList(fund, issueListVO.getName(), issueListVO.getOpen());

        issueService.addIssueListPermission(issueList, userService.getLoggedUser(), rdUsers, wrUsers);

        return factory.createIssueListVO(issueList, true);
    }

    @RequestMapping(value = "/issue_lists/{issueListId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public WfIssueListVO updateIssueList(@PathVariable Integer issueListId, @RequestBody WfIssueListVO issueListVO) {

        Validate.isTrue(issueListId.equals(issueListVO.getId()), "Neplatný identifikátor protokolu [id=" + issueListVO.getId() + "]");

        // kontrola existence a opravneni
        WfIssueList issueList = issueService.getIssueList(issueListId);

        // validace uzivatelu
        Collection<UsrUser> rdUsers = issueListVO.getRdUserIds() != null
                ? userService.getUsers(new HashSet<>(issueListVO.getRdUserIds()))
                : null;

        // validace uzivatelu
        Collection<UsrUser> wrUsers = issueListVO.getWrUserIds() != null
                ? userService.getUsers(new HashSet<>(issueListVO.getWrUserIds()))
                : null;

        issueService.updateIssueList(issueList, issueListVO.getName(), issueListVO.getOpen());

        issueService.updateIssueListPermission(issueList, userService.getLoggedUser(), rdUsers, wrUsers);

        return factory.createIssueListVO(issueList, true);
    }

    @RequestMapping(value = "/issues/{issueId}", method = RequestMethod.GET)
    @Transactional
    public WfIssueVO getIssue(@PathVariable Integer issueId) {
        WfIssue issue = issueService.getIssue(issueId);
        return factory.createIssueVO(issue);
    }

    @RequestMapping(value = "/issues", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public WfIssueVO addIssue(@RequestBody WfIssueVO issueVO) {

        Validate.isTrue(issueVO.getId() == null, "Neplatný identifikátor připomínky [id=" + issueVO.getId() + "]");
        Validate.notNull(issueVO.getIssueListId(), "Chybí identifikátor protokolu [issueListId]");
        Validate.notNull(issueVO.getIssueTypeId(), "Chybí identifikátor druhu připomínky [issueTypeId]");
        Validate.notBlank(issueVO.getDescription(), "Chybí popis připomínky [description]");

        // validace na existenci
        ArrNode node = issueVO.getNodeId() != null ? arrangementService.getNode(issueVO.getNodeId()) : null;
        WfIssueList issueList = issueService.getIssueList(issueVO.getIssueListId());
        WfIssueType issueType = issueService.getIssueType(issueVO.getIssueTypeId());

        UsrUser user = userService.getLoggedUser();
        Validate.notNull(user, "Uzivatel není prihlášený");

        WfIssue issue = issueService.addIssue(issueList, node, issueType, issueVO.getDescription(), user);

        return factory.createIssueVO(issue);
    }

    @RequestMapping(value = "/issues/{issueId}/setState", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public void setIssueState(@PathVariable Integer issueId, @RequestParam Integer issueStateId) {
        // kontrola existence
        WfIssue issue = issueService.getIssue(issueId);
        WfIssueState issueState = issueService.getIssueState(issueStateId);
        issueService.setIssueState(issue, issueState);
    }

    @RequestMapping(value = "/issues/{issueId}/comments", method = RequestMethod.GET)
    @Transactional
    public List<WfCommentVO> findIssueCommentByIssue(@PathVariable Integer issueId) {
        // kontrola existence a opravneni
        WfIssue issue = issueService.getIssue(issueId);
        List<WfComment> commentList = issueService.findCommentByIssueId(issue);
        return commentList.stream().map(comment -> factory.createCommentVO(comment)).collect(Collectors.toList());
    }

    @RequestMapping(value = "/comments/{commentId}", method = RequestMethod.GET)
    @Transactional
    public WfCommentVO getIssueComment(@PathVariable Integer commentId) {
        // kontrola existence a opravneni
        WfComment comment = issueService.getComment(commentId);
        return factory.createCommentVO(comment);
    }

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

        return factory.createCommentVO(issueService.addComment(issue, commentVO.getComment(), newIssueState, user));
    }

}
