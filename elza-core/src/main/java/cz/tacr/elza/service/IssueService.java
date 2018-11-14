package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.WfComment;
import cz.tacr.elza.domain.WfIssue;
import cz.tacr.elza.domain.WfIssueList;
import cz.tacr.elza.domain.WfIssueState;
import cz.tacr.elza.domain.WfIssueType;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.WfCommentRepository;
import cz.tacr.elza.repository.WfIssueListRepository;
import cz.tacr.elza.repository.WfIssueRepository;
import cz.tacr.elza.repository.WfIssueStateRepository;
import cz.tacr.elza.repository.WfIssueTypeRepository;
import cz.tacr.elza.security.UserDetail;

import static cz.tacr.elza.domain.UsrPermission.Permission;

@Service
@Transactional(readOnly = true)
public class IssueService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // --- dao ---

    private final WfCommentRepository commentRepository;
    private final WfIssueListRepository issueListRepository;
    private final WfIssueRepository issueRepository;
    private final WfIssueStateRepository issueStateRepository;
    private final WfIssueTypeRepository issueTypeRepository;

    // --- services ---

    private final UserService userService;

    // --- constructor ---

    @Autowired
    public IssueService(UserService userService, WfCommentRepository commentRepository, WfIssueListRepository issueListRepository, WfIssueRepository issueRepository, WfIssueStateRepository issueStateRepository, WfIssueTypeRepository issueTypeRepository) {
        this.userService = userService;
        this.commentRepository = commentRepository;
        this.issueListRepository = issueListRepository;
        this.issueRepository = issueRepository;
        this.issueStateRepository = issueStateRepository;
        this.issueTypeRepository = issueTypeRepository;
    }

    // --- methods ---

    /**
     * Získání seznamu stavů.
     */
    public List<WfIssueState> findAllState() {
        return issueStateRepository.findAll();
    }

    /**
     * Získání seznamu typů.
     */
    public List<WfIssueType> findAllType() {
        return issueTypeRepository.findAll();
    }

    /**
     * Získání stavu.
     *
     * @param typeId identifikátor stavu
     * @return stav
     */
    public WfIssueState getState(@NotNull Integer stateId) {
        WfIssueState state = issueStateRepository.findOne(stateId);
        if (state == null) {
            throw new ObjectNotFoundException("Stav protokolu nenalezen [issueStateId=" + stateId + "]", BaseCode.ID_NOT_EXIST).setId(stateId);
        }
        return state;
    }

    /**
     * Získání typu.
     *
     * @param typeId identifikátor typu
     * @return typ
     */
    public WfIssueType getType(@NotNull Integer typeId) {
        WfIssueType type = issueTypeRepository.findOne(typeId);
        if (type == null) {
            throw new ObjectNotFoundException("Typ protokolu nenalezen [issueTypeId=" + typeId + "]", BaseCode.ID_NOT_EXIST).setId(typeId);
        }
        return type;
    }

    /**
     * Získání protokolu.
     *
     * @param issueListId identifikátor protokolu
     * @return protokol
     */
    @AuthMethod(permission = {Permission.FUND_ISSUE_ADMIN_ALL, Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR})
    public WfIssueList getIssueList(@AuthParam(type = AuthParam.Type.ISSUE_LIST) @NotNull Integer issueListId) {
        WfIssueList issueList = issueListRepository.findOne(issueListId);
        if (issueList == null) {
            throw new ObjectNotFoundException("Protokol nenalezen [issueListId=" + issueListId + "]", BaseCode.ID_NOT_EXIST).setId(issueListId);
        }
        return issueList;
    }

    /**
     * Získání připomínky.
     *
     * @param issueId identifikátor připomínky
     * @return připomínka
     */
    @AuthMethod(permission = {Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR})
    public WfIssue getIssue(@AuthParam(type = AuthParam.Type.ISSUE) @NotNull Integer issueId) {
        WfIssue issue = issueRepository.findOne(issueId);
        if (issue == null) {
            throw new ObjectNotFoundException("Připomínka nenalezena [issueId=" + issueId + "]", BaseCode.ID_NOT_EXIST).setId(issueId);
        }
        return issue;
    }

    /**
     * Získání komentáře.
     *
     * @param issueId identifikátor komentáře
     * @return komentář
     */
    @AuthMethod(permission = {Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR})
    public WfComment getComment(@AuthParam(type = AuthParam.Type.COMMENT) @NotNull Integer commentId) {
        WfComment comment = commentRepository.findOne(commentId);
        if (comment == null) {
            throw new ObjectNotFoundException("Komentář nenalezen [commentId=" + commentId + "]", BaseCode.ID_NOT_EXIST).setId(commentId);
        }
        return comment;
    }

    public List<WfIssueList> findIssueListByFund(ArrFund fund, UserDetail userDetail) {
        if (userDetail.getId() == null
                || userDetail.hasPermission(Permission.FUND_ISSUE_ADMIN_ALL)
                || userDetail.hasPermission(Permission.FUND_ISSUE_ADMIN_ALL, fund.getFundId())) {
            return findIssueListByFund(fund);
        } else {
            return findIssueListByFundWithPermission(fund, userDetail.getId());
        }
    }

    protected List<WfIssueList> findIssueListByFund(@NotNull ArrFund fund) {
        return issueListRepository.findByFundId(fund.getFundId());
    }

    protected List<WfIssueList> findIssueListByFundWithPermission(@NotNull ArrFund fund, @NotNull Integer userId) {
        Assert.notNull(userId);
        return issueListRepository.findByFundIdWithPermission(fund.getFundId(), userId);
    }

    @AuthMethod(permission = {Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR})
    public List<WfIssue> findIssueByIssueListId(@AuthParam(type = AuthParam.Type.ISSUE_LIST) @NotNull Integer issueListId, WfIssueState issueState, WfIssueType issueType) {
        return issueRepository.findByIssueListId(issueListId, issueState, issueType);
    }

    @AuthMethod(permission = {Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR})
    public List<WfComment> findCommentByIssueId(@AuthParam(type = AuthParam.Type.ISSUE) @NotNull Integer issueId) {
        return commentRepository.findByIssueId(issueId);
    }

    @Transactional
    @AuthMethod(permission = {Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_ADMIN_ALL})
    public WfIssueList addIssueList(@AuthParam(type = AuthParam.Type.FUND) @NotNull ArrFund fund, String name, boolean open) {
        Assert.notNull(fund, "Fund is null");
        Assert.hasText(name, "Empty name");
        WfIssueList issueList = new WfIssueList();
        issueList.setFund(fund);
        issueList.setName(name);
        issueList.setOpen(open);
        return issueListRepository.save(issueList);
    }

    @Transactional
    @AuthMethod(permission = {Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_ADMIN_ALL})
    public void addIssueListPermission(@AuthParam(type = AuthParam.Type.ISSUE_LIST) @NotNull WfIssueList issueList, @NotNull UsrUser owner, Collection<UsrUser> rdUsers, Collection<UsrUser> wrUsers) {

        Map<Integer, UsrUser> users = new HashMap<>();
        Map<Integer, List<Permission>> permissions = new HashMap<>();

        users.put(owner.getUserId(), owner);
        permissions.put(owner.getUserId(), Arrays.asList(Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR));

        if (rdUsers != null) {
            for (UsrUser user : rdUsers) {
                users.put(user.getUserId(), user);
                permissions.computeIfAbsent(user.getUserId(), k -> new ArrayList<>()).add(Permission.FUND_ISSUE_LIST_RD);
            }
        }

        if (wrUsers != null) {
            for (UsrUser user : wrUsers) {
                users.put(user.getUserId(), user);
                permissions.computeIfAbsent(user.getUserId(), k -> new ArrayList<>()).add(Permission.FUND_ISSUE_LIST_WR);
            }
        }

        for (UsrUser user : users.values()) {
            List<UsrPermission> permissionList = permissions.get(user.getUserId()).stream().map(permissionType -> {
                UsrPermission permission = new UsrPermission();
                permission.setPermission(permissionType);
                permission.setUser(user);
                permission.setIssueList(issueList);
                return permission;
            }).collect(Collectors.toList());
            userService.addUserPermission(user, permissionList, false);
        }
    }

    @Transactional
    @AuthMethod(permission = {Permission.FUND_ISSUE_LIST_WR})
    public WfIssue addIssue(@AuthParam(type = AuthParam.Type.ISSUE_LIST) @NotNull WfIssueList issueList, @Nullable ArrNode node, @NotNull WfIssueType issueType, String description, @NotNull UsrUser user) {

        WfIssueState issueState = issueStateRepository.getStartState();

        int number = issueRepository.getNumberMax(issueList.getIssueListId()).orElse(0) + 1;

        WfIssue issue = new WfIssue();
        issue.setIssueList(issueList);
        issue.setNumber(number);
        issue.setNode(node);
        issue.setIssueType(issueType);
        issue.setIssueState(issueState);
        issue.setDescription(description);
        issue.setUserCreate(user);
        issue.setTimeCreated(LocalDateTime.now());
        return issueRepository.save(issue);
    }

    @Transactional
    @AuthMethod(permission = {Permission.FUND_ISSUE_LIST_WR})
    public void setIssueState(@AuthParam(type = AuthParam.Type.ISSUE) @NotNull WfIssue issue, @NotNull WfIssueState issueState) {
        Assert.notNull(issue, "Issue is null");
        Assert.notNull(issueState, "Issue state is null");
        issue.setIssueState(issueState);
        issueRepository.save(issue);
    }

    @Transactional
    @AuthMethod(permission = {Permission.FUND_ISSUE_LIST_WR})
    public WfComment addComment(@AuthParam(type = AuthParam.Type.ISSUE) @NotNull WfIssue issue, String text, @Nullable WfIssueState nextState, @NotNull UsrUser usrUser) {

        WfComment comment = createComment(issue, nextState, text, usrUser);

        if (nextState != null) {
            issue.setIssueState(nextState);
            issueRepository.save(issue);
        }

        return comment;
    }

    protected WfComment createComment(@NotNull WfIssue issue, @Nullable WfIssueState nextState, String text, @NotNull UsrUser usrUser) {
        WfComment comment = new WfComment();
        comment.setIssue(issue);
        comment.setComment(text);
        comment.setUser(usrUser);
        comment.setPrevState(issue.getIssueState());
        comment.setNextState(nextState != null ? nextState : issue.getIssueState());
        comment.setTimeCreated(LocalDateTime.now());
        return commentRepository.save(comment);
    }

}