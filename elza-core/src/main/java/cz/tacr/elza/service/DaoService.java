package cz.tacr.elza.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.DaoViewRequestVO;
import cz.tacr.elza.controller.vo.ExplorerTreeNode;
import cz.tacr.elza.controller.vo.ExplorerTreeNodeFile;
import cz.tacr.elza.controller.vo.LinkedNodeVO;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrDaoLinkRequest.Type;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.DeleteException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.DigitizationCode;
import cz.tacr.elza.repository.DaoFileRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoLinkRequestRepository;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DaoRequestDaoRepository;
import cz.tacr.elza.repository.RequestQueueItemRepository;
import cz.tacr.elza.service.DaoSyncService.DaoDesctItemProvider;
import cz.tacr.elza.service.FundLevelService.AddLevelDirection;
import cz.tacr.elza.service.dao.DaoServiceInternal;
import cz.tacr.elza.service.dao.FileSystemRepoService;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdNodeIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Servisní metody pro digitalizáty
 *
 */
@Service
public class DaoService {

    private Logger logger = LoggerFactory.getLogger(DaoService.class);

    @Autowired
    private RequestQueueService requestQueueService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private DaoLinkRequestRepository daoLinkRequestRepository;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private DaDaoRepository daDaoRepository;

    @Autowired
    ClientFactoryVO clientFactoryVO;

    @Autowired
    private DaDaoFileRepository daDaoFileRepository;

    @Autowired
    private DaoLinkRepository daoLinkRepository;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private ArrangementInternalService arrangementInternalService;

    @Autowired
    private DaoPackageRepository daoPackageRepository;

    @Autowired
    private DaoFileRepository daoFileRepository;

    @Autowired
    private DaoRequestDaoRepository daoRequestDaoRepository;

    @Autowired
    private ArrangementCacheService arrangementCacheService;

    @Autowired
    private RequestQueueItemRepository requestQueueItemRepository;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private FileSystemRepoService fileSystemRepoService;

    @Autowired
    private ApplicationContext appCtx;

    @Autowired
    private DaoServiceInternal daoServiceInternal;

    @Autowired
    private DaDaoFileFolderRepository daoFileFolderRepository;

    @Autowired
    private DaDaoRelationRepository daoRelationRepository;

    @Autowired
    private AipService aipService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private AipStateRepository aipStateRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;

    /**
     * Poskytuje seznam digitálních entit (DAO), které jsou napojené na konkrétní jednotku popisu (JP) nebo nemá žádné napojení (pouze pod archivní souborem (AS)).
     *
     * @param fundVersion archivní soubor
     * @param node        node, pokud je null, najde entity bez napojení
     * @param index       počáteční pozice pro načtení
     * @param maxResults  počet načítaných výsledků
     * @return seznam digitálních entit (DAO)
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public List<ArrDao> findDaos(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                 final ArrNode node, final Integer index, final Integer maxResults) {
        Validate.notNull(fundVersion, "Verze AS musí být vyplněna");
        Validate.notNull(node, "Node musí být vyplněn");

        Pageable pageable = PageRequest.of(index, maxResults);

        return daoRepository.findAttachedByNode(node, pageable).toList();
    }

    @AuthMethod(permission = { UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD })
    public List<ArrDao> findDettachedDaos(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                          final Integer index, final Integer maxResults) {
        Validate.notNull(fundVersion, "Verze AS musí být vyplněna");
        Pageable pageable = PageRequest.of(index, maxResults);

        // Test na externí systémy
        List<ArrDigitalRepository> digitRepositories = externalSystemService.findDigitalRepository();
        if (CollectionUtils.isEmpty(digitRepositories)) {
            return Collections.emptyList();
        }

        return daoRepository.findDettachedByFund(fundVersion.getFund(), pageable).toList();
    }

    /**
     * Poskytuje seznam digitálních entit (DAO), které jsou napojené na konkrétní balíček.
     *
     * @param fundVersion archivní soubor
     * @param daoPackage  package
     * @param index       počáteční pozice pro načtení
     * @param maxResults  počet načítaných výsledků
     * @param unassigned  mají-li se získávat pouze balíčky, které obsahují DAO, které nejsou nikam přirazené (unassigned = true), a nebo úplně všechny (unassigned = false)
     * @return seznam digitálních entit (DAO)
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public List<ArrDao> findDaosByPackage(@AuthParam(type = AuthParam.Type.FUND) final ArrFundVersion fundVersion,
                                          final ArrDaoPackage daoPackage,
                                          final Integer index, final Integer maxResults,
                                          final boolean unassigned) {
        Validate.notNull(fundVersion, "Verze AS musí být vyplněna");
        Validate.notNull(daoPackage, "DAO obal musí být vyplněn");

        Pageable pageable = PageRequest.of(index, maxResults);
        if (unassigned) {
            return daoRepository.findDettachedByPackage(daoPackage, pageable).toList();
        } else {
            return daoRepository.findByPackagePageable(daoPackage, pageable).toList();
        }
    }

    /**
     * Najde existující platné propojení nebo jej vytvoří.
     *
     * @param fundVersion
     * @param change optional current change
     * @param dao  digitalizát
     * @param node node
     * @param scenario jak se připojit k DAO
     * @return nalezené nebo vytvořené propojení
     */
    @Transactional(value = TxType.MANDATORY)
    public ArrDaoLink createOrFindDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
    									   @Nullable final ArrChange change,
                                           final ArrDao dao, final ArrNode node, final String scenario) {
        if (!dao.getValid()) {
            throw new BusinessException("Nelze připojit digitální entitu k JP, protože je nevalidní", ArrangementCode.INVALID_DAO).level(Level.WARNING);
        }

        Set<Integer> nodeIds = new HashSet<>();

        // Vyhledání stávajících vazeb
        final List<ArrDaoLink> linkList = daoLinkRepository.findByDaoAndDeleteChangeIsNull(dao);
        if (!CollectionUtils.isNotEmpty(linkList)) {
            // odstraneni predchozich pripojeni
            // měla by být jen jedna, ale cyklus ošetří i případnou chybu v datech
            for (ArrDaoLink arrDaoLink : linkList) {
                nodeIds.add(arrDaoLink.getNodeId());
                deleteDaoLink(fundVersion, change, arrDaoLink, true);
            }
        }

        final ArrDaoLink resultDaoLink = createArrDaoLink(fundVersion, change, dao, node, scenario);

        nodeIds.add(node.getNodeId());
        updateNodeCacheDaoLinks(nodeIds);

        return resultDaoLink;
    }

    public ArrDaoLink createArrDaoLink(ArrFundVersion fundVersion,
    									@Nullable ArrChange createChange,
    								    ArrDao dao,
                                        ArrNode node, String scenario) {
        // vytvořit změnu
    	if(createChange==null) {
    		createChange = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, node);
    	}

        // vytvořit připojení
        final ArrDaoLink daoLink = new ArrDaoLink();
        daoLink.setCreateChange(createChange);
        daoLink.setDao(dao);
        daoLink.setNode(node);
        daoLink.setScenario(scenario);

        logger.debug("Založeno nové propojení mezi DAO(ID=" + dao.getDaoId() + ") a node(ID=" + node.getNodeId() + ").");
        ArrDaoLink resultDaoLink = daoLinkRepository.save(daoLink);

        // poslat i websockety o připojení
        publishEvent(EventType.DAO_LINK_CREATE, fundVersion, dao, node);

        // poslat notifikaci pouze pokud je zapnutá u digitálního uložiště a nejedná se o souborové úložiště
        ArrDigitalRepository repos = dao.getDaoPackage().getDigitalRepository();
        if (repos.getSendNotification() && !fileSystemRepoService.isFileSystemRepository(repos) ) {
            // vytvořit požadavek pro externí systém na připojení
            final ArrDaoLinkRequest request = requestService.createDaoLinkRequest(fundVersion, dao, createChange, Type.LINK, node);
            requestQueueService.sendRequest(request, fundVersion);
        }

        return resultDaoLink;
    }

    /**
     * Vytvoří změnu o zrušení vazby a nastaví ji na arrDaoLink.
     * Akci provede jen pokud je link platný a nemá dosud vyplněnou změnu o zrušení
     * vazby.
     *
     * Vysokoúrovňová funkce, v případě typu level odstraňuje i úroveň
     *
     * @param daoLink
     *            vazba mezi dao a node
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void deleteDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
    						  @Nullable ArrChange change,
                              final ArrDaoLink daoLink) {

        final ArrDao dao = daoLink.getDao();

        switch (dao.getDaoType()) {
        case LEVEL:
            // odstraneni urovne
            ArrNode deleteNode = daoLink.getNode();
            FundLevelService fundLevelService = appCtx.getBean(FundLevelService.class);
            fundLevelService.deleteLevel(fundVersion, deleteNode, null, true);
            break;
        case ATTACHMENT:
            ArrDaoLink result = deleteDaoLink(fundVersion, change, daoLink, true);
            updateNodeCacheDaoLinks(Collections.singletonList(daoLink.getNodeId()));
            break;
        default:
            throw new SystemException("Unrecognized dao type");
        }
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrDaoLink> deleteDaoLinkByNodes(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                                 ArrChange deleteChange, final Collection<ArrNode> nodes) {
        List<ArrDaoLink> daoLinks = daoLinkRepository.findByNodesAndFetchNodeAndDao(nodes);
        Set<ArrNode> clearNodes = new HashSet<>();
        for (ArrDaoLink daoLink : daoLinks) {
            ArrDaoLink savedDaoLink = deleteDaoLink(fundVersion, deleteChange, daoLink, true);
            if (deleteChange == null) {
            	deleteChange = savedDaoLink.getDeleteChange();
            }
            clearNodes.add(daoLink.getNode());
        }
        arrangementCacheService.clearDaoLinks(clearNodes);
        return daoLinks;
    }

    private ArrDaoLink deleteDaoLink(final ArrFundVersion fundVersion,
    								 @Nullable ArrChange deleteChange, final ArrDaoLink daoLink, boolean notify) {

        // kontrola, že ještě existuje
        if (daoLink.getDeleteChange() != null) {
            logger.debug("Zadané propojení arrDaoLink(ID=" + daoLink.getDaoLinkId() + ") je již zneplatněné.");
            return null; // je rozpojeno, již nenapojovat
        }

        // rozpojit připojení - vytvořit změnu a nastavit na link
        if (deleteChange == null) {
        	deleteChange = arrangementInternalService.createChange(ArrChange.Type.DELETE_DAO_LINK, daoLink.getNode());
        }
        daoLink.setDeleteChange(deleteChange);
        logger.debug("Zadané propojení arrDaoLink(ID=" + daoLink.getDaoLinkId() + ") bylo zneplatněno novou změnou.");
        final ArrDaoLink resultDaoLink = daoLinkRepository.save(daoLink);

        // poslat websockety o odpojení
        publishEvent(EventType.DAO_LINK_DELETE, fundVersion, daoLink.getDao(), daoLink.getNode());

        // poslat notifikaci pouze pokud je zapnutá u digitálního uložiště
        ArrDigitalRepository repos = daoLink.getDao().getDaoPackage().getDigitalRepository();
        if (notify && repos.getSendNotification() && !fileSystemRepoService.isFileSystemRepository(repos) ) {
        	// vytvořit požadavek pro externí systém na odpojení
            final ArrDaoLinkRequest request = requestService.createDaoLinkRequest(fundVersion, daoLink.getDao(), deleteChange, Type.UNLINK, daoLink.getNode());
            requestQueueService.sendRequest(request, fundVersion);
        }

        return resultDaoLink;
    }

    /**
     * Poskytuje seznam digitálních entit (DAO),
     * které jsou napojené na konkrétní jednotku popisu (JP) nebo
     * nemá žádné napojení (pouze pod archivní souborem (AS)).
     *
     * @param fundVersion
     *            id archivního souboru
     * @param search
     *            vyhledává (použití LIKE) nad kódem balíčku, kódem a labelem
     *            arr_dao (přirazený k balíčku), kódem a labelem arr_dao_batch_info
     * @param unassigned
     *            mají-li se získávat pouze balíčky, které obsahují DAO, které
     *            nejsou nikam přirazené (unassigned = true), a nebo úplně všechny
     *            (unassigned = false)
     * @param maxResults
     *            maximální počet vyhledaných balíčků
     * @return seznam balíčků, seřazení je podle ID balíčku sestupně (tzn. poslední
     *         vytvořené budou na začátku seznamu)
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public List<ArrDaoPackage> findDaoPackages(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                               final String search, final Boolean unassigned, final Integer maxResults) {
        // Test na externí systémy
        List<ArrDigitalRepository> digitRepositories = externalSystemService.findDigitalRepository();
        if (CollectionUtils.isEmpty(digitRepositories)) {
            return Collections.emptyList();
        }

        return daoPackageRepository.findDaoPackages(fundVersion, search, unassigned, maxResults);
    }

    /**
     * Zneplatní DAO, pokud není navázané na požadavek ve stavu Příprava, Odesílaný,
     * Odeslaný.
     * Zneplatní všechny nebo nic.
     * Po zneplatnněí DAO zruší jejich návazné linky a pošle notifikace.
     *
     * @param fundVersion
     *
     * @param arrDaos
     *            seznam dao pro zneplatnění
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR,
    		UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.ADMIN})
    public void deleteDaosWithoutLinks(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion, final List<ArrDao> arrDaos) {

        // kontrola, že neexistuje DAO navázané na požadavek ve stavu Příprava, Odesílaný, Odeslaný
        final List<ArrDaoLinkRequest> daoLinkRequests = daoLinkRequestRepository.findByDaosAndStates(arrDaos,
                Arrays.asList(ArrRequest.State.OPEN, ArrRequest.State.QUEUED, ArrRequest.State.SENT));

        if (daoLinkRequests.size() != 0) {
            logger.info("Nelze zneplatnit vybraná dao, počet otevřených požadavků: " + daoLinkRequests.size());
            throw new DeleteException("Selected DAOs cannot be removed. There are pending requests for these objects",
                    DigitizationCode.DAO_HAS_REQUEST)
                            .set("NumRequest", daoLinkRequests.size());
        }

        deleteDaos(fundVersion, arrDaos, true);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR,
    		UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.ADMIN})
    public void deleteDaoPackageWithCascade(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion,
    									    ArrDaoPackage arrDaoPackage) {
        // kontrola, že neexistuje DAO navázané na požadavek ve stavu Příprava, Odesílaný, Odeslaný
        final List<ArrDao> arrDaos = daoRepository.findByPackage(arrDaoPackage);
        final List<ArrDaoLinkRequest> daoLinkRequests = daoLinkRequestRepository.findByDaosAndStates(arrDaos,
                Arrays.asList(ArrRequest.State.OPEN, ArrRequest.State.QUEUED, ArrRequest.State.SENT));

        if (daoLinkRequests.size() > 0) {
            throw new SystemException("Nelze smazat package=" + arrDaoPackage.getCode()
                    + ", počet otevřených požadavků: " + daoLinkRequests.size(), DigitizationCode.DAO_HAS_REQUEST);
        }

        Set<Integer> nodeIds = new HashSet<>();

        ArrChange change = null;

        for (ArrDao arrDao : arrDaos) {
        	logger.debug("Deleting dao {}", arrDao.getDaoId());
            // smazat arr_dao_link
            List<ArrDaoLink> arrDaoLinkList = daoLinkRepository.findByDao(arrDao);

            for (ArrDaoLink arrDaoLink : arrDaoLinkList) {
                if (arrDaoLink.getDeleteChangeId() == null) {
                    Integer fundId = arrDaoLink.getNode().getFundId();
                    deleteDaoLink(fundVersion, change, arrDaoLink);
                }
            }
            daoLinkRepository.deleteAll(arrDaoLinkList);
            
            logger.debug("Deleting dao {} - daoLinksDeleted", arrDao.getDaoId());

            // smazat arr_dao_file
            final List<ArrDaoFile> daoFileList = daoServiceInternal.getFilesByDao(arrDao);
            daoServiceInternal.deleteDaoFiles(daoFileList);

            // smazat arr_dao_file_group
            final List<ArrDaoFileGroup> daoFileGroupList = daoServiceInternal.getFileGroupsByDao(arrDao);
            daoServiceInternal.deleteDaoFileGroups(daoFileGroupList);

            // smazat arr_dao_link_request
            final List<ArrDaoLinkRequest> arrDaoLinkRequestList = daoLinkRequestRepository.findByDao(arrDao);
            if (!arrDaoLinkRequestList.isEmpty()) {
                List<ArrRequestQueueItem> queueItems = requestQueueItemRepository.findByRequests(arrDaoLinkRequestList);
                requestQueueItemRepository.deleteAll(queueItems);
            }
            daoLinkRequestRepository.deleteAll(arrDaoLinkRequestList);

            // smazat arr_dao_request_dao
            final List<ArrDaoRequestDao> arrDaoRequestDaoList = daoRequestDaoRepository.findByDao(arrDao);
            daoRequestDaoRepository.deleteAll(arrDaoRequestDaoList);

            // smazat dao
            daoRepository.delete(arrDao);
            
            logger.debug("Deleted dao: {}", arrDao.getDaoId());
        }

        // smazat package
        daoPackageRepository.delete(arrDaoPackage);

        updateNodeCacheDaoLinks(nodeIds);
    }

    /**
     * Zneplatní DAO a zruší jejich návazné linky a pošle notifikace.
     *
     * @param fundVersion
     *
     * @param arrDaos
     *            seznam dao pro zneplatnění
     *
     * @param notify
     *            priznak pro poslani notifikaci
     *
     */
    @AuthMethod(permission = { Permission.FUND_ARR_ALL, Permission.FUND_ARR })
    public void deleteDaos(@AuthParam(type = AuthParam.Type.FUND) ArrFundVersion fundVersion,
                           final List<ArrDao> arrDaos,
                           boolean notify) {
        Set<Integer> nodeIds = new HashSet<>();

        for (ArrDao arrDao : arrDaos) {
            arrDao.setValid(false);
            daoRepository.save(arrDao);

            ArrChange change = null;

            // zrušit linky a poslat notifikace
            final List<ArrDaoLink> arrDaoLinkList = daoLinkRepository.findByDaoAndDeleteChangeIsNull(arrDao);
            for (ArrDaoLink arrDaoLink : arrDaoLinkList) {
                ArrNode node = arrDaoLink.getNode();
                ArrDaoLink savedDaoLink = deleteDaoLink(fundVersion, change, arrDaoLink, notify);
                if(change==null) {
                	change = savedDaoLink.getDeleteChange();
                }
                nodeIds.add(node.getNodeId());
            }
        }
        updateNodeCacheDaoLinks(nodeIds);
    }

    /**
     * Získání url na dao.
     *
     * @param contextPath
     *
     * @param dao
     *            dao
     * @param daoLink
     *            Optional DAO Link
     * @param digitalRepository
     * @return url
     */
    public String getDaoUrl(String contextPath,
                            final ArrDao dao,
    						final ArrDaoLink daoLink,
                            final ArrDigitalRepository digiRep) {
        String url = digiRep.getViewDaoUrl();

        String daoCode = dao.getCode();
        String daoLinkNodeId, daoLinkNodeUuid;
        if (daoLink != null) {
            daoLinkNodeId = daoLink.getNodeId().toString();
            daoLinkNodeUuid = daoLink.getNode().getUuid();
        } else {
            daoLinkNodeId = "";
            daoLinkNodeUuid = "";
        }

        if (fileSystemRepoService.isFileSystemRepository(digiRep)) {
            // URLs for DAOs are not yet implemented
            return null;
        }

        ElzaTools.UrlParams params = ElzaTools.createUrlParams()
                .add("repoId", digiRep.getExternalSystemId())
                .add("repoCode", digiRep.getElzaCode())
                .add("repoElzaCode", digiRep.getElzaCode())
                .add("code", daoCode)
                .add("label", dao.getLabel())
                .add("id", dao.getDaoId())
                .add("nodeId", daoLinkNodeId)
                .add("nodeUuid", daoLinkNodeUuid);
        return ElzaTools.bindingUrlParams(url, params);
    }

    /**
     * Získání url na dao file.
     * @param daoFile dao file
     * @param repository repository, je předáváno z důvodu výkonu při možných hromadných operacích, jinak se jedná o repository, které je v dohledatelné od DAO
     * @return url
     */
    public String getDaoFileUrl(String contextPath, 
                                final ArrDaoFile daoFile,
                                final ArrDigitalRepository digiRep) {
        String url = digiRep.getViewFileUrl();

        String daoFileCode = daoFile.getCode();
        if (StringUtils.isEmpty(url) && fileSystemRepoService.isFileSystemRepository(digiRep)) {
            if (contextPath == null || contextPath.equals("/")) {
                contextPath = "";
            } else {
                if (contextPath.endsWith("/")) {
                    contextPath = contextPath.substring(0, contextPath.length() - 1);
                }
            }
            Integer fundId = daoFile.getDao().getDaoPackage().getFund().getFundId();
            String encodedPath = StringUtils.isNotBlank(daoFileCode)
                    ? UriUtils.encodeQueryParam(daoFileCode, StandardCharsets.UTF_8)
                    : "";
            return contextPath + "/api/v1/fund/" + fundId
                    + "/fsrepo/" + digiRep.getExternalSystemId()
                    + "/item-data?path=" + encodedPath;
        }

        ElzaTools.UrlParams params = ElzaTools.createUrlParams()
                .add("repoId", digiRep.getExternalSystemId())
                .add("repoCode", digiRep.getElzaCode())
                .add("repoElzaCode", digiRep.getElzaCode())
                .add("code", daoFileCode)
                .add("fileName", daoFile.getFileName());
        return ElzaTools.bindingUrlParams(url, params);
    }

    /**
     * Získání url na dao náhled.
     * @param daoFile dao file
     * @param repository repository, je předáváno z důvodu výkonu při možných hromadných operacích, jinak se jedná o repository, které je v dohledatelné od DAO
     * @return url
     */
    public String getDaoThumbnailUrl(String contextPath,
                                     final ArrDaoFile daoFile,
                                     final ArrDigitalRepository digiRep) {
        String url = digiRep.getViewThumbnailUrl();

        String daoFileCode = daoFile.getCode();
        if (StringUtils.isEmpty(url) && fileSystemRepoService.isFileSystemRepository(digiRep)) {
            if (contextPath == null || contextPath.equals("/")) {
                contextPath = "";
            } else {
                if (contextPath.endsWith("/")) {
                    contextPath = contextPath.substring(0, contextPath.length() - 1);
                }
            }
            Integer fundId = daoFile.getDao().getDaoPackage().getFund().getFundId();
            String encodedPath = StringUtils.isNotBlank(daoFileCode)
                    ? UriUtils.encodeQueryParam(daoFileCode, StandardCharsets.UTF_8)
                    : "";
            return contextPath + "/api/v1/fund/" + fundId
                    + "/fsrepo/" + digiRep.getExternalSystemId()
                    + "/item-data?path=" + encodedPath;
        }

        ElzaTools.UrlParams params = ElzaTools.createUrlParams()
                .add("repoId", digiRep.getExternalSystemId())
                .add("repoCode", digiRep.getCode())
                .add("repoElzaCode", digiRep.getElzaCode())
                .add("code", daoFileCode)
                .add("fileName", daoFile.getFileName());
        return ElzaTools.bindingUrlParams(url, params);
    }

    /**
     * Vrátí list IDs ArrDao, která jsou v nějakém ArrRequestDao pomocí seznamu Arr Dao a jsou v procesu (ve stavu OPEN, QUEUED, SENT)
     *
     * @param arrDaoList seznam dao
     * @return Seznam IDs ArrDao, která jsou v nějaké ArrRequestDao ve stavu OPEN/QUEUED/SENT
     */
    public List<Integer> findProcessingArrDaoRequestDaoArrDaoIds(List<ArrDao> arrDaoList) {
        return arrDaoList.size() > 0
                ? daoRepository.findIdsByDaoIdsWhereArrRequestDaoExistInState(arrDaoList, Arrays.asList(ArrRequest.State.OPEN, ArrRequest.State.QUEUED, ArrRequest.State.SENT))
                : Collections.emptyList();
    }

    public void updateNodeCacheDaoLinks(Collection<Integer> nodeIds) {
        if (CollectionUtils.isNotEmpty(nodeIds)) {
            List<ArrDaoLink> daoLinks = daoLinkRepository.findByNodeIdsAndFetchDao(nodeIds);
            arrangementCacheService.updateDaoLinks(nodeIds, daoLinks);
        }
    }

    private void publishEvent(EventType type, ArrFundVersion fundVersion, ArrDao dao, ArrNode node) {
        EventIdNodeIdInVersion event = new EventIdNodeIdInVersion(type, fundVersion.getFundVersionId(),
                dao.getDaoId(), Collections.singletonList(node.getNodeId()));
        eventNotificationService.publishEvent(event);
    }

    @Transactional(value = TxType.MANDATORY)
    @AuthMethod(permission = { UsrPermission.Permission.FUND_ARR_ALL,
            UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_NODE })
    public ArrDaoLink createDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion,
                                    ArrDao dao,
                                    @AuthParam(type = AuthParam.Type.NODE) ArrNode node) {
        String scenario = null;
        ArrNode linkNode;
        ArrChange change = null;
        // specializace dle typu DAO
        switch (dao.getDaoType()) {
        case LEVEL:
            DaoSyncService daoSyncService = appCtx.getBean(DaoSyncService.class);
            DaoDesctItemProvider descItemProvider = daoSyncService.createDescItemProvider(dao, null);
            FundLevelService fundLevelService = appCtx.getBean(FundLevelService.class);
            List<ArrLevel> levels = fundLevelService.addNewLevel(fundVersion, node, node,
                                                          AddLevelDirection.CHILD, null, null,
                                                          descItemProvider, null, null);
            ArrLevel newLevel = levels.get(0);
            change = newLevel.getCreateChange();
            linkNode = newLevel.getNode();
            scenario = descItemProvider.getScenario();
            break;
        case ATTACHMENT:
            linkNode = node;
            break;
        default:
            throw new SystemException("Unrecognized dao type");
        }
        return createOrFindDaoLink(fundVersion, change, dao, linkNode, scenario);
    }

    /**
     * Vraci seznam DAO vcetne DaoPackage
     *
     * @param repository
     * @param daoCodes
     * @return
     */
    public List<ArrDao> findDaosByRepository(ArrDigitalRepository repository, List<String> daoCodes) {
        List<ArrDao> daos = daoRepository.findByCodes(repository, daoCodes);
        if (daos.size() != daoCodes.size()) {
            Set<String> dbDaoCodes = daos.stream().map(dao -> dao.getCode()).collect(Collectors.toSet());
            List<String> missingCodes = daoCodes.stream()
                    .filter(c -> !dbDaoCodes.contains(c)).collect(Collectors.toList());
            throw new SystemException("DAOs not found: " + Strings.join(missingCodes, ','))
                    .set("missing", missingCodes);
        }
        return daos;
    }

    /**
     * Return list of files for list of daos
     *
     * @param daos
     * @return
     */
    public List<ArrDaoFile> findDaoFiles(List<ArrDao> daos) {
        return daoFileRepository.findByDaoIn(daos);
    }

    @Transactional
    public ExplorerTreeNode findByAipIdAndTypeAndDeleteChangeIsNull(Integer aipId) {
        DaAip aip = aipService.getAip(aipId);
        DaAipState state = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);
        List<DaDao> daDaoList = new ArrayList<>();
        List<ArrDaoLink> aipLinkList = new ArrayList<>();
        Map<Integer, ExplorerTreeNode> itemMap = new HashMap<>();
        Map<Integer, List<ArrDaoLink>> daoLinkMap = new HashMap<>();
        Map<Integer, TreeNodeVO> treeNodeMap = new HashMap<>();
        List<DaDaoFileFolder> folders = new ArrayList<>();
        List<DaDaoFile> files = new ArrayList<>();
        Map<Integer, Integer> fileRepresentationMap = new HashMap<>();

        if (state.getFund() != null) {
            daDaoList = daDaoRepository.findByAipAndDeleteChangeIsNull(aip);

            List<ArrDaoLink> daoLinkList = daoLinkRepository.findByDaDaoInAndDeleteChangeIsNull(daDaoList);
            aipLinkList = daoLinkRepository.findByAip_AipIdAndDaDaoIsNullAndDeleteChangeIsNull(aipId);

            daoLinkMap = daoLinkList.stream()
                    .collect(Collectors.groupingBy(l -> l.getDaDao().getDaoId()));
            Set<Integer> nodeIds = daoLinkList.stream().map(ArrDaoLink::getNodeId).collect(Collectors.toSet());
            ArrFundVersion fundVersion = fundVersionRepository.findByFundIdAndLockChangeIsNull(state.getFund().getFundId());
            treeNodeMap = levelTreeCacheService.getNodesByIds(nodeIds, fundVersion.getFundVersionId()).stream()
                    .collect(Collectors.toMap(TreeNodeVO::getId, t -> t));

            folders = daoFileFolderRepository.findByRepresentationDaoInAndDeleteChangeIsNull(daDaoList);
            List<DaDao> fileDaos = filterDaDaoByType(daDaoList, DaDao.DaoType.FILE);
            files = daDaoFileRepository.findByDaoInAndDeleteChangeIsNull(fileDaos);
            fileRepresentationMap = daoRelationRepository.findByDaoInAndDeleteChangeIsNullAndParentDaoIsRepresentation(fileDaos).stream()
                    .collect(Collectors.toMap(r -> r.getDao().getDaoId(), r -> r.getParentDao().getDaoId()));
        }

        List<ExplorerTreeNodeFile> fileNodes = new ArrayList<>();
        for (DaDaoFile f : files) {
            DaDaoRelation relation =
                    daoRelationRepository.findByDaoInAndDeleteChangeIsNull(Collections.singletonList(f.getDao()))
                            .stream()
                            .filter(i -> i.getParentDao().getType() == DaDao.DaoType.LOGICAL)
                            .toList()
                            .get(0);
            ExplorerTreeNodeFile fileNode =  clientFactoryVO.createExplorerTreeNodeFile(f, daoLinkMap.getOrDefault(f.getDao().getDaoId(), new ArrayList<>()), treeNodeMap);
            fileNode.setParentFolderLogical(createParent(createExplorerTreeNodeWithNodes(relation.getParentDao().getCode(), relation.getParentDao().getDaoId(), relation.getParentDao().getLabel(), null)));
            fileNodes.add(fileNode);
        }
        List<DaDao> representations = daDaoList.stream().filter(i -> i.getType() == DaDao.DaoType.REPRESENTATION).toList();
        Map<Integer, ExplorerTreeNode> representationMap = createRepresentationMap(representations, daoLinkMap, treeNodeMap);

        createRepresentationFolderMap(folders, itemMap, daoLinkMap, treeNodeMap);

        buildFolderHierarchy(folders, itemMap, representationMap);
        addFilesToFolders(fileNodes, itemMap, representationMap, fileRepresentationMap);

        Map<Integer, ExplorerTreeNode> logicalMap = new HashMap<>();
        createLogicalFolderMap(daDaoList, logicalMap, daoLinkMap, treeNodeMap);
        List<DaDao> logicalList = daDaoList.stream().filter(i -> i.getType() == DaDao.DaoType.LOGICAL).toList();

        ExplorerTreeNode logicalRoot = buildLogicalStructure(logicalList, logicalMap);
        addFilesToFoldersLogical(fileNodes, logicalMap, logicalRoot);
        ExplorerTreeNode metadata = buildMetadataStructure(daDaoList, daoLinkMap, treeNodeMap);
        ExplorerTreeNode representation = createExplorerTreeNode(
                -1,
                "Reprezentace",
                representationMap.values().stream().toList()
        );
        ExplorerTreeNode logical = createExplorerTreeNode(
                -2,
                "Logická struktura",
                logicalRoot != null ?  Collections.singletonList(logicalRoot) : null
        );

        ExplorerTreeNode root = createExplorerTreeNode(-3, "Balíček", Arrays.asList(representation, logical, metadata));
        root.setLinkedNodes(clientFactoryVO.createLinkedNodes(aipLinkList, treeNodeMap));
        return root;
    }

    private ExplorerTreeNode createParent(ExplorerTreeNode src) {
        ExplorerTreeNode result = new ExplorerTreeNode();
        result.setUuid(src.getUuid());
        result.setDaoId(src.getDaoId());
        result.setLabel(src.getLabel());
        return result;
    }

    private List<DaDao> filterDaDaoByType(List<DaDao> daDaoList, DaDao.DaoType type) {
        return daDaoList.stream()
                .filter(daDao -> daDao.getType() == type)
                .toList();
    }

    private Map<Integer, ExplorerTreeNode> createRepresentationMap(List<DaDao> representations, Map<Integer, List<ArrDaoLink>> daoLinkMap, Map<Integer, TreeNodeVO> treeNodeMap) {
        Map<Integer, ExplorerTreeNode> representationMap = new HashMap<>();

        for (DaDao dao : representations) {
            ExplorerTreeNode item = createExplorerTreeNodeWithNodes(dao.getCode(), dao.getDaoId(), dao.getLabel(),
                    clientFactoryVO.createLinkedNodes(daoLinkMap.getOrDefault(dao.getDaoId(), new ArrayList<>()), treeNodeMap));
            representationMap.put(dao.getDaoId(), item);
        }

        return representationMap;
    }

    private void createRepresentationFolderMap(List<DaDaoFileFolder> folders, Map<Integer, ExplorerTreeNode> itemMap, Map<Integer, List<ArrDaoLink>> daoLinkMap, Map<Integer, TreeNodeVO> treeNodeMap) {
        folders.forEach(folder -> {
            ExplorerTreeNode item = clientFactoryVO.createExplorerTreeNode(folder, daoLinkMap, treeNodeMap);
            itemMap.put(folder.getDaoFileFolderId(), item);
        });
    }

    private void createLogicalFolderMap(List<DaDao> daDaoList, Map<Integer, ExplorerTreeNode> itemMap, Map<Integer, List<ArrDaoLink>> daoLinkMap, Map<Integer, TreeNodeVO> treeNodeMap) {
        List<DaDao> logicalList = filterDaDaoByType(daDaoList, DaDao.DaoType.LOGICAL);
        logicalList.forEach(dao -> {
            ExplorerTreeNode item = createExplorerTreeNodeWithNodes(dao.getCode(), dao.getDaoId(), dao.getLabel(),
                    clientFactoryVO.createLinkedNodes(daoLinkMap.getOrDefault(dao.getDaoId(), new ArrayList<>()), treeNodeMap));
            itemMap.put(dao.getDaoId(), item);
        });

    }

    private void buildFolderHierarchy(List<DaDaoFileFolder> folders, Map<Integer, ExplorerTreeNode> itemMap, Map<Integer, ExplorerTreeNode> representationMap) {
        for (DaDaoFileFolder folder : folders) {
            ExplorerTreeNode item = itemMap.get(folder.getDaoFileFolderId());
            if(item == null) {
                continue;
            }
            ExplorerTreeNode parent;
            if (folder.getParentFileFolder() == null) {
                parent = representationMap.get(folder.getRepresentationDao().getDaoId());
            } else {
                parent = itemMap.get(folder.getParentFileFolder().getDaoFileFolderId());
            }
            if (parent.getChildFolders() == null) {
                parent.setChildFolders(new ArrayList<>());
            }
            item.setParentFolder(createParent(parent));
            parent.getChildFolders().add(item);
        }
    }

    private void addFilesToFoldersLogical(List<ExplorerTreeNodeFile> files, Map<Integer, ExplorerTreeNode> itemMap, ExplorerTreeNode root) {
        for (ExplorerTreeNodeFile file : files) {
            ExplorerTreeNodeFile copy = clientFactoryVO.copyFile(file);
            copy.setUuid(file.getUuid() + "logical");
            ExplorerTreeNode parent = itemMap.getOrDefault(
                    file.getParentFolderLogical() != null ? file.getParentFolderLogical().getDaoId() : null, root
            );
            if (parent.getChildFiles() == null){
                parent.setChildFiles(new ArrayList<>());
            }
            file.setParentFolderLogical(createParent(parent));
            copy.setIsLogical(true);
            parent.getChildFiles().add(copy);
        }
    }
    private void addFilesToFolders(List<ExplorerTreeNodeFile> files, Map<Integer, ExplorerTreeNode> itemMap, Map<Integer, ExplorerTreeNode> representationMap, Map<Integer, Integer> fileRepresentationMap) {
        for (ExplorerTreeNodeFile file : files) {
            ExplorerTreeNode parent;
            if (file.getDaoFileFolderId() != null) {
                parent = itemMap.getOrDefault(file.getDaoFileFolderId(), null);
            } else {
                parent = representationMap.get(fileRepresentationMap.get(file.getDaoId()));
            }
            file.setParentFolder(createParent(parent));
            if (parent.getChildFiles() == null){
                parent.setChildFiles(new ArrayList<>());
            }
            parent.getChildFiles().add(file);
        }
    }

    private ExplorerTreeNode buildLogicalStructure(List<DaDao> daDaoList, Map<Integer, ExplorerTreeNode> itemMap) {
        ExplorerTreeNode logicalRoot = null;
        for (DaDao dao : daDaoList) {
            List<DaDaoRelation> relations = daoRelationRepository.findByDaoInAndDeleteChangeIsNull(Collections.singletonList(dao));
            ExplorerTreeNode item = itemMap.get(dao.getDaoId());
            if(item == null) {
                continue;
            }
            if (relations.isEmpty()) {
                logicalRoot = item;
            } else {
                for (DaDaoRelation relation : relations) {
                    ExplorerTreeNode parent = itemMap.get(relation.getParentDao().getDaoId());
                    if (parent.getChildFolders() == null) {
                        parent.setChildFolders(new ArrayList<>());
                    }
                    item.setParentFolderLogical(createParent(parent));
                    parent.getChildFolders().add(item);

                    if (item.getChildFiles() == null) {
                        item.setChildFiles(new ArrayList<>());
                    }
                }
            }
        }
        return logicalRoot;
    }

    private ExplorerTreeNode buildMetadataStructure(List<DaDao> daDaoList, Map<Integer, List<ArrDaoLink>> daoLinkMap, Map<Integer, TreeNodeVO> treeNodeMap) {
        List<DaDao> metadataList = daDaoList.stream()
                .filter(daDao -> daDao.getType() == DaDao.DaoType.METAAMD
                        || daDao.getType() == DaDao.DaoType.METADMDINHERENT
                        || daDao.getType() == DaDao.DaoType.METADMDCONTEXTUAL)
                .toList();

        List<ExplorerTreeNodeFile> metadataFiles = daDaoFileRepository.findByDaoInAndDeleteChangeIsNull(metadataList)
                .stream()
                .map(m -> clientFactoryVO.createExplorerTreeNodeFile(m, daoLinkMap.getOrDefault(m.getDao().getDaoId(), new ArrayList<>()), treeNodeMap))
                .toList();

        ExplorerTreeNode metadata = new ExplorerTreeNode();
        metadata.setDaoId(-4);
        metadata.setLabel("Metadata");
        metadata.setUuid(UUID.nameUUIDFromBytes(metadata.getLabel().getBytes()).toString());
        metadata.setChildFiles(metadataFiles);

        return metadata;
    }

    private ExplorerTreeNode createExplorerTreeNodeWithNodes(String uuid, Integer id, String label, List<LinkedNodeVO> linkedNodes) {
        ExplorerTreeNode node = new ExplorerTreeNode();
        node.setUuid(uuid);
        node.setDaoId(id);
        node.setLabel(label);
        node.setLinkedNodes(linkedNodes);
        return node;
    }

    private ExplorerTreeNode createExplorerTreeNode(Integer id, String label, List<ExplorerTreeNode> children) {
        ExplorerTreeNode vo = createExplorerTreeNodeWithNodes(UUID.nameUUIDFromBytes(id.toString().getBytes()).toString(), id, label, null);
        if(children != null && !children.isEmpty()) {
            vo.setChildFolders(children);
        }
        return vo;
    }

    public List<DaDao> getDaosByTypeAndAipIn(List<Integer> aipIds, DaDao.DaoType type) {
        return daDaoRepository.findByAipIdsAndTypeAndDeleteChangeIsNullAndLevelViewIdIsNotNull(aipIds, type);
    }

    public DaoViewRequestVO getDaoViewRequestInfo(Integer id) {
        DaDao dao = daDaoRepository.findById(id).orElse(null);
        if(dao == null) {
            throw new BusinessException("Nepodařilo se najít DaDao s id: " + id, ArrangementCode.INVALID_DAO);
        }
        DaoViewRequestVO request = new DaoViewRequestVO();
        if(dao.getAip() == null) {
            throw new BusinessException("Pro DaDao s id " + id + " neexistuje AIP", ArrangementCode.INVALID_DAO);
        }
        request.setDaoId(dao.getAip().getCode());
        request.setEntityRef(dao.getCode());
        if(dao.getAip().getDigitalRepository() == null) {
            throw new BusinessException("Pro AIP s id " + dao.getAip().getAipId() + " neexistuje digital repository", ArrangementCode.INVALID_DAO);
        }
        request.setViewUrl(dao.getAip().getDigitalRepository().getViewFileUrl());
        return request;
    }
}
