package cz.tacr.elza.service.arrangement;

import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.*;
import cz.tacr.elza.service.eventnotification.EventFactory;
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
import java.util.List;

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
    private UpdateConformityInfoService updateConformityInfoService;
    @Autowired
    private BulkActionService bulkActionService;
    @Autowired
    private PolicyService policyService;
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
    private FundRegisterScopeRepository faRegisterRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;
    @Autowired
    private FundRepository fundRepository;
    @Autowired
    private DataUriRefRepository dataUriRefRepository;

    @Autowired
    private StructuredObjectRepository structureDataRepository;
    @Autowired
    private StructuredItemRepository structureItemRepository;
    @Autowired
    private OutputItemRepository outputItemRepository;

    @Autowired
    private DigitizationRequestNodeRepository digitizationRequestNodeRepository;
    @Autowired
    private DaoLinkRepository daoLinkRepository;

    @Autowired
    private DaoRequestRepository daoRequestRepository;

    @Autowired
    private DaoLinkRequestRepository daoLinkRequestRepository;

    @Autowired
    private DaoRequestDaoRepository daoRequestDaoRepository;

    @Autowired
    private RequestQueueItemRepository requestQueueItemRepository;

    @Autowired
    private DaoFileRepository daoFileRepository;

    @Autowired
    private DaoFileGroupRepository daoFileGroupRepository;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private DaoPackageRepository daoPackageRepository;

    //TODO: Should not be used here, method accessing this repository have to be refactorized
    @Autowired
    private CachedNodeRepository cachedNodeRepository;

    @Autowired
    private OutputRepository outputRepository;
    @Autowired
    private ChangeRepository changeRepository;

    @Autowired
    private LockedValueRepository lockedValueRepository;
    @Autowired
    private DescItemRepository descItemRepository;
    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private NodeOutputRepository nodeOutputRepository;

    @Autowired
    private DmsService dmsService;

    @Autowired
    private OutputResultRepository outputResultRepository;

    @Autowired
    private OutputFileRepository outputFileRepository;

    @Autowired
    private ItemSettingsRepository itemSettingsRepository;

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
    private DataStructureRefRepository dataStructureRefRepository;

    @Autowired
    private FundStructureExtensionRepository fundStructureExtensionRepository;

    @Autowired
    private DataFileRefRepository dataFileRefRepository;

    @Autowired
    private WfCommentRepository commentRepository;

    @Autowired
    private WfIssueListRepository issueListRepository;

    @Autowired
    private WfIssueRepository issueRepository;

    @Autowired
    private PermissionRepository permissionRepository;


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
        this.fund = fundRepository.findOne(fundId);
        if (fund == null) {
            throw new BusinessException("Fund does not exists", BaseCode.ID_NOT_EXIST).set("fundId", fundId);
        }

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
            updateConformityInfoService.terminateWorkerInVersionAndWait(version.getFundVersionId());

            bulkActionService.terminateBulkActions(version.getFundVersionId());
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
        itemRepository.delete(arrItemList);
        em.flush();

        // odmazat opuštěná data
        for (ArrData arrData : arrDataList) {
            final List<ArrItem> itemList = itemRepository.findByData(arrData);
            if (CollectionUtils.isEmpty(itemList)) {
                dataRepository.delete(arrData);
            }
        }
        em.flush();

        // delete arr_level, které mají vyplněn delete_change_id
        final List<ArrLevel> arrLevelList = levelRepository.findHistoricalByFund(fund);
        levelRepository.delete(arrLevelList);
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
        final String timeRange = fundVersion.getDateRange();
        final RulRuleSet ruleSet = fundVersion.getRuleSet();
        final ArrChange createChange = fundVersion.getCreateChange();

        nodeConformityMissingRepository.deleteByNodeConformityNodeFund(fund);
        nodeConformityErrorRepository.deleteByNodeConformityNodeFund(fund);
        nodeConformityInfoRepository.deleteByNodeFund(fund);
        nodeConformityRepository.deleteByNodeFund(fund);

        faBulkActionNodeRepository.deleteByFund(fund);
        faBulkActionRepository.deleteByFund(fund);

        fundVersionRepository.deleteByFund(fund);

        // create new version
        fundVersion = arrangementService.createVersion(createChange, fund, ruleSet, rootNode, timeRange);

        // vynutit uložení změn do DB
        em.flush();

        // odeslání informace o změně verze na klienta
        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.APPROVE_VERSION, fundId));


        logger.info("Fund history deleted: {}", fundId);

    }

    /**
     * Drop all information connected with node
     */
    private void dropNodeInfo(List<Integer> nodeIds) {
        // delete policies
        visiblePolicyRepository.deleteByNodeIdIn(nodeIds);

        userService.deletePermissionByNodeIds(nodeIds);

        // delete node from cache
        cachedNodeRepository.deleteByNodeIdIn(nodeIds);

        // delete node conformity
        nodeConformityErrorRepository.deleteByNodeConformityNodeIdIn(nodeIds);
        nodeConformityMissingRepository.deleteByNodeConformityNodeIdIn(nodeIds);
        nodeConformityInfoRepository.deleteByNodeIdIn(nodeIds);

        // delete attached extensions
        nodeExtensionRepository.deleteByNodeIdIn(nodeIds);

        // ostatní položky navázané na mazané node
        daoLinkRepository.deleteByNodeIdIn(nodeIds);
        digitizationRequestNodeRepository.deleteByNodeFundIdIn(nodeIds);

        dropBulkActions(nodeIds);
        dropOutputs(nodeIds);
        dropDescItems(nodeIds);

        em.flush();
    }

    private void dropDescItems(List<Integer> nodeIds) {
        this.descItemRepository.deleteByNodeIdIn(nodeIds);
        em.flush();
    }

    private void dropOutputs(List<Integer> nodeIds) {
        nodeOutputRepository.deleteByNodeIdIn(nodeIds);
        em.flush();
    }

    private void dropBulkActions(List<Integer> nodeIds) {
        faBulkActionNodeRepository.deleteByNodeIdIn(nodeIds);
        em.flush();
    }

}
