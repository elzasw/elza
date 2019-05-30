package cz.tacr.elza.service.arrangement;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.WfIssueList;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.BulkActionNodeRepository;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.CachedNodeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DaoFileGroupRepository;
import cz.tacr.elza.repository.DaoFileRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoLinkRequestRepository;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DaoRequestDaoRepository;
import cz.tacr.elza.repository.DaoRequestRepository;
import cz.tacr.elza.repository.DataFileRefRepository;
import cz.tacr.elza.repository.DataStructureRefRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DigitizationRequestNodeRepository;
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundStructureExtensionRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemSettingsRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.LockedValueRepository;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeExtensionRepository;
import cz.tacr.elza.repository.NodeOutputRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.repository.OutputFileRepository;
import cz.tacr.elza.repository.OutputItemRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.RequestQueueItemRepository;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.repository.WfCommentRepository;
import cz.tacr.elza.repository.WfIssueListRepository;
import cz.tacr.elza.repository.WfIssueRepository;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.PolicyService;
import cz.tacr.elza.service.RevertingChangesService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Action to delete fund
 *
 * Fund deletion is complex task
 * which is handled by this action.
 */
@Component
@Scope("prototype")
public class DeleteFundAction {

    private static final Logger logger = LoggerFactory.getLogger(DeleteFundAction.class);

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
    private NodeRegisterRepository nodeRegisterRepository;
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
    private OutputDefinitionRepository outputDefinitionRepository;
    @Autowired
    private ChangeRepository changeRepository;

    @Autowired
    private LockedValueRepository lockedValueRepository;
    @Autowired
    private DescItemRepository descItemRepository;
    @Autowired
    private OutputRepository outputRepository;
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

    /**
     * Prepare fund deletion
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

        // terminate all services - for all versions
        List<ArrFundVersion> versions = this.fundVersionRepository.findVersionsByFundIdOrderByCreateDateDesc(fundId);
        for (ArrFundVersion version : versions) {
            updateConformityInfoService.terminateWorkerInVersionAndWait(version.getFundVersionId());

            bulkActionService.terminateBulkActions(version.getFundVersionId());
        }
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
        nodeRepository.deleteByFund(fund);

        // TODO: delete files from DMS - prepare list and do drop at the end of
        // transaction
        dmsService.deleteFilesByFund(fund);

        em.flush();

        //?
        faRegisterRepository.findByFund(fund).forEach(faScope -> faRegisterRepository.delete(faScope));

        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.FUND_DELETE, fundId));

        fundRepository.delete(fundId);

        // TODO: rewrite to better solution
        Query deleteNotUseChangesQuery = revertingChangesService.createDeleteNotUseChangesQuery();
        deleteNotUseChangesQuery.executeUpdate();

        logger.info("Fund deleted: {}", fundId);

    }


    private void dropStructObjs() {

        structureItemRepository.deleteByStructuredObjectFund(fund);
        /*
        List<ArrStructuredObject> objList = structureDataRepository.findByFund(fund);
        objList.forEach(obj -> {
            structureItemRepository.deleteByStructuredObject(obj);
            dataRepository.deleteByStructuredObject(obj);
        });
        structureDataRepository.deleteInBatch(objList);
        */
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
        nodeConformityErrorRepository.deleteByNodeConformityNodeFund(fund);
        nodeConformityMissingRepository.deleteByNodeConformityNodeFund(fund);
        nodeConformityInfoRepository.deleteByNodeFund(fund);

        // delete attached extensions
        nodeExtensionRepository.deleteByNodeFund(fund);

        // delete attached access points
        nodeRegisterRepository.deleteByNodeFund(fund);

        em.flush();
    }

    private void dropDescItems() {
        // drop locked values
        lockedValueRepository.deleteByFund(fund);

        // TODO: drop arr_data and all subtypes

        // drop items
        this.descItemRepository.deleteByNodeFund(fund);

        // drop links from data structured_ref 
        dataStructureRefRepository.deleteByStructuredObjectFund(fund);
        // drop links from data_file_ref
        dataFileRefRepository.deleteByFileFund(fund);

        em.flush();
    }

    private void dropOutputs() {
        // drop outputs
        // TODO: select changeIds and dataIds
        outputRepository.deleteByOutputDefinitionFund(fund);
        outputFileRepository.deleteByOutputResultOutputDefinitionFund(fund);
        outputResultRepository.deleteByOutputDefinitionFund(fund);
        itemSettingsRepository.deleteByOutputDefinitionFund(fund);
        outputItemRepository.deleteByOutputDefinitionFund(fund);
        nodeOutputRepository.deleteByOutputDefinitionFund(fund);
        outputDefinitionRepository.deleteByFund(fund);

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
     * 
     * @param fund
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
     * Smaz�n� protokol�, p�ipom�nek, koment��� a opr�v�n� u�ivatel� pro p��stup k protokol�m
     */
    private void dropIssues() {

        List<WfIssueList> issueLists = issueListRepository.findByFundId(fundId);

        for (WfIssueList issueList : issueLists) {
            permissionRepository.deleteByIssueList(issueList);
        }

        commentRepository.deleteByFundId(fundId);
        issueRepository.deleteByFundId(fundId);
        // issueListRepository.deleteByFundId(fundId);
        issueListRepository.delete(issueLists);

        em.flush();
    }

}
