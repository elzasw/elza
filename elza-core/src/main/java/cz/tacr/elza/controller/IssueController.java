package cz.tacr.elza.controller;

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
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.WfCommentRepository;
import cz.tacr.elza.repository.WfIssueListRepository;
import cz.tacr.elza.repository.WfIssueRepository;
import cz.tacr.elza.repository.WfIssueStateRepository;
import cz.tacr.elza.repository.WfIssueTypeRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.IssueService;
import cz.tacr.elza.service.UserService;

@RestController
@RequestMapping("/api/issue")
public class IssueController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // --- dao ---

    private final FundRepository fundRepository;
    private final NodeRepository nodeRepository;

    private final WfCommentRepository commentRepository;
    private final WfIssueListRepository issueListRepository;
    private final WfIssueRepository issueRepository;
    private final WfIssueStateRepository issueStateRepository;
    private final WfIssueTypeRepository issueTypeRepository;

    // --- services ---

    private final IssueService issueService;

    private final UserService userService;

    // --- fields ---

    private final ClientFactoryVO factoryVo;

    private final WfFactory factory;

    // --- constructor ---

    @Autowired
    public IssueController(IssueService issueService, UserService userService, FundRepository fundRepository, NodeRepository nodeRepository, WfCommentRepository commentRepository, WfIssueListRepository issueListRepository, WfIssueRepository issueRepository, WfIssueStateRepository issueStateRepository, WfIssueTypeRepository issueTypeRepository, ClientFactoryVO factoryVo, WfFactory factory) {
        this.issueService = issueService;
        this.userService = userService;
        this.fundRepository = fundRepository;
        this.nodeRepository = nodeRepository;
        this.commentRepository = commentRepository;
        this.issueListRepository = issueListRepository;
        this.issueRepository = issueRepository;
        this.issueStateRepository = issueStateRepository;
        this.issueTypeRepository = issueTypeRepository;
        this.factoryVo = factoryVo;
        this.factory = factory;
    }

    // --- methods ---

    @RequestMapping(value = "/issue_types", method = RequestMethod.GET)
    public List<WfIssueTypeVO> findAllTypes() {
        List<WfIssueType> issueTypeList = issueTypeRepository.findAll();
        return factoryVo.createIssueTypes(issueTypeList);
    }

    @RequestMapping(value = "/issue_states", method = RequestMethod.GET)
    public List<WfIssueStateVO> findAllStates() {
        List<WfIssueState> issueStateList = issueStateRepository.findAll();
        return factoryVo.createIssueStates(issueStateList);
    }

    @RequestMapping(value = "/funds/{fundId}/issue_lists", method = RequestMethod.GET)
    @Transactional
    public List<WfIssueListVO> findIssueListByFund(@PathVariable Integer fundId) {
        List<WfIssueList> issueListList = issueService.findByFundId(fundId);
        return issueListList.stream().map(issueList -> factory.createIssueListVO(issueList)).collect(Collectors.toList());
    }

    @RequestMapping(value = "/issue_lists/{issueListId}", method = RequestMethod.GET)
    @Transactional
    public WfIssueListVO getIssueList(@PathVariable Integer issueListId) {
        WfIssueList issueList = issueListRepository.getOneCheckExist(issueListId);
        return factory.createIssueListVO(issueList);
    }

    @RequestMapping(value = "/issue_lists/{issueListId}/issues", method = RequestMethod.GET)
    @Transactional
    public List<WfIssueVO> findIssues(@PathVariable Integer issueListId,
                                      @RequestParam(name = "issue_state_id", required = false) Integer stateId,
                                      @RequestParam(name = "issue_type_id", required = false) Integer typeId) {

        WfIssueState state = stateId != null ? issueStateRepository.getOneCheckExist(stateId) : null;
        WfIssueType type = typeId != null ? issueTypeRepository.getOneCheckExist(typeId) : null;

        List<WfIssue> issueList = issueService.findIssueByIssueListId(issueListId, state, type);

        return issueList.stream().map(issue -> factory.createIssueVO(issue)).collect(Collectors.toList());
    }

    @RequestMapping(value = "/issue_lists", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public WfIssueListVO addIssueList(@RequestBody WfIssueListVO issueListVO) {

        Assert.isNull(issueListVO.getId(), "Neplatný identifikátor protokolu [issueListId=" + issueListVO.getId() + "]");
        Assert.notNull(issueListVO.getOpen(), "Chybí příznak stavu protokolu [open]");

        // todo[marek]: opravneni
        /*
        if (userDetail.hasPermission(UsrPermission.Permission.FUND_RD_ALL)) {
        }
        */

        // validace na existenci fundu
        ArrFund fund = fundRepository.getOneCheckExist(issueListVO.getFundId());

        return factory.createIssueListVO(issueService.addIssueList(fund, issueListVO.getName(), issueListVO.getOpen()));
    }

    @RequestMapping(value = "/issues/{issueId}", method = RequestMethod.GET)
    @Transactional
    public WfIssueVO getIssue(@PathVariable Integer issueId) {
        WfIssue issue = issueRepository.getOneCheckExist(issueId);
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
        ArrNode node = issueVO.getNodeId() != null ? nodeRepository.getOneCheckExist(issueVO.getNodeId()) : null;
        WfIssueList issueList = issueListRepository.getOneCheckExist(issueVO.getIssueListId());
        WfIssueType issueType = issueTypeRepository.getOneCheckExist(issueVO.getIssueTypeId());

        UsrUser usrUser = userService.getUser(userDetail.getId());

        return factory.createIssueVO(issueService.addIssue(issueList, node, issueType, issueVO.getDescription(), usrUser));
    }

    @RequestMapping(value = "/issues/{issueId}/setState", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public void setIssueState(@PathVariable Integer issueId, @RequestParam Integer issueStateId) {
        WfIssue issue = issueRepository.getOneCheckExist(issueId);
        WfIssueState issueState = issueStateRepository.getOneCheckExist(issueStateId);
        issueService.setIssueState(issue, issueState);
    }

    @RequestMapping(value = "/issues/{issueId}/comments", method = RequestMethod.GET)
    @Transactional
    public List<WfCommentVO> findCommentByIssue(@PathVariable Integer issueId) {
        WfIssue issue = issueRepository.getOneCheckExist(issueId);
        List<WfComment> commentList = issueService.findCommentByIssueId(issueId);
        return commentList.stream().map(comment -> factory.createCommentVO(comment)).collect(Collectors.toList());
    }

    @RequestMapping(value = "/comments/{commentId}", method = RequestMethod.GET)
    @Transactional
    public WfCommentVO getComment(@PathVariable Integer commentId) {
        WfComment comment = commentRepository.getOneCheckExist(commentId);
        return factory.createCommentVO(comment);
    }

    @RequestMapping(value = "/comments", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public WfCommentVO addComment(@RequestBody WfCommentVO commentVO) {

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
        WfIssue issue = issueRepository.getOneCheckExist(commentVO.getIssueId());

        UsrUser usrUser = userService.getUser(userDetail.getId());

        WfIssueState newIssueState = commentVO.getNextStateId() != null
                ? issueStateRepository.getOneCheckExist(commentVO.getNextStateId())
                : null;

        return factory.createCommentVO(issueService.addComment(issue, commentVO.getComment(), newIssueState, usrUser));
    }

}
