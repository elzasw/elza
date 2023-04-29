package cz.tacr.elza.service;

import static cz.tacr.elza.domain.RulRuleSet.RuleType.ENTITY;
import static cz.tacr.elza.repository.ExceptionThrow.itemType;
import static cz.tacr.elza.repository.ExceptionThrow.node;
import static cz.tacr.elza.repository.ExceptionThrow.version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.controller.vo.ApAccessPointCreateVO;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ApValidationErrorsVO;
import cz.tacr.elza.controller.vo.PartValidationErrorsVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemAccessPointRefVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemBitVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemCoordinatesVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemEnumVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemIntVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemStringVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemTextVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemUnitdateVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemUriRefVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.rules.ItemTypeExtBuilder;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevItem;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrItemSettings;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformity.State;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.domain.ArrNodeExtension;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulExportFilter;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeAction;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.projection.NodeIdFundVersionIdInfo;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;
import cz.tacr.elza.drools.AvailableItemsRules;
import cz.tacr.elza.drools.DrlType;
import cz.tacr.elza.drools.ModelValidationRules;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.drools.model.Ap;
import cz.tacr.elza.drools.model.ApBuilder;
import cz.tacr.elza.drools.model.ApValidationErrors;
import cz.tacr.elza.drools.model.GeoModel;
import cz.tacr.elza.drools.model.Index;
import cz.tacr.elza.drools.model.ItemSpec;
import cz.tacr.elza.drools.model.ItemType;
import cz.tacr.elza.drools.model.ModelAvailable;
import cz.tacr.elza.drools.model.ModelPart;
import cz.tacr.elza.drools.model.ModelValidation;
import cz.tacr.elza.drools.model.Part;
import cz.tacr.elza.drools.model.PartType;
import cz.tacr.elza.drools.model.Relation;
import cz.tacr.elza.drools.model.RequiredType;
import cz.tacr.elza.drools.model.item.AbstractItem;
import cz.tacr.elza.drools.model.item.BoolItem;
import cz.tacr.elza.drools.model.item.IntItem;
import cz.tacr.elza.drools.model.item.Item;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ArrangementExtensionRepository;
import cz.tacr.elza.repository.ExceptionThrow;
import cz.tacr.elza.repository.ExportFilterRepository;
import cz.tacr.elza.repository.ExtensionRuleRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemSettingsRepository;
import cz.tacr.elza.repository.ItemTypeActionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeExtensionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.TemplateRepository;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.eventnotification.events.EventNodeIdVersionInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.validation.ArrDescItemsPostValidator;


/**
 * Servisní třída pro pravidla.
 *
 */
@Service
public class RuleService {

    private static final Logger logger = LoggerFactory.getLogger(RuleService.class);

    @Autowired
    private EntityManager entityManager;
    @Autowired
	private StaticDataService staticDataService;

    @Autowired
    private ArrangementInternalService arrangementInternalService;
    @Autowired
    private RulesExecutor rulesExecutor;
    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private FundVersionRepository fundVersionRepository;
    @Autowired
    private NodeConformityRepository nodeConformityRepository;
    @Autowired
    private NodeConformityErrorRepository nodeConformityErrorRepository;
    @Autowired
    private NodeConformityMissingRepository nodeConformityMissingRepository;
    @Autowired
    private ItemTypeRepository itemTypeRepository;
    @Autowired
    private ArrDescItemsPostValidator descItemsPostValidator;

    @Autowired
    private ItemTypeActionRepository itemTypeActionRepository;

    @Autowired
    private ItemSettingsRepository itemSettingsRepository;
    @Autowired
    private OutputTypeRepository outputTypeRepository;

    @Autowired
    private NodeExtensionRepository nodeExtensionRepository;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private ArrangementCacheService arrangementCacheService;

    @Autowired
    private AsyncRequestService asyncRequestService;

    @Autowired
    private ArrangementExtensionRepository arrangementExtensionRepository;

    @Autowired
    private ExtensionRuleRepository extensionRuleRepository;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private PartService partService;

    @Autowired
    private RevisionItemService revisionItemService;

    @Autowired
    private RevisionPartService revisionPartService;

    @Autowired
    private RevisionService revisionService;

    @Autowired
    private AccessPointItemService accessPointItemService;

    @Autowired
    private AvailableItemsRules availableItemsRules;

    @Autowired
    private ModelValidationRules modelValidationRules;

    @Autowired
    private ApIndexRepository indexRepository;

    @Autowired
    private ApStateRepository stateRepository;

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private AccessPointCacheService accessPointCacheService;

    @Autowired
    private ExportFilterRepository exportFilterRepository;

    private static final String IDN_VALUE = "IDN_VALUE";
    private static final String IDN_TYPE = "IDN_TYPE";
    // why is it here?
    private static final String REL_ENTITY = "REL_ENTITY";
    private static final String ISO3166_2 = "ISO3166_2";
    private static final String ISO3166_3 = "ISO3166_3";
    private static final String GEO_UNIT = "GEO_UNIT";
    private static final String GEO_ADMIN_CLASS = "GEO_ADMIN_CLASS";
    private static final String GEO_TYPE = "GEO_TYPE";

    /**
     * 
     * @param faLevelId
     * @param fundVersionId
     * @param asyncRequestId
     * @return Return null if result is empty (e.g. deletedNode)
     */
    public ArrNodeConformityExt setConformityInfo(final Integer faLevelId, final Integer fundVersionId, final Long asyncRequestId) {
        return setConformityInfo(faLevelId, fundVersionId);
    }

    /**
     * Provede validaci atributů vybraného uzlu a nastaví jejich validační hodnoty.
     *
     * @param faLevelId
     *            id uzlu
     * @param fundVersionId
     *            id verze
     * @return stav validovaného uzlu
     *         Return null if result is empty (e.g. deletedNode)
     */
    public ArrNodeConformityExt setConformityInfo(final Integer faLevelId, final Integer fundVersionId) {
        Validate.notNull(faLevelId, "Musí být vyplněn identifikátor levelu");
        Validate.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");

        ArrLevel level = levelRepository.findById(faLevelId)
                .orElseThrow(ExceptionThrow.level(faLevelId));
        Integer nodeId = level.getNodeId();

        ArrNode nodeBeforeValidation = nodeRepository.getOneCheckExist(nodeId);
        Integer nodeVersionBeforeValidation = nodeBeforeValidation.getVersion();

        ArrFundVersion version = fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));

        // check if level is deleted in the context of fundVersion
        arrangementInternalService.checkLevelInVersion(level, version);
        if (arrangementInternalService.isLevelDeletedInVersion(level, version)) {
            // only delete conformity info and return
            ArrNodeConformity conformityInfo = nodeConformityRepository
                    .findByNodeAndFundVersion(level.getNode(), version);
            if (conformityInfo != null) {
                deleteConformityInfo(Arrays.asList(conformityInfo));
            }
            return null;
        }

        List<DataValidationResult> validationResults = rulesExecutor.executeDescItemValidationRules(level, version);
        List<DataValidationResult> validationResultsBasic = descItemsPostValidator.postValidateNodeDescItems(level, version);

        Iterator<DataValidationResult> iterator = validationResultsBasic.iterator();
        while(iterator.hasNext()) {
            DataValidationResult validationResult = iterator.next();
            if (validationResult.getPolicyTypeCode() == null) {
                throw new SystemException("Validation result without policy type code", BaseCode.INVALID_STATE)
                        .set("message", validationResult.getMessage());
            }
        }

        validationResults.addAll(validationResultsBasic);
        ArrNodeConformityExt result = updateNodeConformityInfo(level, version, validationResults);

        entityManager.detach(nodeBeforeValidation);
        ArrNode nodeAfterValidation = nodeRepository.getOneCheckExist(nodeId);
        Integer nodeVersionAfterValidation = nodeAfterValidation.getVersion();

        if (!nodeVersionBeforeValidation.equals(nodeVersionAfterValidation)) {
            logger.info("Během validace došlo ke změně verze uzlu " + nodeId);
            //throw new LockVersionChangeException("Behem validace doslo ke zmene verze uzlu " + nodeId);
        }

        return result;
    }

    /**
     * Načtení seznamu všech šablon, seřazeného podle názvu.
     * @return seznam šablon
     */
    public List<RulTemplate> getTemplates(final String outputTypeCode) {
        if (outputTypeCode == null) {
            return templateRepository.findAll(Sort.by(Sort.Direction.ASC, RulTemplate.FIELD_NAME));
        }
        RulOutputType outputType = outputTypeRepository.findByCode(outputTypeCode);
        Validate.notNull(outputType, "Typ outputu s kodem '" + outputTypeCode + "' nebyl nalezen");

        return templateRepository.findNotDeletedByOutputType(outputType, Sort.by(Sort.Direction.ASC, RulTemplate.FIELD_NAME));
    }


    /**
     * Provede uložení stavu pro daný uzel podle výsledku validace.
     *
     * @param level             validaovaný uzel
     * @param version           verze, do které spadá uzel
     * @param validationResults seznam validačních chyb
     */
    // Only one thread can update data in the nodeConformity tables
    private ArrNodeConformityExt updateNodeConformityInfo(final ArrLevel level,
                                                          final ArrFundVersion version,
                                                          final List<DataValidationResult> validationResults) {

        ArrNodeConformity conformityInfo = nodeConformityRepository
                .findByNodeAndFundVersion(level.getNode(), version);

        List<ArrNodeConformityMissing> confPrevMissing;
        List<ArrNodeConformityError> confPrevErrors;
        List<ArrNodeConformityMissing> confNextMissing = new ArrayList<>();
        List<ArrNodeConformityError> confNextErrors = new ArrayList<>();
        if (conformityInfo != null) {
            // we can skip reading errors if prev state was ok            
            if (State.OK != conformityInfo.getState()) {
                List<ArrNodeConformity> confInfoList = Collections.singletonList(conformityInfo);
                confPrevMissing = nodeConformityMissingRepository.findByNodeConformityInfos(confInfoList);
                confPrevErrors = this.nodeConformityErrorRepository.findByNodeConformity(conformityInfo);
            } else {
                confPrevMissing = Collections.emptyList();
                confPrevErrors = Collections.emptyList();
            }
        } else {
            conformityInfo = new ArrNodeConformity();
            conformityInfo.setNode(level.getNode());
            conformityInfo.setFundVersion(version);

            confPrevMissing = Collections.emptyList();
            confPrevErrors = Collections.emptyList();
        }
        conformityInfo.setDate(new Date());
        conformityInfo.setState(validationResults.isEmpty()?
                ArrNodeConformity.State.OK:ArrNodeConformity.State.ERR);
        conformityInfo = nodeConformityRepository.save(conformityInfo);
        
        // process errors
        for (DataValidationResult validationResult : validationResults) {
            // policy type has to be set
            Validate.notNull(validationResult.getPolicyType());

            switch (validationResult.getResultType()) {
            case MISSING:
                ArrNodeConformityMissing missing = extractOrCreate(confPrevMissing, validationResult, conformityInfo);
                confNextMissing.add(missing);
                break;
            case ERROR:
                ArrNodeConformityError error = extractOrCreateConfError(confPrevErrors, validationResult,
                                                                        conformityInfo);
                confNextErrors.add(error);
                break;
            default:
                throw new IllegalStateException();
            }
        }
        // delete remaining prev
        if (CollectionUtils.isNotEmpty(confPrevMissing)) {
            this.nodeConformityMissingRepository.deleteAll(confPrevMissing);
        }
        if (CollectionUtils.isNotEmpty(confPrevErrors)) {
            this.nodeConformityErrorRepository.deleteAll(confPrevErrors);
        }

        ArrNodeConformityExt result = new ArrNodeConformityExt(conformityInfo, confNextMissing, confNextErrors);

        return result;
    }

    private ArrNodeConformityError extractOrCreateConfError(List<ArrNodeConformityError> confPrevErrors,
                                                   DataValidationResult validationResult,
                                                   ArrNodeConformity conformityInfo) {
        Integer policyTypeId = validationResult.getPolicyType()!=null?validationResult.getPolicyType().getPolicyTypeId():null;
        Integer descItemId = validationResult.getDescItem()!=null?validationResult.getDescItem().getItemId():null;
        
        for (int i = 0; i < confPrevErrors.size(); i++) {
            ArrNodeConformityError error = confPrevErrors.get(i);
            // compare existing DB object
            if (Objects.equals(policyTypeId, error.getPolicyTypeId()) &&
                    Objects.equals(descItemId, error.getDescItemId()) &&
                    Objects.equals(validationResult.getMessage(), error.getDescription())) {
                confPrevErrors.remove(i);
                return error;
            }

        }

        ArrNodeConformityError error = new ArrNodeConformityError();
        error.setNodeConformity(conformityInfo);
        error.setDescItem(validationResult.getDescItem());
        error.setDescription(validationResult.getMessage());
        error.setPolicyType(validationResult.getPolicyType());
        return nodeConformityErrorRepository.save(error);
    }

    private ArrNodeConformityMissing extractOrCreate(List<ArrNodeConformityMissing> confPrevMissing,
                                                     DataValidationResult validationResult,
                                                     ArrNodeConformity conformityInfo) {
        Integer policyTypeId = validationResult.getPolicyType()!=null?validationResult.getPolicyType().getPolicyTypeId():null;
        Integer itemTypeId = validationResult.getType() != null ? validationResult.getType().getItemTypeId() : null;
        Integer itemSpecId = validationResult.getSpec() != null ? validationResult.getSpec().getItemSpecId() : null;
        
        for(int i=0; i<confPrevMissing.size(); i++) {
            ArrNodeConformityMissing missing = confPrevMissing.get(i);
            // compare existing DB object
            if (Objects.equals(policyTypeId, missing.getPolicyTypeId()) &&
                    Objects.equals(itemTypeId, missing.getItemTypeId()) &&
                    Objects.equals(itemSpecId, missing.getItemSpecId()) &&
                    Objects.equals(validationResult.getMessage(), missing.getDescription())) {
                confPrevMissing.remove(i);
                return missing;
            }

        }
        
        ArrNodeConformityMissing missing = new ArrNodeConformityMissing();
        missing.setNodeConformity(conformityInfo);
        missing.setItemType(validationResult.getType());
        missing.setItemSpec(validationResult.getSpec());
        missing.setDescription(validationResult.getMessage());
        missing.setPolicyType(validationResult.getPolicyType());
        return nodeConformityMissingRepository.save(missing);
    }

    /**
     * Získání stavu JP pro seznam JP.
     *
     * @param nodeIds   seznam id nodů, od kterých se získat stav
     * @param version   verze archivní pomůcky
     * @return  stavy JP
     */
    public Map<Integer, ArrNodeConformityExt> getNodeConformityInfoForNodes(final Collection<Integer> nodeIds,
                                                                           final ArrFundVersion version) {
        Map<Integer, ArrNodeConformityExt> result = new HashMap<>();

        if (nodeIds.size() == 0) {
            return result;
        }

        ObjectListIterator<Integer> iteratorNodeIds = new ObjectListIterator<>(nodeIds);

        while (iteratorNodeIds.hasNext()) {
            List<Integer> partNodeIds = iteratorNodeIds.next();

            List<ArrNodeConformity> conformityInfos = nodeConformityRepository
                    .findByNodeIdsAndFundVersion(partNodeIds, version);

            ArrayList<Integer> conformityInfoIds = conformityInfos.stream().map(ArrNodeConformity::getNodeConformityId)
                    .collect(Collectors.toCollection(ArrayList::new));

            ObjectListIterator<Integer> iteratorConformityIds = new ObjectListIterator<>(conformityInfoIds);

            Map<Integer, List<ArrNodeConformityMissing>> missings = new HashMap<>();
            Map<Integer, List<ArrNodeConformityError>> errors = new HashMap<>();

            while (iteratorConformityIds.hasNext()) {
                List<Integer> partIds = iteratorConformityIds.next();

                List<ArrNodeConformityMissing> partMissings = nodeConformityMissingRepository
                        .findByConformityIds(partIds);

                for (ArrNodeConformityMissing partMissing : partMissings) {
                    Integer conformityId = partMissing.getNodeConformity().getNodeConformityId();
                    List<ArrNodeConformityMissing> missingList = missings.get(conformityId);

                    if (missingList == null) {
                        missingList = new ArrayList<>();
                        missings.put(conformityId, missingList);
                    }

                    missingList.add(partMissing);
                }

                List<ArrNodeConformityError> partErrors = nodeConformityErrorRepository.findByConformityIds(partIds);
                for (ArrNodeConformityError partError : partErrors) {
                    Integer conformityId = partError.getNodeConformity().getNodeConformityId();

                    List<ArrNodeConformityError> errorList = errors.get(conformityId);

                    if (errorList == null) {
                        errorList = new ArrayList<>();
                        errors.put(conformityId, errorList);
                    }

                    errorList.add(partError);
                }
            }

            for (ArrNodeConformity conformityInfo : conformityInfos) {

                List<ArrNodeConformityMissing> missList = missings.get(conformityInfo.getNodeConformityId());
                List<ArrNodeConformityError> errList = errors.get(conformityInfo.getNodeConformityId());
                ArrNodeConformityExt conformity = new ArrNodeConformityExt(conformityInfo,
                        missList, errList);

                result.put(conformityInfo.getNode().getNodeId(), conformity);
            }

        }

        return result;
    }

    /**
     * Provede úpravů (smazání) stavů uzlů podle pravidel.
     *
     * @param fundVersionId       verze nodů
     * @param nodeIds           seznam id nodů, od kterých se má prohledávat
     * @param nodeTypeOperation typ operace
     * @param createDescItems   hodnoty atributů k vytvoření
     * @param updateDescItems   hodnoty atributů k upravení
     * @param deleteDescItems   hodnoty atributů ke smazání
     * @return seznam dopadů
     */
    // TODO: Funkce nedava smysl pokud kombinuje vice uzlu a vice item z techto uzlu
    public Set<RelatedNodeDirection> conformityInfo(final Integer fundVersionId,
                                                    final Collection<Integer> nodeIds,
                                                    final NodeTypeOperation nodeTypeOperation,
                                                    final List<ArrDescItem> createDescItems,
                                                    final List<ArrDescItem> updateDescItems,
                                                    final List<ArrDescItem> deleteDescItems) {

        return conformityInfo(fundVersionId, nodeIds, nodeTypeOperation,
                              createDescItems, updateDescItems, deleteDescItems,
                              null);
    }

    public Set<RelatedNodeDirection> conformityInfo(final Integer fundVersionId,
                                                    final Collection<Integer> nodeIds,
                                                    final NodeTypeOperation nodeTypeOperation,
                                                    final List<ArrDescItem> createDescItems,
                                                    final List<ArrDescItem> updateDescItems,
                                                    final List<ArrDescItem> deleteDescItems,
                                                    final Integer validationPriority) {

        Set<RelatedNodeDirection> impactOnConformityInfo = getImpactOnConformityInfo(fundVersionId, nodeTypeOperation,
                createDescItems, updateDescItems, deleteDescItems);

        revalidateNodes(fundVersionId, nodeIds, impactOnConformityInfo, validationPriority);

        return impactOnConformityInfo;
    }

    /**
     * Provede vytvoření stavů uzlů podle pravidel u nové verze AP.
     *
     * @param fundVersion verze AP
     */
    public void conformityInfoAll(final ArrFundVersion fundVersion) {

        ArrNode rootNode = fundVersion.getRootNode();

        List<ArrNode> nodes = nodeRepository
                .findNodesByDirection(rootNode, fundVersion, RelatedNodeDirection.ALL);

        nodes.add(rootNode);
        logger.info("Conformity Info All");
        if (!nodes.isEmpty()) {
            asyncRequestService.enqueueNodes(fundVersion.getFundVersionId(),
                                             nodes.stream().map(ArrNode::getNodeId).collect(Collectors.toList()));
        }
    }

    /**
     * Zjistí podle pravidel dopad na změnu stavů uzlů.
     *
     * @param fundVersionId       verze nodů
     * @param nodeTypeOperation typ operace
     * @param createDescItems   hodnoty atributů k vytvoření
     * @param updateDescItems   hodnoty atributů k upravení
     * @param deleteDescItems   hodnoty atributů ke smazání
     * @return seznam dopadů
     */
    private Set<RelatedNodeDirection> getImpactOnConformityInfo(final Integer fundVersionId,
                                                                final NodeTypeOperation nodeTypeOperation,
                                                                final List<ArrDescItem> createDescItems,
                                                                final List<ArrDescItem> updateDescItems,
                                                                final List<ArrDescItem> deleteDescItems) {

        ArrFundVersion version = fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));

        return rulesExecutor
                .executeImpactOfChangesLevelStateRules(createDescItems, updateDescItems, deleteDescItems,
                        nodeTypeOperation, version);
    }

    /**
     * Pro vybrané nody s danou verzí přepočte validace
     *
     * @param fundVersionId
     *            verze nodů
     * @param nodeIds
     *            seznam id nodů, od kterých se má prohledávat
     * @param directions
     *            směry prohledávání
     * @param validationPriority
     *            volitelná priorita revalidace
     */
    @Transactional
    public void revalidateNodes(final Integer fundVersionId,
                                final Collection<Integer> nodeIds,
                                @Nullable final Collection<RelatedNodeDirection> directions,
                                @Nullable final Integer validationPriority) {
        Validate.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Validate.notEmpty(nodeIds, "Musí být vyplněna alespoň jedna JP");

        List<ArrNode> nodes = nodeRepository.findAllById(nodeIds);
        Validate.isTrue(nodes.size() == nodeIds.size(), "Some node not found");

        ArrFundVersion version = fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));

        Set<ArrNode> updateNodes = new HashSet<>();

        if (CollectionUtils.isEmpty(directions)) {
            updateNodes.addAll(nodes);
        } else {
            for (RelatedNodeDirection direction : directions) {
                for (ArrNode node : nodes) {
                    updateNodes.addAll(nodeRepository.findNodesByDirection(node, version, direction));
                }
            }
        }

        if (!updateNodes.isEmpty()) {
            asyncRequestService.enqueueNodes(fundVersionId,
                                        updateNodes.stream().map(ArrNode::getNodeId).collect(Collectors.toList()),
                                        validationPriority);
        }
    }

    /**
     * Provádění validaci nody, které mají odkaz na ApAccessPoint
     * 
     * @param accessPointId
     */
    public void revalidateNodes(final Integer accessPointId) {
        List<NodeIdFundVersionIdInfo> nodeIdFundVersionIds = nodeRepository.findNodeIdFundversionIdByAccessPointId(accessPointId);
        Map<Integer, List<Integer>> nodeIdFundVersionMap = nodeIdFundVersionIds.stream()
                .collect(Collectors.groupingBy(i -> i.getFundVersionId(),
                                               Collectors.mapping(i -> i.getNodeId(), 
                                                                  Collectors.toList())));

        nodeIdFundVersionMap.forEach((fundVersionId, nodeIds) -> {
            asyncRequestService.enqueueNodes(fundVersionId, nodeIds);
        });
    }

    /**
     * Smaže všechny vybrané stavy.
     *
     * 
     * @param infos stavy ke smazání
     */
    private void deleteConformityInfo(final Collection<ArrNodeConformity> infos) {

        if (CollectionUtils.isEmpty(infos)) {
            return;
        }

        List<ArrNodeConformityMissing> missing = ObjectListIterator
                .findIterable(infos, page -> nodeConformityMissingRepository.findByNodeConformityInfos(page));

        if (CollectionUtils.isNotEmpty(missing)) {
            ObjectListIterator.forEachPage(missing,
                                           page -> nodeConformityMissingRepository.deleteAll(page));
            nodeConformityMissingRepository.flush();
        }

        List<ArrNodeConformityError> errors = ObjectListIterator
                .findIterable(infos, page -> nodeConformityErrorRepository.findByNodeConformityInfos(page));
        if (CollectionUtils.isNotEmpty(errors)) {
            ObjectListIterator.forEachPage(errors,
                                           page -> nodeConformityErrorRepository.deleteAll(page));
            nodeConformityErrorRepository.flush();
        }

        ObjectListIterator.forEachPage(infos,
                                       page -> nodeConformityRepository.deleteAll(page));

        // Vymazane stavy je nutne propagovat do DB - pred zapisem novych pozadavku
        // jinak hrozi konflikt s validacnim vlaknem
        nodeConformityRepository.flush();
    }

    /**
     * Mazání uzlů podle seznamu id.
     * 
     * Metoda akceptuje velké množství uzlů a zajistí stránkování.
     * 
     * @param unusedNodes
     */
    public void deleteByNodeIdIn(Collection<Integer> nodeIds) {
        List<ArrNodeConformity> deleteInfos = ObjectListIterator
                .findIterable(nodeIds, page -> nodeConformityRepository.findByNodeIds(page));

        deleteConformityInfo(deleteInfos);
    }

    /**
     * Mazání dle seznamu item
     * 
     * Metoda akceptuje velké množství item a zajistí stránkování.
     * 
     * @param unusedNodes
     */
    public void deleteConformityByItems(List<ArrItem> arrItemList) {
        List<ArrNodeConformity> deleteInfos = ObjectListIterator
                .findIterable(arrItemList, page -> {
                    List<Integer> itemIds = page.stream().map(ArrItem::getItemId).collect(Collectors.toList());
                    List<ArrNodeConformityError> nodeErrors = nodeConformityErrorRepository.findByItemIds(itemIds);
                    return nodeErrors.stream().map(ArrNodeConformityError::getNodeConformity)
                            .collect(Collectors.toList());
                });

        deleteConformityInfo(deleteInfos);
    }

    /**
     * Mazání uzlů podle fondu.
     * 
     * @param fund
     */
    public void deleteByNodeFund(ArrFund fund) {
        nodeConformityMissingRepository.deleteByNodeConformityNodeFund(fund);
        nodeConformityErrorRepository.deleteByNodeConformityNodeFund(fund);
        nodeConformityRepository.deleteByNodeFund(fund);
    }

    public List<ArrNodeConformityError> findErrorsByFundVersion(ArrFundVersion fundVersion) {
        return nodeConformityErrorRepository.findErrorsByFundVersion(fundVersion);
    }

    public List<ArrNodeConformityMissing> findMissingsByFundVersion(ArrFundVersion fundVersion) {
        return nodeConformityMissingRepository.findMissingsByFundVersion(fundVersion);
    }

    public List<ArrNodeConformityError> findErrorsByNodeConformity(ArrNodeConformity nodeConformity) {
        return nodeConformityErrorRepository.findByNodeConformity(nodeConformity);
    }

    public List<ArrNodeConformityMissing> findMissingsByNodeConformity(ArrNodeConformity nodeConformity) {
        return nodeConformityMissingRepository.findByNodeConformity(nodeConformity);
    }

    /**
     * Načte počet chyb verze archivní pomůcky.
     *
     * @param fundVersion verze archivní pomůcky
     * @return počet chyb
     */
    public Integer findCountByFundVersionAndStateErr(ArrFundVersion fundVersion) {
        return nodeConformityRepository.findCountByFundVersionAndState(fundVersion, State.ERR);
    }

    public List<ArrNodeConformity> findFirst20ByFundVersionAndStateOrderByNodeConformityIdAsc(ArrFundVersion fundVersion, State state) {
        return nodeConformityRepository.findFirst20ByFundVersionAndStateOrderByNodeConformityIdAsc(fundVersion, state);
    }

    public List<ArrNodeConformity> fetchErrorAndMissingConformity(List<ArrNodeConformity> conformity, ArrFundVersion fundVersion, State state) {
        return nodeConformityRepository.fetchErrorAndMissingConformity(conformity, fundVersion, state);
    }

    /**
     * Získání rozšířených typů hodnot atributů se specifikacemi.
     *
     * @param version    verze archivní pomůcky
     * @param node       uzel
     * @return seznam typů hodnot atributů se specifikacemi
     */
    public List<RulItemTypeExt> getDescriptionItemTypes(final ArrFundVersion version,
                                                        final ArrNode node) {

        ArrLevel level = levelRepository.findByNode(node, version.getLockChange());

        // TODO: Limit itemTypes to itemTypes defined in given RuleSet
		List<RulItemTypeExt> rulDescItemTypeExtList = getRulesetDescriptionItemTypes();

        return rulesExecutor.executeDescItemTypesRules(level, rulDescItemTypeExtList, version);
    }

    /**
     * Získání všech hodnot typů atributů se specifikacemi.
     *
     * @return typy hodnot atributů
     */
	@Transactional
    public List<RulItemTypeExt> getAllDescriptionItemTypes() {
		StaticDataProvider sdp = staticDataService.getData();

		ItemTypeExtBuilder builder = new ItemTypeExtBuilder();
        builder.add(sdp.getItemTypes());

		return builder.getResult();
    }

    /**
	 * Získání typů atributů se specifikacemi pro pravidla
	 *
	 * @return typy hodnot atributů
	 */
	private List<RulItemTypeExt> getRulesetDescriptionItemTypes() {
		StaticDataProvider sdp = staticDataService.getData();

		ItemTypeExtBuilder builder = new ItemTypeExtBuilder();
        builder.add(sdp.getItemTypes());

		return builder.getResult();
	}

    /**
     * Vrací typy atributu.
     *
     * @param output výstup
     * @return seznam typů
     */
    public List<RulItemTypeExt> getOutputItemTypes(final ArrOutput output) {
        RulOutputType outputType = output.getOutputType();
        List<RulItemTypeExt> rulDescItemTypeExtList = getRulesetDescriptionItemTypes();

        List<RulItemTypeAction> itemTypeActions = itemTypeActionRepository.findAll();
        Map<Integer, RulItemType> itemTypeMap = new HashMap<>();

        for (RulItemTypeAction itemTypeAction : itemTypeActions) {
            itemTypeMap.put(itemTypeAction.getItemType().getItemTypeId(), itemTypeAction.getItemType());
        }

        List<ArrItemSettings> settings = itemSettingsRepository.findByOutput(output);
        Map<Integer, Boolean> settingsMap = new HashMap<>();

        for (ArrItemSettings setting : settings) {
            settingsMap.put(setting.getItemType().getItemTypeId(), setting.getBlockActionResult());
        }

        for (RulItemTypeExt rulItemTypeExt : rulDescItemTypeExtList) {
            if (itemTypeMap.get(rulItemTypeExt.getItemTypeId()) != null) {
                rulItemTypeExt.setCalculable(true);
                rulItemTypeExt.setIndefinable(false);
                Boolean state = settingsMap.get(rulItemTypeExt.getItemTypeId());
                rulItemTypeExt.setCalculableState(state == null ? false : state);
            }
        }

        // check if rule exists
        RulComponent component = outputType.getComponent();
        if (component != null) {
            return rulesExecutor.executeOutputItemTypesRules(output, rulDescItemTypeExtList);
        } else {
            // return item types without change if rules do not exists
            return rulDescItemTypeExtList;
        }
    }

    /**
     * Vytvoření vazby mezi JP a definicí řídících pravidel.
     *
     * @param versionId     identifikátor verze AP
     * @param nodeId        identifikátor JP
     * @param nodeExtension  vazba
     * @return  vazba
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_VER_WR})
    public ArrNodeExtension createNodeExtension(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer versionId,
                                                final Integer nodeId,
                                                final ArrNodeExtension nodeExtension) {
        Assert.notNull(nodeExtension, "Přirazení musí být vyplněno");
        Assert.isNull(nodeExtension.getNodeExtensionId(), "Identifikátor přiřazení nesmí být vyplěn");

        ArrNode node = nodeRepository.findById(nodeId)
                .orElseThrow(node(nodeId));

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.ADD_NODE_EXTENSION, node);

        node.setVersion(nodeExtension.getNode().getVersion());
        saveNode(node, change);

        validateNodeExtension(nodeExtension, versionId);

        nodeExtension.setNode(node);
        nodeExtension.setCreateChange(change);
        eventNotificationService.publishEvent(new EventNodeIdVersionInVersion(EventType.FUND_EXTENSION_CHANGE, versionId, nodeExtension.getNode().getNodeId(), nodeExtension.getNode().getVersion()));

        nodeExtensionRepository.saveAndFlush(nodeExtension);
        arrangementCacheService.createNodeExtension(nodeId, nodeExtension);

        revalidateNodes(versionId, Collections.singleton(nodeId),
                        Collections.singleton(RelatedNodeDirection.DESCENDANTS),
                        null);

        return nodeExtension;
    }

    /**
     * Odstranění vazby mezi nodem a definicí řídících pravidel.
     *
     * @param versionId     identifikátor verze AP
     * @param nodeId        identifikátor JP
     * @param nodeExtension  vazba
     * @return vazba
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_VER_WR})
    public ArrNodeExtension deleteNodeExtension(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer versionId,
                                                final Integer nodeId,
                                                final ArrNodeExtension nodeExtension) {
        Assert.notNull(nodeExtension, "Rejstříkové heslo musí být vyplněno");
        Assert.notNull(nodeExtension.getNodeExtensionId(), "Identifikátor musí být vyplněn");

        ArrNodeExtension nodeExtensionDB = nodeExtensionRepository.findById(nodeExtension.getNodeExtensionId())
                .orElseThrow(node(nodeExtension.getNodeExtensionId()));

        ArrNode node = nodeRepository.findById(nodeId)
                .orElseThrow(node(nodeExtension.getNodeExtensionId()));

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.DELETE_NODE_EXTENSION, node);

        node.setVersion(nodeExtension.getNode().getVersion());
        saveNode(node, change);

        validateNodeExtension(nodeExtensionDB, versionId);

        nodeExtensionDB.setDeleteChange(change);

        eventNotificationService.publishEvent(new EventNodeIdVersionInVersion(EventType.FUND_EXTENSION_CHANGE, versionId, node.getNodeId(), node.getVersion()));

        nodeExtensionRepository.save(nodeExtensionDB);
        arrangementCacheService.deleteNodeExtension(nodeId, nodeExtensionDB.getNodeExtensionId());

        revalidateNodes(versionId, Collections.singleton(nodeId),
                        Collections.singleton(RelatedNodeDirection.DESCENDANTS),
                        null);

        return nodeExtensionDB;
    }

    /**
     * Uložení uzlu - optimistické zámky
     *
     * @param node   uzel
     * @param change
     * @return uložený uzel
     */
    private ArrNode saveNode(final ArrNode node, final ArrChange change) {
        node.setLastUpdate(change.getChangeDate().toLocalDateTime());
        nodeRepository.save(node);
        nodeRepository.flush();
        return node;
    }

    /**
     * Validuje entitu před uložením.
     *
     * @param nodeExtension entita
     * @param fundVersionId
     */
    private void validateNodeExtension(final ArrNodeExtension nodeExtension, final Integer fundVersionId) {
        if (nodeExtension.getDeleteChange() != null) {
            throw new IllegalStateException("Nelze vytvářet či modifikovat změnu," +
                    " která již byla smazána (má delete change).");
        }

        if (nodeExtension.getNode() == null) {
            throw new IllegalArgumentException("Není vyplněna JP");
        }
        if (nodeExtension.getArrangementExtension() == null) {
            throw new IllegalArgumentException("Nejsou definovány rozšířené pravidla");
        }
        List<RulArrangementExtension> arrangementExtensions = findArrangementExtensionsByFundVersionId(fundVersionId);
        if (!arrangementExtensions.contains(nodeExtension.getArrangementExtension())) {
            throw new IllegalArgumentException("Řídící pravidla nejsou pro pravidla AS");
        }
    }

    /**
     * Vyhledá možné definice rozšíření pro řídící pravidla podle verze AS.
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @return nalezené definice
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_VER_WR})
    public List<RulArrangementExtension> findArrangementExtensionsByFundVersionId(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        Assert.notNull(fundVersionId, "Identifikátor verze AS musí být vyplněn");
        ArrFundVersion version = fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));
        RulRuleSet ruleSet = version.getRuleSet();
        return arrangementExtensionRepository.findByRuleSet(ruleSet);
    }

    /**
     * Vyhledá nastavené definice rozšíření pro řídící pravidla pro JP z verze AS.
     *
     * @param fundVersionId  identifikátor verze archivní pomůcky
     * @param nodeId identifikátor JP
     * @return nalezené definice
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_VER_WR})
    public List<RulArrangementExtension> findArrangementExtensionsByNodeId(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
                                                                           final Integer nodeId) {
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");
        Assert.notNull(fundVersionId, "Identifikátor verze AS musí být vyplněn");
        ArrFundVersion version = fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));
        ArrNode node = nodeRepository.getOneCheckExist(nodeId);
        if (!node.getFundId().equals(version.getFundId())) {
            throw new IllegalArgumentException("JP nespadá pod verzi AS");
        }
        return arrangementExtensionRepository.findByNode(node);
    }

    /**
     * Vyhledá všechny použité/zděděné definice rozšíření pro řídící pravidla pro JP. Seřazené podle názvu.
     *
     * @param nodeId identifikátor JP
     * @return nalezené definice
     */
    public List<RulArrangementExtension> findAllArrangementExtensionsByNodeId(final Integer nodeId) {
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");
        return arrangementExtensionRepository.findByNodeIdToRoot(nodeId);
    }

    public RulItemType getItemTypeById(final Integer itemTypeId) {
        return itemTypeRepository.findById(itemTypeId)
                .orElseThrow(itemType(itemTypeId));
    }

    public List<RulExtensionRule> findExtensionRuleByNode(final ArrNode node,
                                                          final RulExtensionRule.RuleType attributeTypes) {
        Assert.notNull(node, "JP musí být vyplněna");

        List<ArrNodeExtension> nodeExtensions = nodeExtensionRepository.findAllByNodeIdFromRoot(node.getNodeId());

        LinkedHashSet<RulArrangementExtension> arrangementExtensions = new LinkedHashSet<>();
        for (ArrNodeExtension nodeExtension : nodeExtensions) {
            arrangementExtensions.add(nodeExtension.getArrangementExtension());
        }
        List<RulArrangementExtension> arrangementExtensionsFinal = new ArrayList<>(arrangementExtensions);

        return extensionRuleRepository.findExtensionRules(arrangementExtensionsFinal, attributeTypes);
    }

    /**
     * Nastaví nodu konkrétní extensions. Synchronizuje dodaný set. Dovytvoří potřebné, smaže nepotřebné.
     *
     * @param versionId Fund version Id
     * @param nodeId Node Id
     * @param arrExtensionIds Set Id nastavovaných ArrExtensions
     */
    public void syncNodeExtensions(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer versionId,
                                   final Integer nodeId,
                                   final Set<Integer> arrExtensionIds) {

        Assert.notNull(arrExtensionIds, "Musí být předán seznam rozšíření");

        ArrNode node = nodeRepository.getOneCheckExist(nodeId);

        final Set<Integer> toDeleteIds = arrangementExtensionRepository.findByNode(node).stream().map(RulArrangementExtension::getArrangementExtensionId).collect(Collectors.toSet());
        final Set<Integer> toAddIds = new HashSet<>();
        arrExtensionIds.forEach(i -> {
            if (toDeleteIds.contains(i)) {
                toDeleteIds.remove(i);
            } else {
                toAddIds.add(i);
            }
        });

        if (toDeleteIds.isEmpty() && toAddIds.isEmpty()) {
            // Vše je v konzistentním stavu
            return;
        }

        // Změna pod kterou uvidíme nastavení
        final ArrChange change = arrangementInternalService.createChange(ArrChange.Type.SET_NODE_EXTENSION, node);

        // Uložení node pro zaznamenání change
        saveNode(node, change);

        List<ArrNodeExtension> toDelete = null;
        if (!toDeleteIds.isEmpty()) {
            // Seznam ke smazání
            toDelete = nodeExtensionRepository.findByArrExtensionIdsAndNodeNotDeleted(toDeleteIds, node);

            // Nastavení smazání
            toDelete.forEach(i -> i.setDeleteChange(change));

            toDelete = nodeExtensionRepository.saveAll(toDelete);
        }

        List<ArrNodeExtension> toAdd = null;
        if (!toAddIds.isEmpty()) {
            // Seznam ArrExts k přidání
            final List<RulArrangementExtension> toAddExts = arrangementExtensionRepository.findAllById(toAddIds);

            // Seznam platných rozšíření pro verzi fund
            final List<RulArrangementExtension> validArrExts = findArrangementExtensionsByFundVersionId(versionId);

            // Validace + příprava listu k uložení
            toAdd = toAddExts.stream().map(i -> {
                if (!validArrExts.contains(i)) {
                    throw new IllegalArgumentException("Řídící pravidla nejsou pro pravidla AS");
                }
                final ArrNodeExtension ext = new ArrNodeExtension();
                ext.setCreateChange(change);
                ext.setNode(node);
                ext.setArrangementExtension(i);
                return ext;
            }).collect(Collectors.toList());

            toAdd = nodeExtensionRepository.saveAll(toAdd);
        }

        nodeExtensionRepository.flush();

        eventNotificationService.publishEvent(new EventNodeIdVersionInVersion(EventType.FUND_EXTENSION_CHANGE, versionId, node.getNodeId(), node.getVersion()));

        if (toDelete != null) {
            toDelete.forEach(i -> arrangementCacheService.deleteNodeExtension(nodeId, i.getNodeExtensionId()));
        }

        if (toAdd != null) {
            toAdd.forEach(i -> arrangementCacheService.createNodeExtension(nodeId, i));
        }

        revalidateNodes(versionId, Collections.singleton(nodeId),
                        Lists.newArrayList(RelatedNodeDirection.NODE, RelatedNodeDirection.DESCENDANTS),
                        null);
    }

    /**
     * Získání seznamu typů atributů podle strukt. typu a verze AS.
     *
     * @param structTypeId
     *            strukturovaný typ
     * @param fundVersion
     *            verze AS
     * @param structureItems
     *            seznam položek strukturovaného datového typu
     * @return seznam typu atributů
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<RulItemTypeExt> getStructureItemTypes(final Integer structTypeId,
                                                      @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                                      final List<ArrStructuredItem> structureItems) {
        return getStructureItemTypesInternal(structTypeId, fundVersion, structureItems);
    }

    /**
     * Získání seznamu typů atributů podle strukt. typu a verze AS - internal.
     *
     * @param structTypeId  id strukturovaného typu
     * @param fundVersion    verze AS
     * @param structureItems seznam položek strukturovaného datového typu
     * @return seznam typu atributů
     */
    public List<RulItemTypeExt> getStructureItemTypesInternal(final Integer structTypeId,
                                                              final ArrFundVersion fundVersion,
                                                              final List<ArrStructuredItem> structureItems) {
        List<RulItemTypeExt> rulDescItemTypeExtList = getRulesetDescriptionItemTypes();
        return rulesExecutor.executeStructureItemTypesRules(structTypeId, rulDescItemTypeExtList, fundVersion,
                                                            structureItems);
    }


    /**
     * Vrátí typy atributů ve stejném pořadí v jakém jsou předáná jejich id.
     *
     * @param ids id typů atribuů
     * @return typy atributů
     */
    public List<RulItemType> findItemTypesByIdsOrdered(List<Integer> ids) {
        Map<Integer, RulItemType> rulItemTypeMap = itemTypeRepository.findAllById(ids).stream().
                collect(Collectors.toMap(RulItemType::getItemTypeId, Function.identity()));

        return ids.stream().
                map(id -> rulItemTypeMap.get(id)).
                collect(Collectors.toList());
    }

    @Transactional
    public ModelAvailable executeAvailable(final ApAccessPointCreateVO form) {
        if (form == null || form.getTypeId() == null || form.getPartForm() == null || form.getScopeId() == null) {
            throw new IllegalArgumentException("Třída entity, část a oblast musí být vyplněny");
        }

        StaticDataProvider sdp = staticDataService.getData();

        Integer apTypeId = form.getTypeId();
        Integer accessPointId = form.getAccessPointId();
        ApScope scope = accessPointService.getApScope(form.getScopeId());
        RuleSet ruleSet = sdp.getRuleSetById(scope.getRuleSetId());

        Integer preferredPartId = null;

        ApBuilder apBuilder = new ApBuilder(staticDataService.getData());
        if (accessPointId != null) {
            // ApState
            ApState apState = this.accessPointService.getStateInternal(accessPointId);

            CachedAccessPoint cachedAcessPoint = accessPointCacheService.findCachedAccessPoint(accessPointId);
            apBuilder.setAccessPoint(cachedAcessPoint);

            ApRevision revision = revisionService.findRevisionByState(apState);
            if (revision != null) {
                List<ApRevPart> revParts = revisionPartService.findPartsByRevision(revision);
                List<ApRevItem> revItems = revisionItemService.findByParts(revParts);

                // apply revision data
                apBuilder.setRevision(revision, revParts, revItems);
            }
        } else {
            apBuilder.setAeType(apTypeId);
        }

        Ap ae = apBuilder.build();

        ApPartFormVO partForm = form.getPartForm();
        List<ApItemVO> items = partForm.getItems();
        List<AbstractItem> modelItems = new ArrayList<>();
        for (ApItemVO item : items) {
            AbstractItem ai;
            cz.tacr.elza.core.data.ItemType itemType = sdp.getItemTypeById(item.getTypeId());
            RulItemSpec itemSpec = item.getSpecId() == null ? null : sdp.getItemSpecById(item.getSpecId());

            if (item instanceof ApItemStringVO) {
                ai = new Item(item.getId(), itemType, itemSpec, ((ApItemStringVO) item).getValue());
            } else if (item instanceof ApItemBitVO) {
                ai = new BoolItem(item.getId(), itemType, itemSpec, ((ApItemBitVO) item).getValue());
            } else if (item instanceof ApItemIntVO) {
                ai = new IntItem(item.getId(), itemType, itemSpec, ((ApItemIntVO) item).getValue());
            } else if (item instanceof ApItemTextVO) {
                ai = new Item(item.getId(), itemType, itemSpec, ((ApItemTextVO) item).getValue());
            } else if (item instanceof ApItemEnumVO) {
                ai = new Item(item.getId(), itemType, itemSpec, itemSpec == null ? null : itemSpec.getCode());
            } else if (item instanceof ApItemAccessPointRefVO) {
                ai = new IntItem(item.getId(), itemType, itemSpec, ((ApItemAccessPointRefVO) item).getValue());
            } else if (item instanceof ApItemUnitdateVO) {
                ai = new Item(item.getId(), itemType, itemSpec, ((ApItemUnitdateVO) item).getValue());
            } else if (item instanceof ApItemCoordinatesVO) {
                ai = new Item(item.getId(), itemType, itemSpec, ((ApItemCoordinatesVO) item).getValue());
            } else if (item instanceof ApItemUriRefVO) {
                ai = new Item(item.getId(), itemType, itemSpec, ((ApItemUriRefVO) item).getValue());
            } else {
                throw new NotImplementedException("Neimplementovaná konverze");
            }
            modelItems.add(ai);
        }

        List<ItemType> modelItemTypes = createModelItemTypes();

        Part parentPart = null;
        if(partForm.getParentPartId() != null) {
            parentPart = apBuilder.getPart(partForm.getParentPartId());
            Validate.notNull(parentPart, "Parent part not found, %s", partForm.getParentPartId());
        } else
        if (partForm.getRevParentPartId() != null) {
            parentPart = apBuilder.getPartByRevPartId(partForm.getRevParentPartId());
            Validate.notNull(parentPart, "Parent part in revision not found, %s", partForm.getRevParentPartId());
        }

        boolean isPartPreferred = form.getAccessPointId() == null || (partForm.getPartId() != null && preferredPartId != null && preferredPartId.equals(partForm.getPartId()));

        Part part = new Part(null, PartType.fromValue(partForm.getPartTypeCode()), modelItems, parentPart, isPartPreferred);

        ModelAvailable modelAvailable = new ModelAvailable(ae, part, modelItems, modelItemTypes);

        return executeAvailable(PartType.fromValue(partForm.getPartTypeCode()), modelAvailable, ruleSet);
    }
    
    /**
     * Run access point validation
     * 
     * Method reads current AP state from DB.
     * Method is not using cache.
     * 
     * @param accessPoint
     * @return
     */
    @Transactional(TxType.MANDATORY)
    public ApValidationErrorsVO executeValidation(final ApState apState,
                                                  final boolean includeRevision) {

        // Flush all changes to DB before reading data for validation
        this.entityManager.flush();

        StaticDataProvider sdp = staticDataService.getData();

        ApAccessPoint accessPoint = apState.getAccessPoint();
        ApScope scope = apState.getScope();
        Integer ruleSetId = scope.getRuleSetId();
        RuleSet ruleSet = sdp.getRuleSetById(ruleSetId); 

        List<ApPart> parts = partService.findPartsByAccessPoint(accessPoint);
        List<ApItem> itemList = accessPointItemService.findItemsByParts(parts);
        List<ApIndex> indexList = indexRepository.findIndicesByAccessPoint(apState.getAccessPointId());

        ApBuilder apBuilder = new ApBuilder(sdp);
        apBuilder.setAccessPoint(apState, parts, itemList);

        if (includeRevision) {
            ApRevision revision = revisionService.findRevisionByState(apState);
            if (revision != null) {
                List<ApRevPart> revParts = revisionPartService.findPartsByRevision(revision);
                List<ApRevItem> revItems = revisionItemService.findByParts(revParts);

                // apply revision data
                apBuilder.setRevision(revision, revParts, revItems);
            }
        }
        Ap ap = apBuilder.build();

        Map<PartType, List<Index>> indexMap = indexList.stream()
                .map(index -> createIndex(index, apBuilder.getPart(index.getPartId())))
                .collect(Collectors.groupingBy(index -> index.getPart().getType()));

        GeoModel geoModel = createGeoModel(ap);

        ApValidationErrorsVO apValidationErrorsVO = createAeValidationErrorsVO();

        // vytvoření mapy specifikací vztahů
        Map<Integer, Map<String, Relation>> relationMap = apBuilder.createRelationMap();
        // vytvoření mapy specifikací identifikátorů
        Map<String, Integer> identMap = apBuilder.createIdentMap();

        List<AbstractItem> items = apBuilder.createAbstractItemList();
        ModelValidation modelValidation = new ModelValidation(ap, geoModel, createModelParts(indexMap), new ApValidationErrors(), items);
        ModelValidation validationResult = executeValidation(modelValidation, ruleSet);
        // validace opakovatelnosti partů
        validatePartRepeatability(validationResult);
        // validace opakovatelnosti indexů přes party se stejným part typem
        validateIndexRepeatability(validationResult, apValidationErrorsVO);
        // validace vztahů na nevalidní nebo nahrazené entity
        validateEntityRefs(ap, apValidationErrorsVO);

        if (CollectionUtils.isNotEmpty(validationResult.getApValidationErrors().getErrors())) {
            apValidationErrorsVO.getErrors().addAll(validationResult.getApValidationErrors().getErrors());
        }

        for (Part part : ap.getParts()) {
            ModelAvailable modelAvailable = new ModelAvailable(ap, part, part.getItems(), createModelItemTypes());
            ModelAvailable availableResult = executeAvailable(part.getType(), modelAvailable, ruleSet);

            // validace možných itemů
            List<String> availableErrors = validateAvailableItems(availableResult, part);
            // validace opakovatelnosti vztahů
            validateRelationRepeatabilitySpecs(availableResult, relationMap, apValidationErrorsVO);
            // validace opakovatelnosti identifikátorů
            List<String> identErrors = validateIdentRepeatabilitySpecs(availableResult, identMap);

            if (CollectionUtils.isNotEmpty(availableErrors)) {
                PartValidationErrorsVO partValidationErrorsVO = getPartValidationErrorsVO(apValidationErrorsVO, part.getId());
                partValidationErrorsVO.getErrors().addAll(availableErrors);
            }
            if (CollectionUtils.isNotEmpty(identErrors)) {
                PartValidationErrorsVO partValidationErrorsVO = getPartValidationErrorsVO(apValidationErrorsVO, part.getId());
                partValidationErrorsVO.getErrors().addAll(identErrors);
            }
        }

        return apValidationErrorsVO;
    }

    private void validateEntityRefs(Ap ap, ApValidationErrorsVO apValidationErrorsVO) {
        if (CollectionUtils.isNotEmpty(ap.getParts())) {
            for (Part part : ap.getParts()) {
                if (CollectionUtils.isNotEmpty(part.getItems())) {
                    List<Integer> recordCodes = new ArrayList<>();
                    for (AbstractItem item : part.getItems()) {
                        DataType dataType = DataType.fromCode(item.getDataType());
                        if (item instanceof IntItem && dataType == DataType.RECORD_REF) {
                            IntItem intItem = (IntItem) item;
                            recordCodes.add(intItem.getValue());
                        }
                    }
                    if (CollectionUtils.isNotEmpty(recordCodes)) {
                        List<ApState> stateList = stateRepository.findLastByAccessPointIds(recordCodes);
                        if (CollectionUtils.isNotEmpty(stateList)) {
                            for (ApState state : stateList) {
                                if (state.getDeleteChange() != null) {
                                    PartValidationErrorsVO partValidationErrorsVO = getPartValidationErrorsVO(apValidationErrorsVO, part.getId());
                                    partValidationErrorsVO.getErrors().add("V části typu " + part.getType().value() + " entita odkazuje na neplatnou entitu");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void validatePartRepeatability(final ModelValidation validationResult) {
        Map<String, Integer> partCountMap = new HashMap<>();
        for (Part part : validationResult.getAp().getParts()) {
            if (part.getParent() == null) {
                partCountMap.put(part.getType().value(), partCountMap.getOrDefault(part.getType().value(), 0) + 1);
            }
        }
        for (ModelPart modelPart : validationResult.getModelParts()) {
            if (!modelPart.isRepeatable() && partCountMap.getOrDefault(modelPart.getType().value(), 0) > 1) {
                validationResult.getApValidationErrors().addError("Část " + modelPart.getType() + " je v entitě vícekrát.");
            }
        }
    }

    private void validateIndexRepeatability(ModelValidation validationResult, ApValidationErrorsVO apValidationErrorsVO) {
        for (ModelPart modelPart : validationResult.getModelParts()) {
            if (CollectionUtils.isNotEmpty(modelPart.getIndices())) {
                Map<String, Integer> indexCount = createIndexCountMap(modelPart.getIndices());
                for (Index index : modelPart.getIndices()) {
                    int parentId = index.getPart().getParent() != null ? index.getPart().getParent().getId() : -1;
                    String key = parentId + ":" + index.getIndexType() + ":" + index.getValue();
                    if (!index.isRepeatable() && indexCount.get(key) > 1) {
                        PartValidationErrorsVO partValidationErrorsVO = getPartValidationErrorsVO(apValidationErrorsVO, index.getPart().getId());
                        partValidationErrorsVO.getErrors().add("V části typu " + index.getPart().getType().value() + " je duplicitní index typu "
                                + index.getIndexType() + " hodnoty " + index.getValue());
                    }
                }
            }
        }
    }

    public Map<String, Integer> createIndexCountMap(final List<Index> indices) {
        Map<String, Integer> indexCount = new HashMap<>();
        for (Index index : indices) {
            int parentId = index.getPart().getParent() != null ? index.getPart().getParent().getId() : -1;
            String key = parentId + ":" + index.getIndexType() + ":" + index.getValue();
            indexCount.put(key, indexCount.getOrDefault(key, 0) + 1);
        }
        return indexCount;
    }

    public List<String> validateAvailableItems(ModelAvailable availableResult) {
        return validateAvailableItems(availableResult, null);
    }

    private List<String> validateAvailableItems(final ModelAvailable availableResult, final Part part) {
        List<String> errors = new ArrayList<>();

        validateRequiredItems(availableResult, errors, part);
        validateImpossibleItems(availableResult, errors, part);
        validateItemRepeatability(availableResult, errors, part);

        return errors;
    }

    private void validateRequiredItems(final ModelAvailable availableResult,
                                       final List<String> errors,
                                       final Part part) {
        for (ItemType itemType : availableResult.getItemTypes()) {
            if (itemType.getRequiredType().equals(RequiredType.REQUIRED)) {
                AbstractItem item = availableResult.findItem(itemType.getItemType());
                if (item == null) {
                    String partType = part != null ? " typu " + part.getType().value() : "";
                    errors.add("V části" + partType + " chybí povinný typ prvku "
                            + itemType.getCode() + "-" + itemType.getItemType().getEntity().getName());
                }
            }
        }
    }

    private void validateImpossibleItems(final ModelAvailable availableResult,
                                         final List<String> errors,
                                         final Part part) {
        for (AbstractItem item : availableResult.getItems()) {
            ItemType itemType = availableResult.getItemType(item);
            if (itemType.getRequiredType().equals(RequiredType.IMPOSSIBLE)) {
                String partType = part != null ? " typu " + part.getType().value() : "";
                errors.add("V části" + partType + " je zakázaný prvek typu " + itemType.getCode() + "-" + itemType.getItemType().getEntity().getName());
            } else if (item.getSpec() != null) {
                ItemSpec itemSpec = itemType.getSpec(item.getSpec());
                // specification must exist
                Validate.notNull(itemSpec, "Data inconsistency, specification: %s", item.getSpec());
                        
                if (itemSpec.getRequiredType().equals(RequiredType.IMPOSSIBLE)) {
                    String partType = part != null ? " typu " + part.getType().value() : "";
                    errors.add("V části" + partType + " je zakázaná specifikace prvku " + itemSpec.getCode() + "-"
                            + itemSpec.getItemSpec().getName());
                }
            }
        }
    }

    private void validateItemRepeatability(final ModelAvailable availableResult,
                                           final List<String> errors,
                                           final Part part) {
        StaticDataProvider sdp = staticDataService.getData();
        Map<String, Integer> itemMap = new HashMap<>();
        for (AbstractItem item : availableResult.getItems()) {
            itemMap.put(item.getType(), itemMap.getOrDefault(item.getType(), 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : itemMap.entrySet()) {
            if (entry.getValue() > 1) {
                ItemType itemType = availableResult.getItemType(entry.getKey());
                if (itemType != null) {
                    if (!itemType.isRepeatable()) {
                        RulItemType rulItemType = sdp.getItemTypeByCode(itemType.getCode()).getEntity();
                        String partType = part != null ? " typu " + part.getType().value() : "";
                        errors.add("V části" + partType + " je prvek " + itemType.getCode()
                                + "-" + rulItemType.getName() + " vícekrát.");
                    }
                }
            }
        }

    }

    private void validateRelationRepeatabilitySpecs(ModelAvailable availableResult, Map<Integer, Map<String, Relation>> relationMap, ApValidationErrorsVO aeValidationErrorsVO) {
        StaticDataProvider sdp = staticDataService.getData();
        if (availableResult.getPart().getType().equals(PartType.PT_REL)) {
            Part parent = availableResult.getPart().getParent();
            Integer key = parent != null ? parent.getId() : -1;
            Map<String, Relation> simpleRelationMap = relationMap.get(key);

            // Co dela cela kontrola nize a proc neni v pravidlech?
            cz.tacr.elza.core.data.ItemType itemTypeRelEntity = sdp.getItemTypeByCode(REL_ENTITY);
            if (itemTypeRelEntity == null) {
                Validate.notNull(itemTypeRelEntity, "Chybi itemType " + REL_ENTITY);
            }

            // ?? muze vratit vice item stejneho typu
            AbstractItem item = availableResult.findItem(itemTypeRelEntity);
            if (item != null) {
                Relation simpleRelation = simpleRelationMap.get(item.getSpec());
                if (simpleRelation.getRelationCount() > 1) {
                    // Uplatni se jen pri vetsim poctu vztahu
                    ItemType itemType = availableResult.getItemType(item);
                    ItemSpec itemSpec = itemType.getSpec(item.getSpec());

                    if (itemSpec != null && !itemSpec.isRepeatable()) {
                        if (parent != null) {
                            PartValidationErrorsVO partValidationErrorsVO = getPartValidationErrorsVO(aeValidationErrorsVO, parent.getId());
                            partValidationErrorsVO.getErrors().add("V části typu " + parent.getType().value() + " je vztah "
                                    + itemSpec.getCode() + "-" + itemSpec.getItemSpec().getName() + " vícekrát.");
                        } else {
                            aeValidationErrorsVO.getErrors().add("V entitě je vztah " + itemSpec.getCode() + "-"
                                    + itemSpec.getItemSpec().getName() + " vícekrát.");
                        }
                    }
                }
            }
        }
    }

    private List<String> validateIdentRepeatabilitySpecs(final ModelAvailable availableResult,
                                                        final Map<String, Integer> identMap) {
        List<String> errors = new ArrayList<>();
        if (availableResult.getPart().getType().equals(PartType.PT_IDENT)) {
            // TODO: itemu muze byt vice??
            AbstractItem item = availableResult.findItem(IDN_TYPE);
            if (item != null && identMap.get(item.getSpec()) > 1) {
                ItemType itemType = availableResult.getItemType(item);
                ItemSpec itemSpec = itemType.getSpec(item.getSpec());

                if (itemSpec != null && !itemSpec.isRepeatable()) {
                    errors.add("V části typu " + availableResult.getPart().getType().value() +
                            " je externí identifikátor " + itemSpec.getCode() + "-" + itemSpec.getItemSpec().getName()
                            + " vícekrát.");
                }
            }
        }
        return errors;
    }

    @Nullable
    private GeoModel createGeoModel(final Ap ap) {
        if (ap.getAeType().equals(GEO_UNIT)) {
            Integer parentGeoId = findParentGeoId(ap);
            String country = findEntityCountry(ap);
            if (parentGeoId != null) {
                String parentGeoType = findEntityGeoType(parentGeoId);
                boolean parentExtinct = isParentExtinct(parentGeoId);                
                if (country == null) {
                    country = findEntityCountry(parentGeoId);
                }
                return new GeoModel(parentGeoType, country, parentExtinct);
            }
            if (country != null) {
                return new GeoModel(null, country, false);
            }
        }
        return null;
    }

    @Nullable
    private Integer findParentGeoId(Ap ap) {
        Integer recordId = null;
        for (Part part : ap.getParts()) {
            if (part.getType().equals(PartType.PT_BODY)) {
                IntItem item = (IntItem) findItem(part.getItems(), GEO_ADMIN_CLASS);
                if (item != null) {
                    recordId = item.getValue();
                    break;
                }
            } else if (part.getType().equals(PartType.PT_EXT)) {
                return null;
            }
        }
        return recordId;
    }

    @Nullable
    private Integer findParentGeoId(Integer recordId) {
        StaticDataProvider sdp = staticDataService.getData();
        RulItemType itemType = sdp.getItemTypeByCode(GEO_ADMIN_CLASS).getEntity();
        List<ApItem> items = accessPointItemService.findItems(recordId, itemType, PartType.PT_BODY.value());
        if (CollectionUtils.isEmpty(items)) {
            return null;
        }
        if (items.size() > 1) {
            logger.info("Entity with multiple parents, entityId: {}, parent count: {}", recordId, items.size());
            return null;
        }
        ApItem aeItem = items.get(0);
        ArrDataRecordRef recordRef = (ArrDataRecordRef) aeItem.getData();
        return recordRef.getRecordId();
    }

    // TODO: use cache
    private boolean isParentExtinct(Integer recordId) {
    	ApAccessPoint ap = this.accessPointService.getAccessPointInternal(recordId);
        List<ApPart> parts = partService.findPartsByAccessPoint(ap);
        ApPart ptExt = partService.findFirstPartByCode(PartType.PT_EXT.toString(), parts);
        return ptExt!=null;
    }

    @Nullable
    private String findEntityGeoType(Integer recordId) {
        StaticDataProvider sdp = staticDataService.getData();
        RulItemType rulItemType = sdp.getItemTypeByCode(GEO_TYPE).getEntity();
        List<ApItem> aeItemList = accessPointItemService.findItems(recordId, rulItemType, PartType.PT_BODY.value());
        if (CollectionUtils.isNotEmpty(aeItemList)) {
            RulItemSpec itemSpec = aeItemList.get(0).getItemSpec();
            itemSpec = sdp.getItemSpecById(itemSpec.getItemSpecId());
            return itemSpec.getCode();
        }
        return null;
    }

    @Nullable
    private String findEntityCountry(Ap ap) {
        for (Part part : ap.getParts()) {
            if (part.getType().equals(PartType.PT_IDENT)) {
                AbstractItem idnType = findItem(part.getItems(), IDN_TYPE);
                if (idnType != null && (idnType.getSpec().equals(ISO3166_2) || idnType.getSpec().equals(ISO3166_3))) {
                    Item idnValue = (Item) findItem(part.getItems(), IDN_VALUE);
                    if (idnValue != null) {
                        return idnValue.getValue();
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private String findEntityCountry(Integer recordId) {

        int recurentCounter = 1;
        Integer nextRecordId = recordId;
        Set<Integer> loopDetector = new HashSet<>();
        // protection for infinite loop
        while (recurentCounter < 20) {

            String countryIso = findCountryIso(nextRecordId);
            if (countryIso != null) {
                return countryIso;
            }

            // add item
            loopDetector.add(nextRecordId);

            // find parent
            nextRecordId = findParentGeoId(nextRecordId);
            if (nextRecordId == null) {
                return null;
            }
            if (loopDetector.contains(nextRecordId)) {
                logger.error("Loop detected in parent entities, recordId: {}, repeating entity: {}", recordId,
                             nextRecordId);
                return null;
            }
            recurentCounter++;
        }
        if (recurentCounter >= 20) {
            logger.error("Parent hierarchy is too deep, recordId: {}", recordId);
        }
        return null;
    }

    @Nullable
    private String findCountryIso(Integer recordId) {
        StaticDataProvider sdp = staticDataService.getData();
        RulItemType idnType = sdp.getItemTypeByCode(IDN_TYPE).getEntity();
        RulItemType idnValue = sdp.getItemTypeByCode(IDN_VALUE).getEntity();
        List<RulItemType> itemTypes = Arrays.asList(idnType, idnValue);

        List<ApItem> itemsList = accessPointItemService.findItems(recordId, itemTypes, PartType.PT_IDENT.value());
        List<ApItem> idnTypeItemList = itemsList.stream().filter(i -> i.getItemType().getItemTypeId().equals(idnType.getItemTypeId())).collect(Collectors.toList());
        List<ApItem> idnValueItemList = itemsList.stream().filter(i -> i.getItemType().getItemTypeId().equals(idnValue.getItemTypeId())).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(idnTypeItemList)) {
            for (ApItem idnTypeItem : idnTypeItemList) {
                RulItemSpec itemSpec = sdp.getItemSpecById(idnTypeItem.getItemSpec().getItemSpecId());
                if (itemSpec.getCode().equals(ISO3166_2) || itemSpec.getCode().equals(ISO3166_3)) {
                    if (CollectionUtils.isNotEmpty(idnValueItemList)) {
                        for (ApItem aeItem : idnValueItemList) {
                            if (aeItem.getPartId().equals(idnTypeItem.getPartId())) {
                                return aeItem.getData().getFulltextValue();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private ApValidationErrorsVO createAeValidationErrorsVO() {
        ApValidationErrorsVO aeValidationErrorsVO = new ApValidationErrorsVO();
        aeValidationErrorsVO.setPartErrors(new ArrayList<>());
        aeValidationErrorsVO.setErrors(new ArrayList<>());
        return aeValidationErrorsVO;
    }

    private PartValidationErrorsVO getPartValidationErrorsVO(ApValidationErrorsVO aeValidationErrorsVO, Integer partId) {
        for (PartValidationErrorsVO p : aeValidationErrorsVO.getPartErrors()) {
            if (p.getId().equals(partId)) {
                return p;
            }
        }
        PartValidationErrorsVO partValidationErrorsVO = createPartValidationErrorsVO(partId);
        aeValidationErrorsVO.getPartErrors().add(partValidationErrorsVO);
        return partValidationErrorsVO;
    }

    private PartValidationErrorsVO createPartValidationErrorsVO(Integer partId) {
        PartValidationErrorsVO partValidationErrorsVO = new PartValidationErrorsVO();
        partValidationErrorsVO.setId(partId);
        partValidationErrorsVO.setErrors(new ArrayList<>());
        return partValidationErrorsVO;
    }

    @Nullable
    private Part findPartById(List<Part> partList, Integer partId) {
        if (CollectionUtils.isNotEmpty(partList)) {
            for (Part part : partList) {
                if (part.getId().equals(partId)) {
                    return part;
                }
            }
        }
        return null;
    }

    @Nullable
    private AbstractItem findItem(List<AbstractItem> itemList, String itemTypeCode) {
        for (AbstractItem item : itemList) {
            if (item.getItemType().getCode().equals(itemTypeCode)) {
                return item;
            }
        }
        return null;
    }

    private List<ItemType> createModelItemTypes() {
        StaticDataProvider sdp = staticDataService.getData();
        Collection<cz.tacr.elza.core.data.ItemType> itemTypes = sdp.getItemTypes();
        List<ItemType> modelItemTypes = new ArrayList<>(itemTypes.size());
        for (cz.tacr.elza.core.data.ItemType itemType : itemTypes) {
            modelItemTypes.add(new ItemType(itemType));
        }
        return modelItemTypes;
    }

    private List<ModelPart> createModelParts(Map<PartType, List<Index>> indexMap) {
        List<ModelPart> modelPartList = new ArrayList<>();
        for(PartType partType : PartType.values()) {
            List<Index> indices = indexMap.getOrDefault(partType, null);
            modelPartList.add(new ModelPart(partType, indices));
        }
        return modelPartList;
    }

    private Index createIndex(ApIndex apIndex, Part part) {
        return new Index(apIndex.getIndexType(), apIndex.getValue(), part);
    }

    private ModelAvailable executeAvailable(@NotNull final PartType partType,
                                            @NotNull final ModelAvailable modelAvailable,
                                            @NotNull final RuleSet ruleSet) {
        StaticDataProvider sdp = staticDataService.getData();
        DrlType drlType = DrlType.AVAILABLE_ITEMS;

        Ap ae = modelAvailable.getAp();
        ApType aeType = sdp.getApTypeByCode(ae.getAeType());

        // prepare list of rule codes
        ApType aeTypeProcess = aeType;
        ArrayList<String> executeDrls = new ArrayList<>();
        while (aeTypeProcess != null) {
            executeDrls.add(drlType.value() + "/" + aeTypeProcess.getCode() + "/" + partType.value());
            executeDrls.add(drlType.value() + "/" + aeTypeProcess.getCode());
            aeTypeProcess = aeTypeProcess.getParentApType();
        }
        executeDrls.add(drlType.value() + "/" + partType.value());
        executeDrls.add(drlType.value());
        
        List<RulExtensionRule> rules = prepareExtRuleList(executeDrls, ruleSet);

        try {
            availableItemsRules.execute(rules, modelAvailable);
        } catch (Exception e) {
            throw new SystemException(e);
        }

        return modelAvailable;
    }

    private ModelValidation executeValidation(@NotNull final ModelValidation modelValidation,
                                              @NotNull final RuleSet ruleSet) {
        StaticDataProvider sdp = staticDataService.getData();
        DrlType drlType = DrlType.VALIDATION;

        Ap ae = modelValidation.getAp();
        ApType aeType = sdp.getApTypeByCode(ae.getAeType());

        // prepare list of rule codes
        ApType aeTypeProcess = aeType;
        ArrayList<String> executeDrls = new ArrayList<>();
        while (aeTypeProcess != null) {
            for (PartType partType : PartType.values()) {
                executeDrls.add(drlType.value() + "/" + aeTypeProcess.getCode() + "/" + partType.value());
            }
            executeDrls.add(drlType.value() + "/" + aeTypeProcess.getCode());
            aeTypeProcess = aeTypeProcess.getParentApType();
        }
        for (PartType partType : PartType.values()) {
            executeDrls.add(drlType.value() + "/" + partType.value());
        }
        executeDrls.add(drlType.value());

        List<RulExtensionRule> rules = prepareExtRuleList(executeDrls, ruleSet);

        try {
            modelValidationRules.execute(rules, modelValidation);
        } catch (Exception e) {
            throw new SystemException(e);
        }

        return modelValidation;
    }

    private List<RulExtensionRule> prepareExtRuleList(@NotNull final ArrayList<String> executeDrls,
                                                      @NotNull final RuleSet ruleSet) {
        // add in reverse order
        List<RulExtensionRule> rules = new ArrayList<>(executeDrls.size());
        for (int pos = executeDrls.size() - 1; pos >= 0; pos--) {
            String condition = executeDrls.get(pos);
            List<RulExtensionRule> rulExtensionRule = ruleSet.getExtByCondition(condition);
            if (rulExtensionRule != null) {
                rules.addAll(rulExtensionRule);
            }
        }
        return rules;
    }

    public RulItemSpec getItemSpecById(final Integer specId) {
        StaticDataProvider sdp = staticDataService.getData();
        RulItemSpec itemSpec = sdp.getItemSpecById(specId);
        if (itemSpec == null) {
            throw new ObjectNotFoundException("Neexistuje specifikace", BaseCode.ID_NOT_EXIST).setId(specId);
        }
        return itemSpec;
    }

    public List<RulRuleSet> findAllApRules() {
        return ruleSetRepository.findByRuleType(ENTITY);
    }

    public List<String> getItemTypeCodesByRuleSet(RulRuleSet rulRuleSet) {
        List<String> itemTypeCodes = new ArrayList<>();
        List<ItemType> itemTypeList = null;
        try {
            if (rulRuleSet.getItemTypeComponent() != null) {
                itemTypeList = availableItemsRules.execute(rulRuleSet, createModelItemTypes());
            }
        } catch (Exception e) {
            throw new SystemException(e);
        }

        if (CollectionUtils.isNotEmpty(itemTypeList)) {
            for (ItemType itemType : itemTypeList) {
                if (itemType.getRequiredType() == RequiredType.POSSIBLE || itemType.getRequiredType() == RequiredType.REQUIRED) {
                    itemTypeCodes.add(itemType.getCode());
                }
            }
        }

        return itemTypeCodes;
    }

    /**
     * Return export filter
     * 
     * @param exportFilterId
     * @return
     */
    @Transactional(TxType.MANDATORY)
    public RulExportFilter getExportFilter(Integer exportFilterId) {
        return exportFilterRepository.getOne(exportFilterId);
    }
}
