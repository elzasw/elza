package cz.tacr.elza.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.controller.vo.IssueNodeItem;
import cz.tacr.elza.controller.vo.NodeItemWithParent;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.WfComment;
import cz.tacr.elza.domain.WfIssue;
import cz.tacr.elza.domain.WfIssueList;
import cz.tacr.elza.domain.WfIssueState;
import cz.tacr.elza.domain.WfIssueType;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.WfCommentRepository;
import cz.tacr.elza.repository.WfIssueListRepository;
import cz.tacr.elza.repository.WfIssueRepository;
import cz.tacr.elza.repository.WfIssueStateRepository;
import cz.tacr.elza.repository.WfIssueTypeRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;

import static cz.tacr.elza.domain.UsrPermission.Permission;
import static cz.tacr.elza.utils.CsvUtils.*;
import static org.apache.commons.io.IOUtils.LINE_SEPARATOR_WINDOWS;

@Service
@Transactional(readOnly = true)
public class IssueService {

    private static final int CVS_MAX_TEXT_LENGTH = 32760;

    // --- dao ---

    private final WfCommentRepository commentRepository;
    private final WfIssueListRepository issueListRepository;
    private final WfIssueRepository issueRepository;
    private final WfIssueStateRepository issueStateRepository;
    private final WfIssueTypeRepository issueTypeRepository;
    private final IssueDataService issueDataService;

    // --- services ---

    private final AccessPointService accessPointService;
    private final ArrangementService arrangementService;
    private final LevelTreeCacheService levelTreeCacheService;
    private final IEventNotificationService eventNotificationService;

    // --- constructor ---

    @Autowired
    public IssueService(
            AccessPointService accessPointService,
            ArrangementService arrangementService,
            LevelTreeCacheService levelTreeCacheService,
            IEventNotificationService eventNotificationService,
            WfCommentRepository commentRepository,
            WfIssueListRepository issueListRepository,
            WfIssueRepository issueRepository,
            WfIssueStateRepository issueStateRepository,
            WfIssueTypeRepository issueTypeRepository,
            IssueDataService issueDataService) {
        this.accessPointService = accessPointService;
        this.arrangementService = arrangementService;
        this.levelTreeCacheService = levelTreeCacheService;
        this.eventNotificationService = eventNotificationService;
        this.commentRepository = commentRepository;
        this.issueListRepository = issueListRepository;
        this.issueRepository = issueRepository;
        this.issueStateRepository = issueStateRepository;
        this.issueTypeRepository = issueTypeRepository;
        this.issueDataService = issueDataService;
    }

    // --- methods ---

    /**
     * Získání stavů připomínek.
     *
     * @returns seznam stavů připomínek
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
    @AuthMethod(permission = {Permission.ADMIN, Permission.FUND_ISSUE_ADMIN_ALL, Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR})
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
    @AuthMethod(permission = {Permission.ADMIN, Permission.FUND_ISSUE_ADMIN_ALL, Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR})
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
    @AuthMethod(permission = {Permission.ADMIN, Permission.FUND_ISSUE_ADMIN_ALL, Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR})
    public WfComment getComment(@AuthParam(type = AuthParam.Type.COMMENT) @NotNull Integer commentId) {
        WfComment comment = commentRepository.findOne(commentId);
        if (comment == null) {
            throw new ObjectNotFoundException("Komentář nenalezen [commentId=" + commentId + "]", BaseCode.ID_NOT_EXIST).setId(commentId);
        }
        return comment;
    }

    /**
     * Vyhledá připomínky k danému protokolu - řazeno vzestupně podle čísla připomínky
     *
     * @param issueList protokol
     * @param issueState stav připomínky, dle kterého filtrujeme
     * @param issueType druh připomínky, dle kterého filtrujeme
     */
    @AuthMethod(permission = {Permission.ADMIN, Permission.FUND_ISSUE_ADMIN_ALL, Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR})
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
    @AuthMethod(permission = {Permission.ADMIN, Permission.FUND_ISSUE_ADMIN_ALL, Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR})
    public List<WfComment> findCommentByIssueId(@AuthParam(type = AuthParam.Type.ISSUE) @NotNull WfIssue issue) {
        Validate.notNull(issue, "Issue is null");
        return commentRepository.findByIssueId(issue.getIssueId());
    }

    /**
     * Založí nový protokol k danému AS
     */
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN, Permission.FUND_ISSUE_ADMIN_ALL, Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_LIST_WR})
    public WfIssueList addIssueList(@AuthParam(type = AuthParam.Type.FUND) @NotNull ArrFund fund, String name, boolean open) {

        Validate.notNull(fund, "Fund is null");
        Validate.notBlank(name, "Empty name");

        WfIssueList issueList = new WfIssueList();
        issueList.setFund(fund);
        issueList.setName(name);
        issueList.setOpen(open);
        issueList = issueListRepository.save(issueList);

        publishEvent(issueList.getIssueListId(), EventType.ISSUE_LIST_CREATE);

        return issueList;
    }

    /**
     * Úprava vlastností existujícího protokolu
     */
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN, Permission.FUND_ISSUE_ADMIN_ALL, Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_LIST_WR})
    public WfIssueList updateIssueList(@AuthParam(type = AuthParam.Type.ISSUE_LIST) @NotNull WfIssueList issueList, @Nullable String name, @Nullable Boolean open) {

        Validate.notNull(issueList, "Issue list is null");

        if (name != null) {
            issueList.setName(name);
        }
        if (open != null) {
            issueList.setOpen(open);
        }
        issueList = issueListRepository.save(issueList);

        publishEvent(issueList.getIssueListId(), EventType.ISSUE_LIST_UPDATE);

        return issueList;
    }

    /**
     * Založí novou připomínku k danému protokolu
     */
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN, Permission.FUND_ISSUE_ADMIN_ALL, Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_LIST_WR})
    public WfIssue addIssue(@AuthParam(type = AuthParam.Type.ISSUE_LIST) @NotNull WfIssueList issueList, @Nullable ArrNode node, @NotNull WfIssueType issueType, @NotNull String description, @NotNull UsrUser user) {

        Validate.notNull(issueList, "Issue list is null");
        Validate.notBlank(description, "Empty description");
        Validate.notNull(user, "User is null");

        Validate.isTrue(issueList.getOpen() != null && issueList.getOpen(), "Invalid issue list state - closed");

        WfIssueState issueState = issueStateRepository.getStartState();

        int number = issueRepository.getNumberMaxByFundId(issueList.getFund().getFundId()).orElse(0) + 1;

        WfIssue issue = new WfIssue();
        issue.setIssueList(issueList);
        issue.setNumber(number);
        issue.setNode(node);
        issue.setIssueType(issueType);
        issue.setIssueState(issueState);
        issue.setDescription(description);
        issue.setUserCreate(user);
        issue.setTimeCreated(LocalDateTime.now());

        issue = issueRepository.save(issue);

        publishIssueEvent(issueList.getIssueListId(), issue.getIssueId(), EventType.ISSUE_CREATE);

        if (node != null) {
            ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(issueList.getFund().getFundId());
            publishVersionEvent(node.getNodeId(), fundVersion, EventType.NODES_CHANGE);
        } else {
            publishEvent(issueList.getFund().getFundId(), EventType.FUND_UPDATE);
        }

        return issue;
    }

    /**
     * Upraví připomínku
     */
    @Transactional
    public WfIssue updateIssue(@NotNull WfIssue issue, @Nullable ArrNode newNode, @NotNull WfIssueType issueType, @NotNull WfIssueState issueState, @NotNull String description) {

        Validate.notNull(issue, "Issue is null");
        Validate.notNull(issueType, "Issue type is null");
        Validate.notNull(issueState, "Issue state is null");
        Validate.notBlank(description, "Empty description");

        ArrNode oldNode = issue.getNode();

        issue.setNode(newNode);
        issue.setIssueType(issueType);
        issue.setIssueState(issueState);
        issue.setDescription(description);

        issue = issueRepository.save(issue);

        publishIssueEvent(issue.getIssueList().getIssueListId(), issue.getIssueId(), EventType.ISSUE_UPDATE);

        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(issue.getFund().getFundId());
        if (oldNode != null) {
            // notifikujeme stary node
            publishVersionEvent(oldNode.getNodeId(), fundVersion, EventType.NODES_CHANGE);
        }

        if (newNode != null) {
            // notifikujeme novy node, ale pouze pokud jsme ho jiz nenotifikovali coby stary
            if (oldNode == null || !newNode.getNodeId().equals(oldNode.getNodeId())) {
                publishVersionEvent(newNode.getNodeId(), fundVersion, EventType.NODES_CHANGE);
            }
        } else {
            publishEvent(issue.getIssueList().getFund().getFundId(), EventType.FUND_UPDATE);
        }

        return issue;
    }

    /**
     * Změna druhu připomínky
     */
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN, Permission.FUND_ISSUE_ADMIN_ALL, Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_LIST_WR})
    public void setIssueType(@AuthParam(type = AuthParam.Type.ISSUE) @NotNull WfIssue issue, @NotNull WfIssueType issueType) {

        Validate.notNull(issue, "Issue is null");
        Validate.notNull(issueType, "Issue type is null");

        if (!issue.getIssueType().equals(issueType)) {

            issue.setIssueType(issueType);
            issueRepository.save(issue);

            publishIssueEvent(issue.getIssueList().getIssueListId(), issue.getIssueId(), EventType.ISSUE_UPDATE);
            if (issue.getNode() != null) {
                ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(issue.getFund().getFundId());
                publishVersionEvent(issue.getNode().getNodeId(), fundVersion, EventType.NODES_CHANGE);
            } else {
                publishEvent(issue.getIssueList().getFund().getFundId(), EventType.FUND_UPDATE);
            }
        }
    }

    /**
     * Založí nový komentář k dané připomínce
     */
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN, Permission.FUND_ISSUE_ADMIN_ALL, Permission.FUND_ISSUE_ADMIN, Permission.FUND_ISSUE_LIST_WR})
    public WfComment addComment(@AuthParam(type = AuthParam.Type.ISSUE) @NotNull WfIssue issue, @NotNull String text, @Nullable WfIssueState nextState, @NotNull UsrUser user) {

        Validate.notNull(issue, "Issue is null");
        Validate.notBlank(text, "Empty comment");
        Validate.notNull(user, "User is null");

        WfComment comment = issueDataService.createComment(issue, nextState, text, user);

        if (nextState != null) {

            if (!issue.getIssueState().equals(nextState)) {

                issue.setIssueState(nextState);
                issueRepository.save(issue);

                if (issue.getNode() != null) {
                    ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(issue.getFund().getFundId());
                    publishVersionEvent(issue.getNode().getNodeId(), fundVersion, EventType.NODES_CHANGE);
                } else {
                    publishEvent(issue.getIssueList().getFund().getFundId(), EventType.FUND_UPDATE);
                }
            }
        }

        publishIssueEvent(issue.getIssueList().getIssueListId(), issue.getIssueId(), EventType.ISSUE_UPDATE);

        return comment;
    }

    /**
     * Úprava komentáře
     */
    @Transactional
    public WfComment updateComment(@NotNull WfComment comment, @NotNull String text, @Nullable WfIssueState nextState) {

        Validate.notNull(comment, "Comment is null");
        Validate.notBlank(text, "Empty comment");

        comment.setComment(text);
        if (nextState != null) {
            comment.setNextState(nextState);
        }
        comment = commentRepository.save(comment);

        WfIssue issue = comment.getIssue();

        if (nextState != null) {
            if (!issue.getIssueState().equals(nextState)) {

                issue.setIssueState(nextState);
                issueRepository.save(issue);

                if (issue.getNode() != null) {
                    ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(issue.getFund().getFundId());
                    publishVersionEvent(issue.getNode().getNodeId(), fundVersion, EventType.NODES_CHANGE);
                } else {
                    publishEvent(issue.getIssueList().getFund().getFundId(), EventType.FUND_UPDATE);
                }
            }
        }

        publishIssueEvent(issue.getIssueList().getIssueListId(), issue.getIssueId(), EventType.ISSUE_UPDATE);

        return comment;
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

    @AuthMethod(permission = {Permission.ADMIN, Permission.FUND_ISSUE_LIST_RD, Permission.FUND_ISSUE_LIST_WR})
    public void exportIssueList(@AuthParam(type = AuthParam.Type.ISSUE_LIST) @NotNull WfIssueList issueList, OutputStream os) throws IOException {

        Validate.notNull(issueList, "Issue list is null");

        List<WfIssue> issues = findIssueByIssueListId(issueList, null, null);

        Map<Integer, TreeNodeVO> nodeMap = findNodeReferenceMark(issues);

        Map<Integer, List<WfComment>> issueToCommentMap = issueDataService.groupCommentByIssueId(issues.stream().map(issue -> issue.getIssueId()).collect(Collectors.toList()));

        Map<Integer, ApName> userToAccessPointNameMap = findUserAccessPointNames(issues, issueToCommentMap);

        DateTimeFormatter commentDateFormatter = DateTimeFormatter.ofPattern("d.M.u");

        String[] headers = new String[]{
                "\u010C\u00EDslo JP",
                "\u010C\u00EDslo",
                "Druh",
                "Stav",
                "U\u017Eivatel",
                "Datum",
                "Popis",
                "Koment\u00E1\u0159e"
        };

        CSVPrinter printer = CSV_EXCEL_FORMAT.withHeader(headers).withQuoteMode(QuoteMode.NON_NUMERIC)
                .print(new OutputStreamWriter(os, CSV_EXCEL_CHARSET));

        for (WfIssue issue : issues) {

            TreeNodeVO node = issue.getNode() != null ? nodeMap.get(issue.getNode().getNodeId()) : null;
            printer.print(node != null ? StringUtils.join(node.getReferenceMark()) : null);
            printer.print(issue.getNumber());
            printer.print(issue.getIssueType().getName());
            printer.print(issue.getIssueState().getName());

            printer.print(formatUserName(userToAccessPointNameMap, issue.getUserCreate()));
            printer.print(CVS_DATE_TIME_FORMATTER.format(issue.getTimeCreated()));
            printer.print(issue.getDescription());

            StringBuilder text = new StringBuilder(1024);

            List<WfComment> comments = issueToCommentMap.get(issue.getIssueId());

            if (comments != null) {
                for (WfComment comment : comments) {

                    if (text.length() > 0) {
                        text.append(LINE_SEPARATOR_WINDOWS);
                    }

                    text.append(String.format("%s (%s): %s",
                            commentDateFormatter.format(comment.getTimeCreated()),
                            formatUserName(userToAccessPointNameMap, comment.getUser()),
                            comment.getComment()));

                    if (comment.getPrevState() != comment.getNextState()) {
                        text.append(String.format(" [zm\u011Bna stavu na %s]", comment.getNextState().getName()));
                    }
                }
            }

            if (text.length() > CVS_MAX_TEXT_LENGTH) {
                text.setLength(CVS_MAX_TEXT_LENGTH);
                if (!StringUtils.endsWith(text, LINE_SEPARATOR_WINDOWS)) {
                    text.append(LINE_SEPARATOR_WINDOWS);
                }
                text.append("...");
            }

            printer.print(text);

            printer.println();
        }

        printer.flush();
    }

    /**
     * Sestaví informace o zanoření
     *
     * @param issues seznam připomínek
     */
    public Map<Integer, TreeNodeVO> findNodeReferenceMark(@NotNull Collection<WfIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Integer> fundIds = new HashSet<>();
        Set<Integer> nodeIds = new HashSet<>();
        for (WfIssue issue : issues) {
            if (issue.getNode() != null) {
                fundIds.add(issue.getIssueList().getFund().getFundId());
                nodeIds.add(issue.getNode().getNodeId());
            }
        }
        if (nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Validate.isTrue(fundIds.size() == 1, "Připomínky jsou z různých archivních souborů");
        return levelTreeCacheService.findNodeReferenceMark(fundIds.iterator().next(), nodeIds);
    }

    protected Map<Integer, ApName> findUserAccessPointNames(List<WfIssue> issues, Map<Integer, List<WfComment>> issueCommentMap) {

        Map<Integer, UsrUser> userMap = new HashMap<>();
        for (WfIssue issue : issues) {
            UsrUser user = issue.getUserCreate();
            userMap.put(user.getUserId(), user);
        }
        for (List<WfComment> comments : issueCommentMap.values()) {
            for (WfComment comment : comments) {
                UsrUser user = comment.getUser();
                userMap.put(user.getUserId(), user);
            }
        }

        Set<Integer> accessPointIds = userMap.values().stream()
                .map(user -> user.getParty().getAccessPoint().getAccessPointId())
                .collect(Collectors.toSet());

        Map<Integer, ApName> accessPointToNameMap = accessPointService.findPreferredNamesByAccessPointIds(accessPointIds).stream()
                .collect(Collectors.toMap(apName -> apName.getAccessPoint().getAccessPointId(), apName -> apName));

        return userMap.values().stream()
                .collect(Collectors.toMap(user -> user.getUserId(), user -> accessPointToNameMap.get(user.getParty().getAccessPoint().getAccessPointId())));
    }

    protected String formatUserName(Map<Integer, ApName> userAccessPointNameMap, UsrUser user) {
        return userAccessPointNameMap.get(user.getUserId()).getName();
    }

    protected void publishEvent(Integer id, final EventType type) {
        eventNotificationService.publishEvent(EventFactory.createIdEvent(type, id));
    }

    protected void publishIssueEvent(final Integer issueListId, final Integer issueId, final EventType type) {
        eventNotificationService.publishEvent(EventFactory.createIdEventInIssueList(type, issueListId, issueId));
    }

    protected void publishVersionEvent(final Integer id, final ArrFundVersion fundVersion, final EventType type) {
        eventNotificationService.publishEvent(EventFactory.createIdsInVersionEvent(type, fundVersion, id));
    }

    private List<Integer> findNodeIdWithOpenIssue(@NotNull ArrFund fund, @NotNull UserDetail userDetail) {

        Validate.notNull(fund, "Fund is null");
        if (userDetail == null) {
            return Collections.emptyList();
        }

        Integer userId = userDetail.hasPermission(Permission.ADMIN) ? null : userDetail.getId();

        return issueRepository.findNodeIdWithOpenIssueByFundId(fund.getFundId(), userId);
    }

    /**
     * Vyhledá další uzel s otevřenou připomínkou.
     *
     * @param fundVersion verze AS
     * @param currentNodeId výchozí uzel (default root)
     * @param direction krok
     * @return uzel s připomínkou
     */
    public IssueNodeItem nextIssueNode(@NotNull ArrFundVersion fundVersion, @Nullable Integer currentNodeId, int direction, @NotNull UserDetail userDetail) {

        List<Integer> nodeIdList = findNodeIdWithOpenIssue(fundVersion.getFund(), userDetail);

        if (nodeIdList.isEmpty()) {
            return new IssueNodeItem(0);
        }

        Set<Integer> nodeIds = new HashSet<>(nodeIdList);
        TreeNode root = arrangementService.getRootTreeNode(fundVersion);

        if (currentNodeId == null) {
            currentNodeId = root.getId();
        }

        List<Integer> sortedNodeIds = filterNodes(root, nodeIds, currentNodeId);

        int index = sortedNodeIds.indexOf(currentNodeId);

        if (index == -1) {
            // vychozi node nebyl nalezen v aktualni verzi
            throw new ObjectNotFoundException("Nenalezena JP ve verzi " + fundVersion.getFundVersionId(),
                    ArrangementCode.NODE_NOT_FOUND).setId(currentNodeId);
        }

        // direction = Integer.signum(direction);

        if (nodeIds.contains(currentNodeId)) {
            index += direction;
        } else {
            // vychozi node lezi nekde mezi dvema pripominkama
            sortedNodeIds.remove(index);
            if (direction == 0) {
                // specialni pripad - skok o 0 by znamena aktualni node, ale ten v tomto pripade nema pripominku
                return new IssueNodeItem(sortedNodeIds.size());
            }
            if (direction > 0) {
                index++;
            }
            index += direction;
        }

        index %= sortedNodeIds.size();
        if (index < 0) {
            index += sortedNodeIds.size();
        }

        NodeItemWithParent node = getNodeItemWithParent(sortedNodeIds.get(index), fundVersion);

        return new IssueNodeItem(sortedNodeIds.size(), index, node);
    }

    private List<Integer> filterNodes(TreeNode root, Set<Integer> nodeIds, Integer currentNodeId) {
        List<Integer> sortedNodeIds = new LinkedList<>();
        levelTreeCacheService.walkTree(root, node -> {
            Integer nodeId = node.getId();
            if (nodeIds.contains(nodeId) || nodeId.equals(currentNodeId)) {
                sortedNodeIds.add(nodeId);
            }
        });
        return sortedNodeIds;
    }

    private NodeItemWithParent getNodeItemWithParent(Integer nodeId, ArrFundVersion fundVersion) {
        List<Integer> nodeIds = Collections.singletonList(nodeId);
        List<NodeItemWithParent> nodeItemsWithParents = levelTreeCacheService.getNodeItemsWithParents(nodeIds, fundVersion);
        return nodeItemsWithParents.get(0);
    }
}
