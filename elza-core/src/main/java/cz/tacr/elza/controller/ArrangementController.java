package cz.tacr.elza.controller;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;

import cz.tacr.elza.common.FileDownload;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.AddLevelParam;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ArrCalendarTypeVO;
import cz.tacr.elza.controller.vo.ArrDaoLinkVO;
import cz.tacr.elza.controller.vo.ArrDaoPackageVO;
import cz.tacr.elza.controller.vo.ArrDaoVO;
import cz.tacr.elza.controller.vo.ArrFundFulltextResult;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrOutputRestrictionScopeVO;
import cz.tacr.elza.controller.vo.ArrOutputTemplateVO;
import cz.tacr.elza.controller.vo.ArrOutputVO;
import cz.tacr.elza.controller.vo.ArrRefTemplateEditVO;
import cz.tacr.elza.controller.vo.ArrRefTemplateMapTypeVO;
import cz.tacr.elza.controller.vo.ArrRefTemplateVO;
import cz.tacr.elza.controller.vo.ArrRequestQueueItemVO;
import cz.tacr.elza.controller.vo.ArrRequestVO;
import cz.tacr.elza.controller.vo.CopyNodesParams;
import cz.tacr.elza.controller.vo.CopyNodesValidate;
import cz.tacr.elza.controller.vo.DataGridExportType;
import cz.tacr.elza.controller.vo.FilterNode;
import cz.tacr.elza.controller.vo.FilterNodePosition;
import cz.tacr.elza.controller.vo.FulltextFundRequest;
import cz.tacr.elza.controller.vo.FundListCountResult;
import cz.tacr.elza.controller.vo.NodeItemWithParent;
import cz.tacr.elza.controller.vo.OutputSettingsVO;
import cz.tacr.elza.controller.vo.RulOutputTypeVO;
import cz.tacr.elza.controller.vo.ScenarioOfNewLevelVO;
import cz.tacr.elza.controller.vo.SelectNodeResult;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.TreeNodeWithFundVO;
import cz.tacr.elza.controller.vo.filter.Filters;
import cz.tacr.elza.controller.vo.filter.SearchParam;
import cz.tacr.elza.controller.vo.nodes.ArrNodeExtendVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;
import cz.tacr.elza.controller.vo.nodes.NodeData;
import cz.tacr.elza.controller.vo.nodes.NodeDataParam;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDaoRequest;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutput.OutputState;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.ArrRequestQueueItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ConcurrentUpdateException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.filter.DescItemTypeFilter;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputItemRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.ArrIOService;
import cz.tacr.elza.service.ArrangementFormService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DaoService;
import cz.tacr.elza.service.DaoSyncService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.FilterTreeService;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.PolicyService;
import cz.tacr.elza.service.RequestQueueService;
import cz.tacr.elza.service.RequestService;
import cz.tacr.elza.service.RevertingChangesService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.exception.DeleteFailedException;
import cz.tacr.elza.service.importnodes.ImportFromFund;
import cz.tacr.elza.service.importnodes.ImportNodesFromSource;
import cz.tacr.elza.service.importnodes.vo.ConflictResolve;
import cz.tacr.elza.service.importnodes.vo.ImportParams;
import cz.tacr.elza.service.importnodes.vo.ValidateResult;
import cz.tacr.elza.service.output.OutputData;
import cz.tacr.elza.service.output.OutputRequestStatus;
import cz.tacr.elza.service.vo.ChangesResult;
import cz.tacr.elza.service.vo.UpdateDescItemsParam;

/**
 * Kontroler pro pořádání.
 *
 * @since 7. 1. 2016
 */
@RestController
@RequestMapping("/api/arrangement")
public class ArrangementController {

    private static final Logger logger = LoggerFactory.getLogger(ArrangementController.class);

    /**
     * Formát popisu atributu - krátká verze.
     */
    public static final String FORMAT_ATTRIBUTE_SHORT = "SHORT";

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private DaoPackageRepository daoPackageRepository;

    @Autowired
    private DaoLinkRepository daoLinkRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private OutputItemRepository outputItemRepository;

    @Autowired
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private ClientFactoryVO factoryVo;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private FundLevelService fundLevelService;

    @Autowired
    private DaoService daoService;

    @Autowired
    private DaoSyncService daoSyncService;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private FilterTreeService filterTreeService;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private UserService userService;

    @Autowired
    private OutputService outputService;

    @Autowired
    private ArrIOService arrIOService;

    @Autowired
    private RevertingChangesService revertingChangesService;

    @Autowired
    private ChangeRepository changeRepository;

    @Autowired
    private RequestService requestService;

    @Autowired
    private RequestQueueService requestQueueService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private ImportNodesFromSource importNodesFromSource;

    @Autowired
    private ArrangementFormService formService;
    
    /**
     * Poskytuje seznam balíčků digitalizátů pouze pod archivní souborem (AS).
     *
     * @param fundVersionId id archivního souboru
     * @param search        vyhledává (použití LIKE) nad kódem balíčku, kódem a labelem arr_dao (přirazený k balíčku), kódem a labelem arr_dao_batch_info
     * @param unassigned    mají-li se získávat pouze balíčky, které obsahují DAO, které nejsou nikam přirazené (unassigned = true), a nebo úplně všechny (unassigned = false)
     * @param maxResults    maximální počet vyhledaných balíčků
     * @return seznam balíčků, seřazení je podle ID balíčku sestupně (tzn. poslední vytvořené budou na začátku seznamu)
     */
    @RequestMapping(value = "/daopackages/{fundVersionId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public List<ArrDaoPackageVO> findDaoPackages(
            @PathVariable(value = "fundVersionId") final Integer fundVersionId,
            @RequestParam(value = "search", required = false) final String search,
            @RequestParam(value = "unassigned", required = false, defaultValue = "false") final Boolean unassigned,
            @RequestParam(value = "maxResults", required = false, defaultValue = "200") final Integer maxResults) {
        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);

        final List<ArrDaoPackage> arrDaoList = daoService.findDaoPackages(fundVersion, search, unassigned, maxResults);

        return factoryVo.createDaoPackageList(arrDaoList, unassigned);
    }

    /**
     * Získání potřebných dat pro vybrání JP podle UUID v klientovi.
     *
     * @param nodeUuid unikátní identifikátor JP
     * @return data pro vybranou JP
     */
    @RequestMapping(value = "/selectNode/{nodeUuid}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public SelectNodeResult selectNode(@PathVariable(value = "nodeUuid") final String nodeUuid) {
        ArrNode node = arrangementService.findNodeByUuid(nodeUuid);

        if (node == null) {
            throw new ObjectNotFoundException("JP neexistuje", BaseCode.ID_NOT_EXIST)
                    .setId(nodeUuid);
        }

        ArrFundVO fund = getFund(node.getFundId());

        ArrFundVersionVO fundVersion = fund.getVersions().stream()
                .filter(v -> v.getLockDate() == null)
                .findFirst().orElse(null);

        if (fundVersion == null) {
            throw new ObjectNotFoundException("AS nemá otevřenou verzi", BaseCode.ID_NOT_EXIST)
                    .setId(fund.getId());
        }

        ArrLevel level = fundLevelService.findLevelByNode(node);
        if (level == null) {
            throw new ObjectNotFoundException("JP nebylo dohledáno zařazení v hierarchii AS", BaseCode.ID_NOT_EXIST)
                    .setId(fund.getId());
        }

        TreeNodeVO parentNode = null;
        if (level.getNodeParent() != null) {
            Collection<TreeNodeVO> parentNodes = levelTreeCacheService
                    .getNodesByIds(Collections.singletonList(level.getNodeParent().getNodeId()), fundVersion.getId());
            Assert.notEmpty(parentNodes, "Kolekce JP nesmí být prázdná");
            parentNode = parentNodes.iterator().next();
        }

        NodeWithParent nodeWithParent = new NodeWithParent(ArrNodeVO.valueOf(node), parentNode);

        SelectNodeResult result = new SelectNodeResult();
        result.setFund(fund);
        result.setNodeWithParent(nodeWithParent);
        return result;
    }

    /**
     * Poskytuje seznam digitálních entit (DAO), které jsou napojené na konkrétní jednotku popisu (JP) nebo nemá žádné napojení (pouze pod archivní souborem (AS)).
     *
     * @param fundVersionId id archivního souboru
     * @param nodeId        id node, pokud je null, najde entity bez napojení
     * @param detail        načíst detailní informace (plnit struktutu vč návazných), výchozí hodnota false
     * @param index         počáteční pozice pro načtení
     * @param maxResults    počet načítaných výsledků
     * @return seznam digitálních entit (DAO)
     */
    @RequestMapping(value = "/daos/{fundVersionId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public List<ArrDaoVO> findDaos(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                   @RequestParam(value = "nodeId", required = false) final Integer nodeId,
                                   @RequestParam(value = "detail", required = false, defaultValue = "false") final Boolean detail,
                                   @RequestParam(value = "index", required = false, defaultValue = "0") final Integer index,
                                   @RequestParam(value = "maxResults", required = false, defaultValue = "99999") final Integer maxResults) {
        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);

        ArrNode node = null;
        if (nodeId != null) {
            node = nodeRepository.getOneCheckExist(nodeId);
        }

        final List<ArrDao> arrDaoList = daoService.findDaos(fundVersion, node, index, maxResults);

        return factoryVo.createDaoList(arrDaoList, BooleanUtils.isTrue(detail), fundVersion);
    }

    /**
     * Poskytuje seznam digitálních entit (DAO), které jsou napojené na konkrétní balíček.
     *
     * @param fundVersionId id archivního souboru
     * @param daoPackageId  id package
     * @param unassigned    mají-li se získávat pouze balíčky, které obsahují DAO, které nejsou nikam přirazené (unassigned = true), a nebo úplně všechny (unassigned = false)
     * @param index         počáteční pozice pro načtení
     * @param maxResults    počet načítaných výsledků
     * @return seznam digitálních entit (DAO)
     */
    @RequestMapping(value = "/daos/{fundVersionId}/{daoPackageId}",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public List<ArrDaoVO> findDaosByPackage(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                            @PathVariable(value = "daoPackageId") final Integer daoPackageId,
                                            @RequestParam(value = "detail", required = false, defaultValue = "false") final Boolean detail,
                                            @RequestParam(value = "unassigned", required = false, defaultValue = "false") final Boolean unassigned,
                                            @RequestParam(value = "index", required = false, defaultValue = "0") final Integer index,
                                            @RequestParam(value = "maxResults", required = false, defaultValue = "99999") final Integer maxResults) {
        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(daoPackageId, "Idenitifikátor DAO obalu musí být vyplněn");

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        final ArrDaoPackage arrDaoPackage = daoPackageRepository.getOneCheckExist(daoPackageId);


        final List<ArrDao> arrDaoList = daoService.findDaosByPackage(fundVersion.getFundId(), arrDaoPackage, index,
                maxResults,
                BooleanUtils.isTrue(unassigned));

        final List<ArrDaoVO> daoList = factoryVo.createDaoList(arrDaoList, BooleanUtils.isTrue(detail), fundVersion);

        final List<Integer> processingArrDaoIds = daoService.findProcessingArrDaoRequestDaoArrDaoIds(arrDaoList);

        daoList.forEach(i -> i.setExistInArrDaoRequest(processingArrDaoIds.contains(i.getId())));

        return daoList;
    }

    /**
     * připojení digitalizát na JP (vytvoření záznamu v arr_dao_link)
     *
     * @param daoId  DAO pro propojení
     * @param nodeId Node pro propojení
     */
    @RequestMapping(value = "/daos/{fundVersionId}/{daoId}/{nodeId}/create",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrDaoLinkVO createDaoLink(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                      @PathVariable(value = "daoId") final Integer daoId,
                                      @PathVariable(value = "nodeId") final Integer nodeId) {
        Validate.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Validate.notNull(daoId, "Identifikátor DAO musí být vyplněn");
        Validate.notNull(nodeId, "Identifikátor JP musí být vyplněn");

        // create dao link in separate transaction
        // dao link might create level and data from levelTreeCache are available
        // in new transaction
        ArrDaoLink daoLink = daoService.createDaoLink(fundVersionId, daoId, nodeId);

        Validate.notNull(daoLink);
        Validate.notNull(daoLink.getDaoLinkId());
        Validate.notNull(daoLink.getNodeId());

        // we are outside transaction
        // only initialized fields might be touched on daoLink
        return factoryVo.createDaoLink(daoLink.getDaoLinkId(), daoLink.getNodeId(), fundVersionId);
    }

    /**
     * Zavolá WS pro synchronizaci digitalizátů a aktualizuje metadata pro daný node a DAO.
     *
     * @param fundVersionId verze AS
     * @param nodeId        node pro synchronizaci
     */
    @Transactional
    @RequestMapping(value = "/daos/{fundVersionId}/nodes/{nodeId}/sync",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void syncDaoLink(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                            @PathVariable(value = "nodeId") final Integer nodeId) {
        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");

        final ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        final ArrNode node = nodeRepository.getOneCheckExist(nodeId);

        daoSyncService.syncDaoLink(fundVersion, node);
    }

    /**
     * Spustí asynchronní synchronizaci digitalizátů a aktualizuje metadata pro všechny nody z AS, které mají připojené DAO.
     *
     * @param fundVersionId verze AS
     */
    @Transactional
    @RequestMapping(value = "/daos/{fundVersionId}/all/sync",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void syncDaosByFund(@PathVariable(value = "fundVersionId") final Integer fundVersionId) {
        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        daoSyncService.syncDaosAll(fundVersionId);
    }

    /**
     * Odpojí digitalizát od JP (vyplnění záznamu delete_change_id v arr_dao_link)
     *
     * @param daoLinkId ID požadovaného linku k rozpojení
     */
    @Transactional
    @RequestMapping(value = "/daolinks/{fundVersionId}/{daoLinkId}",
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void deleteDaoLink(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                              @PathVariable(value = "daoLinkId") final Integer daoLinkId) {
        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(daoLinkId, "Identifikátor DAO linku musí být vyplněn");

        final ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        final ArrDaoLink daoLink = daoLinkRepository.getOneCheckExist(daoLinkId);

        daoService.deleteDaoLink(fundVersion, daoLink);
    }

    /**
     * Získání odkazovaných JP.
     *
     * @param fundVersionId verze AS
     * @param nodeId        JP pro kterou zjišťujeme odkazované JP
     * @return seznam JP
     */
    @RequestMapping(value = "/nodes/{nodeId}/{fundVersionId}/links", method = RequestMethod.GET)
    @Transactional
    public List<TreeNodeWithFundVO> findLinkedNodes(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                    @PathVariable(value = "nodeId") final Integer nodeId) {
        Set<Integer> nodeIds = arrangementService.findLinkedNodes(fundVersionId, nodeId);
        return levelTreeCacheService.getTreeNodesWithFunds(nodeIds);
    }

    /**
     * Smazání hodnot atributu podle typu.
     *
     * @param fundVersionId  identfikátor verze AP
     * @param nodeId         identfikátor JP
     * @param nodeVersion    verze JP
     * @param descItemTypeId identfikátor typu hodnoty atributu
     */
    @Transactional
    @RequestMapping(value = "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/{descItemTypeId}",
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescItemResult deleteDescItemsByType(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                @PathVariable(value = "nodeId") final Integer nodeId,
                                                @PathVariable(value = "nodeVersion") final Integer nodeVersion,
                                                @PathVariable(value = "descItemTypeId") final Integer descItemTypeId) {

        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(nodeId, "Nebyl vyplněn identifikátor JP");
        Assert.notNull(nodeVersion, "Nebyla vyplněna verze JP");
        Assert.notNull(descItemTypeId, "Nebyl vyplněn identifikátor typu atributu");

        ArrNode node = descriptionItemService
                .deleteDescriptionItemsByType(fundVersionId, nodeId, nodeVersion, descItemTypeId, false);

        DescItemResult descItemResult = new DescItemResult();
        descItemResult.setItem(null);
        descItemResult.setParent(ArrNodeVO.valueOf(node));

        return descItemResult;
    }

    /**
     * Smazání hodnot atributu podle typu.
     *
     * @param fundVersionId identfikátor verze AP
     * @param outputId      identfikátor výstupu
     * @param outputVersion verze výstupu
     * @param itemTypeId    identfikátor typu hodnoty atributu
     */
    @Transactional
    @RequestMapping(value = "/outputItems/{fundVersionId}/{outputId}/{outputVersion}/{itemTypeId}",
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OutputItemResult deleteOutputItemsByType(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                    @PathVariable(value = "outputId") final Integer outputId,
                                                    @PathVariable(value = "outputVersion") final Integer outputVersion,
                                                    @PathVariable(value = "itemTypeId") final Integer itemTypeId) {

        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(outputVersion, "Nebyla vyplněna verze výstupu");
        Assert.notNull(itemTypeId, "Nebyl vyplněn identifikátor typu atributu");
        Assert.notNull(outputId, "Identifikátor výstupu musí být vyplněn");

        ArrOutput output = outputService
                .deleteOutputItemsByType(fundVersionId, outputId, outputVersion, itemTypeId);

        OutputItemResult outputItemResult = new OutputItemResult();
        outputItemResult.setItem(null);
        outputItemResult.setParent(factoryVo.createOutput(output));

        return outputItemResult;
    }

    /**
     * Smazání hodnoty atributu.
     *
     * @param descItemVO    hodnota atributu
     * @param fundVersionId identfikátor verze AP
     * @param nodeVersion   verze JP
     */
    @Transactional
    @RequestMapping(value = "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/delete",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescItemResult deleteDescItem(@RequestBody final ArrItemVO descItemVO,
                                         @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                         @PathVariable(value = "nodeId") final Integer nodeId,
                                         @PathVariable(value = "nodeVersion") final Integer nodeVersion) {
        Assert.notNull(descItemVO, "Hodnota atributu musí být vyplněna");
        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(nodeVersion, "Nebyla vyplněna verze JP");

        ArrDescItem descItemDeleted = descriptionItemService
                .deleteDescriptionItem(descItemVO.getDescItemObjectId(), nodeVersion, nodeId, fundVersionId, false);

        DescItemResult descItemResult = new DescItemResult();
        descItemResult.setItem(null);
        descItemResult.setParent(ArrNodeVO.valueOf(descItemDeleted.getNode()));

        return descItemResult;
    }

    /**
     * Stažení CSV souboru z hodnoty atributu.
     *
     * @param response         response
     * @param fundVersionId    verze souboru
     * @param descItemObjectId object id atributu
     * @throws IOException
     */
    @RequestMapping(value = "/descItems/{fundVersionId}/csv/export",
            method = RequestMethod.GET,
            produces = "text/csv")
    @Transactional
    public void descItemCsvExport(
            final HttpServletResponse response,
            @PathVariable(value = "fundVersionId") final Integer fundVersionId,
            @RequestParam(value = "descItemObjectId") final Integer descItemObjectId) throws IOException {
        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(descItemObjectId, "Nebyl vyplněn jednoznačný identifikátor descItem");

        ArrDescItem descItem = descItemRepository.findOpenDescItem(descItemObjectId);
        if (!"JSON_TABLE".equals(descItem.getItemType().getDataType().getCode())) {
            throw new SystemException("Pouze typ JSON_TABLE může být exportován pomocí CSV.", BaseCode.PROPERTY_HAS_INVALID_TYPE)
                    .set("property", "descItemObjectId")
                    .set("expected", "JSON_TABLE")
                    .set("actual", descItem.getItemType().getDataType().getCode());
        }

        OutputStream os = response.getOutputStream();
        FileDownload.addContentDispositionAsAttachment(response, "desc-item-" + descItemObjectId + ".csv");
        arrIOService.csvExport(descItem, os);
        os.close();
    }

    /**
     * Stažení CSV souboru z hodnoty atributu.
     *
     * @param response         response
     * @param fundVersionId    verze souboru
     * @param descItemObjectId object id atributu
     * @throws IOException
     */
    @RequestMapping(value = "/outputItems/{fundVersionId}/csv/export",
            method = RequestMethod.GET,
            produces = "text/csv")
    @Transactional
    public void outputItemCsvExport(
            final HttpServletResponse response,
            @PathVariable(value = "fundVersionId") final Integer fundVersionId,
            @RequestParam(value = "descItemObjectId") final Integer descItemObjectId) throws IOException {
        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(descItemObjectId, "Nebyl vyplněn jednoznačný identifikátor descItem");

        ArrOutputItem outputItem = outputItemRepository.findOpenOutputItem(descItemObjectId);
        if (!"JSON_TABLE".equals(outputItem.getItemType().getDataType().getCode())) {
            throw new SystemException("Pouze typ JSON_TABLE může být exportován pomocí CSV.", BaseCode.PROPERTY_HAS_INVALID_TYPE)
                    .set("property", "descItemObjectId")
                    .set("expected", "JSON_TABLE")
                    .set("actual", outputItem.getItemType().getDataType().getCode());
        }

        //outputItem = itemService.loadData(outputItem);
        OutputStream os = response.getOutputStream();
        FileDownload.addContentDispositionAsAttachment(response, "output-item-" + descItemObjectId + ".csv");
        arrIOService.csvExport(outputItem, os);
        os.close();
    }

    /**
     * Import CSV souboru, založí se nová hodnota s obsahem souboru.
     *
     * @param fundVersionId  verze souboru
     * @param nodeVersion    verze node
     * @param nodeId         id node
     * @param descItemTypeId id typu atributu
     * @param importFile     soubor soubor pro import
     * @throws IOException chyba
     */
    @Transactional
    @RequestMapping(value = "/descItems/{fundVersionId}/csv/import",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DescItemResult descItemCsvImport(
            @PathVariable(value = "fundVersionId") final Integer fundVersionId,
            @RequestParam(value = "nodeVersion") final Integer nodeVersion,
            @RequestParam(value = "nodeId") final Integer nodeId,
            @RequestParam(value = "descItemTypeId", required = false) final Integer descItemTypeId,
            @RequestParam(value = "file") final MultipartFile importFile) throws IOException {
        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(nodeVersion, "Nebyla vyplněna verze JP");
        Assert.notNull(descItemTypeId, "Nebyl vyplněn identifikátor typu atributu");

        InputStream is = importFile.getInputStream();
        ArrDescItem descItemCreated = arrIOService.csvDescImport(fundVersionId, nodeId, nodeVersion, descItemTypeId, is);
        is.close();

        DescItemResult descItemResult = new DescItemResult();
        descItemResult.setItem(factoryVo.createItem(descItemCreated));
        descItemResult.setParent(ArrNodeVO.valueOf(descItemCreated.getNode()));
        return descItemResult;
    }

    /**
     * Import CSV souboru, založí se nová hodnota s obsahem souboru.
     *
     * @param fundVersionId  verze souboru
     * @param outputVersion  verze výstupu
     * @param outputId       id výstupu
     * @param descItemTypeId id typu atributu
     * @param importFile     soubor soubor pro import
     * @throws IOException chyba
     */
    @Transactional
    @RequestMapping(value = "/outputItems/{fundVersionId}/csv/import",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OutputItemResult outputItemCsvImport(
            @PathVariable(value = "fundVersionId") final Integer fundVersionId,
            @RequestParam(value = "outputVersion") final Integer outputVersion,
            @RequestParam(value = "outputId", required = false) final Integer outputId,
            @RequestParam(value = "descItemTypeId", required = false) final Integer descItemTypeId,
            @RequestParam(value = "file") final MultipartFile importFile) throws IOException {
        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(outputVersion, "Verze definice výstupu musí být vyplněna");
        Assert.notNull(descItemTypeId, "Nebyl vyplněn identifikátor typu atributu");

        InputStream is = importFile.getInputStream();
        ArrOutputItem outputItemCreated = arrIOService.csvOutputImport(fundVersionId, outputId, outputVersion, descItemTypeId, is);
        is.close();

        OutputItemResult outputItemResult = new OutputItemResult();
        outputItemResult.setItem(factoryVo.createItem(outputItemCreated));
        outputItemResult.setParent(factoryVo.createOutput(outputItemCreated.getOutput()));
        return outputItemResult;
    }

    /**
     * Aktualizace hodnoty atributu.
     *
     * @param descItemVO       hodnota atributu
     * @param fundVersionId    identfikátor verze AP
     * @param nodeVersion      verze JP
     * @param createNewVersion vytvořit novou verzi?
     */
    @Transactional
    @RequestMapping(value = "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/update/{createNewVersion}",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescItemResult updateDescItem(@RequestBody final ArrItemVO descItemVO,
                                         @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                         @PathVariable(value = "nodeId") final Integer nodeId,
                                         @PathVariable(value = "nodeVersion") final Integer nodeVersion,
                                         @PathVariable(value = "createNewVersion") final Boolean createNewVersion) {
        Validate.notNull(descItemVO, "Hodnota atributu musí být vyplněna");
        Validate.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Validate.notNull(nodeId, "Nebyl vyplněn identifikátor JP");
        Validate.notNull(nodeVersion, "Nebyla vyplněna verze JP");
        Validate.notNull(createNewVersion, "Vytvořit novou verzi musí být vyplněno");

        return formService.updateDescItem(fundVersionId, nodeId, nodeVersion, descItemVO, createNewVersion.booleanValue());
    }

    /**
     * Nastavení atributu na "Nezjištěno".
     *
     * @param fundVersionId    id archivního souboru
     * @param nodeId           id JP
     * @param nodeVersion      verze JP
     * @param descItemTypeId   identfikátor typu hodnoty atributu
     * @param descItemSpecId   identfikátor specifikace hodnoty atributu
     * @param descItemObjectId identifikátor existující hodnoty atributu
     * @return upravená hodnota atributu nastavená na nezjištěno
     */
    @Transactional
    @RequestMapping(value = "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/notUndefined/set",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescItemResult setNotIdentifiedDescItem(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                   @PathVariable(value = "nodeId") final Integer nodeId,
                                                   @PathVariable(value = "nodeVersion") final Integer nodeVersion,
                                                   @RequestParam(value = "descItemTypeId") final Integer descItemTypeId,
                                                   @RequestParam(value = "descItemSpecId", required = false) final Integer descItemSpecId,
                                                   @RequestParam(value = "descItemObjectId", required = false) final Integer descItemObjectId) {
        ArrDescItem descItemUpdated = descriptionItemService
                .setNotIdentifiedDescItem(descItemTypeId, nodeId, nodeVersion, fundVersionId, descItemSpecId, descItemObjectId);

        DescItemResult descItemResult = new DescItemResult();
        descItemResult.setItem(factoryVo.createItem(descItemUpdated));
        descItemResult.setParent(ArrNodeVO.valueOf(descItemUpdated.getNode()));

        return descItemResult;
    }

    /**
     * Zrušení nastavení atributu na "Nezjištěno".
     *
     * @param fundVersionId    id archivního souboru
     * @param nodeId           id JP
     * @param nodeVersion      verze JP
     * @param descItemTypeId   identfikátor typu hodnoty atributu
     * @param descItemSpecId   identfikátor specifikace hodnoty atributu
     * @param descItemObjectId identifikátor existující hodnoty atributu
     * @return odstraněný atribut
     */
    @Transactional
    @RequestMapping(value = "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/notUndefined/unset",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescItemResult unsetNotIdentifiedDescItem(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                     @PathVariable(value = "nodeId") final Integer nodeId,
                                                     @PathVariable(value = "nodeVersion") final Integer nodeVersion,
                                                     @RequestParam(value = "descItemTypeId") final Integer descItemTypeId,
                                                     @RequestParam(value = "descItemSpecId", required = false) final Integer descItemSpecId,
                                                     @RequestParam(value = "descItemObjectId", required = false) final Integer descItemObjectId) {
        ArrDescItem descItemDeleted = descriptionItemService
                .deleteDescriptionItem(descItemObjectId, nodeVersion, nodeId, fundVersionId, false);
        DescItemResult descItemResult = new DescItemResult();
        descItemResult.setItem(null);
        descItemResult.setParent(ArrNodeVO.valueOf(descItemDeleted.getNode()));
        return descItemResult;
    }

    /**
     * Nastavení atributu na "Nezjištěno".
     *
     * @param fundVersionId      id archivního souboru
     * @param outputId           identifikátor výstupu
     * @param outputVersion      verze výstupu
     * @param outputItemTypeId   dentfikátor typu hodnoty atributu
     * @param outputItemSpecId   identfikátor specifikace hodnoty atributu
     * @param outputItemObjectId identifikátor existující hodnoty atributu
     * @return upravená hodnota atributu nastavená na nezjištěno
     */
    @Transactional
    @RequestMapping(value = "/outputItems/{fundVersionId}/{outputId}/{outputVersion}/notUndefined/set",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OutputItemResult setNotIdentifiedOutputItem(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                       @PathVariable(value = "outputId") final Integer outputId,
                                                       @PathVariable(value = "outputVersion") final Integer outputVersion,
                                                       @RequestParam(value = "outputItemTypeId") final Integer outputItemTypeId,
                                                       @RequestParam(value = "outputItemSpecId", required = false) final Integer outputItemSpecId,
                                                       @RequestParam(value = "outputItemObjectId", required = false) final Integer outputItemObjectId) {
        ArrOutputItem outputItemUpdated = outputService
                .setNotIdentifiedDescItem(outputItemTypeId, outputId, outputVersion, fundVersionId, outputItemSpecId, outputItemObjectId);
        OutputItemResult outputItemResult = new OutputItemResult();
        outputItemResult.setItem(factoryVo.createItem(outputItemUpdated));
        outputItemResult.setParent(factoryVo.createOutput(outputItemUpdated.getOutput()));
        return outputItemResult;
    }


    /**
     * Zrušení nastavení atributu na "Nezjištěno".
     *
     * @param fundVersionId      id archivního souboru
     * @param outputId           identifikátor výstupu
     * @param outputVersion      verze výstupu
     * @param outputItemTypeId   dentfikátor typu hodnoty atributu
     * @param outputItemSpecId   identfikátor specifikace hodnoty atributu
     * @param outputItemObjectId identifikátor existující hodnoty atributu
     * @return odstraněný atribut
     */
    @Transactional
    @RequestMapping(value = "/outputItems/{fundVersionId}/{outputId}/{outputVersion}/notUndefined/unset",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OutputItemResult unsetNotIdentifiedOutputItem(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                                         @PathVariable(value = "outputId") final Integer outputId,
                                                         @PathVariable(value = "outputVersion") final Integer outputVersion,
                                                         @RequestParam(value = "outputItemTypeId") final Integer outputItemTypeId,
                                                         @RequestParam(value = "outputItemSpecId", required = false) final Integer outputItemSpecId,
                                                         @RequestParam(value = "outputItemObjectId", required = false) final Integer outputItemObjectId) {
        ArrOutputItem descItemDeleted = outputService
                .deleteOutputItem(outputItemObjectId, outputVersion, fundVersionId);
        OutputItemResult outputItemResult = new OutputItemResult();
        outputItemResult.setItem(null);
        outputItemResult.setParent(factoryVo.createOutput(descItemDeleted.getOutput()));
        return outputItemResult;
    }

    /**
     * Vytvoření hodnoty atributu.
     *
     * @param descItemVO     hodnota atributu
     * @param fundVersionId  identfikátor verze AP
     * @param descItemTypeId identfikátor typu hodnoty atributu
     * @param nodeId         identfikátor JP
     * @param nodeVersion    verze JP
     * @return hodnota atributu
     */
    @Transactional
    @RequestMapping(value = "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/{descItemTypeId}/create",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescItemResult createDescItem(@RequestBody final ArrItemVO descItemVO,
                                         @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                         @PathVariable(value = "descItemTypeId") final Integer descItemTypeId,
                                         @PathVariable(value = "nodeId") final Integer nodeId,
                                         @PathVariable(value = "nodeVersion") final Integer nodeVersion) {
        Assert.notNull(descItemVO, "Hodnota atributu musí být vyplněna");
        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(descItemTypeId, "Nebyl vyplněn identifikátor typu atributu");
        Assert.notNull(nodeId, "Nebyl vyplněn identifikátor JP");
        Assert.notNull(nodeVersion, "Nebyla vyplněna verze JP");
        ArrDescItem descItem = factoryDO.createDescItem(descItemVO, descItemTypeId);

        ArrDescItem descItemCreated = descriptionItemService.createDescriptionItem(descItem, nodeId, nodeVersion,
                fundVersionId);

        DescItemResult descItemResult = new DescItemResult();
        descItemResult.setItem(factoryVo.createItem(descItemCreated));
        descItemResult.setParent(ArrNodeVO.valueOf(descItemCreated.getNode()));

        return descItemResult;
    }

    @Transactional
    @RequestMapping(value = "/levels/copy/validate", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ValidateResult copyLevelsValidate(@RequestBody final CopyNodesValidate copyNodesValidate) {
        Assert.notNull(copyNodesValidate, "Neplatná struktura");
        Integer sourceFundVersionId = copyNodesValidate.getSourceFundVersionId();
        Integer targetFundVersionId = copyNodesValidate.getTargetFundVersionId();
        Assert.notNull(sourceFundVersionId, "Neplatný identifikátor zdrojové verze AS");
        Assert.notEmpty(copyNodesValidate.getSourceNodes(), "Musí být vybrána alespoň jedna cílová JP");
        Assert.notNull(targetFundVersionId, "Neplatný identifikátor cílové verze AS");

        ArrFundVersion sourceFundVersion = arrangementService.getFundVersion(sourceFundVersionId);
        ArrFundVersion targetFundVersion = arrangementService.getFundVersion(targetFundVersionId);

        List<ArrNode> sourceNodes = factoryDO.createNodes(copyNodesValidate.getSourceNodes());

        ImportFromFund importFromFund = importNodesFromSource.createImportFromFund();
        importFromFund.init(sourceNodes, copyNodesValidate.isIgnoreRootNodes());

        return importNodesFromSource.validateData(importFromFund, targetFundVersion);
    }

    @Transactional
    @RequestMapping(value = "/levels/copy", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void copyLevels(@RequestBody final CopyNodesParams copyNodesParams) {
         Integer targetFundVersionId = copyNodesParams.getTargetFundVersionId();
        Assert.notNull(copyNodesParams, "Neplatná struktura");
        Assert.notNull(copyNodesParams.getSourceFundVersionId(), "Neplatný identifikátor zdrojové verze AS");
        Assert.notEmpty(copyNodesParams.getSourceNodes(), "Musí být vybrána alespoň jedna cílová JP");
        Assert.notNull(targetFundVersionId, "Neplatný identifikátor cílové verze AS");
        Assert.notNull(copyNodesParams.getTargetStaticNode(), "Neplatná cílová JP");
        Assert.notNull(copyNodesParams.getSelectedDirection(), "Neplatný směr vložení");

        ArrFundVersion targetFundVersion = arrangementService.getFundVersion(targetFundVersionId);

        ArrNode targetStaticNode = factoryDO.createNode(copyNodesParams.getTargetStaticNode());
        ArrNode targetStaticParentNode = copyNodesParams.getTargetStaticNodeParent() == null ? null : factoryDO
                .createNode(copyNodesParams.getTargetStaticNodeParent());

        List<ArrNode> sourceNodes = factoryDO.createNodes(copyNodesParams.getSourceNodes());


        ImportFromFund importFromFund = importNodesFromSource.createImportFromFund();
        importFromFund.init(sourceNodes, copyNodesParams.isIgnoreRootNodes());

        importNodesFromSource.importData(importFromFund, new ImportParams() {
            @Override
            public ConflictResolve getFileConflictResolve() {
                cz.tacr.elza.controller.vo.ConflictResolve fileResolveType = copyNodesParams.getFilesConflictResolve();
                if (fileResolveType != null) {
                    String name = fileResolveType.name();
                    return ConflictResolve.valueOf(name);
                } else {
                    return ConflictResolve.USE_TARGET;
                }
            }

            @Override
            public ConflictResolve getStructuredConflictResolve() {
                cz.tacr.elza.controller.vo.ConflictResolve packetResolveType = copyNodesParams
                        .getStructuredsConflictResolve();
                if (packetResolveType != null) {
                    String name = packetResolveType.name();
                    return ConflictResolve.valueOf(name);
                } else {
                    return ConflictResolve.COPY_AND_RENAME;
                }
            }
        }, targetFundVersion, targetStaticNode, targetStaticParentNode, copyNodesParams.getSelectedDirection());
    }

    @Transactional
    @RequestMapping(value = "/outputItems/{fundVersionId}/{outputVersion}/delete",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OutputItemResult deleteOutputItem(@RequestBody final ArrItemVO outputItemVO,
                                             @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                             @PathVariable(value = "outputVersion") final Integer outputVersion) {
        Validate.notNull(outputItemVO, "Výstup musí být vyplněn");
        Validate.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Validate.notNull(outputVersion, "Verze definice výstupu musí být vyplněna");

        ArrOutputItem outputItemDeleted = outputService
                .deleteOutputItem(outputItemVO.getDescItemObjectId(), outputVersion, fundVersionId);

        OutputItemResult outputItemResult = new OutputItemResult();
        outputItemResult.setItem(null);
        outputItemResult.setParent(factoryVo.createOutput(outputItemDeleted.getOutput()));

        return outputItemResult;
    }

    @Transactional
    @RequestMapping(value = "/outputItems/{fundVersionId}/{outputId}/{outputVersion}/{itemTypeId}/create",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OutputItemResult createOutputItem(@RequestBody final ArrItemVO outputItemVO,
                                             @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                             @PathVariable(value = "itemTypeId") final Integer itemTypeId,
                                             @PathVariable(value = "outputId") final Integer outputId,
                                             @PathVariable(value = "outputVersion") final Integer outputVersion) {
        Assert.notNull(outputItemVO, "Výstup musí být vyplněn");
        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(itemTypeId, "Nebyl vyplněn identifikátor typu atributu");
        Assert.notNull(outputId, "Identifikátor výstupu musí být vyplněn");
        Assert.notNull(outputVersion, "Verze výstupu musí být vyplněna");

        ArrOutputItem outputItem = factoryDO.createOutputItem(outputItemVO, itemTypeId);

        ArrOutputItem outputItemCreated = outputService.createOutputItem(outputItem, outputId,
                outputVersion, fundVersionId);

        OutputItemResult outputItemResult = new OutputItemResult();
        outputItemResult.setItem(factoryVo.createItem(outputItemCreated));
        outputItemResult.setParent(factoryVo.createOutput(outputItemCreated.getOutput()));

        return outputItemResult;
    }

    @Transactional
    @RequestMapping(value = "/outputItems/{fundVersionId}/{outputVersion}/update/{createNewVersion}",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OutputItemResult updateOutputItem(@RequestBody final ArrItemVO outputItemVO,
                                             @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                             @PathVariable(value = "outputVersion") final Integer outputVersion,
                                             @PathVariable(value = "createNewVersion") final Boolean createNewVersion) {
        Assert.notNull(outputItemVO, "Výstup musí být vyplněn");
        Assert.notNull(fundVersionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(outputVersion, "Verze výstupu musí být vyplněna");
        Validate.isTrue(createNewVersion); // TODO: remove from API (update client)

        ArrOutputItem outputItem = factoryDO.createOutputItem(outputItemVO);

        ArrOutputItem outputItemUpdated = outputService.updateOutputItem(outputItem, outputVersion, fundVersionId);

        OutputItemResult outputItemResult = new OutputItemResult();
        outputItemResult.setItem(factoryVo.createItem(outputItemUpdated));
        outputItemResult.setParent(factoryVo.createOutput(outputItemUpdated.getOutput()));

        return outputItemResult;
    }

    /**
     * Přepnutí na automatickou/uživatelskou úpravu typu atributu.
     *
     * @param outputId      identifikátor výstupu
     * @param fundVersionId identfikátor verze AS
     * @param itemTypeId    identfikátor typu hodnoty atributu
     */
    @Transactional
    @RequestMapping(value = "/output/{outputId}/{fundVersionId}/{itemTypeId}/switch", method = RequestMethod.POST)
    public boolean switchOutputCalculating(@PathVariable(value = "outputId") final Integer outputId,
                                           @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                           @PathVariable(value = "itemTypeId") final Integer itemTypeId,
                                           @RequestParam(value = "strict", required = false, defaultValue = "false") final Boolean strict) {
        ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);
        ArrOutput output = outputService.getOutput(outputId);
        RulItemType itemType = itemTypeRepository.findById(itemTypeId)
                .orElseThrow(() -> new ObjectNotFoundException("Typ atributu neexistuje", BaseCode.ID_NOT_EXIST).setId(itemTypeId));

        return outputService.switchOutputCalculating(output, fundVersion, itemType, strict);
    }

    /**
     * Získání dat pro formulář.
     *
     * @param outputId      identifikátor výstupu
     * @param fundVersionId id verze stromu
     * @return formulář
     */
    @Transactional
    @RequestMapping(value = "/output/{outputId}/{fundVersionId}/form", method = RequestMethod.GET)
    public OutputFormDataNewVO getOutputFormData(@PathVariable(value = "outputId") final Integer outputId,
                                                 @PathVariable(value = "fundVersionId") final Integer fundVersionId) {
        Assert.notNull(fundVersionId, "Identifikátor verze musí být vyplněn");
        Assert.notNull(outputId, "Identifikátor výstupu musí být vyplněn");

        ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);

        ArrOutput output = outputService.getOutput(outputId);
        List<ArrOutputItem> outputItems = outputService.getOutputItems(fundVersion, output);

        List<RulItemTypeExt> itemTypes;
        try {
            itemTypes = ruleService.getOutputItemTypes(output);
        } catch (Exception e) {
            logger.error("Chyba při zpracování pravidel", e);
            itemTypes = new ArrayList<>();
        }

        List<RulItemTypeExt> hiddenItemTypes = outputService.findHiddenItemTypes(fundVersion, output, itemTypes, outputItems);

        Integer fundId = fundVersion.getFund().getFundId();
        String ruleCode = fundVersion.getRuleSet().getCode();

        ArrOutputVO outputVO = factoryVo.createOutput(output);
        //ArrOutputVO outputVO = factoryVo.createOutputExt(output, fundVersion);
        List<ArrItemVO> descItems = factoryVo.createItems(outputItems);
        List<ItemTypeLiteVO> itemTypeLites = factoryVo.createItemTypes(ruleCode, fundId, itemTypes);
        return new OutputFormDataNewVO(outputVO, descItems, itemTypeLites,
                hiddenItemTypes.stream().map(RulItemTypeExt::getItemTypeId).collect(toList()));
    }

    /**
     * Seznam AP.
     *
     * @param fulltext fulltext podle názvu a interního čísla AS
     * @param max      maximální počet záznamů
     * @return seznam AP
     */
    @RequestMapping(value = "/getFunds", method = RequestMethod.GET)
    @Transactional
    public FundListCountResult getFunds(@RequestParam(value = "fulltext", required = false) final String fulltext,
                                        @RequestParam(value = "max", required = false, defaultValue = "200") final Integer max,
                                        @RequestParam(value = "from", required = false, defaultValue = "0") final Integer from) {
        UserDetail userDetail = userService.getLoggedUserDetail();

        FilteredResult<ArrFund> funds;

        if (userDetail.hasPermission(UsrPermission.Permission.FUND_RD_ALL)) {
            // read all funds
            funds = fundRepository.findFunds(fulltext, null, from, max);
        } else {
            Integer userId = userDetail.getId();
            funds = fundRepository.findFundsWithPermissions(fulltext, null, from, max, userId);
        }

		/*
		List<ArrFundOpenVersion> funds = fundRepository.findByFulltext(fulltext, max, userId);
		int fundsCount = funds.size();
		if (fundsCount >= max) {
			// read real funds count
			fundsCount = fundRepository.findCountByFulltext(fulltext, userId);
		}*/

        List<ArrFund> fundList = funds.getList();

        List<ArrFundVO> fundVOList = new ArrayList<>(fundList.size());
        fundList.forEach(f -> {
            ArrFundVO fundVO = factoryVo.createFundVO(f.getFund(), true, userService.getLoggedUserDetail());
            //fundVO.setVersions(Arrays.asList(factoryVo.createFundVersion(f.getOpenVersion())));
            fundVOList.add(fundVO);
        });

        return new FundListCountResult(fundVOList, funds.getTotalCount());
    }

    /**
     * Načtení souboru na základě id.
     *
     * @param fundId id souboru
     * @return konkrétní AP
     */
    @RequestMapping(value = "/getFund/{fundId}", method = RequestMethod.GET)
    @Transactional
    public ArrFundVO getFund(@PathVariable("fundId") final Integer fundId) {
        ArrFund fund = arrangementService.getFund(fundId);
        return factoryVo.createFundVO(fund, true, userService.getLoggedUserDetail());
    }

    @RequestMapping(value = "/nodeInfo/{fundVersionId}/{nodeId}", method = RequestMethod.GET)
    @Transactional
    public ArrNodeExtendVO getNode(@PathVariable(value = "fundVersionId") final Integer fundVersionId, @PathVariable("nodeId") final Integer nodeId) {
        ArrNodeExtendVO node = levelTreeCacheService.getSimpleNode(fundVersionId, nodeId);
        return node;
    }

    /**
     * Smazání celého archivního souboru.
     *
     * @param fundId id archivního souboru
     * @throws DeleteFailedException Nelze smazat archivní soubor, pro který existuje alespoň jeden
     *                               výstup.
     */
    @RequestMapping(value = "/deleteFund/{fundId}", method = RequestMethod.DELETE)
    public void deleteFund(@PathVariable("fundId") final Integer fundId) throws DeleteFailedException {

        arrangementService.deleteFund(fundId);
    }

    /**
     * Smazání historie archivního souboru.
     *
     * @param fundId id archivního souboru
     */
    @RequestMapping(value = "/deleteFundHistory/{fundId}", method = RequestMethod.DELETE)
    public void deleteFundHistory(@PathVariable("fundId") final Integer fundId) throws DeleteFailedException {

        arrangementService.deleteFundHistory(fundId);
    }


    /**
     * Načte AS pro dané verze.
     *
     * @param idsParam id verzí
     * @return seznam AS, každá obsahuje pouze jednu verzi, jinak je vrácená víckrát
     */
    @RequestMapping(value = "/getVersions", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public List<ArrFundVO> getFundsByVersionIds(@RequestBody final IdsParam idsParam) {

        if (CollectionUtils.isEmpty(idsParam.getIds())) {
            return Collections.emptyList();
        }

        List<ArrFundVersion> versions = fundVersionRepository.findAllById(idsParam.getIds());

        UserDetail user = userService.getLoggedUserDetail();
        List<ArrFundVO> result = new LinkedList<>();
        for (ArrFundVersion version : versions) {
            ArrFundVO fund = factoryVo.createFundVO(version.getFund(), false, user);
            ArrFundVersionVO versionVo = factoryVo.createFundVersion(version, user);
            fund.setVersions(Collections.singletonList(versionVo));

            result.add(fund);
        }

        return result;
    }


    /**
     * Provede načtení stromu uzlů. Uzly mohou být rozbaleny.
     *
     * @param input vstupní data pro načtení
     * @return data stromu
     */
    @RequestMapping(value = "/fundTree", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public TreeData getFundTree(final @RequestBody FaTreeParam input) {
        Assert.notNull(input, "Vstupní data musí být vyplněny");
        Assert.notNull(input.getVersionId(), "Nebyl vyplněn identifikátor verze AS");

        return levelTreeCacheService
                .getFaTree(input.getVersionId(), input.getNodeId(), input.getExpandedIds(), input.getIncludeIds());
    }

    /**
     * Získání dat pro JP.
     *
     * @param param parametry dat, které chceme získat (formálář, sourozence, potomky, předky, ...)
     * @return požadovaná data
     */
    @RequestMapping(value = "/nodeData", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public NodeData getNodeData(final @RequestBody NodeDataParam param) {
        return levelTreeCacheService.getNodeData(param, userService.getLoggedUserDetail());
    }

    /**
     * Provede načtení požadovaných uzlů ze stromu.
     *
     * @param input vstupní data pro načtení
     * @return data stromu
     */
    @RequestMapping(value = "/fundTree/nodes", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<TreeNodeVO> getFundTreeNodes(final @RequestBody FaTreeNodesParam input) {
        Assert.notNull(input, "Vstupní data musí být vyplněny");
        Assert.notNull(input.getVersionId(), "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(input.getNodeIds(), "Nebyly vyplně¨ny identifikátoy JP");

        return levelTreeCacheService.getFaTreeNodes(input.getVersionId(), input.getNodeIds());
    }

    /**
     * Uzavře otevřenou verzi archivní pomůcky a otevře novou verzi.
     *
     * @param versionId verze, která se má uzavřít
     * @return nová verze archivní pomůcky
     * @throws ConcurrentUpdateException chyba při současné manipulaci s položkou více uživateli
     */
    @Transactional
    @RequestMapping(value = "/approveVersion", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFundVersionVO approveVersion(@RequestParam("versionId") final Integer versionId) {
        Assert.notNull(versionId, "Nebyl vyplněn identifikátor verze AS");

        ArrFundVersion version = arrangementService.getFundVersion(versionId);

        UserDetail user = userService.getLoggedUserDetail();
        ArrFundVersion nextVersion = arrangementService.approveVersion(version);
        return factoryVo.createFundVersion(nextVersion, user);
    }

    /**
     * Získání dat pro formulář.
     *
     * @param nodeId    identfikátor JP
     * @param versionId id verze stromu
     * @return formulář
     */
    @RequestMapping(value = "/nodes/{nodeId}/{versionId}/form", method = RequestMethod.GET)
    @Transactional
    public DescFormDataNewVO getNodeFormData(@PathVariable(value = "nodeId") final Integer nodeId,
                                             @PathVariable(value = "versionId") final Integer versionId) {
        Validate.notNull(versionId, "Identifikátor verze musí být vyplněn");
        Validate.notNull(nodeId, "Identifikátor uzlu musí být vyplněn");

        return formService.getNodeFormData(versionId, nodeId);
    }

    /**
     * Získání dat pro formuláře.
     *
     * @param nodeIds   identfikátory JP
     * @param versionId id verze stromu
     * @return formuláře
     */
    @RequestMapping(value = "/nodes/{versionId}/forms", method = RequestMethod.GET)
    @Transactional
    public NodeFormsDataVO getNodeFormsData(@RequestParam(value = "nodeIds") final Integer[] nodeIds,
                                            @PathVariable(value = "versionId") final Integer versionId) {
        Assert.notNull(versionId, "Identifikátor verze musí být vyplněn");
        Assert.notNull(nodeIds, "Identifikátory uzlů musí být vyplněny");

        Map<Integer, DescFormDataNewVO> forms = new HashMap<>();

        for (int i = 0; i < nodeIds.length; i++) {
            forms.put(nodeIds[i], getNodeFormData(nodeIds[i], versionId));
        }

        return new NodeFormsDataVO(forms);
    }

    /**
     * Získání dat formuláře pro JP a jeho okolí.
     *
     * @param nodeId    identfikátory JP
     * @param versionId id verze stromu
     * @param around    velikost okolí - počet před a za uvedeným uzlem
     * @return formuláře
     */
    @RequestMapping(value = "/nodes/{versionId}/{nodeId}/{around}/forms", method = RequestMethod.GET)
    @Transactional
    public NodeFormsDataVO getNodeWithAroundFormsData(@PathVariable(value = "versionId") final Integer versionId,
                                                      @PathVariable(value = "nodeId") final Integer nodeId,
                                                      @PathVariable(value = "around") final Integer around) {
        Assert.notNull(versionId, "Identifikátor verze musí být vyplněn");
        Assert.notNull(nodeId, "Identifikátor uzlu musí být vyplněn");
        Assert.notNull(around, "Velikost okolí musí být vyplněno");

        ArrFundVersion fundVersion = arrangementService.getFundVersion(versionId);
        ArrNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ObjectNotFoundException("JP neexistuje", BaseCode.ID_NOT_EXIST).setId(nodeId));

        Assert.notNull(fundVersion, "Verze AP neexistuje");
        Assert.notNull(node, "Uzel neexistuje");

        List<ArrNode> nodes = arrangementService.findSiblingsAroundOfNode(fundVersion, node, around);

        Map<Integer, DescFormDataNewVO> forms = new HashMap<>();

        for (ArrNode arrNode : nodes) {
            DescFormDataNewVO formData = formService.getNodeFormData(fundVersion, arrNode.getNodeId());
            forms.put(arrNode.getNodeId(), formData);
        }

        return new NodeFormsDataVO(forms);
    }

    /**
     * Načte číselník typů kalendářů.
     *
     * @return typy kalendářů
     */
    @RequestMapping(value = "/calendarTypes", method = RequestMethod.GET)
    public List<ArrCalendarTypeVO> getCalendarTypes() {
        List<ArrCalendarType> calendarTypes = calendarTypeRepository.findAll();
        return factoryVo.createCalendarTypes(calendarTypes);
    }

    /**
     * Přesun uzlů se stejným rodičem před jiný uzel.
     *
     * @param moveParam vstupní parametry
     */
    @Transactional
    @RequestMapping(value = "/moveLevelBefore", method = RequestMethod.PUT)
    public void moveLevelBefore(@RequestBody final LevelMoveParam moveParam) {
        Assert.notNull(moveParam, "Parametry přesunu musí být vyplněny");


        Integer fundVersionId = moveParam.getVersionId();
        ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);

        ArrNode staticNode = factoryDO.createNode(moveParam.getStaticNode());
        ArrNode staticNodeParent = factoryDO.createNode(moveParam.getStaticNodeParent());
        List<ArrNode> transportNodes = factoryDO.createNodes(moveParam.getTransportNodes());
        ArrNode transportNodeParent = factoryDO.createNode(moveParam.getTransportNodeParent());

        /*
        descriptionItemService.checkNodeWritePermission(fundVersionId, staticNodeParent.getNodeId(), staticNodeParent.getVersion());
        descriptionItemService.checkNodeWritePermission(fundVersionId, staticNode.getNodeId(), staticNode.getVersion());
        descriptionItemService.checkNodeWritePermission(fundVersionId, transportNodeParent.getNodeId(), transportNodeParent.getVersion());
        transportNodes.forEach(node -> descriptionItemService.checkNodeWritePermission(fundVersionId, node.getNodeId(), node.getVersion()));
        */

        fundLevelService.moveLevelsBefore(fundVersion, staticNode, staticNodeParent,
                transportNodes, transportNodeParent);
    }

    /**
     * Přesun uzlů se stejným rodičem za jiný uzel.
     *
     * @param moveParam vstupní parametry
     */
    @Transactional
    @RequestMapping(value = "/moveLevelAfter", method = RequestMethod.PUT)
    public void moveLevelAfter(@RequestBody final LevelMoveParam moveParam) {
        Assert.notNull(moveParam, "Parametry přesunu musí být vyplněny");


        Integer fundVersionId = moveParam.getVersionId();
        ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);

        ArrNode staticNode = factoryDO.createNode(moveParam.getStaticNode());
        ArrNode staticNodeParent = factoryDO.createNode(moveParam.getStaticNodeParent());
        List<ArrNode> transportNodes = factoryDO.createNodes(moveParam.getTransportNodes());
        ArrNode transportNodeParent = factoryDO.createNode(moveParam.getTransportNodeParent());

        /*
        descriptionItemService.checkNodeWritePermission(fundVersionId, staticNodeParent.getNodeId(), staticNodeParent.getVersion());
        descriptionItemService.checkNodeWritePermission(fundVersionId, staticNode.getNodeId(), staticNode.getVersion());
        descriptionItemService.checkNodeWritePermission(fundVersionId, transportNodeParent.getNodeId(), transportNodeParent.getVersion());
        transportNodes.forEach(node -> descriptionItemService.checkNodeWritePermission(fundVersionId, node.getNodeId(), node.getVersion()));
        */

        fundLevelService.moveLevelsAfter(fundVersion, staticNode, staticNodeParent,
                transportNodes, transportNodeParent);
    }


    /**
     * Přesun uzlů se stejným rodičem pod jiný uzel.
     *
     * @param moveParam vstupní parametry
     */
    @Transactional
    @RequestMapping(value = "/moveLevelUnder", method = RequestMethod.PUT)
    public void moveLevelUnder(@RequestBody final LevelMoveParam moveParam) {
        Assert.notNull(moveParam, "Parametry přesunu musí být vyplněny");

        Integer fundVersionId = moveParam.getVersionId();
        ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);

        ArrNode staticNode = factoryDO.createNode(moveParam.getStaticNode());
        List<ArrNode> transportNodes = factoryDO.createNodes(moveParam.getTransportNodes());
        ArrNode transportNodeParent = factoryDO.createNode(moveParam.getTransportNodeParent());

        /*
        descriptionItemService.checkNodeWritePermission(fundVersionId, staticNode.getNodeId(), staticNode.getVersion());
        descriptionItemService.checkNodeWritePermission(fundVersionId, transportNodeParent.getNodeId(), transportNodeParent.getVersion());
        transportNodes.forEach(node -> descriptionItemService.checkNodeWritePermission(fundVersionId, node.getNodeId(), node.getVersion()));
        */

        fundLevelService.moveLevelsUnder(fundVersion, staticNode,
                transportNodes, transportNodeParent);
    }

    /**
     * Vyhledá scénáře pro možné archivní pomůcky
     *
     * @param param vstupní parametry
     * @return List scénářů
     */
    @RequestMapping(value = "/scenarios", method = RequestMethod.POST)
    @Transactional
    public List<ScenarioOfNewLevelVO> getDescriptionItemTypesForNewLevel(
            @RequestParam(required = false, value = "withGroups") final Boolean withGroups,
            @RequestBody final DescriptionItemParam param) {

        Integer fundVersionId = param.getVersionId();
        ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);
        Validate.notNull(fundVersion, "Neplatná verze AP");

        Integer fundId = fundVersion.getFund().getFundId();
        String ruleCode = fundVersion.getRuleSet().getCode();
        ArrNodeVO nodeVo = param.getNode();
        Validate.notNull(nodeVo);

        return factoryVo.createScenarioOfNewLevelList(descriptionItemService
                .getDescriptionItemTypesForNewLevel(nodeVo.getId(), param.getDirection(),
                        param.getVersionId()), withGroups, ruleCode, fundId);
    }

    /**
     * Načte seznam uzlů podle jejich id.
     *
     * @param idsParam seznam id
     * @return seznam vo uzlů s danými id
     */
    @RequestMapping(value = "/nodes", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public List<TreeNodeVO> getNodes(@RequestBody final IdsParam idsParam) {
        Assert.notNull(idsParam.getVersionId(), "Nebyla zadána verze stromu.");

        List<Integer> nodeIds = idsParam.getIds();
        if (nodeIds.isEmpty()) {
            return Collections.emptyList();
        }

        return levelTreeCacheService.getNodesByIds(nodeIds, idsParam.getVersionId());
    }


    /**
     * Přidání uzlu do stromu.
     *
     * @param addLevelParam vstupní parametry
     * @return nový přidaný uzel
     */
    @Transactional
    @RequestMapping(value = "/levels", method = RequestMethod.PUT)
    public NodeWithParent addLevel(@RequestBody final AddLevelParam addLevelParam) {
        Assert.notNull(addLevelParam, "Parametry musí být vyplněny");
        Assert.notNull(addLevelParam.getVersionId(), "Nebyl vyplněn identifikátor verze AS");

        Assert.notNull(addLevelParam.getDirection(), "Směr musí být vyplněn");

        ArrFundVersion fundVersion = arrangementService.getFundVersion(addLevelParam.getVersionId());

        ArrNode staticNode = factoryDO.createNode(addLevelParam.getStaticNode());
        ArrNode staticParentNode = addLevelParam.getStaticNodeParent() == null ? null : factoryDO
                .createNode(addLevelParam.getStaticNodeParent());

        Set<RulItemType> descItemCopyTypes = new HashSet<>();
        if (CollectionUtils.isNotEmpty(addLevelParam.getDescItemCopyTypes())) {
            descItemCopyTypes.addAll(itemTypeRepository.findAllById(addLevelParam.getDescItemCopyTypes()));
        }


        ArrLevel newLevel = fundLevelService.addNewLevel(fundVersion, staticNode, staticParentNode,
                addLevelParam.getDirection(), addLevelParam.getScenarioName(),
                                                         descItemCopyTypes, null);

        if (CollectionUtils.isNotEmpty(addLevelParam.getCreateItems())) {
            UpdateDescItemsParam params = new UpdateDescItemsParam(
                    addLevelParam.getCreateItems(),
                    Collections.emptyList(),
                    Collections.emptyList());
            formService.updateDescItems(fundVersion.getFundVersionId(), newLevel.getNodeId(), newLevel.getNode().getVersion(), params, null);
        }

        Collection<TreeNodeVO> nodeClients = levelTreeCacheService
                .getNodesByIds(Arrays.asList(newLevel.getNodeParent().getNodeId()), fundVersion.getFundVersionId());
        Assert.notEmpty(nodeClients, "Kolekce JP nesmí být prázdná");
        return new NodeWithParent(ArrNodeVO.valueOf(newLevel.getNode()), nodeClients.iterator().next());
    }

    /**
     * Smazání uzlu.
     *
     * @param nodeParam vstupní parametry pro smazání
     */
    @Transactional
    @RequestMapping(value = "/levels", method = RequestMethod.DELETE)
    public NodeWithParent deleteLevel(@RequestBody final NodeParam nodeParam) {
        Assert.notNull(nodeParam, "Parametry JP musí být vyplněny");
        Assert.notNull(nodeParam.getVersionId(), "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(nodeParam.getStaticNode(), "Nebyla zvolena referenční JP");

        ArrNode deleteNode = factoryDO.createNode(nodeParam.getStaticNode());
        ArrNode deleteParent = nodeParam.getStaticNodeParent() == null ? null : factoryDO
                .createNode(nodeParam.getStaticNodeParent());

        ArrFundVersion fundVersion = arrangementService.getFundVersion(nodeParam.getVersionId());

        ArrLevel deleteLevel = fundLevelService.deleteLevel(fundVersion, deleteNode, deleteParent);

        Collection<TreeNodeVO> nodeClients = levelTreeCacheService
                .getNodesByIds(Arrays.asList(deleteLevel.getNodeParent().getNodeId()),
                        fundVersion.getFundVersionId());
        Assert.notEmpty(nodeClients, "Kolekce JP nesmí být prázdná");
        return new NodeWithParent(ArrNodeVO.valueOf(deleteLevel.getNode()), nodeClients.iterator().next());
    }

    /**
     * Provede zkopírování atributu daného typu ze staršího bratra uzlu.
     *
     * @param versionId      id verze stromu
     * @param descItemTypeId typ atributu, který chceme zkopírovat
     * @param nodeVO         uzel, na který nastavíme hodnoty ze staršího bratra
     * @return vytvořené hodnoty
     */
    @Transactional
    @RequestMapping(value = "/copyOlderSiblingAttribute", method = RequestMethod.PUT)
    public CopySiblingResult copyOlderSiblingAttribute(
            @RequestParam(required = true) final Integer versionId,
            @RequestParam(required = true) final Integer descItemTypeId,
            @RequestBody final ArrNodeVO nodeVO) {

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(versionId);
        RulItemType descItemType = itemTypeRepository.getOneCheckExist(descItemTypeId);

        ArrNode node = factoryDO.createNode(nodeVO);
        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_DESC_ITEM, node);
        ArrLevel level = arrangementService.lockNode(node, fundVersion, change);

        List<ArrDescItem> newDescItems = arrangementService.copyOlderSiblingAttribute(fundVersion, descItemType, level, change);

        RulDescItemTypeDescItemsVO descItemTypeVO = factoryVo.createDescItemTypeVO(descItemType);
        descItemTypeVO.setDescItems(factoryVo.createItems(newDescItems));

        ArrNodeVO resultNode = ArrNodeVO.valueOf(level.getNode());

        return new CopySiblingResult(resultNode, descItemTypeVO);
    }


    /**
     * Provede načtení stromu uzlů. Uzly mohou být rozbaleny.
     *
     * @param input vstupní data pro načtení
     * @return data stromu
     */
    @RequestMapping(value = "/fulltext", method = RequestMethod.POST)
    public List<TreeNodeFulltext> fulltext(final @RequestBody FaFulltextParam input) {
        Assert.notNull(input, "Vstupní data musí být vyplněny");
        Assert.notNull(input.getVersionId(), "Nebyl vyplněn identifikátor verze AS");

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(input.getVersionId());

        Set<Integer> nodeIds;
        List<SearchParam> searchParams = input.getSearchParams();
        if (CollectionUtils.isNotEmpty(searchParams)) {
            nodeIds = arrangementService.findNodeIdsBySearchParams(fundVersion, input.getNodeId(), searchParams,
                    input.getDepth());
        } else if (input.getLuceneQuery()) {
            nodeIds = arrangementService.findNodeIdsByLuceneQuery(fundVersion, input.getNodeId(), input.getSearchValue(),
                    input.getDepth());
        } else {
            nodeIds = arrangementService.findNodeIdsByFulltext(fundVersion, input.getNodeId(),
                    input.getSearchValue(), input.getDepth());
        }

        return arrangementService.createTreeNodeFulltextList(nodeIds, fundVersion);
    }


    /**
     * Seznam AS serazeny podle poctu vyhledanych JP.
     * Jsou zohlednena opravneni uzivatele k AS.
     * Vysledek vyhledavani je ulozeny v user session pro pouziti v {@link ArrangementController#fundFulltext(java.lang.Integer)}.
     *
     * @param input vstupni data pro fultextove vyhledavani
     * @return seznam AS razeny podle poctu vyhledanych JP
     * @see ArrangementController#fundFulltext(java.lang.Integer)
     */
    @RequestMapping(value = "/fundFulltext", method = RequestMethod.POST)
    public List<ArrFundFulltextResult> fundFulltext(final @RequestBody FulltextFundRequest input) {
        // vyhledáš, vrátíš seznam AS s počtem nalezených JP
        // + uložíš si do session uživatele Map<fundId, List<nodeId>

        UserDetail userDetail = userService.getLoggedUserDetail();

        List<ArrFund> fundList = fundRepository.findFundByFulltext(null, userDetail.hasPermission(UsrPermission.Permission.FUND_RD_ALL) ? null : userDetail.getId());

        if (fundList.isEmpty()) {
            return Collections.emptyList();
        }

        return arrangementService.findFundsByFulltext(input.getSearchValue(), fundList);
    }

    /**
     * Seznam uzlu daneho AS serazeny podle relevance pri vyhledani.
     * Seznam je vytazeny z user session viz {@link ArrangementController#fundFulltext(cz.tacr.elza.controller.vo.FulltextFundRequest)}.
     *
     * @param fundId ID uzlu
     * @return seznam uzlu daneho AS serazeny podle relevance pri vyhledani
     * @see ArrangementController#fundFulltext(cz.tacr.elza.controller.vo.FulltextFundRequest)
     */
    @RequestMapping(value = "/fundFulltext/{fundId}", method = RequestMethod.GET)
    public List<TreeNodeVO> fundFulltext(final @PathVariable(value = "fundId") Integer fundId) {
        // vybereš ze session seznam nodeId podle AS a vytvoří TreeNodeVO
        return arrangementService.getNodeListByFulltext(fundId);
    }

    /**
     * Validuje verzi archivní pomůcky a vrátí list chyb.
     * Pokud je počet chyb 0 pak předpokládáme že stav AP = OK
     *
     * @param versionId verze, která se má validovat
     * @return Objekt s listem (prvních 20) chyb
     */
    @RequestMapping(value = "/validateVersion/{versionId}/{showAll}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<VersionValidationItem> validateVersion(@PathVariable("versionId") final Integer versionId,
                                                       @PathVariable("showAll") final Boolean showAll) {
        Assert.notNull(versionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(showAll, "Parametr musí být vyplněn");

        ArrFundVersion fundVersion = arrangementService.getFundVersion(versionId);

        List<ArrNodeConformity> validationErrors = arrangementService.findConformityErrors(fundVersion, showAll);

        return arrangementService.createVersionValidationItems(validationErrors, fundVersion);
    }

    /**
     * Validuje verzi archivní pomůcky a vrátí počet chyb
     * Pokud je počet chyb 0 pak předpokládáme že stav AP = OK
     *
     * @param versionId verze, která se má validovat
     * @return počet chyb
     */
    @RequestMapping(value = "/validateVersionCount/{versionId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Integer validateVersionCount(@PathVariable("versionId") final Integer versionId) {
        Assert.notNull(versionId, "Nebyl vyplněn identifikátor verze AS");

        ArrFundVersion fundVersion = arrangementService.getFundVersion(versionId);

        return arrangementService.getVersionErrorCount(fundVersion);
    }


    /**
     * Provede filtraci uzlů podle filtru a uloží filtrované id do session.
     *
     * @param versionId id verze
     * @param filters   filtry
     * @return počet všech záznamů splňujících filtry
     */
    @RequestMapping(value = "/filterNodes/{versionId}", method = RequestMethod.PUT)
    public Integer filterNodes(@PathVariable("versionId") final Integer versionId,
                               @RequestBody(required = false) final Filters filters) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(versionId);
        List<DescItemTypeFilter> descItemFilters = factoryDO.createFilters(filters);
        return filterTreeService.filterData(fundVersion, descItemFilters, filters.getNodeId());
    }

    /**
     * Do filtrovaného seznamu načte hodnoty atributů a vrátí podstránku záznamů.
     *
     * @param versionId       id verze
     * @param page            číslo stránky, od 0
     * @param pageSize        velikost stránky
     * @param descItemTypeIds id typů atributů, které chceme načíst
     * @return mapa hodnot atributů nodeId -> descItemId -> value
     */
    @RequestMapping(value = "/getFilterNodes/{versionId}", method = RequestMethod.PUT)
    public List<FilterNode> getFilteredNodes(@PathVariable("versionId") final Integer versionId,
                                             @RequestParam("page") final Integer page,
                                             @RequestParam("pageSize") final Integer pageSize,
                                             @RequestBody final List<Integer> descItemTypeIds) {

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(versionId);

        return filterTreeService.getFilteredData(fundVersion, page, pageSize, descItemTypeIds, false);
    }

    /**
     * Export dat z tabulkového zobrazení.
     *
     * @param response       http response
     * @param versionId      id verze archivního souboru
     * @param exportType     typ exportu
     * @param rulItemTypeIds id typů atributů v pořadí v jakém se mají exportovat
     * @throws IOException chyba při zápisu dat
     */
    @RequestMapping(value = "/dataGrid/export/{versionId}/{exportType}", method = RequestMethod.GET)
    public void exportDataGrid(final HttpServletResponse response,
                               @PathVariable("versionId") final Integer versionId,
                               @PathVariable("exportType") final DataGridExportType exportType,
                               @RequestParam("rulItemTypeIds") final List<Integer> rulItemTypeIds) throws IOException {
        Assert.notNull(versionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(exportType, "Nebyl vyplněn typ exportu");

        switch (exportType) {
            case DATA:
                arrIOService.dataGridDataExport(response, versionId, rulItemTypeIds);
                break;
            case TABLE:
                arrIOService.dataGridTableExport(response, versionId, rulItemTypeIds);
                break;
            default:
                throw new IllegalStateException("Neznámý typ exportu " + exportType);
        }
    }

    /**
     * Ve filtrovaném seznamu najde uzly podle fulltextu. Vrací seřazený seznam uzlů podle jejich indexu v seznamu
     * všech
     * filtrovaných uzlů.
     *
     * @param versionId id verze stromu
     * @return seznam uzlů a jejich indexu v seznamu filtrovaných uzlů, seřazené podle indexu
     */
    @RequestMapping(value = "/getFilteredFulltext/{versionId}", method = RequestMethod.POST)
    @Transactional
    public List<FilterNodePosition> getFilteredFulltextNodes(@PathVariable("versionId") final Integer versionId,
                                                             @RequestBody final FaFilteredFulltextParam param) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(versionId);

        return filterTreeService.getFilteredFulltextIds(fundVersion, param.getFulltext(), param.getLuceneQuery(), param.getSearchParams());
    }

    /**
     * Získání unikátních hodnot atributů podle typu.
     *
     * @param versionId      verze stromu
     * @param descItemTypeId id typu atributu
     * @param fulltext       fultextové hledání
     * @param max            maximální počet záznamů
     * @param specIds        id specifikací / id typů atributů
     * @return seznam unikátních hodnot
     */
    @RequestMapping(value = "/filterUniqueValues/{versionId}", method = RequestMethod.PUT)
    @Transactional
    public List<String> filterUniqueValues(@PathVariable("versionId") final Integer versionId,
                                           @RequestParam("descItemTypeId") final Integer descItemTypeId,
                                           @RequestParam(value = "fulltext", required = false) final String fulltext,
                                           @RequestParam(value = "max", required = true) final Integer max,
                                           @RequestBody(required = false) final Set<Integer> specIds) {

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(versionId);
        RulItemType descItemType = ruleService.getItemTypeById(descItemTypeId);

        return filterTreeService.filterUniqueValues(fundVersion, descItemType, specIds, fulltext, max);
    }

    /**
     * Získání unikátních specifikací atributů podle typu.
     *
     * @param fundVersionId verze stromu
     * @param itemTypeId    id typu atributu
     * @return seznam unikátních hodnot
     */
    @RequestMapping(value = "/findUniqueSpecIds/{fundVersionId}", method = RequestMethod.POST)
    public List<Integer> findUniqueSpecIds(@PathVariable("fundVersionId") final Integer fundVersionId,
                                           @RequestParam("itemTypeId") final Integer itemTypeId,
                                           @RequestBody final Filters filters) {

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        RulItemType descItemType = ruleService.getItemTypeById(itemTypeId);
        List<DescItemTypeFilter> descItemFilters = factoryDO.createFilters(filters);
        List<Integer> specIds = filterTreeService.findUniqueSpecIds(fundVersion, descItemType, descItemFilters, filters.getNodeId());
        specIds.add(null); // pro "Prázdné" položky
        return specIds;
    }

    /**
     * Nahrazení textu v hodnotách textových atributů.
     *
     * @param versionId       id verze stromu
     * @param descItemTypeId  typ atributu
     * @param searchText      hledaný text v atributu
     * @param replaceText     text, který nahradí hledaný text v celém textu
     * @param replaceDataBody seznam uzlů, ve kterých hledáme a seznam specifikací
     */
    @Transactional
    @RequestMapping(value = "/replaceDataValues/{versionId}", method = RequestMethod.PUT)
    public void replaceDataValues(@PathVariable("versionId") final Integer versionId,
                                  @RequestParam("descItemTypeId") final Integer descItemTypeId,
                                  @RequestParam("searchText") final String searchText,
                                  @RequestParam("replaceText") final String replaceText,
                                  @RequestBody final ReplaceDataBody replaceDataBody) {

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(versionId);
        RulItemType descItemType = ruleService.getItemTypeById(descItemTypeId);

        replaceDataBody.getNodes()
                .forEach(node -> descriptionItemService.checkNodeWritePermission(versionId, node.getId(), node.getVersion()));

        Set<ArrNode> nodesDO = new HashSet<>(factoryDO.createNodes(replaceDataBody.getNodes()));

        Set<RulItemSpec> specifications =
                CollectionUtils.isEmpty(replaceDataBody.getSpecIds()) ? null :
                        new HashSet<>(itemSpecRepository.findAllById(replaceDataBody.getSpecIds()));

        descriptionItemService.replaceDescItemValues(fundVersion, descItemType, nodesDO, specifications, searchText, replaceText, replaceDataBody.getSelectionType() == SelectionType.FUND, false);
    }

    /**
     * Nastavení textu hodnotám atributu..
     *
     * @param versionId         id verze stromu
     * @param descItemTypeId    typ atributu
     * @param newDescItemSpecId pokud se jedná o atribut se specifikací -> id specifikace, která bude nastavena
     * @param text              text, který nahradí hledaný text v celém textu
     * @param replaceDataBody   seznam uzlů, ve kterých hledáme a seznam specifikací
     */
    @Transactional
    @RequestMapping(value = "/placeDataValues/{versionId}", method = RequestMethod.PUT)
    public void placeDataValues(@PathVariable("versionId") final Integer versionId,
                                @RequestParam("descItemTypeId") final Integer descItemTypeId,
                                @RequestParam(value = "newDescItemSpecId", required = false) final Integer newDescItemSpecId,
                                @RequestParam("text") final String text,
                                @RequestBody final ReplaceDataBody replaceDataBody) {

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(versionId);
        RulItemType descItemType = ruleService.getItemTypeById(descItemTypeId);

        replaceDataBody.getNodes()
                .forEach(node -> descriptionItemService.checkNodeWritePermission(versionId, node.getId(), node.getVersion()));

        Set<ArrNode> nodesDO = new HashSet<>(factoryDO.createNodes(replaceDataBody.getNodes()));

        RulItemSpec newDescItemSpec = newDescItemSpecId == null ? null : ruleService.getItemSpecById(newDescItemSpecId);
        Set<RulItemSpec> specifications = CollectionUtils.isEmpty(replaceDataBody.getSpecIds()) ? null :
                new HashSet<>(itemSpecRepository.findAllById(replaceDataBody.getSpecIds()));

        descriptionItemService
                .placeDescItemValues(fundVersion, descItemType, nodesDO, newDescItemSpec, specifications, text, replaceDataBody.getSelectionType() == SelectionType.FUND);
    }

    /**
     * Nastavit specifikaci hodnotám atributu.
     *
     * @param fundVersionId   id verze stromu
     * @param itemTypeId      typ atributu
     * @param replaceSpecId   id specifikace, která bude nastavena
     * @param replaceDataBody seznam uzlů, ve kterých hledáme a seznam specifikací
     */
    @Transactional
    @RequestMapping(value = "/setSpecification/{fundVersionId}", method = RequestMethod.PUT)
    public void setSpecification(@PathVariable("fundVersionId") final Integer fundVersionId,
                                 @RequestParam("itemTypeId") final Integer itemTypeId,
                                 @RequestParam("replaceSpecId") final Integer replaceSpecId,
                                 @RequestBody final ReplaceDataBody replaceDataBody) {

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        RulItemType descItemType = ruleService.getItemTypeById(itemTypeId);

        replaceDataBody.getNodes()
                .forEach(node -> descriptionItemService.checkNodeWritePermission(fundVersionId, node.getId(), node.getVersion()));

        Set<ArrNode> nodesDO = new HashSet<>(factoryDO.createNodes(replaceDataBody.getNodes()));

        RulItemSpec setSpecification = replaceSpecId == null ? null : ruleService.getItemSpecById(replaceSpecId);
        Set<RulItemSpec> specifications = CollectionUtils.isEmpty(replaceDataBody.getSpecIds()) ? null :
                new HashSet<>(itemSpecRepository.findAllById(replaceDataBody.getSpecIds()));

        descriptionItemService.setSpecification(fundVersion, descItemType, nodesDO,
                setSpecification, specifications, replaceDataBody.getSpecIds().contains(null),
                replaceDataBody.getSelectionType() == SelectionType.FUND);
    }

    /**
     * Smazání hodnot atributů daného typu pro vybrané uzly.
     *
     * @param versionId       id verze stromu
     * @param descItemTypeId  typ atributu
     * @param replaceDataBody seznam uzlů, ve kterých hledáme a seznam specifikací
     */
    @Transactional
    @RequestMapping(value = "/deleteDataValues/{versionId}", method = RequestMethod.PUT)
    public void deleteDataValues(@PathVariable("versionId") final Integer versionId,
                                 @RequestParam("descItemTypeId") final Integer descItemTypeId,
                                 @RequestBody final ReplaceDataBody replaceDataBody) {

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(versionId);
        RulItemType descItemType = ruleService.getItemTypeById(descItemTypeId);

        replaceDataBody.getNodes()
                .forEach(node -> descriptionItemService.checkNodeWritePermission(versionId, node.getId(), node.getVersion()));

        Set<ArrNode> nodesDO = new HashSet<>(factoryDO.createNodes(replaceDataBody.getNodes()));

        Set<RulItemSpec> specifications = CollectionUtils.isEmpty(replaceDataBody.getSpecIds()) ? null :
                new HashSet<>(itemSpecRepository.findAllById(replaceDataBody.getSpecIds()));

        descriptionItemService.deleteDescItemValues(fundVersion, descItemType, nodesDO, specifications, replaceDataBody.getSelectionType() == SelectionType.FUND);
    }

    @RequestMapping(value = "/validation/{fundVersionId}/{fromIndex}/{toIndex}", method = RequestMethod.GET)
    @Transactional
    public ValidationItems getValidation(@PathVariable("fundVersionId") final Integer fundVersionId,
                                         @PathVariable(value = "fromIndex") final Integer fromIndex,
                                         @PathVariable(value = "toIndex") final Integer toIndex) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        return arrangementService.getValidationNodes(fundVersion, fromIndex, toIndex);
    }

    @RequestMapping(value = "/validation/{fundVersionId}/find/{nodeId}/{direction}", method = RequestMethod.GET)
    @Transactional
    public ValidationItems findValidationError(@PathVariable("fundVersionId") final Integer fundVersionId,
                                               @PathVariable(value = "nodeId") final Integer nodeId,
                                               @PathVariable(value = "direction") final Integer direction) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        return arrangementService.findErrorNode(fundVersion, nodeId, direction);
    }

    @RequestMapping(value = "/fund/policy/{fundVersionId}", method = RequestMethod.GET)
    public List<NodeItemWithParent> getAllNodesVisiblePolicy(@PathVariable(value = "fundVersionId") final Integer fundVersionId) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        return policyService.getTreePolicy(fundVersion);
    }

    @RequestMapping(value = "/output/types/{versionId}", method = RequestMethod.GET)
    public List<RulOutputTypeVO> getOutputTypes(@PathVariable("versionId") final Integer versionId) {
        List<RulOutputType> outputTypes = outputService.getOutputTypes(versionId);
        return factoryVo.createOutputTypes(outputTypes);
    }


    /**
     * Načtení seznamu outputů - objekt outputu s vazbou na objekt named output.
     *
     * @param fundVersionId identfikátor verze AS
     * @return seznam outputů
     */
    @RequestMapping(value = "/output/{fundVersionId}", method = RequestMethod.GET)
    @Transactional
    public List<ArrOutputVO> getOutputs(@PathVariable(value = "fundVersionId") final Integer fundVersionId, @RequestParam(value = "state", required = false) final OutputState state) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        List<ArrOutput> outputs = state == null ? outputService.getSortedOutputs(fundVersion) : outputService.getSortedOutputsByState(fundVersion, state);
        return factoryVo.createOutputList(outputs);
    }

    /**
     * Načtení detailu outputu objekt output s vazbou na named output a seznamem připojených node.
     *
     * @param fundVersionId identfikátor verze AS
     * @param outputId      identifikátor výstupu
     * @return output
     */
    @RequestMapping(value = "/output/{fundVersionId}/{outputId}", method = RequestMethod.GET)
    @Transactional
    public ArrOutputVO getOutput(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                 @PathVariable(value = "outputId") final Integer outputId) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        ArrOutput output = outputService.getOutput(outputId);
        outputService.getOutput(fundVersion, output);
        return factoryVo.createOutputExt(output, fundVersion);
    }

    /**
     * Konfigurace generovaných výstupů
     *
     * @param outputId identifikátor výstupů
     */
    @RequestMapping(value = "/output/{outputId}/settings", method = RequestMethod.PUT)
    public void updateOutputSettings(@PathVariable(value = "outputId") final Integer outputId,
                                     @RequestBody final OutputSettingsVO outputSettings) throws JsonProcessingException {

        outputService.setOutputSettings(outputSettings, outputId);
    }

    @RequestMapping(value = "/output/generate/{outputId}", method = RequestMethod.GET)
    @Transactional
    public GenerateOutputResult generateOutput(@PathVariable(value = "outputId") int outputId,
                                               @RequestParam(value = "forced", defaultValue = "false") boolean forced) {
        ArrOutput output = outputService.getOutput(outputId);

        // get logged user
        UsrUser loggedUser = userService.getLoggedUser();
        Integer userId = loggedUser != null ? loggedUser.getUserId() : null;

        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());
        OutputRequestStatus requestStatus = outputService.addRequest(outputId, fundVersion, !forced, userId);

        GenerateOutputResult generateOutputResult = new GenerateOutputResult();
        generateOutputResult.setStatus(requestStatus);
        return generateOutputResult;
    }

    @RequestMapping(value = "/output/send/{outputId}", method = RequestMethod.GET)
    @Transactional
    public void sendOutput(@PathVariable(value = "outputId") int outputId) {

        ArrOutput output = outputService.getOutput(outputId);
        outputService.sendOutput(output);
    }

    /**
     * Vytvoření nového pojmenovaného výstupu.
     *
     * @param fundVersionId identfikátor verze AS
     * @param param         vstupní parametry pro vytvoření outputu
     * @return vytvořený výstup
     */
    @Transactional
    @RequestMapping(value = "/output/{fundVersionId}", method = RequestMethod.PUT)
    public ArrOutputVO createNamedOutput(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                         @RequestBody final OutputNameParam param) {
        Assert.notNull(param, "Vstupní data musí být vyplněny");
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        
        Set<Integer> templateIds = new HashSet<>();
        if(param.getTemplateId()!=null) {
        	templateIds.add(param.getTemplateId()); 
        }
        if(param.getTemplateIds()!=null) {
        	templateIds.addAll(param.getTemplateIds());
        }
        OutputData outputData = outputService.createOutput(fundVersion, param.getName(), param.getInternalCode(),
                param.getOutputTypeId(), templateIds);
        return factoryVo.createOutputExt(outputData.getOutput(), fundVersion);
    }

    /**
     * Přidání uzlů k výstupu.
     *
     * @param fundVersionId identfikátor verze AS
     * @param outputId      identifikátor výstupu
     * @param nodeIds       seznam přidáváných identifikátorů uzlů
     */
    @Transactional
    @RequestMapping(value = "/output/{fundVersionId}/{outputId}/add", method = RequestMethod.POST)
    public void addNodesNamedOutput(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                    @PathVariable(value = "outputId") final Integer outputId,
                                    @RequestBody final List<Integer> nodeIds) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        ArrOutput output = outputService.getOutput(outputId);
        outputService.addNodesNamedOutput(fundVersion, output, nodeIds);
    }

    /**
     * Odebrání uzlů u výstupu.
     *
     * @param fundVersionId identfikátor verze AS
     * @param outputId      identifikátor výstupu
     * @param nodeIds       seznam odebíraných identifikátorů uzlů
     */
    @Transactional
    @RequestMapping(value = "/output/{fundVersionId}/{outputId}/remove", method = RequestMethod.POST)
    public void removeNodesNamedOutput(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                       @PathVariable(value = "outputId") final Integer outputId,
                                       @RequestBody final List<Integer> nodeIds) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        ArrOutput output = outputService.getOutput(outputId);
        outputService.removeNodesNamedOutput(fundVersion, output, nodeIds);
    }

    /**
     * Smazání pojmenovaného výstupu.
     *
     * @param fundVersionId identfikátor verze AS
     * @param outputId      identifikátor výstupu
     */
    @Transactional
    @RequestMapping(value = "/output/{fundVersionId}/{outputId}", method = RequestMethod.DELETE)
    public void deleteNamedOutput(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                  @PathVariable(value = "outputId") final Integer outputId) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        ArrOutput output = outputService.getOutput(outputId);
        outputService.deleteNamedOutput(fundVersion, output);
    }

    /**
     * Vrácení stavu pojmenovaného výstupu do stavu otevřený.
     *
     * @param fundVersionId identfikátor verze AS
     * @param outputId      identifikátor výstupu
     */
    @Transactional
    @RequestMapping(value = "/output/{fundVersionId}/{outputId}/revert", method = RequestMethod.POST)
    public void revertToOpenState(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                  @PathVariable(value = "outputId") final Integer outputId) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        ArrOutput output = outputService.getOutput(outputId);
        outputService.revertToOpenState(fundVersion, output);
    }

    /**
     * Vytvoření kopie outputu
     *
     * @param fundVersionId identfikátor verze AS
     * @param outputId      identifikátor výstupu
     * @return kopie výstupu
     */
    @Transactional
    @RequestMapping(value = "/output/{fundVersionId}/{outputId}/clone", method = RequestMethod.POST)
    public ArrOutputVO cloneOutput(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                   @PathVariable(value = "outputId") final Integer outputId) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        ArrOutput output = outputService.getOutput(outputId);
        
        OutputData outputData = outputService.cloneOutput(fundVersion, output);
        
        return factoryVo.createOutputExt(outputData.getOutput(), fundVersion);
    }

    /**
     * Upravení výstupu.
     *
     * @param fundVersionId identfikátor verze AS
     * @param outputId      identfikátor výstupu
     * @param param         vstupní parametry pro úpravu outputu
     */
    @Transactional
    @RequestMapping(value = "/output/{fundVersionId}/{outputId}/update", method = RequestMethod.POST)
    public void updateNamedOutput(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                  @PathVariable(value = "outputId") final Integer outputId,
                                  @RequestBody final OutputNameParam param) {
        Assert.notNull(param, "Vstupní data musí být vyplněny");
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        ArrOutput output = outputService.getOutput(outputId);
        outputService.updateNamedOutput(fundVersion, output, param.getName(), param.getInternalCode(), param.getTemplateId(), param.getAnonymizedAp());
    }

    /**
     * Přidání omezujícího rejstříku k výstupu
     *
     * @param outputId identifikátor výstupu
     * @param scopeId identifikátor rejstříku
     */
    @Transactional
    @RequestMapping(value = "/output/{outputId}/restrict/{scopeId}", method = RequestMethod.PUT)
    public ArrOutputRestrictionScopeVO addRestrictedScope(@PathVariable(value = "outputId") final Integer outputId,
                                                          @PathVariable(value = "scopeId") final Integer scopeId) {
        return outputService.addRestrictedScope(outputId, scopeId);
    }

    /**
     * Odebrání omezujícího rejstříku z výstupu
     *
     * @param outputId identifikátor výstupu
     * @param scopeId identifikátor rejstříku
     */
    @Transactional
    @RequestMapping(value = "/output/{outputId}/restrict/{scopeId}", method = RequestMethod.DELETE)
    public void deleteRestrictedScope(@PathVariable(value = "outputId") final Integer outputId,
                                      @PathVariable(value = "scopeId") final Integer scopeId) {
        outputService.deleteRestrictedScope(outputId, scopeId);
    }

    /**
     * Přidání šablony k výstupu
     *
     * @param outputId identifikátor výstupu
     * @param templateId identifikátor šablony
     */
    @Transactional
    @RequestMapping(value = "/output/{outputId}/template/{templateId}", method = RequestMethod.PUT)
    public ArrOutputTemplateVO addOutputTemplate(@PathVariable(value = "outputId") final Integer outputId, 
    							  @PathVariable(value = "templateId") final Integer templateId) {
        ArrOutput output = outputService.getOutput(outputId);
    	return outputService.addOutputTemplate(output.getFundId(), output, templateId);
    }

    /**
     * Odebrání šablony z výstupu
     *
     * @param outputId identifikátor výstupu
     * @param templateId identifikátor šablony
     */
    @Transactional
    @RequestMapping(value = "/output/{outputId}/template/{templateId}", method = RequestMethod.DELETE)
    public void deleteOutputTemplate(@PathVariable(value = "outputId") final Integer outputId,
                                     @PathVariable(value = "templateId") final Integer templateId) {
    	ArrOutput output = outputService.getOutput(outputId);
    	outputService.deleteOutputTemplate(output.getFundId(), output, templateId);
    }

    /**
     * Vyhledání provedení změn nad AS, případně nad konkrétní JP z AS.
     *
     * @param fundVersionId identfikátor verze AS
     * @param maxSize       maximální počet záznamů
     * @param offset        počet přeskočených záznamů
     * @param changeId      identifikátor změny, vůči které chceme počítat offset (pokud není vyplněn, bere se vždy poslední)
     * @param nodeId        identifikátor JP u které vyhledáváme změny (pokud není vyplněn, vyhledává se přes celý AS)
     * @return výsledek hledání
     */
    @RequestMapping(value = "/changes/{fundVersionId}", method = RequestMethod.GET)
    @Transactional
    public ChangesResult findChanges(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                     @RequestParam(value = "maxSize", required = false, defaultValue = "20") final Integer maxSize,
                                     @RequestParam(value = "offset", required = false, defaultValue = "0") final Integer offset,
                                     @RequestParam(value = "changeId", required = false) final Integer changeId,
                                     @RequestParam(value = "nodeId", required = false) final Integer nodeId) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        if (fundVersion.getLockChange() != null) {
            throw new BusinessException("Nelze prováděn změny v uzavřené verzi", ArrangementCode.VERSION_ALREADY_CLOSED);
        }
        ArrChange change = null;
        if (changeId != null) {
            change = changeRepository.getOneCheckExist(changeId);
        }
        ArrNode node = null;
        if (nodeId != null) {
            node = nodeRepository.getOneCheckExist(nodeId);
        }
        return revertingChangesService.findChanges(fundVersion, node, maxSize, offset, change);
    }

    /**
     * Vyhledání provedení změn nad AS, případně nad konkrétní JP z AS.
     *
     * @param fundVersionId identfikátor verze AS
     * @param maxSize       maximální počet záznamů
     * @param fromDate      datum vůči kterému vyhledávám v seznamu (př. formátu query parametru: 2016-11-07T10:32:04)
     * @param changeId      identifikátor změny, vůči které chceme počítat offset (pokud není vyplněn, bere se vždy poslední)
     * @param nodeId        identifikátor JP u které vyhledáváme změny (pokud není vyplně, vyhledává se přes celý AS)
     * @return výsledek hledání
     */
    @RequestMapping(value = "/changes/{fundVersionId}/date", method = RequestMethod.GET)
    @Transactional
    public ChangesResult findChangesByDate(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                           @RequestParam(value = "maxSize", required = false, defaultValue = "20") final Integer maxSize,
                                           @RequestParam(value = "fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime fromDate,
                                           @RequestParam(value = "changeId") final Integer changeId,
                                           @RequestParam(value = "nodeId", required = false) final Integer nodeId) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        if (fundVersion.getLockChange() != null) {
            throw new BusinessException("Nelze prováděn změny v uzavřené verzi", ArrangementCode.VERSION_ALREADY_CLOSED);
        }
        ArrChange change = changeRepository.getOneCheckExist(changeId);
        ArrNode node = null;
        if (nodeId != null) {
            node = nodeRepository.getOneCheckExist(nodeId);
        }
        return revertingChangesService.findChangesByDate(fundVersion, node, maxSize, fromDate, change);
    }

    /**
     * Provede revertování AS / JP k požadovanému stavu.
     *
     * @param fundVersionId identfikátor verze AS
     * @param fromChangeId  identifikátor změny, vůči které provádíme revertování (od)
     * @param toChangeId    identifikátor změny, ke které provádíme revertování (do)
     * @param nodeId        identifikátor JP u které provádíme změny (pokud není vyplněn, revertuje se přes celý AS)
     */
    @RequestMapping(value = "/changes/{fundVersionId}/revert", method = RequestMethod.GET)
    @Transactional
    public void revertChanges(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                              @RequestParam(value = "fromChangeId") final Integer fromChangeId,
                              @RequestParam(value = "toChangeId") final Integer toChangeId,
                              @RequestParam(value = "nodeId", required = false) final Integer nodeId) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        if (fundVersion.getLockChange() != null) {
            throw new BusinessException("Nelze prováděn změny v uzavřené verzi", ArrangementCode.VERSION_ALREADY_CLOSED);
        }
        ArrChange fromChange = changeRepository.getOneCheckExist(fromChangeId);
        ArrChange toChange = changeRepository.getOneCheckExist(toChangeId);
        ArrNode node = null;
        if (nodeId != null) {
            node = nodeRepository.getOneCheckExist(nodeId);
        }
        revertingChangesService.revertChanges(fundVersion.getFund(), node, fromChange, toChange);
    }

    /**
     * Vytvoření požadavku nebo přidání JP k existujícímu požadavku.
     *
     * @param fundVersionId identifikátor verze AS
     * @param send          současně odeslat požadavek?
     * @param param         parametry požadavku
     */
    @RequestMapping(value = "/requests/{fundVersionId}/digitization/add", method = RequestMethod.POST)
    @Transactional
    public void digitizationRequestAdd(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                       @RequestParam(name = "send", defaultValue = "false") final Boolean send,
                                       @RequestBody final DigitizationRequestParam param) {
        Assert.notNull(param, "Vstupní data musí být vyplněny");
        Assert.notEmpty(param.nodeIds, "Musí být vyplněna alespoň jedna JP");

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        List<ArrNode> nodes = nodeRepository.findAllById(param.nodeIds);

        ArrDigitizationFrontdesk digitizationFrontdesk = externalSystemService.findDigitizationFrontdesk(param.digitizationFrontdeskId);

        if (nodes.size() != param.nodeIds.size()) {
            throw new SystemException("Neplatný počet nalezených jednotek popisu (" + nodes.size() + ", " + param.nodeIds.size() + ")", BaseCode.ID_NOT_EXIST);
        }

        ArrDigitizationRequest digitizationRequest;
        if (param.id == null) {
            digitizationRequest = requestService.createDigitizationRequest(nodes, digitizationFrontdesk, param.description, fundVersion);
        } else {
            digitizationRequest = requestService.getDigitizationRequest(param.id);
            requestService.addNodeDigitizationRequest(digitizationRequest, nodes, digitizationFrontdesk, fundVersion, param.getDescription());
        }

        if (BooleanUtils.isTrue(send)) {
            requestService.sendRequest(digitizationRequest, fundVersion);
        }
    }


    /**
     * Vytvoření požadavku nebo přidání DAO k existujícímu požadavku.
     *
     * @param fundVersionId identifikátor verze AS
     * @param send          současně odeslat požadavek?
     * @param param         parametry požadavku
     */
    @RequestMapping(value = "/requests/{fundVersionId}/dao/add", method = RequestMethod.POST)
    @Transactional
    public void daoRequestAdd(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                              @RequestParam(name = "send", defaultValue = "false") final Boolean send,
                              @RequestBody final DaoRequestParam param) {
        Assert.notNull(param, "Vstupní data musí být vyplněny");
        Assert.notNull(param.type, "Musí být vyplněn typ");
        Assert.notEmpty(param.daoIds, "Musí být vyplněno alespoň jedno DAO");

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        List<ArrDao> daos = daoRepository.findAllById(param.daoIds);

        if (daos.size() != param.daoIds.size()) {
            throw new SystemException("Neplatný počet nalezených digitalizátů (" + daos.size() + ", " + param.daoIds.size() + ")", BaseCode.ID_NOT_EXIST);
        }

        if (daos.stream().map(o -> o.getDaoPackage().getDigitalRepository().getExternalSystemId()).collect(toSet()).size() > 1) {
            throw new BusinessException("DAO musí mít stejná úložiště", ArrangementCode.INVALID_REQUEST_DIGITAL_REPOSITORY_DAO);
        }
        ArrDigitalRepository digitalRepository = daos.get(0).getDaoPackage().getDigitalRepository();

        ArrDaoRequest daoRequest;
        if (param.id == null) {
            daoRequest = requestService.createDaoRequest(daos, param.description, param.type, fundVersion, digitalRepository);
        } else {
            daoRequest = requestService.getDaoRequest(param.id);
            requestService.addDaoDaoRequest(daoRequest, daos, fundVersion, param.getDescription());
        }

        if (BooleanUtils.isTrue(send)) {
            requestService.sendRequest(daoRequest, fundVersion);
        }
    }

    /**
     * Odeslání požadavku.
     *
     * @param fundVersionId identifikátor verze AS
     * @param requestId     identifikátor požadavku
     */
    @RequestMapping(value = "/requests/{fundVersionId}/{requestId}/send", method = RequestMethod.POST)
    @Transactional
    public void requestSend(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                            @PathVariable(value = "requestId") final Integer requestId) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        ArrRequest request = requestService.getRequest(requestId);
        if (!fundVersion.getFundId().equals(request.getFund().getFundId())) {
            throw new SystemException("Neplatná verze AS (" + fundVersion.getFundVersionId() + ")", ArrangementCode.INVALID_VERSION);
        }
        requestService.sendRequest(request, fundVersion);
    }

    /**
     * Změna požadavku.
     *
     * @param fundVersionId identfikátor verze AS
     * @param param         parametry požadavku
     */
    @RequestMapping(value = "/requests/{fundVersionId}/{requestId}", method = RequestMethod.PUT)
    @Transactional
    public void requestChange(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                              @PathVariable(value = "requestId") final Integer digitizationId,
                              @RequestBody final DigitizationRequestParam param) {
        Assert.notNull(param, "Vstupní data musí být vyplněny");
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        ArrRequest request = requestService.getRequest(digitizationId);
        requestService.changeRequest(request, fundVersion, param.getDescription());
    }

    /**
     * Odebrání JP z požadavku.
     *
     * @param fundVersionId identfikátor verze AS
     * @param param         parametry požadavku
     */
    @RequestMapping(value = "/requests/{fundVersionId}/digitization/remove", method = RequestMethod.POST)
    @Transactional
    public void digitizationRequestRemove(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                          @RequestBody final DigitizationRequestParam param) {
        Assert.notNull(param, "Vstupní data musí být vyplněny");
        Assert.notNull(param.id, "Nebyl vyplněn identifikátor");
        Assert.notEmpty(param.nodeIds, "Musí být vyplněna alespoň jedna JP");

        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        List<ArrNode> nodes = nodeRepository.findAllById(param.nodeIds);

        if (nodes.size() != param.nodeIds.size()) {
            throw new SystemException("Neplatný počet nalezených jednotek popisu (" + nodes.size() + ", " + param.nodeIds.size() + ")", BaseCode.ID_NOT_EXIST);
        }

        ArrDigitizationRequest digitizationRequest = requestService.getDigitizationRequest(param.id);
        requestService.removeNodeDigitizationRequest(digitizationRequest, nodes, fundVersion);
    }

    /**
     * Vyhledání požadavků.
     *
     * @param fundVersionId identfikátor verze AS
     * @param state         stav požadavku
     * @param type          typ požadavku
     * @param detail        vyplnit detailní informace o požadavku?
     * @return seznam odpovídajících požadavků
     */
    @RequestMapping(value = "/requests/{fundVersionId}", method = RequestMethod.GET)
    @Transactional
    public List<ArrRequestVO> findRequests(@PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                           @RequestParam(value = "state", required = false) final ArrRequest.State state,
                                           @RequestParam(value = "type", required = false) final ArrRequest.ClassType type,
                                           @RequestParam(value = "detail", required = false, defaultValue = "false") final Boolean detail,
                                           @RequestParam(value = "description", required = false) final String description,
                                           @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime fromDate,
                                           @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime toDate,
                                           @RequestParam(value = "subType", required = false) final String subType) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        List<ArrRequest> requests = requestService.findRequests(fundVersion.getFund(), state, type, description, fromDate, toDate, subType);
        return factoryVo.createRequest(requests, detail, fundVersion);
    }


    @RequestMapping(value = "/requests/queued", method = RequestMethod.GET)
    @Transactional
    public List<ArrRequestQueueItemVO> findQueuedRequests() {
        List<ArrRequestQueueItem> requestQueueItems = requestQueueService.findQueued();
        return factoryVo.createRequestQueueItem(requestQueueItems);
    }


    /**
     * Získání konkrétního požadavku.
     *
     * @param fundVersionId identfikátor verze AS
     * @param requestId     identifikátor požadavku
     * @param detail        vyplnit detailní informace o požadavku?
     * @return nalezený požadavek
     */
    @RequestMapping(value = "/requests/{fundVersionId}/{requestId}", method = RequestMethod.GET)
    @Transactional
    public ArrRequestVO getRequest(
            @PathVariable(value = "fundVersionId") final Integer fundVersionId,
            @PathVariable(value = "requestId") final Integer requestId,
            @RequestParam(value = "detail", required = false, defaultValue = "false") final Boolean detail) {
        ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        ArrRequest request = requestService.getRequest(requestId);
        return factoryVo.createRequest(request, detail, fundVersion);
    }

    @Transactional
    @RequestMapping(value = "/requests/{requestId}", method = RequestMethod.DELETE)
    public void deleteRequest(@PathVariable(value = "requestId") final Integer requestId) {
        ArrRequest request = requestService.getRequest(requestId);
        requestService.deleteRequest(request);
    }

    /**
     * Založení šablony pro JP
     *
     * @param fundId identifikátor AS
     * @return šablona pro JP
     */
    @RequestMapping(value = "/nodes/{fundId}/template/create", method = RequestMethod.PUT)
    @Transactional
    public ArrRefTemplateVO createRefTemplate(@PathVariable final Integer fundId) {
        return arrangementService.createRefTemplate(fundId);
    }

    /**
     * Úprava šablony pro JP
     *
     * @param templateId identifikátor šablony
     * @param refTemplateVO formulář editace
     * @return šablona JP
     */
    @RequestMapping(value = "/nodes/template/{templateId}", method = RequestMethod.POST)
    @Transactional
    public ArrRefTemplateVO updateRefTemplate(@PathVariable(value = "templateId") final Integer templateId,
                                              @RequestBody final ArrRefTemplateEditVO refTemplateVO) {
        return arrangementService.updateRefTemplate(templateId, refTemplateVO);
    }

    /**
     * Smazání šablony pro JP
     *
     * @param templateId identifikátor šablony
     */
    @RequestMapping(value = "/nodes/template/{templateId}", method = RequestMethod.DELETE)
    @Transactional
    public void deleteRefTemplate(@PathVariable(value = "templateId") final Integer templateId) {
        arrangementService.deleteRefTemplate(templateId);
    }

    /**
     * Získání seznamu šablon pro AS
     *
     * @param fundId identifikátor AS
     * @return seznam šablon
     */
    @RequestMapping(value = "/nodes/{fundId}/template", method = RequestMethod.GET)
    @Transactional
    public List<ArrRefTemplateVO> getRefTemplates(@PathVariable final Integer fundId) {
        return arrangementService.getRefTemplates(fundId);
    }

    /**
     * Založení nového mapování pro šablonu
     *
     * @param templateId identifikátor šablony
     * @param refTemplateMapTypeFormVO formulář mapování
     */
    @RequestMapping(value = "/nodes/template/{templateId}/map-type", method = RequestMethod.POST)
    @Transactional
    public ArrRefTemplateMapTypeVO createRefTemplateMapType(@PathVariable (value = "templateId") final Integer templateId,
                                         @RequestBody final ArrRefTemplateMapTypeVO refTemplateMapTypeFormVO) {
        return arrangementService.createRefTemplateMapType(templateId, refTemplateMapTypeFormVO);
    }

    /**
     * Editace mapování šablony
     *
     * @param templateId identifikátor šablony
     * @param mapTypeId identifikátor mapování
     * @param refTemplateMapTypeFormVO formulář mapování
     */
    @RequestMapping(value = "/nodes/template/{templateId}/map-type/{mapTypeId}", method = RequestMethod.POST)
    @Transactional
    public ArrRefTemplateMapTypeVO updateRefTemplateMapType(@PathVariable (value = "templateId") final Integer templateId,
                                         @PathVariable (value = "mapTypeId") final Integer mapTypeId,
                                         @RequestBody final ArrRefTemplateMapTypeVO refTemplateMapTypeFormVO) {
        return arrangementService.updateRefTemplateMapType(templateId, mapTypeId, refTemplateMapTypeFormVO);
    }

    /**
     * Smazání mapování šablony
     *
     * @param templateId identifikátor šablony
     * @param mapTypeId identifikátor mapování
     */
    @RequestMapping(value = "/nodes/template/{templateId}/map-type/{mapTypeId}", method = RequestMethod.DELETE)
    @Transactional
    public void deleteRefTemplateMapType(@PathVariable (value = "templateId") final Integer templateId,
                                         @PathVariable (value = "mapTypeId") final Integer mapTypeId) {
        arrangementService.deleteRefTemplateMapType(templateId, mapTypeId);
    }

    @RequestMapping(value = "/nodes/{nodeId}/{nodeVersion}/sync")
    @Transactional
    public void synchronizeNodes(@PathVariable (value = "nodeId") final Integer nodeId,
                                 @PathVariable (value = "nodeVersion") final Integer nodeVersion,
                                 @RequestParam (value = "childrenNodes") final Boolean childrenNodes) {
        ArrChange change = arrangementService.createChange(ArrChange.Type.SYNCHRONIZE_JP);
        arrangementService.synchronizeNodes(nodeId, nodeVersion, childrenNodes, change);
    }

    /**
     * Výstupní objekt pro chybové jednotky popisu.
     */
    public static class ValidationItems {

        /**
         * JP s chybou.
         */
        private List<NodeItemWithParent> items;

        /**
         * Celkový počet chyb v AS.
         */
        private Integer count;

        public ValidationItems() {
        }

        public ValidationItems(final List<NodeItemWithParent> items, final Integer count) {
            this.items = items;
            this.count = count;
        }

        public List<NodeItemWithParent> getItems() {
            return items;
        }

        public void setItems(final List<NodeItemWithParent> items) {
            this.items = items;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(final Integer count) {
            this.count = count;
        }
    }

    public static class VersionValidationItem {

        private int nodeId;

        private String description;

        private TreeNodeVO parent;

        public int getNodeId() {
            return nodeId;
        }

        public void setNodeId(final int nodeId) {
            this.nodeId = nodeId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public TreeNodeVO getParent() {
            return parent;
        }

        public void setParent(final TreeNodeVO parent) {
            this.parent = parent;
        }
    }

    /**
     * Výstupní objekt pro získaná data pro formuláře detailu uzlu.
     */
    public static class NodeFormsDataVO {

        /**
         * Formuláře
         */
        private Map<Integer, DescFormDataNewVO> forms;

        public NodeFormsDataVO() {
        }

        public NodeFormsDataVO(final Map<Integer, DescFormDataNewVO> forms) {
            this.forms = forms;
        }

        public Map<Integer, DescFormDataNewVO> getForms() {
            return forms;
        }

        public void setForms(final Map<Integer, DescFormDataNewVO> forms) {
            this.forms = forms;
        }
    }

    /**
     * Výstupní objekt pro získaná data pro formulář detailu.
     *
     * @param <T> typ nadřazené entity, např. ArrNodeVO nebo output atp.
     */
    public static abstract class FormDataNewVO<T> {

        /**
         * parent
         */
        private T parent;

        /**
         * seznam hodnot atributu
         */
        private List<ArrItemVO> descItems;

        /**
         * typy atributů
         */
        private List<ItemTypeLiteVO> itemTypes;

        public FormDataNewVO() {

        }

        public FormDataNewVO(final T parent,
                             final List<ArrItemVO> descItems,
                             final List<ItemTypeLiteVO> itemTypes) {
            this.parent = parent;
            this.descItems = descItems;
            this.itemTypes = itemTypes;
        }

        public List<ArrItemVO> getDescItems() {
            return descItems;
        }

        public void setDescItems(final List<ArrItemVO> descItems) {
            this.descItems = descItems;
        }

        public List<ItemTypeLiteVO> getItemTypes() {
            return itemTypes;
        }

        public void setItemTypes(final List<ItemTypeLiteVO> itemTypes) {
            this.itemTypes = itemTypes;
        }

        public T getParent() {
            return parent;
        }

        public void setParent(final T parent) {
            this.parent = parent;
        }
    }

    public static class DescFormDataNewVO extends FormDataNewVO<ArrNodeVO> {
        private ArrNodeVO parent;

        /**
         * Oprávnění pořádat v dané JP. Nevyhodnocuje se, pokud {@link TreeData#fullArrPerm} je true.
         */
        private boolean arrPerm;

        public DescFormDataNewVO() {
        }

        public DescFormDataNewVO(final ArrNodeVO parent,
                                 final List<ArrItemVO> descItems,
                                 final List<ItemTypeLiteVO> itemTypes, final boolean arrPerm) {
            super(parent, descItems, itemTypes);
            this.parent = parent;
            this.arrPerm = arrPerm;
        }

        @Override
        public ArrNodeVO getParent() {
            return parent;
        }

        @Override
        public void setParent(final ArrNodeVO parent) {
            this.parent = parent;
        }

        public boolean isArrPerm() {
            return arrPerm;
        }

        public void setArrPerm(final boolean arrPerm) {
            this.arrPerm = arrPerm;
        }
    }

    public static class OutputFormDataNewVO extends FormDataNewVO<ArrOutputVO> {
        private ArrOutputVO parent;

        List<Integer> unusedItemTypeIds;

        public OutputFormDataNewVO() {
        }

        public OutputFormDataNewVO(final ArrOutputVO parent,
                                   final List<ArrItemVO> descItems,
                                   final List<ItemTypeLiteVO> itemTypes,
                                   final List<Integer> unusedItemTypeIds) {
            super(parent, descItems, itemTypes);
            this.unusedItemTypeIds = unusedItemTypeIds;
            this.parent = parent;
        }

        @Override
        public ArrOutputVO getParent() {
            return parent;
        }

        @Override
        public void setParent(final ArrOutputVO parent) {
            this.parent = parent;
        }

        public List<Integer> getUnusedItemTypeIds() {
            return unusedItemTypeIds;
        }

        public void setUnusedItemTypeIds(final List<Integer> unusedItemTypeIds) {
            this.unusedItemTypeIds = unusedItemTypeIds;
        }
    }

    /**
     * Vstupní parametry pro metodu /faTree {@link #getFundTree(FaTreeParam)}.
     */
    public static class FaTreeParam {

        /**
         * Id verze.
         */
        private Integer versionId;
        /**
         * Id kořenového uzlu vrácených výsledků.
         */
        private Integer nodeId;
        /**
         * Množina rozobalených uzlů.
         */
        private Set<Integer> expandedIds;
        /**
         * Množina id uzlů, které chceme zviditelnit.
         */
        private Set<Integer> includeIds;

        public Integer getVersionId() {
            return versionId;
        }

        public void setVersionId(final Integer versionId) {
            this.versionId = versionId;
        }

        public Integer getNodeId() {
            return nodeId;
        }

        public void setNodeId(final Integer nodeId) {
            this.nodeId = nodeId;
        }

        public Set<Integer> getExpandedIds() {
            return expandedIds;
        }

        public void setExpandedIds(final Set<Integer> expandedIds) {
            this.expandedIds = expandedIds;
        }

        public Set<Integer> getIncludeIds() {
            return includeIds;
        }

        public void setIncludeIds(final Set<Integer> includeIds) {
            this.includeIds = includeIds;
        }
    }

    /**
     * Vstupní parametry pro metodu /faTree/nodes {@link #getFundTreeNodes(FaTreeNodesParam)}.
     */
    public static class FaTreeNodesParam {

        /**
         * Id verze.
         */
        private Integer versionId;

        /**
         * Seznam požadovaných uzlů.
         */
        private List<Integer> nodeIds;

        public Integer getVersionId() {
            return versionId;
        }

        public void setVersionId(final Integer versionId) {
            this.versionId = versionId;
        }

        public List<Integer> getNodeIds() {
            return nodeIds;
        }

        public void setNodeIds(final List<Integer> nodeIds) {
            this.nodeIds = nodeIds;
        }
    }

    /**
     * Výstupní objekt pro hodnotu atributu a nadřazenou entitu.
     * - pro create / delete / update
     *
     * @param <T> typ nadřazené entity, např. ArrNodeVO nebo output atp.
     */
    public static abstract class ItemResult<T> {

        /**
         * hodnota atributu
         */
        private ArrItemVO descItem;

        public abstract T getParent();

        public abstract void setParent(final T parent);

        public ArrItemVO getItem() {
            return descItem;
        }

        public void setItem(final ArrItemVO descItem) {
            this.descItem = descItem;
        }
    }

    public static class DescItemResult extends ItemResult<ArrNodeVO> {
        private ArrNodeVO parent;

        @Override
        public ArrNodeVO getParent() {
            return parent;
        }

        @Override
        public void setParent(final ArrNodeVO parent) {
            this.parent = parent;
        }
    }

    public static class OutputItemResult extends ItemResult<ArrOutputVO> {
        private ArrOutputVO parent;

        @Override
        public ArrOutputVO getParent() {
            return parent;
        }

        @Override
        public void setParent(final ArrOutputVO parent) {
            this.parent = parent;
        }
    }

    /**
     * Vstupní parametry pro přesuny uzlů.
     */
    public static class LevelMoveParam extends NodeParam {

        /**
         * Seznam uzlů, které přesouváme.
         */
        private List<ArrNodeVO> transportNodes;
        /**
         * Rodič uzlů, které přesouváme.
         */
        private ArrNodeVO transportNodeParent;

        public List<ArrNodeVO> getTransportNodes() {
            return transportNodes;
        }

        public void setTransportNodes(final List<ArrNodeVO> transportNodes) {
            this.transportNodes = transportNodes;
        }

        public ArrNodeVO getTransportNodeParent() {
            return transportNodeParent;
        }

        public void setTransportNodeParent(final ArrNodeVO transportNodeParent) {
            this.transportNodeParent = transportNodeParent;
        }
    }

    public static class GenerateOutputResult {
        private OutputRequestStatus status;

        public OutputRequestStatus getStatus() {
            return status;
        }

        public void setStatus(final OutputRequestStatus status) {
            this.status = status;
        }
    }

    public static class NodeParam {

        /**
         * Id verze stromu.
         */
        private Integer versionId;
        /**
         * Statický uzel (za/před/pod který přidáváme)
         */
        private ArrNodeVO staticNode;
        /**
         * Rodič statického uzlu (za/před/pod který přidáváme)
         */
        private ArrNodeVO staticNodeParent;

        public Integer getVersionId() {
            return versionId;
        }

        public void setVersionId(final Integer versionId) {
            this.versionId = versionId;
        }

        public ArrNodeVO getStaticNode() {
            return staticNode;
        }

        public void setStaticNode(final ArrNodeVO staticNode) {
            this.staticNode = staticNode;
        }

        public ArrNodeVO getStaticNodeParent() {
            return staticNodeParent;
        }

        public void setStaticNodeParent(final ArrNodeVO staticNodeParent) {
            this.staticNodeParent = staticNodeParent;
        }
    }

    /**
     * Vstupní parametry pro metodu /getFilteredFulltext/{versionId} {@link #getFilteredFulltextNodes(Integer, FaFilteredFulltextParam)}.
     */
    public static class FaFilteredFulltextParam {

        /**
         * Hledaná hodnota.
         */
        private String fulltext;

        /**
         * Příznak že v hodnotě fulltext je lucene query (např: +specification:*čís* -fulltextValue:ddd), false - normální fulltext
         */
        private boolean luceneQuery;

        /**
         * Parametry pro rozšířené vyhledávání.
         */
        @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
        private List<SearchParam> searchParams;

        public String getFulltext() {
            return fulltext;
        }

        public void setFulltext(final String fulltext) {
            this.fulltext = fulltext;
        }

        public boolean getLuceneQuery() {
            return luceneQuery;
        }

        public void setLuceneQuery(final boolean luceneQuery) {
            this.luceneQuery = luceneQuery;
        }

        public List<SearchParam> getSearchParams() {
            return searchParams;
        }

        public void setSearchParams(final List<SearchParam> searchParams) {
            this.searchParams = searchParams;
        }
    }

    /**
     * Vstupní parametry pro metodu /fulltext {@link #fulltext(FaFulltextParam)}.
     */
    public static class FaFulltextParam {

        /**
         * Id verze.
         */
        private Integer versionId;
        /**
         * Id uzlu pod kterým se má hledat.
         */
        private Integer nodeId;
        /**
         * Hledaná hodnota.
         */
        private String searchValue;
        /**
         * Hloubka v jaké se má hledat pokud je předáno nodeId.
         */
        private Depth depth;

        /**
         * Parametry pro rozšířené vyhledávání.
         */
        @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
        private List<SearchParam> searchParams;

        /**
         * Příznak že v hodnotě searchValue je lucene query (např: +specification:*čís* -fulltextValue:ddd), false - normální fulltext
         */
        private boolean luceneQuery;

        public Integer getVersionId() {
            return versionId;
        }

        public void setVersionId(final Integer versionId) {
            this.versionId = versionId;
        }

        public Integer getNodeId() {
            return nodeId;
        }

        public void setNodeId(final Integer nodeId) {
            this.nodeId = nodeId;
        }

        public String getSearchValue() {
            return searchValue;
        }

        public void setSearchValue(final String searchValue) {
            this.searchValue = searchValue;
        }

        public Depth getDepth() {
            return depth;
        }

        public void setDepth(final Depth depth) {
            this.depth = depth;
        }

        public List<SearchParam> getSearchParams() {
            return searchParams;
        }

        public void setSearchParams(final List<SearchParam> searchParams) {
            this.searchParams = searchParams;
        }

        public boolean getLuceneQuery() {
            return luceneQuery;
        }

        public void setLuceneQuery(final boolean luceneQuery) {
            this.luceneQuery = luceneQuery;
        }
    }

    /**
     * Hloubka v jaké se bude ve stromu vyhledávat.
     */
    public static enum Depth {

        /**
         * Vyhledává se v celém podstromu.
         */
        SUBTREE,
        /**
         * Vyhledává se jen na úrovni pod předaným nodeId.
         */
        ONE_LEVEL;
    }

    /**
     * Výstup metody /fulltext {@link #fulltext(FaFulltextParam)}.
     */
    public static class TreeNodeFulltext {

        /**
         * Id nalezeného nodu.
         */
        private Integer nodeId;

        /**
         * Rodič nalezeného nodu.
         */
        private TreeNodeVO parent;

        public Integer getNodeId() {
            return nodeId;
        }

        public void setNodeId(final Integer nodeId) {
            this.nodeId = nodeId;
        }

        public TreeNodeVO getParent() {
            return parent;
        }

        public void setParent(final TreeNodeVO parent) {
            this.parent = parent;
        }
    }

    public static class IdsParam {

        private List<Integer> ids;
        private Integer versionId;

        public List<Integer> getIds() {
            return ids;
        }

        public void setIds(final List<Integer> ids) {
            this.ids = ids;
        }

        public Integer getVersionId() {
            return versionId;
        }

        public void setVersionId(final Integer versionId) {
            this.versionId = versionId;
        }
    }

    public static class CopySiblingResult {

        private ArrNodeVO node;
        private RulDescItemTypeDescItemsVO type;

        public CopySiblingResult(final ArrNodeVO node, final RulDescItemTypeDescItemsVO type) {
            this.node = node;
            this.type = type;
        }

        public CopySiblingResult() {
        }

        public ArrNodeVO getNode() {
            return node;
        }

        public void setNode(final ArrNodeVO node) {
            this.node = node;
        }

        public RulDescItemTypeDescItemsVO getType() {
            return type;
        }

        public void setType(final RulDescItemTypeDescItemsVO type) {
            this.type = type;
        }
    }

    public static class DescriptionItemParam {
        /**
         * Id verze stromu.
         */
        private Integer versionId;
        /**
         * Statický uzel (za/před/pod který přidáváme)
         */
        private ArrNodeVO node;
        /**
         * Směr přidávání uzlu (před, za, pod)
         */
        private DirectionLevel direction;

        public Integer getVersionId() {
            return versionId;
        }

        public void setVersionId(final Integer versionId) {
            this.versionId = versionId;
        }

        public ArrNodeVO getNode() {
            return node;
        }

        public void setNode(final ArrNodeVO node) {
            this.node = node;
        }

        public DirectionLevel getDirection() {
            return direction;
        }

        public void setDirection(final DirectionLevel direction) {
            this.direction = direction;
        }
    }

    /**
     * Jednotka popisu - node + node parent
     */
    public static class NodeWithParent {

        /**
         * Jednotka popisu.
         */
        private ArrNodeVO node;

        /**
         * Rodič jednotky popisu.
         */
        private TreeNodeVO parentNode;

        public ArrNodeVO getNode() {
            return node;
        }

        public void setNode(final ArrNodeVO node) {
            this.node = node;
        }

        public TreeNodeVO getParentNode() {
            return parentNode;
        }

        public void setParentNode(final TreeNodeVO parentNode) {
            this.parentNode = parentNode;
        }

        public NodeWithParent() {
        }

        public NodeWithParent(final ArrNodeVO node, final TreeNodeVO parentNode) {
            this.node = node;
            this.parentNode = parentNode;
        }
    }

    public static class ReplaceDataBody {

        private SelectionType selectionType;

        private Set<ArrNodeVO> nodes;

        private Set<Integer> specIds;

        public SelectionType getSelectionType() {
            return selectionType;
        }

        public void setSelectionType(final SelectionType selectionType) {
            this.selectionType = selectionType;
        }

        public Set<ArrNodeVO> getNodes() {
            return nodes;
        }

        public void setNodes(final Set<ArrNodeVO> nodes) {
            this.nodes = nodes;
        }

        public Set<Integer> getSpecIds() {
            return specIds;
        }

        public void setSpecIds(final Set<Integer> specIds) {
            this.specIds = specIds;
        }
    }

    /**
     * Výběrový typ.
     *
     * @since 05.09.2017
     */
    public enum SelectionType {

        /**
         * Akce se provádí podle nodů.
         */
        NODES,

        /**
         * Akce se provede nad celým AS.
         */
        FUND

    }

    /**
     * Pomocná třídat pro parametry vytvoření pojmenovaného výstupu.
     */
    public static class OutputNameParam {

        /**
         * Název výstupu.
         */
        private String name;

        /**
         * Kód výstupu.
         */
        private String internalCode;

        /**
         * Rul Output Type ID
         */
        private Integer outputTypeId;

        /**
         * Template id.
         */
        private Integer templateId;
        
        /**
         * List of templates
         */
        private List<Integer> templateIds;

        private ApAccessPointVO anonymizedAp;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getInternalCode() {
            return internalCode;
        }

        public void setInternalCode(final String internalCode) {
            this.internalCode = internalCode;
        }

        public Integer getOutputTypeId() {
            return outputTypeId;
        }

        public void setOutputTypeId(final Integer outputTypeId) {
            this.outputTypeId = outputTypeId;
        }

        public Integer getTemplateId() {
            return templateId;
        }

        public void setTemplateId(final Integer templateId) {
            this.templateId = templateId;
        }
        
        public List<Integer> getTemplateIds() {
			return templateIds;
		}

		public void setTemplateIds(List<Integer> templateIds) {
			this.templateIds = templateIds;
		}

		public ApAccessPointVO getAnonymizedAp() {
            return anonymizedAp;
        }

        public void setAnonymizedAp(ApAccessPointVO anonymizedAp) {
            this.anonymizedAp = anonymizedAp;
        }
    }

    public static class DigitizationRequestParam {

        private Integer id;

        private List<Integer> nodeIds;

        private String description;

        private Integer digitizationFrontdeskId;

        public Integer getId() {
            return id;
        }

        public void setId(final Integer id) {
            this.id = id;
        }

        public List<Integer> getNodeIds() {
            return nodeIds;
        }

        public void setNodeIds(final List<Integer> nodeIds) {
            this.nodeIds = nodeIds;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public Integer getDigitizationFrontdeskId() {
            return digitizationFrontdeskId;
        }

        public void setDigitizationFrontdeskId(final Integer digitizationFrontdeskId) {
            this.digitizationFrontdeskId = digitizationFrontdeskId;
        }
    }

    public static class DaoRequestParam {

        private Integer id;

        private List<Integer> daoIds;

        private String description;

        private ArrDaoRequest.Type type;

        public Integer getId() {
            return id;
        }

        public void setId(final Integer id) {
            this.id = id;
        }

        public List<Integer> getDaoIds() {
            return daoIds;
        }

        public void setDaoIds(final List<Integer> daoIds) {
            this.daoIds = daoIds;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public ArrDaoRequest.Type getType() {
            return type;
        }

        public void setType(final ArrDaoRequest.Type type) {
            this.type = type;
        }
    }
}
