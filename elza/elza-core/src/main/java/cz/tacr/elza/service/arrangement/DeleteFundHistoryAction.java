package cz.tacr.elza.service.arrangement;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.*;
import cz.tacr.elza.service.eventnotification.events.EventFund;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cz.tacr.elza.repository.ExceptionThrow.fund;

/**
 * Action to delete fund history
 * <p>
 * Fund history deletion is complex task
 * which is handled by this action.
 */
@Component
@Scope("prototype")
public class DeleteFundHistoryAction {

    private static final Logger logger = LoggerFactory.getLogger(DeleteFundHistoryAction.class);

    @Autowired
    private UserService userService;
    @Autowired
    private IEventNotificationService eventNotificationService;
    @Autowired
    private NodeConformityRepository nodeConformityInfoRepository;
    @Autowired
    private NodeConformityMissingRepository nodeConformityMissingRepository;
    @Autowired
    private NodeConformityErrorRepository nodeConformityErrorRepository;
    @Autowired
    private BulkActionRunRepository faBulkActionRepository;
    @Autowired
    private BulkActionNodeRepository faBulkActionNodeRepository;
    @Autowired
    private AsyncRequestService asyncRequestService;

    @Autowired
    private FundVersionRepository fundVersionRepository;
    @Autowired
    private FundRepository fundRepository;
    @Autowired
    private DataUriRefRepository dataUriRefRepository;

    @Autowired
    private DigitizationRequestNodeRepository digitizationRequestNodeRepository;
    @Autowired
    private DaoLinkRepository daoLinkRepository;

    //TODO: Should not be used here, method accessing this repository have to be refactorized
    @Autowired
    private CachedNodeRepository cachedNodeRepository;
    @Autowired
    private ChangeRepository changeRepository;
    @Autowired
    private DescItemRepository descItemRepository;
    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private NodeOutputRepository nodeOutputRepository;

    @Autowired
    private RevertingChangesService revertingChangesService;

    private Integer fundId;

    private ArrFund fund;

    private ArrFundVersion fundVersion;

    private ArrNode rootNode;

    @Autowired
    private EntityManager em;

    @Autowired
    private NodeExtensionRepository nodeExtensionRepository;

    @Autowired
    private NodeConformityRepository nodeConformityRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private VisiblePolicyRepository visiblePolicyRepository;


    /**
     * Prepare fund history deletion
     */
    private void prepare() {

        // Check if exists
        this.fund = fundRepository.findById(fundId).orElseThrow(fund(fundId));

        // get last version and rootId
        this.fundVersion = fundVersionRepository.findByFundIdAndLockChangeIsNull(fundId);
        if (fundVersion == null) {
            throw new BusinessException("Fund has no active version", BaseCode.ID_NOT_EXIST).set("fundId", fundId);
        }

        // set root level
        rootNode = fundVersion.getRootNode();
        if (rootNode == null) {
            throw new BusinessException("Version without root node", BaseCode.ID_NOT_EXIST)
                    .set("fundVersionId", fundVersion.getFundVersionId());
        }

        // terminate all services - for all versions, stejně se budou verze mazat
        List<ArrFundVersion> versions = this.fundVersionRepository.findVersionsByFundIdOrderByCreateDateDesc(fundId);
        for (ArrFundVersion version : versions) {
            asyncRequestService.terminateNodeWorkersByFund(version.getFundVersionId());
            asyncRequestService.terminateBulkActions(version.getFundVersionId());
        }
    }

    public void run(Integer fundId) {

        logger.info("Deleting history of fund: {}", fundId);

        this.fundId = fundId;

        prepare();

        // delete arr_item, které mají vyplněn delete_change_id + najít podřízená data
        final List<ArrItem> arrItemList = itemRepository.findHistoricalByFund(fund);
        final List<ArrData> arrDataList = new ArrayList<>();
        for (ArrItem arrItem : arrItemList) {
            final ArrData data = arrItem.getData();
            if (data != null) {
                arrDataList.add(data);
            }
        }
        iterateAction(arrItemList, itemRepository::deleteAll);
        em.flush();

        // odmazat opuštěná data
        // fixme: je potřeba vůbec tahle kontrola? navíc dost pomalá - pro každé ArrData check do DB;
        //  myslím si, že by se mohlo vždy mazat všechny ArrData; pokud by to bylo opravdu pomalé, přepsat
        for (ArrData arrData : arrDataList) {
            final List<ArrItem> itemList = itemRepository.findByData(arrData);
            if (CollectionUtils.isEmpty(itemList)) {
                dataRepository.delete(arrData);
            }
        }
        em.flush();

        // delete arr_level, které mají vyplněn delete_change_id
        final List<ArrLevel> arrLevelList = levelRepository.findHistoricalByFund(fund);
        iterateAction(arrLevelList, levelRepository::deleteAll);
        em.flush();

        // arr_node se také smazají, pokud se na ně neodkazuje žádný level a musí se smazat i návazné entity jako výstupy, a podobně
        final List<Integer> unusedNodeIdsByFund = nodeRepository.findUnusedNodeIdsByFund(fund);
        if (!unusedNodeIdsByFund.isEmpty()) {
            dropNodeInfo(unusedNodeIdsByFund);
            changeRepository.deleteByPrimaryNodeIds(unusedNodeIdsByFund);

            dataUriRefRepository.updateByNodesIdIn(unusedNodeIdsByFund);

            nodeRepository.deleteByNodeIdIn(unusedNodeIdsByFund);
        }
        em.flush();

        // vyčištění nepoužitých arr_change
        Query deleteNotUseChangesQuery = revertingChangesService.createDeleteNotUseChangesQuery();
        deleteNotUseChangesQuery.executeUpdate();
        em.flush();

        // verze AS budou vymazány všechny a založí se nová verze kopii poslední, aktuální
        RulRuleSet ruleSet = fundVersion.getRuleSet();

        nodeConformityMissingRepository.deleteByNodeConformityNodeFund(fund);
        nodeConformityErrorRepository.deleteByNodeConformityNodeFund(fund);
        nodeConformityInfoRepository.deleteByNodeFund(fund);
        nodeConformityRepository.deleteByNodeFund(fund);

        faBulkActionNodeRepository.deleteByFund(fund);
        faBulkActionRepository.deleteByFund(fund);

        fundVersionRepository.deleteByFund(fund);

        ArrChange change = arrangementService.createChange(ArrChange.Type.CREATE_AS);
        // create new version
        fundVersion = arrangementService.createVersion(change, fund, ruleSet, rootNode);

        updateChanges(fund, change);

        // vynutit uložení změn do DB
        em.flush();

        // odeslání informace o změně verze na klienta
        eventNotificationService.publishEvent(new EventFund(EventType.APPROVE_VERSION, fundVersion.getFund().getFundId(), fundVersion.getFundVersionId()));


        logger.info("Fund history deleted: {}", fundId);

    }

    private void updateChanges(final ArrFund fund, final ArrChange change) {
        Set<ArrChange> changes = new HashSet<>();

        List<ArrItem> items = itemRepository.findByFund(fund);
        Set<Integer> itemIds = new HashSet<>(items.size());
        for (ArrItem item : items) {
            changes.add(item.getCreateChange());
            itemIds.add(item.getItemId());
        }
        iterateAction(itemIds, ids -> itemRepository.updateCreateChange(ids, change));

        List<ArrLevel> levels = levelRepository.findByFund(fund);
        Set<Integer> levelIds = new HashSet<>();
        for (ArrLevel level : levels) {
            changes.add(level.getCreateChange());
            levelIds.add(level.getLevelId());
        }
        iterateAction(levelIds, ids -> levelRepository.updateCreateChange(ids, change));

        iterateAction(changes, changeRepository::deleteAll);
    }

    /**
     * Drop all information connected with node
     */
    private void dropNodeInfo(List<Integer> nodeIds) {
        // delete policies
        iterateAction(nodeIds, visiblePolicyRepository::deleteByNodeIdIn);

        userService.deletePermissionByNodeIds(nodeIds);

        // delete node from cache
        iterateAction(nodeIds, cachedNodeRepository::deleteByNodeIdIn);

        // delete node conformity
        iterateAction(nodeIds, nodeConformityErrorRepository::deleteByNodeConformityNodeIdIn);
        iterateAction(nodeIds, nodeConformityMissingRepository::deleteByNodeConformityNodeIdIn);
        iterateAction(nodeIds, nodeConformityInfoRepository::deleteByNodeIdIn);

        // delete attached extensions
        iterateAction(nodeIds, nodeExtensionRepository::deleteByNodeIdIn);

        // ostatní položky navázané na mazané node
        iterateAction(nodeIds, daoLinkRepository::deleteByNodeIdIn);
        iterateAction(nodeIds, digitizationRequestNodeRepository::deleteByNodeFundIdIn);

        dropBulkActions(nodeIds);
        dropOutputs(nodeIds);
        dropDescItems(nodeIds);
    }

    private void dropDescItems(List<Integer> nodeIds) {
        iterateAction(nodeIds, descItemRepository::deleteByNodeIdIn);
        em.flush();
    }

    private void dropOutputs(List<Integer> nodeIds) {
        iterateAction(nodeIds, nodeOutputRepository::deleteByNodeIdIn);
        em.flush();
    }

    private void dropBulkActions(List<Integer> nodeIds) {
        iterateAction(nodeIds, faBulkActionNodeRepository::deleteByNodeIdIn);
        em.flush();
    }

    @FunctionalInterface
    interface ActionCallback<T> {
        void action(Collection<T> items);
    }

    private <T> void iterateAction(final Collection<T> items, final ActionCallback<T> callback) {
        ObjectListIterator<T> iterator = new ObjectListIterator<>(items);
        while (iterator.hasNext()) {
            List<T> next = iterator.next();
            callback.action(next);
        }
    }

}
