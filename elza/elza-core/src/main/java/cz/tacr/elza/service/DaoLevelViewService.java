package cz.tacr.elza.service;

import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaDaoRelation;
import cz.tacr.elza.domain.DaLevelView;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaDaoRelationRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.DaLevelViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class DaoLevelViewService {

    @Autowired
    private DaDaoRelationRepository daoRelationRepository;

    @Autowired
    private DaDaoRepository daoRepository;

    @Autowired
    private AipStateRepository aipStateRepository;

    @Autowired
    private DaLevelViewRepository daLevelViewRepository;

    @Autowired
    private DaChangeRepository daChangeRepository;

    private static final Logger logger = LoggerFactory.getLogger(DaoLevelViewService.class);


    public void processLevelViewForAip(DaAip daAip) {
        List<DaDao> daDaoList = daoRepository.findByAipAndDeleteChangeIsNull(daAip);
        DaChange change = new DaChange();
        change.setDaAip(daAip);
        change.setType(DaChangeType.AIP_CREATE);
        change.setChangeDate(LocalDateTime.now());
        daChangeRepository.save(change);

        for (DaDao daDao : daDaoList) {
            processDao(daDao, change, null);
        }
    }

    private void processDao(DaDao daDao, DaChange change, DaLevelView parentLevelView) {

        if (!daDao.getType().equals(DaDao.DaoType.LOGICAL)) {
            return;
        }

        List<DaDaoRelation> childrenList = daoRelationRepository.findByDaoInAndDeleteChangeIsNull(Collections.singletonList(daDao));
        if (!childrenList.isEmpty() && parentLevelView == null) {
            return;
        }

        List<DaDaoRelation> parentList = daoRelationRepository.findByParentDaoAndDeleteChangeIsNull(daDao);
        if (parentList.isEmpty()) {
            return;
        }

        List<DaDaoRelation> otherThenLogicalRelations = parentList.stream().filter(dr -> !dr.getDao().getType().equals(DaDao.DaoType.LOGICAL)).toList();
        if (!otherThenLogicalRelations.isEmpty()) {
            return;
        }

        List<DaDaoRelation> daDaoRelations = parentList.stream().filter(dr -> dr.getDao().getType().equals(DaDao.DaoType.LOGICAL)).toList();
        if (daDaoRelations.size() != 1) {
            return;
        }

        DaLevelView levelView = daLevelViewRepository.findByParentLevelViewAndLabelAndDeleteChangeIsNull(parentLevelView, daDao.getLabel());
        DaAipState aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(daDao.getAip());
        if (aipState == null) {
            logger.error("Nebyl nalezen aip state pro da_aip s id:{}", daDao.getAip().getAipId());
            throw new ObjectNotFoundException("Nebyl nalezen aip state pro da_aip s id:" + daDao.getAip().getAipId(), BaseCode.ID_NOT_EXIST);
        }
        if (levelView == null) {
            levelView = new DaLevelView();
            levelView.setLabel(daDao.getLabel());
            levelView.setFund(aipState.getFund());
            levelView.setCreateChange(change);
            levelView.setParentLevelView(parentLevelView);
            daLevelViewRepository.save(levelView);
        }

        daDao.setLevelView(levelView);
        daoRepository.save(daDao);
        DaDao dao = daDaoRelations.get(0).getDao();
        processDao(dao, change, levelView);
    }

}
