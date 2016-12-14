package cz.tacr.elza.service;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    private DaoRepository daoRepository;

    @Autowired
    private DaoLinkRepository daoLinkRepository;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private DaoPackageRepository daoPackageRepository;

    /**
     * Poskytuje seznam digitálních entit (DAO), které jsou napojené na konkrétní jednotku popisu (JP) nebo nemá žádné napojení (pouze pod archivní souborem (AS)).
     *
     * @param fundVersion archivní soubor
     * @param node        node, pokud je null, najde entity bez napojení
     * @return seznam digitálních entit (DAO)
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public List<ArrDao> findDaos(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion, ArrNode node) {

        List<ArrDao> arrDaoList;
        if (node != null) {
            arrDaoList = daoRepository.findByFundAndNode(fundVersion.getFund().getFundId(), node.getNodeId());
        } else {
            arrDaoList = daoRepository.findByFundAndNotExistsNode(fundVersion.getFund().getFundId());
        }

        return arrDaoList;
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
    public ArrDaoLink createOrFindDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion, ArrDao dao, ArrNode node) {
        // kontrola, že ještě neexistuje
        final List<ArrDaoLink> daoLinkList = daoLinkRepository.findByDaoAndNodeAndDeleteChangeIsNull(dao, node);

        final ArrDaoLink resultDaoLink;
        if (CollectionUtils.isEmpty(daoLinkList)) {
            // vytvořit změnu
            final ArrChange createChange = arrangementService.createChange(ArrChange.Type.CREATE_DAO_LINK, node);

            // vytvořit připojení
            final ArrDaoLink daoLink = new ArrDaoLink();
            daoLink.setCreateChange(createChange);
            daoLink.setDao(dao);
            daoLink.setNode(node);

            logger.debug("Založeno nové propojení mezi DAO(ID=" + dao.getDaoId() + ") a node(ID=" + node.getNodeId() + ").");
            resultDaoLink = daoLinkRepository.save(daoLink);
        } else if (daoLinkList.size() == 1) {
            logger.debug("Nalezeno existující platné propojení mezi DAO(ID=" + dao.getDaoId() + ") a node(ID=" + node.getNodeId() + ").");
            resultDaoLink = daoLinkList.get(0); // vrací jediný prvek
        } else {
            // Nalezeno více než jedno platné propojení mezi digitalizátem a uzlem popisu.
            throw new BusinessException(ArrangementCode.ALREADY_ADDED);
        }

        // poslat i websockety o připojení
        EventId event = EventFactory.createIdEvent(EventType.DAO_LINK_CREATE, fundVersion.getFundVersionId());
        eventNotificationService.publishEvent(event);

        // TODO Lebeda - vytvořit požadavek pro externí systém na připojení


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
    public ArrDaoLink deleteDaoLink(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion, ArrDaoLink daoLink) {
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

        // poslat websockety o odpojení
        EventId event = EventFactory.createIdEvent(EventType.DAO_LINK_DELETE, fundVersion.getFundVersionId());
        eventNotificationService.publishEvent(event);

        // TODO Lebeda - vytvořit požadavek pro externí systém na odpojení


        return resultDaoLink;
    }

    // TODO - JavaDoc - Lebeda
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public List<ArrDaoPackage> findDaoPackages(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion,
                                               String search, Boolean unassigned, Integer maxResults) {
        return daoPackageRepository.findDaoPackages(fundVersion, search, unassigned, maxResults);
    }
}
