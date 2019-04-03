package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
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
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdNodeIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Servisní metory pro  digitalizáty
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 12.12.16
 */

@Service
public class DaoService {

    private Log logger = LogFactory.getLog(this.getClass());

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
        return daoRepository.findByFundAndNodePaginating(fundVersion, node, index, maxResults);
    }

    /**
     * Poskytuje seznam digitálních entit (DAO), které jsou napojené na konkrétní balíček.
     *
     * @param fundVersion archivní soubor
     * @param daoPackage  package
     * @param index       počáteční pozice pro načtení
     * @param maxResults  počet načítaných výsledků
     * @param unassigned mají-li se získávat pouze balíčky, které obsahují DAO, které nejsou nikam přirazené (unassigned = true), a nebo úplně všechny (unassigned = false)
     * @return seznam digitálních entit (DAO)
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public List<ArrDao> findDaosByPackage(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                          final ArrDaoPackage daoPackage, final Integer index, final Integer maxResults, final boolean unassigned) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(daoPackage, "DAO obal musí být vyplněn");
        return daoRepository.findByFundAndPackagePaginating(fundVersion, daoPackage, index, maxResults, unassigned);
    }

    /**
     * Najde existující platné propojení nebo jej vytvoří.
     *
     * @param dao digitalizát
     * @param node node
     *
     * @return nalezené nebo vytvořené propojení
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrDaoLink createOrFindDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion, final ArrDao dao, final ArrNode node) {
        if (!dao.getValid()) {
            throw new BusinessException("Nelze připojit digitální entitu k JP, protože je nevalidní", ArrangementCode.INVALID_DAO).level(Level.WARNING);
        }

        // kontrola, že ještě neexistuje vazba na zadaný node
        final List<ArrDaoLink> daoLinkList = daoLinkRepository.findByDaoAndNodeAndDeleteChangeIsNull(dao, node);

        final ArrDaoLink resultDaoLink;
        if (CollectionUtils.isEmpty(daoLinkList)) {
            // Pokud má DAO jinou platnou vazbu, bude nejprve zneplatněna
            final List<ArrDaoLink> linkList = daoLinkRepository.findByDaoAndDeleteChangeIsNull(dao);
            if (CollectionUtils.isNotEmpty(linkList)) {
                // měla by být jen jedna, ale cyklus ošetří i případnou chybu v datech
                for (ArrDaoLink arrDaoLink : linkList) {
                    deleteDaoLink(fundVersion, arrDaoLink);
                }
            }

            // vytvořit změnu
            final ArrChange createChange = arrangementService.createChange(ArrChange.Type.CREATE_DAO_LINK, node);

            // vytvořit připojení
            final ArrDaoLink daoLink = new ArrDaoLink();
            daoLink.setCreateChange(createChange);
            daoLink.setDao(dao);
            daoLink.setNode(node);

            logger.debug("Založeno nové propojení mezi DAO(ID=" + dao.getDaoId() + ") a node(ID=" + node.getNodeId() + ").");
            resultDaoLink = daoLinkRepository.save(daoLink);

            arrangementCacheService.createDaoLink(daoLink.getNodeId(), resultDaoLink);

            // poslat i websockety o připojení
            EventIdNodeIdInVersion event = new EventIdNodeIdInVersion(EventType.DAO_LINK_CREATE, fundVersion.getFundVersionId(),
                    dao.getDaoId(), Collections.singletonList(node.getNodeId()));
            eventNotificationService.publishEvent(event);

            // poslat notifikaci pouze pokud je zapnutá u digitálního uložiště
            if (dao.getDaoPackage().getDigitalRepository().getSendNotification()) {
                // vytvořit požadavek pro externí systém na připojení
                final ArrDaoLinkRequest request = requestService.createDaoLinkRequest(fundVersion, dao, createChange, Type.LINK, node);
                requestQueueService.sendRequest(request, fundVersion);
            }
        } else if (daoLinkList.size() == 1) {
            logger.debug("Nalezeno existující platné propojení mezi DAO(ID=" + dao.getDaoId() + ") a node(ID=" + node.getNodeId() + ").");
            resultDaoLink = daoLinkList.get(0); // vrací jediný prvek
        } else {
            // Nalezeno více než jedno platné propojení mezi digitalizátem a uzlem popisu.
            throw new BusinessException("Propojení DAO(ID=" + dao.getDaoId() + ") a node(ID=" + node.getNodeId() + ") již existuje", ArrangementCode.ALREADY_ADDED);
        }

        return resultDaoLink;
    }



    /**
     * Vytvoří změnu o zrušení vazby a nastaví ji na arrDaoLink.
     * Akci provede jen pokud je link platný a nemá dosud vyplněnou změnu o zrušení vazby.
     *
     * @param daoLink vazba mezi dao a node
     * @return upravená kopie linku nebo null pokud ke změně nedošlo
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrDaoLink deleteDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion, final ArrDaoLink daoLink) {
        return deleteArrDaoLink(Collections.singletonList(fundVersion), daoLink);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrDaoLink> deleteDaoLinkByNode(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion, final ArrNode node) {
        List<ArrDaoLink> daoLinks = daoLinkRepository.findByNodeIdInAndDeleteChangeIsNull(Collections.singletonList(node.getNodeId()));
        for (ArrDaoLink daoLink : daoLinks) {
            deleteArrDaoLink(Collections.singletonList(fundVersion), daoLink);
        }
        return daoLinks;
    }

    private ArrDaoLink deleteArrDaoLink(final List<ArrFundVersion> fundVersionList, final ArrDaoLink daoLink) {
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

            if (arrFundVersion.getLockChange() == null) {
                arrangementCacheService.deleteDaoLink(daoLink.getNodeId(), daoLink.getDaoLinkId());
            }

            // poslat websockety o odpojení
            EventIdNodeIdInVersion event = new EventIdNodeIdInVersion(EventType.DAO_LINK_DELETE, arrFundVersion.getFundVersionId(),
                    daoLink.getDao().getDaoId(), Collections.singletonList(daoLink.getNode().getNodeId()));
            eventNotificationService.publishEvent(event);

            // poslat notifikaci pouze pokud je zapnutá u digitálního uložiště
            if (daoLink.getDao().getDaoPackage().getDigitalRepository().getSendNotification()) {
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
     * Zneplatní DAO, pokud není navázané na požadavek ve stavu Příprava, Odesílaný, Odeslaný.
     * Zneplatní všechny nebo nic.
     * Po zneplatnněí DAO zruší jejich návazné linky a pošle notifikace.
     *
     * @param arrDaos seznam dao pro zneplatnění
     * @return seznam zneplatněných ArrDao
     */
    public List<ArrDao> deleteDaosWithoutLinks(final List<ArrDao> arrDaos) {
        List<ArrDao> result = new ArrayList<>();

        // kontrola, že neexistuje DAO navázané na požadavek ve stavu Příprava, Odesílaný, Odeslaný
        final List<ArrDaoLinkRequest> daoLinkRequests = daoLinkRequestRepository.findByDaosAndStates(arrDaos,
                Arrays.asList(ArrRequest.State.OPEN, ArrRequest.State.QUEUED, ArrRequest.State.SENT));

        if (daoLinkRequests.size() == 0) {
            result.addAll(deleteDaos(arrDaos));
        } else {
            logger.info("Nelze zneplatnit vybraná dao, počet otevřených požadavků: " + daoLinkRequests.size());
        }

        return result;
    }

    public void deleteDaoPackageWithCascade(String packageIdentifier, ArrDaoPackage arrDaoPackage) {
            // kontrola, že neexistuje DAO navázané na požadavek ve stavu Příprava, Odesílaný, Odeslaný
            final List<ArrDao> arrDaos = daoRepository.findByPackage(arrDaoPackage);
            final List<ArrDaoLinkRequest> daoLinkRequests = daoLinkRequestRepository.findByDaosAndStates(arrDaos,
                    Arrays.asList(ArrRequest.State.OPEN, ArrRequest.State.QUEUED, ArrRequest.State.SENT));

            if (daoLinkRequests.size() > 0) {
                throw new SystemException("Nelze smazat package=" + packageIdentifier + ", počet otevřených požadavků: " + daoLinkRequests.size(), DigitizationCode.DAO_HAS_REQUEST);
            }

            for (ArrDao arrDao : arrDaos) {
                // smazat arr_dao_link
                final List<ArrDaoLink> arrDaoLinkList = daoLinkRepository.findByDao(arrDao);
                for (ArrDaoLink arrDaoLink : arrDaoLinkList) {
                    daoLinkRepository.delete(arrDaoLink);
                }

                // smazat arr_dao_file
                final List<ArrDaoFile> daoFileList = daoFileRepository.findByDao(arrDao);
                for (ArrDaoFile arrDaoFile : daoFileList) {
                    daoFileRepository.delete(arrDaoFile);
                }

                // smazat arr_dao_file_group
                final List<ArrDaoFileGroup> daoFileGroupList = daoFileGroupRepository.findByDaoOrderByCodeAsc(arrDao);
                for (ArrDaoFileGroup arrDaoFileGroup : daoFileGroupList) {
                    daoFileGroupRepository.delete(arrDaoFileGroup);
                }

                // smazat arr_dao_link_request
                final List<ArrDaoLinkRequest> arrDaoLinkRequestList = daoLinkRequestRepository.findByDao(arrDao);
                for (ArrDaoLinkRequest arrDaoLinkRequest : arrDaoLinkRequestList) {
                    daoLinkRequestRepository.delete(arrDaoLinkRequest);
                }

                // smazat arr_dao_request_dao
                final List<ArrDaoRequestDao> arrDaoRequestDaoList = daoRequestDaoRepository.findByDao(arrDao);
                for (ArrDaoRequestDao arrDaoRequestDao : arrDaoRequestDaoList) {
                    daoRequestDaoRepository.delete(arrDaoRequestDao);
                }

                // smazat dao
                daoRepository.delete(arrDao);
            }

            // smazat package
            daoPackageRepository.delete(arrDaoPackage);
        }

    /**
     * Zneplatní DAO a zruší jejich návazné linky a pošle notifikace.
     *
     * @param arrDaos seznam dao pro zneplatnění
     * @return seznam zneplatněných ArrDao
     */
    public List<ArrDao> deleteDaos(final List<ArrDao> arrDaos) {
        List<ArrDao> result = new ArrayList<>();
        for (ArrDao arrDao : arrDaos) {
            arrDao.setValid(false);
            result.add(daoRepository.save(arrDao));

            // zrušit linky a poslat notifikace
            final List<ArrDaoLink> arrDaoLinkList = daoLinkRepository.findByDaoAndDeleteChangeIsNull(arrDao);
            for (ArrDaoLink arrDaoLink : arrDaoLinkList) {
                deleteArrDaoLink(arrDaoLink.getNode().getFund().getVersions(), arrDaoLink);
            }
        }
        return result;
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
                .add("code", daoFile.getCode());
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
                .add("code", daoFile.getCode());
        return ElzaTools.bindingUrlParams(repository.getViewThumbnailUrl(), params);
    }

    /**
     * Vrátí list IDs ArrDao, která jsou v nějakém ArrRequestDao pomocí seznamu Arr Dao a jsou v procesu (ve stavu OPEN, QUEUED, SENT)
     *
     * @param arrDaoList seznam dao
     * @return Seznam IDs ArrDao, která jsou v nějaké ArrRequestDao ve stavu OPEN/QUEUED/SENT
     */
    public List<Integer> findProcessingArrDaoRequestDaoArrDaoIds(List<ArrDao> arrDaoList) {
        return daoRepository.findIdsByDaoIdsWhereArrRequestDaoExistInState(arrDaoList, Arrays.asList(ArrRequest.State.OPEN, ArrRequest.State.QUEUED, ArrRequest.State.SENT));
    }
}
