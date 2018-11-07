package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.WfComment;
import cz.tacr.elza.domain.WfIssue;
import cz.tacr.elza.domain.WfIssueList;
import cz.tacr.elza.domain.WfIssueState;
import cz.tacr.elza.domain.WfIssueType;
import cz.tacr.elza.repository.WfCommentRepository;
import cz.tacr.elza.repository.WfIssueListRepository;
import cz.tacr.elza.repository.WfIssueRepository;
import cz.tacr.elza.repository.WfIssueStateRepository;
import cz.tacr.elza.repository.WfIssueTypeRepository;

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

    // --- constructor ---

    @Autowired
    public IssueService(WfCommentRepository commentRepository, WfIssueListRepository issueListRepository, WfIssueRepository issueRepository, WfIssueStateRepository issueStateRepository, WfIssueTypeRepository issueTypeRepository) {
        this.commentRepository = commentRepository;
        this.issueListRepository = issueListRepository;
        this.issueRepository = issueRepository;
        this.issueStateRepository = issueStateRepository;
        this.issueTypeRepository = issueTypeRepository;
    }

    // --- methods ---

    public List<WfIssueList> findByFundId(Integer fundId) {
        return issueListRepository.findByFundId(fundId);
    }

    public List<WfIssue> findIssueByIssueListId(Integer issueListId, WfIssueState issueState, WfIssueType issueType) {
        return issueRepository.findByIssueListId(issueListId, issueState, issueType);
    }

    public List<WfComment> findCommentByIssueId(Integer issueId) {
        return commentRepository.findByIssueId(issueId);
    }

    @Transactional
    public void setIssueState(WfIssue issue, WfIssueState issueState) {
        Assert.notNull(issue, "Issue is null");
        Assert.notNull(issueState, "Issue state is null");
        issue.setIssueState(issueState);
        issueRepository.save(issue);
    }

    @Transactional
    public WfIssueList addIssueList(ArrFund fund, String name, boolean open) {
        Assert.notNull(fund, "Fund is null");
        Assert.hasText(name, "Empty name");
        WfIssueList issueList = new WfIssueList();
        issueList.setFund(fund);
        issueList.setName(name);
        issueList.setOpen(open);
        return issueListRepository.save(issueList);
    }

    @Transactional
    public WfIssue addIssue(WfIssueList issueList, @Nullable ArrNode node, WfIssueType issueType, String description, UsrUser user) {

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
    public WfComment addComment(WfIssue issue, String text, @Nullable WfIssueState nextState, UsrUser usrUser) {

        WfComment comment = createComment(issue, nextState, text, usrUser);

        if (nextState != null) {
            issue.setIssueState(nextState);
            issueRepository.save(issue);
        }

        return comment;
    }

    protected WfComment createComment(WfIssue issue, @Nullable WfIssueState nextState, String text, UsrUser usrUser) {
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
