package cz.tacr.elza.service.arrangement;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
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
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataStructureRefRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DigitizationRequestNodeRepository;
import cz.tacr.elza.repository.DigitizationRequestRepository;
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundStructureExtensionRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemSettingsRepository;
import cz.tacr.elza.repository.LevelRepository;
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
import cz.tacr.elza.repository.RequestQueueItemRepository;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.ArrangementCacheService;
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
    private ArrangementCacheService arrangementCacheService;
    @Autowired
    private DataRepository dataRepository;
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
    private DigitizationRequestRepository digitizationRequestRepository;

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

    private ArrLevel rootLevel;

    private ArrNode rootNode;

    @Autowired
    private EntityManager em;

    @Autowired
    private NodeExtensionRepository nodeExtensionRepository;

    @Autowired
    private DataStructureRefRepository dataStructureRefRepository;

    @Autowired
    private FundStructureExtensionRepository fundStructureExtensionRepository;

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

        this.rootLevel = levelRepository.findByNodeAndDeleteChangeIsNull(rootNode);

        // terminate all services - for all versions
        List<ArrFundVersion> versions = this.fundVersionRepository.findVersionsByFundIdOrderByCreateDateDesc(fundId);
        for (ArrFundVersion version : versions) {
            updateConformityInfoService.terminateWorkerInVersionAndWait(version.getFundVersionId());

            bulkActionService.terminateBulkActions(version.getFundVersionId());
        }
    }



    private void deleteDescItemForce(final ArrDescItem descItem, final Set<ArrData> dataToDelete) {
        Assert.notNull(descItem, "Hodnota atributu musí být vyplněna");

        ArrData data = descItem.getData();

        descItemRepository.delete(descItem);

        if (data != null) {
            dataToDelete.add(data);
        }
    }

    public void run(Integer fundId) {

        logger.info("Deleting fund: {}", fundId);

        this.fundId = fundId;

        prepare();

        dropDaos();
        dropBulkActions();
        dropOutputs();
        dropNodeInfo();
        dropDescItems();
        dropStructObjs();

        // TODO: delete all change ids
        changeRepository.deleteByPrimaryNodeFund(fund);

        levelRepository.deleteByNodeFund(fund);
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
        userService.deleteByFund(fund);

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
        // TODO: drop arr_data and all subtypes

        // drop links from data structured_ref 
        dataStructureRefRepository.deleteByStructuredObjectFund(fund);

        // drop items
        this.descItemRepository.deleteByNodeFund(fund);

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
        faBulkActionNodeRepository.deleteByNodeFund(fund);
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
        digitizationRequestRepository.deleteByFund(fund);

        daoLinkRepository.deleteByNodeFund(fund);
        daoLinkRequestRepository.deleteByFund(fund);

        daoRequestDaoRepository.deleteByFund(fund);
        daoRequestRepository.deleteByFund(fund);

        daoFileRepository.deleteByFund(fund);
        daoFileGroupRepository.deleteByFund(fund);
        daoRepository.deleteByFund(fund);
        daoPackageRepository.deleteByFund(fund);

        em.flush();
    }

}