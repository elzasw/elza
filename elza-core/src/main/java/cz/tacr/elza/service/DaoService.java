package cz.tacr.elza.service;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.controller.vo.ArrDaoFileGroupVO;
import cz.tacr.elza.controller.vo.ArrDaoFileVO;
import cz.tacr.elza.controller.vo.ArrDaoVO;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoFile;
import cz.tacr.elza.domain.ArrDaoFileGroup;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.DaoFileGroupRepository;
import cz.tacr.elza.repository.DaoFileRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.utils.LocalDateTimeConverter;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Servisní metory pro  digitalizáty
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 12.12.16
 */

@Service
public class DaoService implements InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());

    private DefaultMapperFactory factory;
    private MapperFacade facade;
//    private LinkedHashMap<Class, JpaRepository> mapRepository;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private DaoFileRepository daoFileRepository;

    @Autowired
    private DaoLinkRepository daoLinkRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private DaoFileGroupRepository daoFileGroupRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        factory = new DefaultMapperFactory.Builder().build();
        factory.getConverterFactory().registerConverter(new LocalDateTimeConverter());

        factory.classMap(ArrDaoFile.class, ArrDaoFileVO.class)
                .field("daoFileId", "id")
                .exclude("dao")
                .byDefault()
                .register();

        factory.classMap(ArrDaoFileGroup.class, ArrDaoFileGroupVO.class)
                .field("daoFileGroupId", "id")
                .exclude("fileList")
                .exclude("fileCount")
                .exclude("dao")
                .byDefault()
                .register();

        facade = factory.getMapperFacade();
    }

    /**
     * Poskytuje seznam digitálních entit (DAO), které jsou napojené na konkrétní jednotku popisu (JP) nebo nemá žádné napojení (pouze pod archivní souborem (AS)).
     *
     * @param fundVersion archivní soubor
     * @param node        node, pokud je null, najde entity bez napojení
     * @param detail      načíst detailní informace (plnit struktutu vč návazných), výchozí hodnota false
     * @return seznam digitálních entit (DAO)
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public List<ArrDaoVO> findDaos(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion, ArrNode node, boolean detail) {
        List<ArrDaoVO> voList = new ArrayList<>();

        List<ArrDao> arrDaoList;
        if (node != null) {
            arrDaoList = daoRepository.findByFundAndNode(fundVersion.getFund().getFundId(), node.getNodeId());
        } else {
            arrDaoList = daoRepository.findByFundAndNotExistsNode(fundVersion.getFund().getFundId());
        }

        for (ArrDao arrDao : arrDaoList) {
            ArrDaoVO vo = new ArrDaoVO();
            vo.setId(arrDao.getDaoId());
            vo.setCode(arrDao.getCode());
            vo.setLabel(arrDao.getLabel());
            vo.setValid(arrDao.getValid());

            if (detail) {
                // TODO Lebeda - optimalizovat na jeden dotaz seskupený dle DAO / vyčlenit do samostatné metody
                final List<ArrDaoFile> daoFileList = daoFileRepository.findByDaoAndDaoFileGroupIsNull(arrDao);
                final List<ArrDaoFileVO> daoFileVOList = facade.mapAsList(daoFileList, ArrDaoFileVO.class);
                vo.addAllFile(daoFileVOList);

                final List<ArrDaoFileGroup> daoFileGroups = daoFileGroupRepository.findByDaoOrderByCodeAsc(arrDao);
                final List<ArrDaoFileGroupVO> daoFileGroupVOList = new ArrayList<>();
                for (ArrDaoFileGroup daoFileGroup : daoFileGroups) {
                    final ArrDaoFileGroupVO daoFileGroupVO = facade.map(daoFileGroup, ArrDaoFileGroupVO.class);
                    // TODO Lebeda - optimalizovat na jeden dotaz seskupený dle DAO
                    final List<ArrDaoFile> arrDaoFileList = daoFileRepository.findByDaoAndDaoFileGroup(arrDao, daoFileGroup);
                    daoFileGroupVO.addAllFile(facade.mapAsList(arrDaoFileList, ArrDaoFileVO.class));
                    daoFileGroupVOList.add(daoFileGroupVO);
                }

                vo.addAllFileGroup(daoFileGroupVOList);
            } else {
                // TODO Lebeda - optimalizovat na jeden dotaz seskupený dle DAO
                vo.setFileCount(daoFileRepository.countByDaoAndDaoFileGroupIsNull(arrDao));
                vo.setFileGroupCount(daoFileGroupRepository.countByDao(arrDao));
            }

            voList.add(vo);
        }

        return voList;
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

        if (CollectionUtils.isEmpty(daoLinkList)) {
            // vytvořit změnu
            final ArrChange createChange = arrangementService.createChange(ArrChange.Type.CREATE_DAO_LINK, node);

            // vytvořit připojení
            final ArrDaoLink daoLink = new ArrDaoLink();
            daoLink.setCreateChange(createChange);
            daoLink.setDao(dao);
            daoLink.setNode(node);

            logger.debug("Založeno nové propojení mezi DAO(ID=" + dao.getDaoId() + ") a node(ID=" + node.getNodeId() + ").");
            return daoLinkRepository.save(daoLink);
        } else if (daoLinkList.size() == 1) {
            logger.debug("Nalezeno existující platné propojení mezi DAO(ID=" + dao.getDaoId() + ") a node(ID=" + node.getNodeId() + ").");
            return daoLinkList.get(0); // vrací jediný prvek
        } else {
            // Nalezeno více než jedno platné propojení mezi digitalizátem a uzlem popisu.
            throw new BusinessException(ArrangementCode.ALREADY_ADDED);
        }

        // TODO Lebeda - poslat i websockety o připojení
        // TODO Lebeda - vytvořit požadavek pro externí systém na připojení
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
        return daoLinkRepository.save(daoLink);

        // TODO Lebeda - poslat i websockety o odpojení
        // TODO Lebeda - vytvořit požadavek pro externí systém na odpojení
    }
}
