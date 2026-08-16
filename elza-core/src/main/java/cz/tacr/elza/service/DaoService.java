package cz.tacr.elza.service;

import java.io.IOException;
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
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.ArrFsLinkRepository;
import cz.tacr.elza.repository.ArrLegacyDaoLinkRepository;
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
import cz.tacr.elza.service.dao.FileSystemRepoBrowser;
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
    private ArrLegacyDaoLinkRepository legacyDaoLinkRepository;

    @Autowired
    private ArrDaLinkRepository daLinkRepository;

    @Autowired
    private ArrFsLinkRepository fsLinkRepository;

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
    private FileSystemRepoBrowser fileSystemRepoBrowser;

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

    /**
     * Živé vazby jednotky popisu na souborové repozitáře. Doplněk k
     * {@link #findDaos} — fs vazby nemají {@link ArrDao} a do jeho výsledku
     * se nedostanou.
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public List<ArrFsLink> findFsLinks(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                       final ArrNode node) {
        Validate.notNull(fundVersion, "Verze AS musí být vyplněna");
        Validate.notNull(node, "Node musí být vyplněn");

        return fsLinkRepository.findByNodeAndDeleteChangeIsNull(node);
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
    public ArrLegacyDaoLink createOrFindDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
    									   @Nullable final ArrChange change,
                                           final ArrDao dao, final ArrNode node, final String scenario) {
        if (!dao.getValid()) {
            throw new BusinessException("Nelze připojit digitální entitu k JP, protože je nevalidní", ArrangementCode.INVALID_DAO).level(Level.WARNING);
        }

        final List<ArrLegacyDaoLink> linkList = legacyDaoLinkRepository.findByDaoAndDeleteChangeIsNull(dao);

        // Existující vazba na stejný node → idempotentně vrátit
        for (ArrLegacyDaoLink existing : linkList) {
            if (existing.getNodeId().equals(node.getNodeId())) {
                return existing;
            }
        }

		// Zákaz více vazeb podle nastavení repository
        if (!linkList.isEmpty()) {
            ArrDigitalRepository repos = dao.getDaoPackage().getDigitalRepository();
            if (!Boolean.TRUE.equals(repos.getMultipleLinks())) {
                throw new BusinessException(
                        "DAO je již připojeno k jiné jednotce popisu; opakované napojení není povoleno.",
                        ArrangementCode.INVALID_DAO).level(Level.WARNING);
            }
        }

        final ArrLegacyDaoLink resultDaoLink = createArrDaoLink(fundVersion, change, dao, node, scenario);
        updateNodeCacheDaoLinks(Collections.singleton(node.getNodeId()));

        return resultDaoLink;
    }

    public ArrLegacyDaoLink createArrDaoLink(ArrFundVersion fundVersion,
    									@Nullable ArrChange createChange,
    								    ArrDao dao,
                                        ArrNode node, String scenario) {
        // vytvořit změnu
    	if(createChange==null) {
    		createChange = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, node);
    	}

        // vytvořit připojení
        final ArrLegacyDaoLink daoLink = new ArrLegacyDaoLink();
        daoLink.setCreateChange(createChange);
        daoLink.setDao(dao);
        daoLink.setNode(node);
        daoLink.setScenario(scenario);

        logger.debug("Založeno nové propojení mezi DAO(ID=" + dao.getDaoId() + ") a node(ID=" + node.getNodeId() + ").");
        ArrLegacyDaoLink resultDaoLink = daoLinkRepository.save(daoLink);

        // poslat i websockety o připojení
        publishEvent(EventType.DAO_LINK_CREATE, fundVersion, dao.getDaoId(), node);

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

        if (daoLink instanceof ArrLegacyDaoLink legacyLink) {
            switch (legacyLink.getDao().getDaoType()) {
            case LEVEL:
                // odstraneni urovne
                ArrNode deleteNode = daoLink.getNode();
                FundLevelService fundLevelService = appCtx.getBean(FundLevelService.class);
                fundLevelService.deleteLevel(fundVersion, deleteNode, null, true);
                break;
            case ATTACHMENT:
                deleteDaoLink(fundVersion, change, daoLink, true);
                updateNodeCacheDaoLinks(Collections.singletonList(daoLink.getNodeId()));
                break;
            default:
                throw new SystemException("Unrecognized dao type");
            }
        } else {
            // fs a da vazby nemají typovou specializaci — prosté rozpojení
            deleteDaoLink(fundVersion, change, daoLink, true);
            updateNodeCacheDaoLinks(Collections.singletonList(daoLink.getNodeId()));
        }
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrDaoLink> deleteDaoLinkByNodes(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                                 ArrChange deleteChange, final Collection<ArrNode> nodes) {
        List<ArrDaoLink> daoLinks = new ArrayList<>();
        daoLinks.addAll(legacyDaoLinkRepository.findByNodesAndFetchNodeAndDao(nodes));
        daoLinks.addAll(fsLinkRepository.findByNodeInAndDeleteChangeIsNull(nodes));
        daoLinks.addAll(daLinkRepository.findByNodeInAndDeleteChangeIsNull(nodes));
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

        if (daoLink instanceof ArrLegacyDaoLink legacyLink) {
            // poslat websockety o odpojení
            publishEvent(EventType.DAO_LINK_DELETE, fundVersion, legacyLink.getDao().getDaoId(), daoLink.getNode());

            // poslat notifikaci pouze pokud je zapnutá u digitálního uložiště
            ArrDigitalRepository repos = legacyLink.getDao().getDaoPackage().getDigitalRepository();
            if (notify && repos.getSendNotification() && !fileSystemRepoService.isFileSystemRepository(repos) ) {
            	// vytvořit požadavek pro externí systém na odpojení
                final ArrDaoLinkRequest request = requestService.createDaoLinkRequest(fundVersion, legacyLink.getDao(), deleteChange, Type.UNLINK, daoLink.getNode());
                requestQueueService.sendRequest(request, fundVersion);
            }
        } else {
            // fs a da vazby: událost nese id vazby, externí notifikace se neposílají
            publishEvent(EventType.DAO_LINK_DELETE, fundVersion, daoLink.getDaoLinkId(), daoLink.getNode());
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
            List<ArrLegacyDaoLink> arrDaoLinkList = legacyDaoLinkRepository.findByDao(arrDao);

            for (ArrLegacyDaoLink arrDaoLink : arrDaoLinkList) {
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
            final List<ArrLegacyDaoLink> arrDaoLinkList = legacyDaoLinkRepository.findByDaoAndDeleteChangeIsNull(arrDao);
            for (ArrLegacyDaoLink arrDaoLink : arrDaoLinkList) {
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
            // cache nese legacy a fs vazby; da vazby v ní nikdy nebyly
            List<ArrDaoLink> daoLinks = new ArrayList<>();
            daoLinks.addAll(legacyDaoLinkRepository.findByNodeIdsAndFetchDao(nodeIds));
            daoLinks.addAll(fsLinkRepository.findByNodeIdInAndDeleteChangeIsNull(nodeIds));
            arrangementCacheService.updateDaoLinks(nodeIds, daoLinks);
        }
    }

    private void publishEvent(EventType type, ArrFundVersion fundVersion, Integer id, ArrNode node) {
        EventIdNodeIdInVersion event = new EventIdNodeIdInVersion(type, fundVersion.getFundVersionId(),
                id, Collections.singletonList(node.getNodeId()));
        eventNotificationService.publishEvent(event);
    }

    /**
     * Připojí položku souborového repozitáře (cesta relativní ke kořeni, NULL =
     * kořen) k jednotce popisu. Existující živá vazba na stejnou JP se vrátí
     * idempotentně; vazbu na jinou JP povoluje jen repozitář s multiple_links.
     */
    @Transactional(value = TxType.MANDATORY)
    @AuthMethod(permission = { UsrPermission.Permission.FUND_ARR_ALL,
            UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_NODE })
    public ArrFsLink createFsDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion,
                                     ArrDigitalRepository digiRepo,
                                     @AuthParam(type = AuthParam.Type.NODE) ArrNode node,
                                     String path) {
        path = FileSystemRepoService.normalizeRelatPath(StringUtils.trimToNull(path));

        // containment check only; the resolved path itself is not stored
        java.nio.file.Path repoPath = fileSystemRepoService.getPath(digiRepo, fundVersion.getFund());
        fileSystemRepoService.resolvePath(repoPath, path);

        List<ArrFsLink> existing = path == null
                ? fsLinkRepository.findByDigitalRepositoryAndPathIsNullAndDeleteChangeIsNull(digiRepo)
                : fsLinkRepository.findByDigitalRepositoryAndPathAndDeleteChangeIsNull(digiRepo, path);

        // existující vazba na stejný node → idempotentně vrátit
        for (ArrFsLink link : existing) {
            if (link.getNodeId().equals(node.getNodeId())) {
                return link;
            }
        }

        // zákaz více vazeb podle nastavení repository
        if (!existing.isEmpty() && !Boolean.TRUE.equals(digiRepo.getMultipleLinks())) {
            throw new BusinessException(
                    "Položka souborového repozitáře je již připojena k jiné jednotce popisu;"
                            + " opakované napojení není povoleno.",
                    ArrangementCode.INVALID_DAO).level(Level.WARNING);
        }

        ArrChange createChange = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, node);

        ArrFsLink link = new ArrFsLink();
        link.setCreateChange(createChange);
        link.setDigitalRepository(digiRepo);
        link.setPath(path);
        link.setNode(node);

        ArrFsLink result = daoLinkRepository.save(link);
        logger.debug("Založeno nové propojení mezi fs repozitářem(ID={}, path={}) a node(ID={}).",
                     digiRepo.getExternalSystemId(), path, node.getNodeId());

        publishEvent(EventType.DAO_LINK_CREATE, fundVersion, result.getDaoLinkId(), node);
        updateNodeCacheDaoLinks(Collections.singleton(node.getNodeId()));

        return result;
    }

    /**
     * Atomically moves an existing FS dao-link to another node for the same
     * (repository, path). Both delete of the source link and create of the
     * target link share one ArrChange so audit reads the pair as a move.
     * Idempotent when the source is already on the target node.
     */
    @Transactional(value = TxType.MANDATORY)
    @AuthMethod(permission = { UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR })
    public ArrFsLink moveFsDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion,
                                   Integer oldDaoLinkId,
                                   ArrNode newNode) {
        ArrFsLink oldLink = fsLinkRepository.findById(oldDaoLinkId)
                .orElseThrow(() -> new BusinessException(
                        "FS dao-link not found: " + oldDaoLinkId, ArrangementCode.INVALID_DAO));
        if (oldLink.getDeleteChange() != null) {
            throw new BusinessException(
                    "FS dao-link already deleted: " + oldDaoLinkId, ArrangementCode.INVALID_DAO);
        }
        if (oldLink.getNodeId().equals(newNode.getNodeId())) {
            return oldLink;
        }

        ArrDigitalRepository repo = oldLink.getDigitalRepository();
        String path = oldLink.getPath();

        // if the target node already has a live link to (repo, path),
        // don't duplicate — just remove the source and return existing
        List<ArrFsLink> existing = path == null
                ? fsLinkRepository.findByDigitalRepositoryAndPathIsNullAndDeleteChangeIsNull(repo)
                : fsLinkRepository.findByDigitalRepositoryAndPathAndDeleteChangeIsNull(repo, path);
        for (ArrFsLink link : existing) {
            if (link.getNodeId().equals(newNode.getNodeId())) {
                ArrChange delChange = arrangementInternalService.createChange(
                        ArrChange.Type.DELETE_DAO_LINK, oldLink.getNode());
                deleteDaoLink(fundVersion, delChange, oldLink);
                updateNodeCacheDaoLinks(Collections.singletonList(oldLink.getNodeId()));
                return link;
            }
        }

        ArrChange change = arrangementInternalService.createChange(
                ArrChange.Type.CREATE_DAO_LINK, newNode);

        ArrFsLink newLink = new ArrFsLink();
        newLink.setCreateChange(change);
        newLink.setDigitalRepository(repo);
        newLink.setPath(path);
        newLink.setNode(newNode);
        ArrFsLink saved = daoLinkRepository.save(newLink);

        deleteDaoLink(fundVersion, change, oldLink);

        publishEvent(EventType.DAO_LINK_CREATE, fundVersion, saved.getDaoLinkId(), newNode);
        updateNodeCacheDaoLinks(Arrays.asList(oldLink.getNodeId(), newNode.getNodeId()));

        return saved;
    }    

    @Transactional(value = TxType.MANDATORY)
    @AuthMethod(permission = { UsrPermission.Permission.FUND_ARR_ALL,
            UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_NODE })
    public ArrLegacyDaoLink createDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion,
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

    /**
     * Files of one DAO plus the total count and a truncation flag for
     * filesystem DAOs whose live listing hit
     * {@link FileSystemRepoBrowser#DAO_FILE_LIMIT}.
     */
    public record DaoFileListing(List<ArrDaoFile> files, long total, boolean truncated) {
    }

    /**
     * Files of one DAO with truncation info. Filesystem DAOs are listed live
     * from the repository (recursive under the DAO's path, capped at
     * {@link FileSystemRepoBrowser#DAO_FILE_LIMIT}) as transient entities
     * without a persistent id; other repository types read persisted
     * arr_dao_file rows and are never truncated.
     */
    public DaoFileListing getDaoFileListing(ArrDao dao) {
        if (isFileSystemDao(dao)) {
            FileSystemRepoBrowser.FsDaoListing listing = listFsDaoFiles(dao);
            List<ArrDaoFile> result = new ArrayList<>(listing.files().size());
            for (FileSystemRepoBrowser.FsDaoFile entry : listing.files()) {
                ArrDaoFile daoFile = new ArrDaoFile();
                daoFile.setDao(dao);
                daoFile.setCode(entry.relatPath());
                daoFile.setFileName(entry.fileName());
                daoFile.setSize(entry.size());
                daoFile.setMimetype(entry.mimetype());
                result.add(daoFile);
            }
            return new DaoFileListing(result, result.size(), listing.truncated());
        }
        List<ArrDaoFile> persisted = daoFileRepository.findByDao(dao);
        return new DaoFileListing(persisted, persisted.size(), false);
    }

    private boolean isFileSystemDao(ArrDao dao) {
        return fileSystemRepoService.isFileSystemRepository(dao.getDaoPackage().getDigitalRepository());
    }

    /**
     * Live listing of the files under a filesystem link's path. An unavailable
     * repository or a vanished path yields an empty listing so one broken link
     * does not fail the caller's whole response.
     */
    public FileSystemRepoBrowser.FsDaoListing listFsLinkFiles(ArrFsLink link) {
        try {
            return fileSystemRepoBrowser.listDaoFiles(link.getDigitalRepository(),
                                                      link.getNode().getFund(),
                                                      link.getPath(),
                                                      FileSystemRepoBrowser.DAO_FILE_LIMIT);
        } catch (IOException | BusinessException e) {
            logger.warn("Failed to list files of filesystem link {} ({}): {}",
                        link.getDaoLinkId(), link.getPath(), e.toString());
            return new FileSystemRepoBrowser.FsDaoListing(Collections.emptyList(), false);
        }
    }

    /**
     * Live listing of a filesystem DAO's files. An unavailable repository or a
     * vanished path yields an empty listing so one broken DAO does not fail
     * the caller's whole response.
     */
    private FileSystemRepoBrowser.FsDaoListing listFsDaoFiles(ArrDao dao) {
        ArrDaoPackage daoPackage = dao.getDaoPackage();
        try {
            return fileSystemRepoBrowser.listDaoFiles(daoPackage.getDigitalRepository(),
                                                      daoPackage.getFund(),
                                                      dao.getCode(),
                                                      FileSystemRepoBrowser.DAO_FILE_LIMIT);
        } catch (IOException | BusinessException e) {
            logger.warn("Failed to list files of filesystem DAO {} ({}): {}",
                        dao.getDaoId(), dao.getCode(), e.toString());
            return new FileSystemRepoBrowser.FsDaoListing(Collections.emptyList(), false);
        }
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

            List<ArrDaLink> daoLinkList = daLinkRepository.findByDaDaoInAndDeleteChangeIsNull(daDaoList);
            aipLinkList = new ArrayList<>(daLinkRepository.findByAip_AipIdAndDaDaoIsNullAndDeleteChangeIsNull(aipId));

            daoLinkMap = daoLinkList.stream()
                    .collect(Collectors.groupingBy(l -> l.getDaDao().getDaoId(),
                                                   Collectors.mapping(l -> (ArrDaoLink) l, Collectors.toList())));
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
