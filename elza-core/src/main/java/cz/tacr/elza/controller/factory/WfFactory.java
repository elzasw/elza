package cz.tacr.elza.controller.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.vo.WfConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.service.IssueService;

@Service
@Transactional(readOnly = true)
public class WfFactory {

    // --- other factory ---

    private final ClientFactoryVO factoryVO;

    // --- dao ---

    private final PermissionRepository permissionRepository;

    // --- service ---

    private final IssueService issueService;

    // --- constructor ---

    @Autowired
    public WfFactory(final ClientFactoryVO factoryVO,
                     final PermissionRepository permissionRepository,
                     final IssueService issueService) {
        this.factoryVO = factoryVO;
        this.permissionRepository = permissionRepository;
        this.issueService = issueService;
    }

    // --- methods ---

    /**
     * Seznam protokolů
     *
     * @param withPermissions vyplnit i oprávnění
     * @returns seznam protokolů
     */
    public WfIssueListVO createIssueListVO(WfIssueList issueList, boolean withPermissions) {

        WfIssueListVO issueListVO = new WfIssueListVO();
        issueListVO.setId(issueList.getIssueListId());
        issueListVO.setFundId(issueList.getFund().getFundId());
        issueListVO.setName(issueList.getName());
        issueListVO.setOpen(issueList.getOpen());

        if (withPermissions) {

            Map<Integer, UsrUser> rdUserMap = new HashMap<>();
            Map<Integer, UsrUser> wrUserMap = new HashMap<>();

            List<UsrPermission> permissionList = permissionRepository.findByIssueListId(issueList.getIssueListId());
            for (UsrPermission permission : permissionList) {
                UsrUser user = permission.getUser();
                if (Permission.FUND_ISSUE_LIST_RD.equals(permission.getPermission())) {
                    rdUserMap.put(user.getUserId(), user);
                }
                if (Permission.FUND_ISSUE_LIST_WR.equals(permission.getPermission())) {
                    wrUserMap.put(user.getUserId(), user);
                }
            }

            issueListVO.setRdUsers(factoryVO.createUserList(new ArrayList<>(rdUserMap.values()), false));
            issueListVO.setWrUsers(factoryVO.createUserList(new ArrayList<>(wrUserMap.values()), false));
        }

        return issueListVO;
    }

    /**
     * Připomínka
     *
     * @returns seznam připomínek
     */
    public WfIssueVO createIssueVO(WfIssue issue) {
        if (issue == null) {
            return null;
        }
        return createIssueVO(Collections.singleton(issue)).get(0);
    }

    /**
     * Seznam připomínek
     *
     * @return seznam připomínek
     */
    public List<WfIssueVO> createIssueVO(final Collection<WfIssue> issues) {
        if (issues == null) {
            return null;
        }

        Set<UsrUser> users = new HashSet<>();
        for (WfIssue issue : issues) {
            users.add(issue.getUserCreate());
        }

        Map<Integer, UsrUserVO> usersMap = factoryVO.createUserList(users, false).stream()
                .collect(Collectors.toMap(UsrUserVO::getId, Function.identity()));

        Map<Integer, TreeNodeVO> nodeReferenceMarkMap = issueService.findNodeReferenceMark(issues);

        List<WfIssueVO> issueVOS = new ArrayList<>();
        for (WfIssue issue : issues) {

            Integer nodeId;
            String[] referenceMark;
            if (issue.getNode() != null) {
                nodeId = issue.getNode().getNodeId();
                TreeNodeVO treeNodeVO = nodeReferenceMarkMap.get(nodeId);
                referenceMark = treeNodeVO != null ? treeNodeVO.getReferenceMark() : null;
            } else {
                nodeId = null;
                referenceMark = null;
            }

            WfIssueVO issueVO = new WfIssueVO();
            issueVO.setId(issue.getIssueId());
            issueVO.setIssueListId(issue.getIssueList().getIssueListId());
            issueVO.setNumber(issue.getNumber());
            issueVO.setNodeId(nodeId);
            issueVO.setReferenceMark(referenceMark);
            issueVO.setIssueTypeId(issue.getIssueType().getIssueTypeId());
            issueVO.setIssueStateId(issue.getIssueState().getIssueStateId());
            issueVO.setDescription(issue.getDescription());
            issueVO.setUserCreate(usersMap.get(issue.getUserCreate().getUserId()));
            issueVO.setTimeCreated(issue.getTimeCreated());

            issueVOS.add(issueVO);
        }

        return issueVOS;
    }

    /**
     * Komentář.
     *
     * @return komentář
     */
    public WfCommentVO createCommentVO(final WfComment comment) {
        if (comment == null) {
            return null;
        }
        return createCommentVO(Collections.singleton(comment)).get(0);
    }

    /**
     * Seznam komentářů.
     *
     * @return seznam komentářů
     */
    public List<WfCommentVO> createCommentVO(final Collection<WfComment> comments) {
        if (comments == null) {
            return null;
        }

        Set<UsrUser> users = new HashSet<>();
        for (WfComment comment : comments) {
            users.add(comment.getUser());
        }

        Map<Integer, UsrUserVO> usersMap = factoryVO.createUserList(users, false).stream()
                .collect(Collectors.toMap(UsrUserVO::getId, Function.identity()));

        List<WfCommentVO> commentVOS = new ArrayList<>();
        for (WfComment comment : comments) {
            WfCommentVO commentVO = new WfCommentVO();
            commentVO.setId(comment.getCommentId());
            commentVO.setIssueId(comment.getIssue().getIssueId());
            commentVO.setComment(comment.getComment());
            commentVO.setUser(usersMap.get(comment.getUser().getUserId()));
            commentVO.setPrevStateId(comment.getPrevState().getIssueStateId());
            commentVO.setNextStateId(comment.getNextState().getIssueStateId());
            commentVO.setTimeCreated(comment.getTimeCreated());
            commentVOS.add(commentVO);
        }

        return commentVOS;
    }

    public List<WfSimpleIssueVO> createSimpleIssues(final ArrFund fund, final UserDetail user) {
        List<WfIssue> issues = issueService.findOpenIssueByFundIdAndNodeNull(fund, user);
        return issues.stream().map(factoryVO::createSimpleIssueVO).collect(Collectors.toList());
    }

    public WfConfigVO createConfig(final ArrFundVersion fundVersion) {
        WfConfig config = issueService.getConfig(fundVersion.getRuleSet());
        return config == null ? null : new WfConfigVO(config.getColors(), config.getIcons());
    }
}
