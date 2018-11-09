package cz.tacr.elza.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
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

import com.fasterxml.jackson.annotation.JsonInclude;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.IssueStateVO;
import cz.tacr.elza.controller.vo.IssueTypeVO;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.WfComment;
import cz.tacr.elza.domain.WfIssue;
import cz.tacr.elza.domain.WfIssueList;
import cz.tacr.elza.domain.WfIssueState;
import cz.tacr.elza.domain.WfIssueType;
import cz.tacr.elza.repository.FilteredResult;
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

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

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

    // --- constructor ---

    @Autowired
    public IssueController(IssueService issueService, UserService userService, FundRepository fundRepository, NodeRepository nodeRepository, WfCommentRepository commentRepository, WfIssueListRepository issueListRepository, WfIssueRepository issueRepository, WfIssueStateRepository issueStateRepository, WfIssueTypeRepository issueTypeRepository, ClientFactoryVO factoryVo) {
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
    }

    // --- methods ---

    @RequestMapping(value = "/issue_types", method = RequestMethod.GET)
    public List<IssueTypeVO> findAllTypes() {
        List<WfIssueType> issueTypeList = issueTypeRepository.findAll();
        return factoryVo.createIssueTypes(issueTypeList);
    }

    @RequestMapping(value = "/issue_states", method = RequestMethod.GET)
    public List<IssueStateVO> findAllStates() {
        List<WfIssueState> issueStateList = issueStateRepository.findAll();
        return factoryVo.createIssueStates(issueStateList);
    }

    @RequestMapping(value = "/funds/{fundId}/issue_lists", method = RequestMethod.GET)
    @Transactional
    public List<IssueListVO> findIssueListByFund(@PathVariable Integer fundId) {
        List<WfIssueList> issueListList = issueService.findByFundId(fundId);
        return issueListList.stream().map(issueList -> createIssueListVO(issueList)).collect(Collectors.toList());
    }

    @RequestMapping(value = "/issue_lists/{issueListId}", method = RequestMethod.GET)
    @Transactional
    public IssueListVO getIssueList(@PathVariable Integer issueListId) {
        WfIssueList issueList = issueListRepository.getOneCheckExist(issueListId);
        return createIssueListVO(issueList);
    }

    @RequestMapping(value = "/issue_lists/{issueListId}/issues", method = RequestMethod.GET)
    @Transactional
    public List<IssueVO> findIssues(@PathVariable Integer issueListId,
                                    @RequestParam(name = "issue_state_id", required = false) Integer stateId,
                                    @RequestParam(name = "issue_type_id", required = false) Integer typeId) {

        WfIssueState state = stateId != null ? issueStateRepository.getOneCheckExist(stateId) : null;
        WfIssueType type = typeId != null ? issueTypeRepository.getOneCheckExist(typeId) : null;

        List<WfIssue> issueList = issueService.findIssueByIssueListId(issueListId, state, type);

        return issueList.stream().map(issue -> createIssueVO(issue)).collect(Collectors.toList());
    }

    @RequestMapping(value = "/issue_lists", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public void addIssueList(@RequestBody IssueListVO issueListVO) {

        Assert.isNull(issueListVO.getIssueListId(), "Neplatný identifikátor protokolu [issueListId=" + issueListVO.getIssueListId() + "]");
        Assert.notNull(issueListVO.getOpen(), "Chybí příznak stavu protokolu [open]");

        // validace na existenci fundu
        ArrFund fund = fundRepository.getOneCheckExist(issueListVO.getFundId());

        issueService.addIssueList(fund, issueListVO.getName(), issueListVO.getOpen());
    }

    @RequestMapping(value = "/issues/{issueId}", method = RequestMethod.GET)
    @Transactional
    public IssueVO getIssue(@PathVariable Integer issueId) {
        WfIssue issue = issueRepository.getOneCheckExist(issueId);
        return createIssueVO(issue);
    }

    @RequestMapping(value = "/issues", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public void addIssue(@RequestBody IssueVO issueVO) {

        Assert.isNull(issueVO.getIssueId(), "Neplatný identifikátor připomínky [issueId=" + issueVO.getIssueId() + "]");
        Assert.notNull(issueVO.getIssueListId(), "Chybí identifikátor protokolu [issueListId]");
        Assert.notNull(issueVO.getIssueTypeId(), "Chybí identifikátor druhu připomínky [issueTypeId]");
        Assert.hasText(issueVO.getDescription(), "Chybí popis připomínky [description]");

        // validace na existenci issue listu
        WfIssueList issueList = issueListRepository.getOneCheckExist(issueVO.getIssueListId());
        ArrNode node = issueVO.getNodeId() != null ? nodeRepository.getOneCheckExist(issueVO.getNodeId()) : null;

        UserDetail userDetail = userService.getLoggedUserDetail();

        FilteredResult<ArrFund> funds;

        // todo[marek]: opravneni
        /*
        if (userDetail.hasPermission(UsrPermission.Permission.FUND_RD_ALL)) {
            // read all funds
        } else {
            Integer userId = userDetail.getId();
        }
        */

        WfIssueType issueType = issueTypeRepository.getOneCheckExist(issueVO.getIssueTypeId());

        UsrUser usrUser = userService.getUser(userDetail.getId());

        issueService.addIssue(issueList, node, issueType, issueVO.getDescription(), usrUser);
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
    public List<CommentVO> findCommentByIssue(@PathVariable Integer issueId) {
        WfIssue issue = issueRepository.getOneCheckExist(issueId);
        List<WfComment> commentList = issueService.findCommentByIssueId(issueId);
        return commentList.stream().map(comment -> createCommentVO(comment)).collect(Collectors.toList());
    }

    @RequestMapping(value = "/comments/{commentId}", method = RequestMethod.GET)
    @Transactional
    public CommentVO getComment(@PathVariable Integer commentId) {
        WfComment comment = commentRepository.getOneCheckExist(commentId);
        return createCommentVO(comment);
    }

    @RequestMapping(value = "/comments", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public void addComment(@RequestBody CommentVO commentVO) {

        Assert.isNull(commentVO.getCommentId(), "Neplatný identifikátor komentáře [commentId=" + commentVO.getCommentId() + "");
        Assert.notNull(commentVO.getIssueId(), "Chybí identifikátor připomínky [issueId]");
        Assert.hasText(commentVO.getComment(), "Chybí text připomínky [comment]");

        // validace na existenci issue
        WfIssue issue = issueRepository.getOneCheckExist(commentVO.getIssueId());

        UserDetail userDetail = userService.getLoggedUserDetail();

        FilteredResult<ArrFund> funds;

        // todo[marek]: opravneni
        /*
        if (userDetail.hasPermission(UsrPermission.Permission.FUND_RD_ALL)) {
            // read all funds
        } else {
            Integer userId = userDetail.getId();
        }
        */

        UsrUser usrUser = userService.getUser(userDetail.getId());

        WfIssueState newIssueState = commentVO.getNextStateId() != null
                ? issueStateRepository.getOneCheckExist(commentVO.getNextStateId())
                : null;

        issueService.addComment(issue, commentVO.getComment(), newIssueState, usrUser);
    }

    protected IssueListVO createIssueListVO(WfIssueList issueList) {
        IssueListVO issueListVO = new IssueListVO();
        issueListVO.setIssueListId(issueList.getIssueListId());
        issueListVO.setFundId(issueList.getFund().getFundId());
        issueListVO.setName(issueList.getName());
        issueListVO.setOpen(issueList.getOpen());
        return issueListVO;
    }

    protected IssueVO createIssueVO(WfIssue issue) {
        IssueVO issueVO = new IssueVO();
        issueVO.setIssueId(issue.getIssueId());
        issueVO.setIssueListId(issue.getIssueList().getIssueListId());
        issueVO.setNodeId(issue.getNode().getNodeId());
        issueVO.setIssueTypeId(issue.getIssueType().getIssueTypeId());
        issueVO.setIssueStateId(issue.getIssueState().getIssueStateId());
        issueVO.setDescription(issue.getDescription());
        issueVO.setUserCreateId(issue.getUserCreate().getUserId());
        return issueVO;
    }

    protected CommentVO createCommentVO(WfComment comment) {
        CommentVO commentVO = new CommentVO();
        commentVO.setCommentId(comment.getCommentId());
        commentVO.setIssueId(comment.getIssue().getIssueId());
        commentVO.setComment(comment.getComment());
        commentVO.setUserId(comment.getUser().getUserId());
        commentVO.setPrevStateId(comment.getPrevState().getIssueStateId());
        commentVO.setNextStateId(comment.getNextState().getIssueStateId());
        commentVO.setTimeCreated(comment.getTimeCreated());
        return commentVO;
    }

    // --- classes ---

    public static class IssueListVO {

        // --- fields ---

        private Integer issueListId;
        @NotNull
        private Integer fundId;
        @NotBlank
        private String name;
        @NotNull
        private Boolean open;

        // --- getters/setters ---

        public Integer getIssueListId() {
            return issueListId;
        }

        public void setIssueListId(Integer issueListId) {
            this.issueListId = issueListId;
        }

        public Integer getFundId() {
            return fundId;
        }

        public void setFundId(Integer fundId) {
            this.fundId = fundId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Boolean getOpen() {
            return open;
        }

        public void setOpen(Boolean open) {
            this.open = open;
        }
    }

    public static class IssueVO {

        // --- fields ---

        private Integer issueId;
        private Integer issueListId;
        @JsonInclude(Include.NON_DEFAULT)
        private Integer nodeId;
        private Integer issueTypeId;
        private Integer issueStateId;
        private String description;
        private Integer userCreateId;

        // --- getters/setters ---

        public Integer getIssueId() {
            return issueId;
        }

        public void setIssueId(Integer issueId) {
            this.issueId = issueId;
        }

        public Integer getIssueListId() {
            return issueListId;
        }

        public void setIssueListId(Integer issueListId) {
            this.issueListId = issueListId;
        }

        public Integer getNodeId() {
            return nodeId;
        }

        public void setNodeId(Integer nodeId) {
            this.nodeId = nodeId;
        }

        public Integer getIssueTypeId() {
            return issueTypeId;
        }

        public void setIssueTypeId(Integer issueTypeId) {
            this.issueTypeId = issueTypeId;
        }

        public Integer getIssueStateId() {
            return issueStateId;
        }

        public void setIssueStateId(Integer issueStateId) {
            this.issueStateId = issueStateId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getUserCreateId() {
            return userCreateId;
        }

        public void setUserCreateId(Integer userCreateId) {
            this.userCreateId = userCreateId;
        }
    }

    public static class CommentVO {

        // --- fields ---

        private Integer commentId;
        private Integer issueId;
        private String comment;
        private Integer userId;
        private Integer prevStateId;
        private Integer nextStateId;
        private LocalDateTime timeCreated;

        // --- getters/setters ---

        public Integer getCommentId() {
            return commentId;
        }

        public void setCommentId(Integer commentId) {
            this.commentId = commentId;
        }

        public Integer getIssueId() {
            return issueId;
        }

        public void setIssueId(Integer issueId) {
            this.issueId = issueId;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        public Integer getPrevStateId() {
            return prevStateId;
        }

        public void setPrevStateId(Integer prevStateId) {
            this.prevStateId = prevStateId;
        }

        public Integer getNextStateId() {
            return nextStateId;
        }

        public void setNextStateId(Integer nextStateId) {
            this.nextStateId = nextStateId;
        }

        public LocalDateTime getTimeCreated() {
            return timeCreated;
        }

        public void setTimeCreated(LocalDateTime timeCreated) {
            this.timeCreated = timeCreated;
        }
    }
}
