package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import cz.tacr.elza.repository.PermissionRepository;
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

    private final PermissionRepository permissionRepository;

    // --- services ---

    private final UserService userService;

    // --- constructor ---

    @Autowired
    public IssueService(UserService userService, WfCommentRepository commentRepository, WfIssueListRepository issueListRepository, WfIssueRepository issueRepository, WfIssueStateRepository issueStateRepository, WfIssueTypeRepository issueTypeRepository, PermissionRepository permissionRepository) {
        this.userService = userService;
        this.commentRepository = commentRepository;
        this.issueListRepository = issueListRepository;
        this.issueRepository = issueRepository;
        this.issueStateRepository = issueStateRepository;
        this.issueTypeRepository = issueTypeRepository;
        this.permissionRepository = permissionRepository;
    }

    // --- methods ---

    /**
     * Získání stavů připomínek.
     *
     * @returns zeznam stavů připomínek
     */
    public List<WfIssueState> findAllIssueStates() {
        return issueStateRepository.findAll();
    }

    /**
     * Získání druhů připomnek.
     *
     * @returns seznam druhů připomínek
     */
    public List<WfIssueType> findAllIssueTypes() {
        return issueTypeRepository.findAllOrderByViewOrder();
    }

    /**
     * Získání detailu stavu připomínek.
     *
     * @param stateId identifikátor stavu
     * @return stav
     */
    public WfIssueState getIssueState(@NotNull Integer stateId) {
        WfIssueState state = issueStateRepository.findOne(stateId);
        if (state == null) {
            throw new ObjectNotFoundException("Stav protokolu nenalezen [issueStateId=" + stateId + "]", BaseCode.ID_NOT_EXIST).setId(stateId);
        }
        return state;
    }

    /**
     * Získání detailu druhu připomínky.
     *
     * @param typeId identifikátor druhu
     * @return druh
     */
    public WfIssueType getIssueType(@NotNull Integer typeId) {
        WfIssueType type = issueTypeRepository.findOne(typeId);
        if (type == null) {
            throw new ObjectNotFoundException("Typ protokolu nenalezen [issueTypeId=" + typeId + "]", BaseCode.ID_NOT_EXIST).setId(typeId);
        }
        return type;
    }

    /**
     * Získání detailu protokolu.
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
     * Získání detailu připomínky.
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
     * Získání detailu komentáře.
     *
     * @param commentId identifikátor komentáře
     * @returns komentář
     */
    @AuthMethod(permission = {Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR})
    public WfComment getComment(@AuthParam(type = AuthParam.Type.COMMENT) @NotNull Integer commentId) {
        WfComment comment = commentRepository.findOne(commentId);
        if (comment == null) {
            throw new ObjectNotFoundException("Komentář nenalezen [commentId=" + commentId + "]", BaseCode.ID_NOT_EXIST).setId(commentId);
        }
        return comment;
    }

    /**
     * Vyhledá protokoly k danému archivní souboru - řazeno nejprve otevřené a pak uzavřené
     *
     * @param fund AS
     * @return seznam protokolů
     */
    public List<WfIssueList> findIssueListByFund(@NotNull ArrFund fund, @NotNull UserDetail userDetail) {
        Validate.notNull(fund, "Fund is null");
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
        return issueListRepository.findByFundIdWithPermission(fund.getFundId(), userId);
    }

    /**
     * Vyhledá připomínky k danému protokolu - řazeno vzestupně podle čísla připomínky
     *
     * @param issueList protokol
     * @param issueState stav připomínky, dle kterého filtrujeme
     * @param issueType druh připomínky, dle kterého filtrujeme
     */
    @AuthMethod(permission = {Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR})
    public List<WfIssue> findIssueByIssueListId(@AuthParam(type = AuthParam.Type.ISSUE_LIST) @NotNull WfIssueList issueList, @Nullable WfIssueState issueState, @Nullable WfIssueType issueType) {
        Validate.notNull(issueList, "Issue list is null");
        return issueRepository.findByIssueListId(issueList.getIssueListId(), issueState, issueType);
    }

    /**
     * Vyhledá komentáře k dané připomínce - řazeno vzestupně podle času
     *
     * @param issue připomínka
     * @return seznam komentářů
     */
    @AuthMethod(permission = {Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR})
    public List<WfComment> findCommentByIssueId(@AuthParam(type = AuthParam.Type.ISSUE) @NotNull WfIssue issue) {
        Validate.notNull(issue, "Issue is null");
        return commentRepository.findByIssueId(issue.getIssueId());
    }

    /**
     * Založí nový protokol k danému AS
     */
    @Transactional
    @AuthMethod(permission = {Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_ADMIN_ALL})
    public WfIssueList addIssueList(@AuthParam(type = AuthParam.Type.FUND) @NotNull ArrFund fund, String name, boolean open) {
        Validate.notNull(fund, "Fund is null");
        Validate.notBlank(name, "Empty name");
        WfIssueList issueList = new WfIssueList();
        issueList.setFund(fund);
        issueList.setName(name);
        issueList.setOpen(open);
        return issueListRepository.save(issueList);
    }

    /**
     * Úprava vlastností existujícího protokolu
     */
    @Transactional
    @AuthMethod(permission = {Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_ADMIN_ALL})
    public WfIssueList updateIssueList(@AuthParam(type = AuthParam.Type.ISSUE_LIST) @NotNull WfIssueList issueList, @Nullable String name, @Nullable Boolean open) {
        Validate.notNull(issueList, "Issue list is null");
        if (name != null) {
            issueList.setName(name);
        }
        if (open != null) {
            issueList.setOpen(open);
        }
        return issueListRepository.save(issueList);
    }

    /**
     * Nastavení oprávnění k novému protokolu
     */
    @Transactional
    @AuthMethod(permission = {Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_ADMIN_ALL})
    public void addIssueListPermission(@AuthParam(type = AuthParam.Type.ISSUE_LIST) @NotNull WfIssueList issueList, @NotNull UsrUser admin, Collection<UsrUser> rdUsers, Collection<UsrUser> wrUsers) {

        Validate.notNull(issueList, "Issue list is null");
        Validate.notNull(admin, "User is null");

        Map<Integer, UsrUser> users = new HashMap<>();
        Map<Integer, List<Permission>> permissions = new HashMap<>();

        // users.put(admin.getUserId(), admin);
        // permissions.put(admin.getUserId(), Arrays.asList(Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR));

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

        permissionRepository.flush();
    }

    /**
     * Nastavení oprávnění k existujícímu protokolu
     */
    @Transactional
    @AuthMethod(permission = {Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_ADMIN_ALL})
    public void updateIssueListPermission(@AuthParam(type = AuthParam.Type.ISSUE_LIST) @NotNull WfIssueList issueList, @NotNull UsrUser admin, Collection<UsrUser> rdUsers, Collection<UsrUser> wrUsers) {

        Validate.notNull(issueList, "Issue list is null");
        Validate.notNull(admin, "User is null");

        if (rdUsers != null) {
            userService.deletePermissionsByIssueList(issueList, Permission.FUND_ISSUE_LIST_RD);
        }

        if (wrUsers != null) {
            userService.deletePermissionsByIssueList(issueList, Permission.FUND_ISSUE_LIST_WR);
        }

        addIssueListPermission(issueList, admin, rdUsers, wrUsers);
    }

    /**
     * Založí novou připomínku k danému protokolu
     */
    @Transactional
    @AuthMethod(permission = {Permission.FUND_ISSUE_LIST_WR})
    public WfIssue addIssue(@AuthParam(type = AuthParam.Type.ISSUE_LIST) @NotNull WfIssueList issueList, @Nullable ArrNode node, @NotNull WfIssueType issueType, String description, @NotNull UsrUser user) {

        Validate.notNull(issueList, "Issue list is null");
        Validate.notBlank(description, "Empty description");
        Validate.notNull(user, "User is null");

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

    /**
     * Změna stavu připomínky
     */
    @Transactional
    @AuthMethod(permission = {Permission.FUND_ISSUE_LIST_WR})
    public void setIssueState(@AuthParam(type = AuthParam.Type.ISSUE) @NotNull WfIssue issue, @NotNull WfIssueState issueState) {
        Validate.notNull(issue, "Issue is null");
        Validate.notNull(issueState, "Issue state is null");
        issue.setIssueState(issueState);
        issueRepository.save(issue);
    }

    /**
     * Založí nový komentář k dané připomínce
     */
    @Transactional
    @AuthMethod(permission = {Permission.FUND_ISSUE_LIST_WR})
    public WfComment addComment(@AuthParam(type = AuthParam.Type.ISSUE) @NotNull WfIssue issue, String text, @Nullable WfIssueState nextState, @NotNull UsrUser user) {

        Validate.notNull(issue, "Issue is null");
        Validate.notBlank(text, "Empty comment");
        Validate.notNull(user, "User is null");

        WfComment comment = createComment(issue, nextState, text, user);

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

    /**
     * V případě, že při UNDO dochází k rušení celého nodu, musí se zrušit vazba z issue.
     */
    @Transactional
    public void resetIssueNode(Collection<Integer> deleteNodeIds) {
        if (deleteNodeIds != null && !deleteNodeIds.isEmpty()) {
            issueRepository.resetNodes(deleteNodeIds);
        }
    }
}