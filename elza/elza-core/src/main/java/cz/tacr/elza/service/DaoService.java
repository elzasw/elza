package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoFile;
import cz.tacr.elza.domain.ArrDaoFileGroup;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDaoLinkRequest;
import cz.tacr.elza.domain.ArrDaoLinkRequest.Type;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDaoRequestDao;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.ArrRequestQueueItem;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.DeleteException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.DigitizationCode;
import cz.tacr.elza.repository.DaoFileGroupRepository;
import cz.tacr.elza.repository.DaoFileRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoLinkRequestRepository;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DaoRequestDaoRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.RequestQueueItemRepository;
import cz.tacr.elza.repository.vo.DaoExternalSystemVO;
import cz.tacr.elza.service.DaoSyncService.DaoDesctItemProvider;
import cz.tacr.elza.service.FundLevelService.AddLevelDirection;
import cz.tacr.elza.service.arrangement.DesctItemProvider;
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
    private DaoLinkRepository daoLinkRepository;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private DaoPackageRepository daoPackageRepository;

    @Autowired
    private DaoFileRepository daoFileRepository;

    @Autowired
    private DaoRequestDaoRepository daoRequestDaoRepository;

    @Autowired
    private DaoFileGroupRepository daoFileGroupRepository;

    @Autowired
    private ArrangementCacheService arrangementCacheService;

    @Autowired
    private RequestQueueItemRepository requestQueueItemRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ApplicationContext appCtx;

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
                                 @Nullable final ArrNode node, final Integer index, final Integer maxResults) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Pageable pageable = PageRequest.of(index, maxResults);  
        if (node == null) {
            return daoRepository.findDettachedByFund(fundVersion.getFund(), pageable).toList();
        } else {
            return daoRepository.findAttachedByNode(node, pageable).toList();
        }
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
    public List<ArrDao> findDaosByPackage(@AuthParam(type = AuthParam.Type.FUND) final Integer fundId,
                                          final ArrDaoPackage daoPackage,
                                          final Integer index, final Integer maxResults, 
                                          final boolean unassigned) {
        Validate.notNull(fundId, "Verze AS musí být vyplněna");
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
     * @param dao  digitalizát
     * @param node node
     * @param scenario jak se připojit k DAO
     * @return nalezené nebo vytvořené propojení
     */
    @Transactional(value = TxType.MANDATORY)
    private ArrDaoLink createOrFindDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
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
                deleteDaoLink(Collections.singletonList(fundVersion), arrDaoLink, true);
            }
        }

        final ArrDaoLink resultDaoLink = createArrDaoLink(fundVersion, dao, node, scenario);

        nodeIds.add(node.getNodeId());
        updateNodeCacheDaoLinks(nodeIds);

        return resultDaoLink;
    }

    private ArrDaoLink createArrDaoLink(ArrFundVersion fundVersion, ArrDao dao,
                                        ArrNode node, String scenario) {
        // vytvořit změnu
        final ArrChange createChange = arrangementService.createChange(ArrChange.Type.CREATE_DAO_LINK, node);

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

        // poslat notifikaci pouze pokud je zapnutá u digitálního uložiště
        if (dao.getDaoPackage().getDigitalRepository().getSendNotification()) {
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
            ArrDaoLink result = deleteDaoLink(Collections.singletonList(fundVersion), daoLink, true);
            updateNodeCacheDaoLinks(Collections.singletonList(daoLink.getNodeId()));
            break;
        default:
            throw new SystemException("Unrecognized dao type");
        }
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrDaoLink> deleteDaoLinkByNode(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion, final ArrNode node) {
        List<ArrDaoLink> daoLinks = daoLinkRepository.findByNodeIdInAndDeleteChangeIsNull(Collections.singletonList(node.getNodeId()));
        for (ArrDaoLink daoLink : daoLinks) {
            deleteDaoLink(Collections.singletonList(fundVersion), daoLink, true);
        }
        arrangementCacheService.clearDaoLinks(node.getNodeId());
        return daoLinks;
    }

    private ArrDaoLink deleteDaoLink(final List<ArrFundVersion> fundVersionList, final ArrDaoLink daoLink, boolean notify) {

        // kontrola, že ještě existuje
        if (daoLink.getDeleteChange() != null) {
            logger.debug("Zadané propojení arrDaoLink(ID=" + daoLink.getDaoLinkId() + ") je již zneplatněné.");
            return null; // je rozpojeno, již nenapojovat
        }

        // rozpojit připojení - vytvořit změnu a nastavit na link
        final ArrChange deleteChange = arrangementService.createChange(ArrChange.Type.DELETE_DAO_LINK, daoLink.getNode());
        daoLink.setDeleteChange(deleteChange);
        logger.debug("Zadané propojení arrDaoLink(ID=" + daoLink.getDaoLinkId() + ") bylo zneplatněno novou změnou.");
        final ArrDaoLink resultDaoLink = daoLinkRepository.save(daoLink);

        for (ArrFundVersion arrFundVersion : fundVersionList) {

            // poslat websockety o odpojení
            publishEvent(EventType.DAO_LINK_DELETE, arrFundVersion, daoLink.getDao(), daoLink.getNode());

            // poslat notifikaci pouze pokud je zapnutá u digitálního uložiště
            if (notify && daoLink.getDao().getDaoPackage().getDigitalRepository().getSendNotification()) {
                // vytvořit požadavek pro externí systém na odpojení
                final ArrDaoLinkRequest request = requestService.createDaoLinkRequest(arrFundVersion, daoLink.getDao(), deleteChange, Type.UNLINK, daoLink.getNode());
                requestQueueService.sendRequest(request, arrFundVersion);
            }
        }

        return resultDaoLink;
    }

    /**
     * Poskytuje seznam digitálních entit (DAO), které jsou napojené na konkrétní jednotku popisu (JP) nebo nemá žádné napojení (pouze pod archivní souborem (AS)).
     *
     * @param fundVersion id archivního souboru
     * @param search      vyhledává (použití LIKE) nad kódem balíčku, kódem a labelem arr_dao (přirazený k balíčku), kódem a labelem arr_dao_batch_info
     * @param unassigned  mají-li se získávat pouze balíčky, které obsahují DAO, které nejsou nikam přirazené (unassigned = true), a nebo úplně všechny (unassigned = false)
     * @param maxResults  maximální počet vyhledaných balíčků
     * @return seznam balíčků, seřazení je podle ID balíčku sestupně (tzn. poslední vytvořené budou na začátku seznamu)
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public List<ArrDaoPackage> findDaoPackages(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                               final String search, final Boolean unassigned, final Integer maxResults) {
        return daoPackageRepository.findDaoPackages(fundVersion, search, unassigned, maxResults);
    }

    /**
     * Zneplatní DAO, pokud není navázané na požadavek ve stavu Příprava, Odesílaný,
     * Odeslaný.
     * Zneplatní všechny nebo nic.
     * Po zneplatnněí DAO zruší jejich návazné linky a pošle notifikace.
     * 
     * @param fund
     *
     * @param arrDaos
     *            seznam dao pro zneplatnění
     */
    public void deleteDaosWithoutLinks(ArrFund fund, final List<ArrDao> arrDaos) {

        // kontrola, že neexistuje DAO navázané na požadavek ve stavu Příprava, Odesílaný, Odeslaný
        final List<ArrDaoLinkRequest> daoLinkRequests = daoLinkRequestRepository.findByDaosAndStates(arrDaos,
                Arrays.asList(ArrRequest.State.OPEN, ArrRequest.State.QUEUED, ArrRequest.State.SENT));

        if (daoLinkRequests.size() != 0) {
            logger.info("Nelze zneplatnit vybraná dao, počet otevřených požadavků: " + daoLinkRequests.size());
            throw new DeleteException("Selected DAOs cannot be removed. There are pending requests for these objects",
                    DigitizationCode.DAO_HAS_REQUEST)
                            .set("NumRequest", daoLinkRequests.size());
        }

        deleteDaos(fund, arrDaos, true);
    }

    public void deleteDaoPackageWithCascade(ArrDaoPackage arrDaoPackage) {
        // kontrola, že neexistuje DAO navázané na požadavek ve stavu Příprava, Odesílaný, Odeslaný
        final List<ArrDao> arrDaos = daoRepository.findByPackage(arrDaoPackage);
        final List<ArrDaoLinkRequest> daoLinkRequests = daoLinkRequestRepository.findByDaosAndStates(arrDaos,
                Arrays.asList(ArrRequest.State.OPEN, ArrRequest.State.QUEUED, ArrRequest.State.SENT));

        if (daoLinkRequests.size() > 0) {
            throw new SystemException("Nelze smazat package=" + arrDaoPackage.getCode()
                    + ", počet otevřených požadavků: " + daoLinkRequests.size(), DigitizationCode.DAO_HAS_REQUEST);
        }

        Set<Integer> nodeIds = new HashSet<>();

        for (ArrDao arrDao : arrDaos) {
            // smazat arr_dao_link
            List<ArrDaoLink> arrDaoLinkList = daoLinkRepository.findByDao(arrDao);

            for (ArrDaoLink arrDaoLink : arrDaoLinkList) {
                if (arrDaoLink.getDeleteChangeId() == null) {
                    Integer fundId = arrDaoLink.getNode().getFundId();
                    ArrFundVersion fundVersion = fundVersionRepository.findByFundIdAndLockChangeIsNull(fundId);
                    deleteDaoLink(fundVersion, arrDaoLink);
                }
            }
            daoLinkRepository.deleteAll(arrDaoLinkList);

            // smazat arr_dao_file
            final List<ArrDaoFile> daoFileList = daoFileRepository.findByDao(arrDao);
            daoFileRepository.deleteAll(daoFileList);

            // smazat arr_dao_file_group
            final List<ArrDaoFileGroup> daoFileGroupList = daoFileGroupRepository.findByDaoOrderByCodeAsc(arrDao);
            daoFileGroupRepository.deleteAll(daoFileGroupList);

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
        }

        // smazat package
        daoPackageRepository.delete(arrDaoPackage);

        updateNodeCacheDaoLinks(nodeIds);
    }

    /**
     * Zneplatní DAO a zruší jejich návazné linky a pošle notifikace.
     * 
     * @param arrFund
     *
     * @param arrDaos
     *            seznam dao pro zneplatnění
     * 
     * @param notify
     *            priznak pro poslani notifikaci
     * 
     */
    @AuthMethod(permission = { Permission.FUND_ARR_ALL, Permission.FUND_ARR })
    public void deleteDaos(@AuthParam(type = AuthParam.Type.FUND) ArrFund arrFund,
                           final List<ArrDao> arrDaos,
                           boolean notify) {
        Set<Integer> nodeIds = new HashSet<>();

        for (ArrDao arrDao : arrDaos) {
            arrDao.setValid(false);
            daoRepository.save(arrDao);

            // zrušit linky a poslat notifikace
            final List<ArrDaoLink> arrDaoLinkList = daoLinkRepository.findByDaoAndDeleteChangeIsNull(arrDao);
            for (ArrDaoLink arrDaoLink : arrDaoLinkList) {
                ArrNode node = arrDaoLink.getNode();
                deleteDaoLink(node.getFund().getVersions(), arrDaoLink, notify);
                nodeIds.add(node.getNodeId());
            }
        }
        updateNodeCacheDaoLinks(nodeIds);
    }

    /**
     * Získání url na dao.
     * @param dao dao
     * @param repository repository, je předáváno z důvodu výkonu při možných hromadných operacích, jinak se jedná o repository, které je v dohledatelné od DAO
     * @return url
     */
    public String getDaoUrl(final ArrDao dao, final ArrDigitalRepository repository) {
        ElzaTools.UrlParams params = ElzaTools.createUrlParams()
                .add("code", dao.getCode())
                .add("label", dao.getLabel())
                .add("id", dao.getDaoId());
        return ElzaTools.bindingUrlParams(repository.getViewDaoUrl(), params);
    }

    /**
     * Získání url na dao file.
     * @param daoFile dao file
     * @param repository repository, je předáváno z důvodu výkonu při možných hromadných operacích, jinak se jedná o repository, které je v dohledatelné od DAO
     * @return url
     */
    public String getDaoFileUrl(final ArrDaoFile daoFile, final ArrDigitalRepository repository) {
        ElzaTools.UrlParams params = ElzaTools.createUrlParams()
                .add("code", daoFile.getCode())
                .add("fileName", daoFile.getFileName());
        return ElzaTools.bindingUrlParams(repository.getViewFileUrl(), params);
    }

    /**
     * Získání url na dao náhled.
     * @param daoFile dao file
     * @param repository repository, je předáváno z důvodu výkonu při možných hromadných operacích, jinak se jedná o repository, které je v dohledatelné od DAO
     * @return url
     */
    public String getDaoThumbnailUrl(final ArrDaoFile daoFile, final ArrDigitalRepository repository) {
        ElzaTools.UrlParams params = ElzaTools.createUrlParams()
                .add("code", daoFile.getCode())
                .add("fileName", daoFile.getFileName());
        return ElzaTools.bindingUrlParams(repository.getViewThumbnailUrl(), params);
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
            List<ArrDaoLink> daoLinks = daoLinkRepository.findByNodeIdInAndDeleteChangeIsNull(nodeIds);
            arrangementCacheService.updateDaoLinks(nodeIds, daoLinks);
        }
    }

    private void publishEvent(EventType type, ArrFundVersion fundVersion, ArrDao dao, ArrNode node) {
        EventIdNodeIdInVersion event = new EventIdNodeIdInVersion(type, fundVersion.getFundVersionId(),
                dao.getDaoId(), Collections.singletonList(node.getNodeId()));
        eventNotificationService.publishEvent(event);
    }

    /**
     * Create DAO link
     * 
     * @param fundVersionId
     * @param daoId
     * @param nodeId
     * @return
     */
    @Transactional
    @AuthMethod(permission = { UsrPermission.Permission.FUND_ARR_ALL,
            UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_NODE })
    public ArrDaoLink createDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) Integer fundVersionId,
                                    Integer daoId,
                                    @AuthParam(type = AuthParam.Type.NODE) Integer nodeId) {
        final ArrFundVersion fundVersion = fundVersionRepository.getOneCheckExist(fundVersionId);
        final ArrDao dao = daoRepository.getOneCheckExist(daoId);
        final ArrNode node = nodeRepository.getOneCheckExist(nodeId);

        return createDaoLink(fundVersion, dao, node);
    }

    @Transactional(value = TxType.MANDATORY)
    @AuthMethod(permission = { UsrPermission.Permission.FUND_ARR_ALL,
            UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_NODE })
    public ArrDaoLink createDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion,
                                    ArrDao dao,
                                    @AuthParam(type = AuthParam.Type.NODE) ArrNode node) {
        String scenario = null;
        ArrNode linkNode;
        // specializace dle typu DAO
        switch (dao.getDaoType()) {
        case LEVEL:
            DaoSyncService daoSyncService = appCtx.getBean(DaoSyncService.class);
            DaoDesctItemProvider descItemProvider = daoSyncService.createDescItemProvider(dao);
            FundLevelService fundLevelService = appCtx.getBean(FundLevelService.class);
            ArrLevel level = fundLevelService.addNewLevel(fundVersion, node, node,
                                                          AddLevelDirection.CHILD, null, null,
                                                          descItemProvider);
            linkNode = level.getNode();
            scenario = descItemProvider.getScenario();
            break;
        case ATTACHMENT:
            linkNode = node;
            break;
        default:
            throw new SystemException("Unrecognized dao type");
        }
        return createOrFindDaoLink(fundVersion, dao, linkNode, scenario);
    }
}
