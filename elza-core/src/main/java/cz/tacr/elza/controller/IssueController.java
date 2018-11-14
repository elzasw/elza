package cz.tacr.elza.controller;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
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
        List<WfIssueType> issueTypeList = issueService.findAllType();
        return factoryVo.createIssueTypes(issueTypeList);
    }

    @RequestMapping(value = "/issue_states", method = RequestMethod.GET)
    public List<WfIssueStateVO> findAllIssueStates() {
        List<WfIssueState> issueStateList = issueService.findAllState();
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
        // kontrola existence a opravneni
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

        WfIssueState state = stateId != null ? issueService.getState(stateId) : null;
        WfIssueType type = typeId != null ? issueService.getType(typeId) : null;

        List<WfIssue> issues = issueService.findIssueByIssueListId(issueListId, state, type);

        return issues.stream().map(issue -> factory.createIssueVO(issue)).collect(Collectors.toList());
    }

    @RequestMapping(value = "/issue_lists", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public WfIssueListVO addIssueList(@RequestBody WfIssueListVO issueListVO) {

        Assert.isNull(issueListVO.getId(), "Neplatný identifikátor protokolu [issueListId=" + issueListVO.getId() + "]");
        Assert.notNull(issueListVO.getOpen(), "Chybí příznak stavu protokolu [open]");

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

        UsrUser admin = userService.getLoggedUser();

        WfIssueList issueList = issueService.addIssueList(fund, issueListVO.getName(), issueListVO.getOpen());

        issueService.addIssueListPermission(issueList, admin, rdUsers, wrUsers);

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

        Assert.isNull(issueVO.getId(), "Neplatný identifikátor připomínky [issueId=" + issueVO.getId() + "]");
        Assert.notNull(issueVO.getIssueListId(), "Chybí identifikátor protokolu [issueListId]");
        Assert.notNull(issueVO.getIssueTypeId(), "Chybí identifikátor druhu připomínky [issueTypeId]");
        Assert.hasText(issueVO.getDescription(), "Chybí popis připomínky [description]");

        UserDetail userDetail = userService.getLoggedUserDetail();

        Integer userId = userDetail != null ? userDetail.getId() : null;
        Assert.notNull(userId, "Uzivatel není prihlášený");

        // todo[marek]: opravneni
        /*
        if (userDetail.hasPermission(UsrPermission.Permission.FUND_RD_ALL)) {
        }
        */

        // validace na existenci
        ArrNode node = issueVO.getNodeId() != null ? arrangementService.getNode(issueVO.getNodeId()) : null;
        WfIssueList issueList = issueService.getIssueList(issueVO.getIssueListId());
        WfIssueType issueType = issueService.getType(issueVO.getIssueTypeId());

        UsrUser usrUser = userService.getUser(userDetail.getId());

        WfIssue issue = issueService.addIssue(issueList, node, issueType, issueVO.getDescription(), usrUser);

        return factory.createIssueVO(issue);
    }

    @RequestMapping(value = "/issues/{issueId}/setState", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public void setIssueState(@PathVariable Integer issueId, @RequestParam Integer issueStateId) {
        // kontrola existence
        WfIssue issue = issueService.getIssue(issueId);
        WfIssueState issueState = issueService.getState(issueStateId);
        issueService.setIssueState(issue, issueState);
    }

    @RequestMapping(value = "/issues/{issueId}/comments", method = RequestMethod.GET)
    @Transactional
    public List<WfCommentVO> findIssueCommentByIssue(@PathVariable Integer issueId) {
        // kontrola existence a opravneni
        WfIssue issue = issueService.getIssue(issueId);
        List<WfComment> commentList = issueService.findCommentByIssueId(issueId);
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

        Assert.isNull(commentVO.getId(), "Neplatný identifikátor komentáře [commentId=" + commentVO.getId() + "");
        Assert.notNull(commentVO.getIssueId(), "Chybí identifikátor připomínky [issueId]");
        Assert.hasText(commentVO.getComment(), "Chybí text připomínky [comment]");

        UserDetail userDetail = userService.getLoggedUserDetail();

        Integer userId = userDetail != null ? userDetail.getId() : null;
        Assert.notNull(userId, "Uzivatel není prihlášený");

        // todo[marek]: opravneni
        /*
        if (userDetail.hasPermission(UsrPermission.Permission.FUND_RD_ALL)) {
        }
        */

        // validace na existenci issue
        WfIssue issue = issueService.getIssue(commentVO.getIssueId());

        UsrUser usrUser = userService.getUser(userDetail.getId());

        WfIssueState newIssueState = commentVO.getNextStateId() != null
                ? issueService.getState(commentVO.getNextStateId())
                : null;

        return factory.createCommentVO(issueService.addComment(issue, commentVO.getComment(), newIssueState, usrUser));
    }

}
