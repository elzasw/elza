package cz.tacr.elza.service.arrangement;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundStructureExtension;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrLockedValue;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeExtension;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.ArrRequestQueueItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ArrFileRepository;
import cz.tacr.elza.repository.BulkActionNodeRepository;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.CachedNodeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataUriRefRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DigitizationRequestNodeRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundStructureExtensionRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.ItemSettingsRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.LockedValueRepository;
import cz.tacr.elza.repository.NodeExtensionRepository;
import cz.tacr.elza.repository.NodeOutputRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputFileRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.repository.OutputTemplateRepository;
import cz.tacr.elza.repository.RequestQueueItemRepository;
import cz.tacr.elza.repository.RequestRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.repository.VisiblePolicyRepository;
import cz.tacr.elza.repository.vo.ItemChange;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.RevertingChangesService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.eventnotification.events.EventFund;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private BulkActionRunRepository faBulkActionRepository;
    @Autowired
    private BulkActionNodeRepository faBulkActionNodeRepository;
    @Autowired
    private AsyncRequestService asyncRequestService;
    @Autowired
    private DmsService dmsService;

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
    private RequestRepository requestRepository;
    @Autowired
    private RequestQueueItemRepository requestQueueItemRepository;
    @Autowired
    private FundStructureExtensionRepository fundStructureExtensionRepository;
    @Autowired
    private StructuredObjectRepository structuredObjectRepository;
    @Autowired
    private LockedValueRepository lockedValueRepository;
    @Autowired
    private OutputRepository outputRepository;
    @Autowired
    private OutputTemplateRepository outputTemplateRepository;
    @Autowired
    private OutputFileRepository outputFileRepository;
    @Autowired
    private OutputResultRepository outputResultRepository;

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
    ArrangementService arrangementService;

    @Autowired
    private ArrangementInternalService arrangementInternalService;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private VisiblePolicyRepository visiblePolicyRepository;

    @Autowired
    private ItemSettingsRepository itemSettingsRepository;

    @Autowired
    private ArrFileRepository arrFileRepository;

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
        // drop ArrNodeConformityError for given items
        ruleService.deleteConformityByItems(arrItemList);

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

        // odstranění všech arr_file a soubory na disku
        final List<ArrFile> arrFiles = arrFileRepository.findHistoricalByFund(fund);
        final List<Integer> fileIds = arrFiles.stream().map(p -> p.getFileId()).collect(Collectors.toList());
        dmsService.deleteFilesAfterCommitByIds(fileIds);
        iterateAction(arrFiles, arrFileRepository::deleteAll);
        em.flush();

        // delete arr_level, které mají vyplněn delete_change_id
        final List<ArrLevel> arrLevelList = levelRepository.findHistoricalByFund(fund);
        iterateAction(arrLevelList, levelRepository::deleteAll);
        em.flush();

        // výstupy
        nodeOutputRepository.deleteByFundAndDeleteChangeIsNotNull(fund);
        outputTemplateRepository.deleteByFundAndDeleteChangeIsNotNull(fund);
        List<ArrOutputFile> outputFiles = outputFileRepository.findByFundAndDeleteChangeIsNotNull(fund);
        dmsService.deleteFilesAfterCommit(outputFiles);
        outputFileRepository.deleteByFundAndDeleteChangeIsNotNull(fund);

        outputResultRepository.deleteByFundAndDeleteChangeIsNotNull(fund);
        itemSettingsRepository.deleteByFundAndDeleteChangeIsNotNull(fund);
        outputRepository.deleteByFundAndDeleteChangeIsNotNull(fund);
        structuredObjectRepository.deleteByFundAndDeleteChangeIsNotNull(fund);

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_AS);

        // arr_node se také smazají, pokud se na ně neodkazuje žádný level a musí se smazat i návazné entity jako výstupy, a podobně
        List<Integer> unusedNodeIdsByFund = nodeRepository.findUnusedNodeIdsByFund(fund);
        if (!unusedNodeIdsByFund.isEmpty()) {
            List<Integer> changesIds = new ArrayList<>();
            ObjectListIterator<Integer> iterator = new ObjectListIterator<>(unusedNodeIdsByFund);
            while (iterator.hasNext()) {
                List<Integer> next = iterator.next();
                changesIds.addAll(changeRepository.findChangesIdsByPrimaryNodeIds(next));
            }
            dropNodeInfo(unusedNodeIdsByFund);

            iterateAction(changesIds, (ids) -> levelRepository.updateCreateChangeByChangeIds(ids, change));
            changeRepository.deleteByPrimaryNodeIdsWithIgnoredId(unusedNodeIdsByFund, change.getChangeId());

            dataUriRefRepository.updateByNodesIdIn(unusedNodeIdsByFund);

            nodeRepository.deleteByNodeIdIn(unusedNodeIdsByFund);
        }
        em.flush();

        // vyčištění nepoužitých arr_change
        Query deleteNotUseChangesQuery = revertingChangesService.createDeleteNotUseChangesQuery(change.getChangeId());
        deleteNotUseChangesQuery.executeUpdate();
        em.flush();

        // verze AS budou vymazány všechny a založí se nová verze kopii poslední, aktuální
        RulRuleSet ruleSet = fundVersion.getRuleSet();

        ruleService.deleteByNodeFund(fund);

        faBulkActionNodeRepository.deleteByFund(fund);
        faBulkActionRepository.deleteByFund(fund);

        fundVersionRepository.deleteByFund(fund);

        em.flush();

        // create new version
        fundVersion = arrangementService.createVersion(change, fund, ruleSet, rootNode);

        updateChanges(fund, change);

        // vynutit uložení změn do DB
        em.flush();

        // odeslání informace o změně verze na klienta
        eventNotificationService.publishEvent(new EventFund(EventType.APPROVE_VERSION, fundVersion.getFund().getFundId(), fundVersion.getFundVersionId()));

        logger.info("Fund history deleted: {}", fundId);
    }

    /**
     * Aktualizuje change (createChange) u položek:
     * - {@link ArrItem}
     * - {@link ArrLevel}
     * - {@link ArrNodeExtension}
     * - {@link ArrNodeOutput}
     * - {@link ArrOutputResult}
     * - {@link ArrDaoLink}
     * - {@link ArrRequest}
     * - {@link ArrRequestQueueItem}
     * - {@link ArrFundStructureExtension}
     * - {@link ArrStructuredObject}
     * - {@link ArrLockedValue}
     * - {@link ArrOutput}
     *
     * @param fund   archivní soubor, u kterého upravujeme všechny change
     * @param newChange nastavovaná change
     */
    private void updateChanges(final ArrFund fund, final ArrChange newChange) {
        // seznam change, který nahrazujeme
        Set<Integer> changes = new HashSet<>();

        processUpdateChanges(changes, newChange, fund, itemRepository);
        processUpdateChanges(changes, newChange, fund, levelRepository);
        processUpdateChanges(changes, newChange, fund, nodeExtensionRepository);
        processUpdateChanges(changes, newChange, fund, nodeOutputRepository);
        processUpdateChanges(changes, newChange, fund, outputResultRepository);
        processUpdateChanges(changes, newChange, fund, daoLinkRepository);
        processUpdateChanges(changes, newChange, fund, requestRepository);
        processUpdateChanges(changes, newChange, fund, requestQueueItemRepository);
        processUpdateChanges(changes, newChange, fund, fundStructureExtensionRepository);
        processUpdateChanges(changes, newChange, fund, structuredObjectRepository);
        processUpdateChanges(changes, newChange, fund, lockedValueRepository);
        processUpdateChanges(changes, newChange, fund, outputRepository);

        // smazání všech nepotřebaných change
        iterateAction(changes, changeRepository::deleteAllByIds);
    }

    private void processUpdateChanges(final Set<Integer> changes,
                                      final ArrChange newChange,
                                      final ArrFund fund,
                                      final DeleteFundHistory repository) {
        List<ItemChange> items = repository.findByFund(fund);
        Set<Integer> ids = new HashSet<>(items.size());
        for (ItemChange item : items) {
            if (!item.getChangeId().equals(newChange.getChangeId())) {
                changes.add(item.getChangeId());
                ids.add(item.getId());
            }
        }
        iterateAction(ids, partIds -> repository.updateCreateChange(partIds, newChange));
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
        ruleService.deleteByNodeIdIn(nodeIds);

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
