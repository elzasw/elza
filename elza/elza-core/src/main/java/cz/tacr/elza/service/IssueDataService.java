package cz.tacr.elza.service;

import com.google.common.eventbus.Subscribe;
import cz.tacr.elza.EventBusListener;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.packageimport.xml.SettingFundIssues;
import cz.tacr.elza.repository.SettingsRepository;
import cz.tacr.elza.repository.WfCommentRepository;
import cz.tacr.elza.repository.WfIssueListRepository;
import cz.tacr.elza.repository.WfIssueRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import cz.tacr.elza.service.vo.WfConfig;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cz.tacr.elza.domain.UsrPermission.Permission;

@Service
@Transactional(readOnly = true)
@EventBusListener
public class IssueDataService {

    private static final Logger logger = LoggerFactory.getLogger(IssueDataService.class);

    // --- dao ---

    private final WfIssueRepository issueRepository;
    private final SettingsRepository settingsRepository;
    private final WfIssueListRepository issueListRepository;
    private final WfCommentRepository commentRepository;

    private final StaticDataService staticDataService;


    // --- fields ---

    private Map<String, WfConfig> configs;

    // --- constructor ---

    @Autowired
    public IssueDataService(WfIssueRepository issueRepository,
                            final SettingsRepository settingsRepository,
                            final WfIssueListRepository issueListRepository,
                            final WfCommentRepository commentRepository,
                            final StaticDataService staticDataService) {
        this.issueRepository = issueRepository;
        this.settingsRepository = settingsRepository;
        this.issueListRepository = issueListRepository;
        this.commentRepository = commentRepository;
        this.staticDataService = staticDataService;
    }

    @Subscribe
    public synchronized void invalidateCache(final CacheInvalidateEvent cacheInvalidateEvent) {
        if (cacheInvalidateEvent.contains(CacheInvalidateEvent.Type.VIEW)) {
            if (configs != null) {
                logger.info("Issues configs invalidated.");
            }
            configs = null;
        }
    }

    /**
     * Seznam otevřených připomínek (tzn. nejsou v koncovém stavu) ze všech otevřených protokolů ke kontrétní JP.
     *
     * @param nodeIds    seznam identifikátorů jednotek popisu
     * @param userDetail přihlášený uživatel
     * @return seznam připomínek groupovaných podle identifikátoru jednotky popisu
     */
    public Map<Integer, List<WfIssue>> groupOpenIssueByNodeId(@NotNull Collection<Integer> nodeIds, UserDetail userDetail) {
        Validate.notNull(nodeIds, "Node ID list is null");
        if (nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (userDetail == null) {
            return Collections.emptyMap();
        }
        Integer userId = userDetail.hasPermission(Permission.ADMIN) ? null : userDetail.getId();
        List<WfIssue> issueList = issueRepository.findOpenByNodeId(nodeIds, userId);
        return issueList.stream().collect(Collectors.groupingBy(issue -> issue.getNode().getNodeId()));
    }

    /**
     * Seznam otevřených připomínek (tzn. nejsou v koncovém stavu) ze všech otevřených protokolů ke kontrétní JP.
     *
     * @param nodeId     identifikátor jednotky popisu
     * @param userDetail přihlášený uživatel
     */
    public List<WfIssue> findOpenIssueByNodeId(@NotNull Integer nodeId, UserDetail userDetail) {
        Validate.notNull(nodeId, "Node ID is null");
        if (userDetail == null) {
            return Collections.emptyList();
        }
        Integer userId = userDetail.hasPermission(Permission.ADMIN) ? null : userDetail.getId();
        return issueRepository.findOpenByNodeId(Collections.singletonList(nodeId), userId);
    }

    /**
     * Seznam otevřených připomínek (tzn. nejsou v koncovém stavu) ze všech otevřených protokolů bez JP na konkrétní AS.
     *
     * @param fund       archivní soubor
     * @param userDetail přihlášený uživatel
     */
    public List<WfIssue> findOpenIssueByFundIdAndNodeNull(@NotNull ArrFund fund, UserDetail userDetail) {
        Validate.notNull(fund, "Fund is null");
        if (userDetail == null) {
            return Collections.emptyList();
        }
        Integer userId = userDetail.hasPermission(Permission.ADMIN) ? null : userDetail.getId();
        return issueRepository.findOpenByFundIdAndNodeNull(fund.getFundId(), userId);
    }


    /**
     * Získání oprávnění podle pravidel.
     *
     * @param ruleSet pravidla
     * @return nastavení
     */
    @Nullable
    public WfConfig getConfig(final RulRuleSet ruleSet) {
        StaticDataProvider sdp = staticDataService.getData();
        if (configs == null) {
            configs = new HashMap<>();

            // load relevant settings
            List<UISettings> uiSettingsList = settingsRepository.findByUserAndSettingsTypeAndEntityType(null, 
                                                                                                        UISettings.SettingsType.FUND_ISSUES.toString(), 
                                                                                                        UISettings.SettingsType.FUND_ISSUES.getEntityType());

            uiSettingsList.forEach(uiSettings -> {
                RulRuleSet rulRuleSet = sdp.getRuleSetById(uiSettings.getEntityId());
                SettingFundIssues setting = SettingFundIssues.newInstance(uiSettings);
                configs.put(rulRuleSet.getCode(), new WfConfig(setting.getIssueTypeColors(), setting.getIssueStateIcons()));
            });
        }
        return configs.get(ruleSet.getCode());
    }


    /**
     * Vyhledá protokoly k danému archivní souboru - řazeno nejprve otevřené a pak uzavřené
     *
     * @param fund AS
     * @param open filtr pro stav (otevřený/uzavřený)
     * @return seznam protokolů
     */
    public List<WfIssueList> findIssueListByFund(@NotNull ArrFund fund, @Nullable Boolean open, @NotNull UserDetail userDetail) {

        Validate.notNull(fund, "Fund is null");

        if (userDetail == null) {
            return Collections.emptyList();
        }

        Integer userId = userDetail.getId() == null // virtuální uživatel, obdobně jako superadmin
                || userDetail.hasPermission(Permission.ADMIN)
                || userDetail.hasPermission(Permission.FUND_ISSUE_ADMIN_ALL)
                || userDetail.hasPermission(Permission.FUND_ISSUE_ADMIN, fund.getFundId())
                ? null
                : userDetail.getId();

        return issueListRepository.findByFundIdWithPermission(fund.getFundId(), open, userId);
    }


    /**
     * Získání posledního komentáře k dané připomínce
     *
     * @param issue připomínka
     * @return komentář
     */
    public WfComment getLastComment(WfIssue issue) {
        List<WfComment> commentList = commentRepository.findLastByIssueId(issue.getIssueId(), PageRequest.of(0, 1));
        return !commentList.isEmpty() ? commentList.get(0) : null;
    }

    public WfComment createComment(@NotNull WfIssue issue, @Nullable WfIssueState nextState, @NotNull String text, @NotNull UsrUser user) {
        WfComment comment = new WfComment();
        comment.setIssue(issue);
        comment.setComment(text);
        comment.setUser(user);
        comment.setPrevState(issue.getIssueState());
        comment.setNextState(nextState != null ? nextState : issue.getIssueState());
        comment.setTimeCreated(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    public List<WfComment> findCommentByIssueIds(List<Integer> issueIds) {
        if (issueIds == null || issueIds.isEmpty()) {
            return Collections.emptyList();
        }
        return commentRepository.findByIssueIds(issueIds);
    }

    public Map<Integer, List<WfComment>> groupCommentByIssueId(List<Integer> issueIds) {
        return findCommentByIssueIds(issueIds).stream().collect(Collectors.groupingBy(comment -> comment.getIssue().getIssueId()));
    }


}
