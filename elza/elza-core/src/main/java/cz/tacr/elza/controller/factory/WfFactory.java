package cz.tacr.elza.controller.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.controller.vo.WfCommentVO;
import cz.tacr.elza.controller.vo.WfConfigVO;
import cz.tacr.elza.controller.vo.WfIssueListVO;
import cz.tacr.elza.controller.vo.WfIssueStateVO;
import cz.tacr.elza.controller.vo.WfIssueTypeVO;
import cz.tacr.elza.controller.vo.WfIssueVO;
import cz.tacr.elza.controller.vo.WfSimpleIssueVO;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.WfComment;
import cz.tacr.elza.domain.WfIssue;
import cz.tacr.elza.domain.WfIssueList;
import cz.tacr.elza.domain.WfIssueState;
import cz.tacr.elza.domain.WfIssueType;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.IssueDataService;
import cz.tacr.elza.service.IssueService;
import cz.tacr.elza.service.vo.WfConfig;

@Service
@Transactional(readOnly = true)
public class WfFactory {

    // --- dao ---

    private final PermissionRepository permissionRepository;
    private final IssueDataService issueDataService;

    // --- service ---
    private final IssueService issueService;

    // --- constructor ---

    @Autowired
    public WfFactory(final PermissionRepository permissionRepository,
                     final IssueDataService issueDataService, final IssueService issueService) {
        this.permissionRepository = permissionRepository;
        this.issueDataService = issueDataService;
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

            issueListVO.setRdUsers(FactoryUtils.transformList(rdUserMap.values(), UsrUserVO::newInstance));
            issueListVO.setWrUsers(FactoryUtils.transformList(wrUserMap.values(), UsrUserVO::newInstance));
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

        Map<Integer, UsrUserVO> usersMap = FactoryUtils.transformMap(users, UsrUser::getUserId, UsrUserVO::newInstance);

        Map<Integer, TreeNodeVO> nodeReferenceMarkMap = issueService.findNodeReferenceMark(issues);

        List<WfIssueVO> issueVOS = new ArrayList<>();
        for (WfIssue issue : issues) {

            WfIssueVO issueVO = new WfIssueVO();
            issueVO.setId(issue.getIssueId());
            issueVO.setIssueListId(issue.getIssueList().getIssueListId());
            issueVO.setNumber(issue.getNumber());
            issueVO.setDescription(issue.getDescription());

            // TODO: Improve reading of type and state            

            issueVO.setIssueTypeId(issue.getIssueType().getIssueTypeId());
            issueVO.setIssueStateId(issue.getIssueState().getIssueStateId());
            issueVO.setUserCreate(usersMap.get(issue.getUserCreate().getUserId()));
            issueVO.setTimeCreated(issue.getTimeCreated());

            Integer nodeId = issue.getNodeId();
            if (nodeId != null) {
                issueVO.setNodeId(nodeId);
                TreeNodeVO treeNodeVO = nodeReferenceMarkMap.get(nodeId);
                if (treeNodeVO != null) {
                    issueVO.setReferenceMark(treeNodeVO.getReferenceMark());
                } else {
                    // level does not exists -> was deleted
                    issueVO.setLevelDeleted(true);
                }
            }

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

        Map<Integer, UsrUserVO> usersMap = FactoryUtils.transformMap(users, UsrUser::getUserId, UsrUserVO::newInstance);

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
        List<WfIssue> issues = issueDataService.findOpenIssueByFundIdAndNodeNull(fund, user);
        return issues.stream().map(WfSimpleIssueVO::newInstance).collect(Collectors.toList());
    }

    public WfConfigVO createConfig(final ArrFundVersion fundVersion) {
        WfConfig config = issueDataService.getConfig(fundVersion.getRuleSet());
        return config == null ? null : new WfConfigVO(config.getColors(), config.getIcons());
    }

    /**
     * Seznam druhů připomínek.
     *
     * @return seznam druhů připomínek
     */
    public List<WfIssueTypeVO> createIssueTypes(final List<WfIssueType> issueTypeList) {
        return FactoryUtils.transformList(issueTypeList, WfIssueTypeVO::newInstance);
    }

    /**
     * Seznam stavů připomínek.
     *
     * @return seznam stavů připomínek
     */
    public List<WfIssueStateVO> createIssueStates(final List<WfIssueState> issueStateList) {
        return FactoryUtils.transformList(issueStateList, WfIssueStateVO::newInstance);
    }
}
