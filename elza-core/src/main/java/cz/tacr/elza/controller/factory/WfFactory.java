package cz.tacr.elza.controller.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.controller.vo.WfCommentVO;
import cz.tacr.elza.controller.vo.WfIssueListVO;
import cz.tacr.elza.controller.vo.WfIssueVO;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.WfComment;
import cz.tacr.elza.domain.WfIssue;
import cz.tacr.elza.domain.WfIssueList;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.WfCommentRepository;
import cz.tacr.elza.repository.WfIssueListRepository;
import cz.tacr.elza.repository.WfIssueRepository;
import cz.tacr.elza.repository.WfIssueStateRepository;
import cz.tacr.elza.repository.WfIssueTypeRepository;

@Service
@Transactional(readOnly = true)
public class WfFactory {

    // --- dao ---

    private final WfCommentRepository commentRepository;
    private final WfIssueListRepository issueListRepository;
    private final WfIssueRepository issueRepository;
    private final WfIssueStateRepository issueStateRepository;
    private final WfIssueTypeRepository issueTypeRepository;
    private final PermissionRepository permissionRepository;

    // --- constructor ---

    @Autowired
    public WfFactory(WfCommentRepository commentRepository, WfIssueListRepository issueListRepository, WfIssueRepository issueRepository, WfIssueStateRepository issueStateRepository, WfIssueTypeRepository issueTypeRepository, PermissionRepository permissionRepository) {
        this.commentRepository = commentRepository;
        this.issueListRepository = issueListRepository;
        this.issueRepository = issueRepository;
        this.issueStateRepository = issueStateRepository;
        this.issueTypeRepository = issueTypeRepository;
        this.permissionRepository = permissionRepository;
    }

    // --- methods ---

    public WfIssueListVO createIssueListVO(WfIssueList issueList, boolean withPermissions) {

        WfIssueListVO issueListVO = new WfIssueListVO();
        issueListVO.setId(issueList.getIssueListId());
        issueListVO.setFundId(issueList.getFund().getFundId());
        issueListVO.setName(issueList.getName());
        issueListVO.setOpen(issueList.getOpen());

        if (withPermissions) {

            List<UsrUser> rdUsers = new ArrayList<>();
            List<UsrUser> wrUsers = new ArrayList<>();

            List<UsrPermission> permissionList = permissionRepository.findByIssueListId(issueList.getIssueListId());
            for (UsrPermission permission : permissionList) {
                if (Permission.FUND_ISSUE_LIST_RD.equals(permission.getPermission())) {
                    rdUsers.add(permission.getUser());
                }
                if (Permission.FUND_ISSUE_LIST_WR.equals(permission.getPermission())) {
                    wrUsers.add(permission.getUser());
                }
            }

            issueListVO.setRdUserIds(rdUsers.stream().map(user -> user.getUserId()).distinct().collect(Collectors.toList()));
            issueListVO.setWrUserIds(wrUsers.stream().map(user -> user.getUserId()).distinct().collect(Collectors.toList()));
        }

        return issueListVO;
    }

    public WfIssueVO createIssueVO(WfIssue issue) {
        WfIssueVO issueVO = new WfIssueVO();
        issueVO.setId(issue.getIssueId());
        issueVO.setIssueListId(issue.getIssueList().getIssueListId());
        issueVO.setNodeId(issue.getNode() != null ? issue.getNode().getNodeId() : null);
        issueVO.setIssueTypeId(issue.getIssueType().getIssueTypeId());
        issueVO.setIssueStateId(issue.getIssueState().getIssueStateId());
        issueVO.setDescription(issue.getDescription());
        issueVO.setUserCreateId(issue.getUserCreate().getUserId());
        return issueVO;
    }

    public WfCommentVO createCommentVO(WfComment comment) {
        WfCommentVO commentVO = new WfCommentVO();
        commentVO.setId(comment.getCommentId());
        commentVO.setIssueId(comment.getIssue().getIssueId());
        commentVO.setComment(comment.getComment());
        commentVO.setUserId(comment.getUser().getUserId());
        commentVO.setPrevStateId(comment.getPrevState().getIssueStateId());
        commentVO.setNextStateId(comment.getNextState().getIssueStateId());
        commentVO.setTimeCreated(comment.getTimeCreated());
        return commentVO;
    }
}
