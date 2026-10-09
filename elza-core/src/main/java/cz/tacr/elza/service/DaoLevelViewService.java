package cz.tacr.elza.service;

import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaDaoRelation;
import cz.tacr.elza.domain.DaLevelView;
import cz.tacr.elza.repository.DaDaoRelationRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.DaLevelViewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class DaoLevelViewService {

    @Autowired
    private DaDaoRelationRepository daoRelationRepository;

    @Autowired
    private DaDaoRepository daoRepository;

    @Autowired
    private DaLevelViewRepository daLevelViewRepository;

    public void processLevelViewForAip(DaAip daAip, DaChange change) {
        List<DaDao> daDaoList = daoRepository.findByAipAndDeleteChangeIsNull(daAip);

        for (DaDao daDao : daDaoList) {
            processDao(daDao, change, null);
        }

        deleteDisconnectedLevelViews(change);
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
        if (levelView == null) {
            levelView = new DaLevelView();
            levelView.setLabel(daDao.getLabel());
            levelView.setCreateChange(change);
            levelView.setParentLevelView(parentLevelView);
            daLevelViewRepository.save(levelView);
        }

        daDao.setLevelView(levelView);
        daoRepository.save(daDao);
        DaDao dao = daDaoRelations.get(0).getDao();
        processDao(dao, change, levelView);
    }

    public void deleteDisconnectedLevelViews(DaChange change) {
        List<DaLevelView> levelViewList = daLevelViewRepository.findDisconnectedLevelViews();

        levelViewList.forEach(d -> d.setDeleteChange(change));
        daLevelViewRepository.saveAll(levelViewList);
    }
}
