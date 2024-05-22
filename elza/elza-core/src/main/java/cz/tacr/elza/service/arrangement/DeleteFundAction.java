package cz.tacr.elza.service.arrangement;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.*;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.List;
import java.util.Set;

import static cz.tacr.elza.repository.ExceptionThrow.fund;

/**
 * Action to delete fund
 * <p>
 * Fund deletion is complex task
 * which is handled by this action.
 */
@Component
@Scope("prototype")
public class DeleteFundAction {

    private static final Logger logger = LoggerFactory.getLogger(DeleteFundAction.class);

    @Autowired
    private AsyncRequestService asyncRequestService;
    @Autowired
    private PolicyService policyService;
    @Autowired
    private UserService userService;
    @Autowired
    private IEventNotificationService eventNotificationService;
    @Autowired
    private BulkActionRunRepository faBulkActionRepository;
    @Autowired
    private BulkActionNodeRepository faBulkActionNodeRepository;
    @Autowired
    private FundRegisterScopeRepository faRegisterRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private FundVersionRepository fundVersionRepository;
    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private StructuredObjectRepository structureDataRepository;
    @Autowired
    private StructuredItemRepository structureItemRepository;
    @Autowired
    private StructObjValueService structObjValueService;

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
    private OutputTemplateRepository outputTemplateRepository;
    @Autowired
    private ChangeRepository changeRepository;

    @Autowired
    private LockedValueRepository lockedValueRepository;
    @Autowired
    private InhibitedItemRepository inhibitedItemRepository;
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
    private ArrRefTemplateRepository arrRefTemplateRepository;

    @Autowired
    private ArrRefTemplateMapTypeRepository arrRefTemplateMapTypeRepository;

    @Autowired
    private ArrRefTemplateMapSpecRepository arrRefTemplateMapSpecRepository;

    @Autowired
    private OutputResultRepository outputResultRepository;

    @Autowired
    private OutputFileRepository outputFileRepository;

    @Autowired
    private ItemSettingsRepository itemSettingsRepository;

    @Autowired
    private RevertingChangesService revertingChangesService;

    @Autowired
    private DataUriRefRepository dataUriRefRepository;

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
    private NodeCacheService nodeCacheService;

    /**
     * Prepare fund deletion
     */
    private void prepare() {

        // check if exists
        this.fund = fundRepository.findById(fundId).orElseThrow(fund(fundId));

        // only superuser can delete the fund if fund.managed is true
        if (fund.getManaged() && !userService.hasPermission(UsrPermission.Permission.ADMIN)) {
            throw new BusinessException("Only Superuser (admin) can delete the fund", BaseCode.INSUFFICIENT_PERMISSIONS).set("fundId", fundId);
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

        // terminate all services - for all versions
        List<ArrFundVersion> versions = this.fundVersionRepository.findVersionsByFundIdOrderByCreateDateDesc(fundId);
        for (ArrFundVersion version : versions) {
            asyncRequestService.terminateNodeWorkersByFund(version.getFundVersionId());
            asyncRequestService.terminateBulkActions(version.getFundVersionId());
        }

        structObjValueService.deleteFundRequests(fundId);
    }

    public void run(Integer fundId) {

        logger.info("Deleting fund: {}", fundId);

        this.fundId = fundId;

        prepare();

        dropIssues();
        dropDaos();
        dropBulkActions();
        dropOutputs();
        dropNodeInfo();
        dropDescItems();
        dropStructObjs();

        // Delete levels connected by nodes to the fund
        levelRepository.deleteByNodeFund(fund);

        // TODO: delete all change ids
        changeRepository.deleteByPrimaryNodeFund(fund);

        // delete all versions
        fundVersionRepository.deleteByFund(fund);

        // Remove from URI-REF
        updateUriRefs();

        nodeRepository.deleteByFund(fund);

        // TODO: delete files from DMS - prepare list and do drop at the end of
        // transaction
        dmsService.deleteFilesByFund(fund);

        // Odstranění šablony pro mapování prvků popisu:
        // arr_ref_template_map_spec & arr_ref_template_map_type & arr_ref_template 
        arrRefTemplateMapSpecRepository.deleteByFund(fund);
        arrRefTemplateMapTypeRepository.deleteByFund(fund);
        arrRefTemplateRepository.deleteByFund(fund);

        em.flush();

        //?
        faRegisterRepository.findByFund(fund).forEach(faScope -> faRegisterRepository.delete(faScope));

        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.FUND_DELETE, fundId));

        fundRepository.deleteById(fundId);

        // TODO: rewrite to better solution
        Query deleteNotUseChangesQuery = revertingChangesService.createDeleteNotUseChangesQuery();
        deleteNotUseChangesQuery.executeUpdate();

        logger.info("Fund deleted: {}", fundId);

    }

    private void updateUriRefs() {
        Set<Integer> nodeIds = nodeRepository.findNodeIdsByFund(fund);
        if (!nodeIds.isEmpty()) {
            // dohledání JP, ze kterých se odkazovalo na JP z mazaného AS
            Set<Integer> referralNodeIds = ObjectListIterator.findIterableSet(nodeIds, dataUriRefRepository::findReferralNodeIdsByNodesIdIn);
            ObjectListIterator.forEachPage(nodeIds, dataUriRefRepository::updateByNodesIdIn);
            nodeCacheService.removeReferralNodeIds(nodeIds, referralNodeIds);
        }
    }

    /**
     * Drop all Structured Objects by Fund
     */
    private void dropStructObjs() {

        structureItemRepository.deleteByStructuredObjectFund(fund);
        structureDataRepository.deleteByFund(fund);

        fundStructureExtensionRepository.deleteByFund(fund);
        em.flush();
    }

    /**
     * Drop all information connected with node
     */
    private void dropNodeInfo() {
        policyService.deleteFundVisiblePolicies(fund);
        userService.deletePermissionsByFund(fund);

        // delete node from cache
        cachedNodeRepository.deleteByFund(fund);

        // delete node conformity
        ruleService.deleteByNodeFund(fund);

        // delete attached extensions
        nodeExtensionRepository.deleteByNodeFund(fund);

        em.flush();
    }

    private void dropDescItems() {
        // drop locked values
        int numDeleted = lockedValueRepository.deleteByFund(fund);
        if (numDeleted > 0) {
            logger.debug("Deleted locked values fundId: {}, count: {}", fund.getFundId(), numDeleted);
            lockedValueRepository.flush();
        }

        // TODO: drop arr_data and all subtypes

        // drop inhibited items
        inhibitedItemRepository.deleteByNodeFund(fund);

        // drop items
        descItemRepository.deleteByNodeFund(fund);

        // drop links from data structured_ref
        dataStructureRefRepository.deleteByStructuredObjectFund(fund);
        // drop links from data_file_ref
        dataFileRefRepository.deleteByFileFund(fund);

        em.flush();
    }

    private void dropOutputs() {
        // drop outputs
        // TODO: select changeIds and dataIds
        outputFileRepository.deleteByOutputResultOutputFund(fund);
        outputResultRepository.deleteByOutputFund(fund);
        itemSettingsRepository.deleteByOutputFund(fund);
        outputItemRepository.deleteByOutputFund(fund);
        nodeOutputRepository.deleteByOutputFund(fund);
        outputTemplateRepository.deleteByFund(fund);
        outputRepository.deleteByFund(fund);

        em.flush();
    }

    private void dropBulkActions() {
        // drop bulk actions

        // TODO: Rewrite as criteria query
        faBulkActionNodeRepository.deleteByNodeFund(fund);
        // TODO: Rewrite as criteria query
        faBulkActionRepository.deleteByFundVersionFund(fund);

        em.flush();
    }

    /**
     * Delete DAOs
     */
    private void dropDaos() {

        requestQueueItemRepository.deleteByFund(fund);

        // dao objects
        digitizationRequestNodeRepository.deleteByFund(fund);
        //
        //em.createNativeQuery("delete from ");
        CriteriaBuilder cmBuilder = em.getCriteriaBuilder();
        CriteriaDelete<ArrDigitizationRequest> deleteDigitRequests = cmBuilder.createCriteriaDelete(
                ArrDigitizationRequest.class);
        // subquery to select request
        Subquery<Integer> deleteDigitReqsSubquery = deleteDigitRequests.subquery(Integer.class);
        Root<ArrRequest> fromDigitReqsSubquery = deleteDigitReqsSubquery.from(ArrRequest.class);
        deleteDigitReqsSubquery.select(fromDigitReqsSubquery.get(ArrRequest.FIELD_REQUEST_ID));
        deleteDigitReqsSubquery.where(cmBuilder.equal(fromDigitReqsSubquery.get(ArrRequest.FIELD_FUND), fund));

        Root<ArrDigitizationRequest> fromDigitRequests = deleteDigitRequests.from(ArrDigitizationRequest.class);
        deleteDigitRequests.where(cmBuilder.in(fromDigitRequests.get(ArrRequest.FIELD_REQUEST_ID)).value(
                deleteDigitReqsSubquery));
        em.createQuery(deleteDigitRequests).executeUpdate();

        // TOOD: rewrite as criteria query
        daoLinkRepository.deleteByNodeFund(fund);
        // TOOD: rewrite as criteria query
        daoLinkRequestRepository.deleteByFund(fund);

        // Query is OK
        daoRequestDaoRepository.deleteByFund(fund);
        // TOOD: rewrite as criteria query
        daoRequestRepository.deleteByFund(fund);

        // Query is OK
        daoFileRepository.deleteByFund(fund);
        // Query is OK
        daoFileGroupRepository.deleteByFund(fund);
        // Query is OK
        daoRepository.deleteByFund(fund);
        // TOOD: rewrite as criteria query
        daoPackageRepository.deleteByFund(fund);

        em.flush();
    }

    /**
     * Smazání protokolů připomínek, komentáři a oprávnění uživatelů pro přístup k protokolům
     */
    private void dropIssues() {

        List<WfIssueList> issueLists = issueListRepository.findByFundId(fundId);

        for (WfIssueList issueList : issueLists) {
            permissionRepository.deleteByIssueList(issueList);
        }

        commentRepository.deleteByFundId(fundId);
        issueRepository.deleteByFundId(fundId);
        // issueListRepository.deleteByFundId(fundId);
        issueListRepository.deleteAll(issueLists);

        em.flush();
    }

}
