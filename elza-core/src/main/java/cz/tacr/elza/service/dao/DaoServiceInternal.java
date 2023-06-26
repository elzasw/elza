package cz.tacr.elza.service.dao;

import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDao.DaoType;
import cz.tacr.elza.domain.ArrDaoBatchInfo;
import cz.tacr.elza.domain.ArrDaoFile;
import cz.tacr.elza.domain.ArrDaoFileGroup;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.repository.DaoFileGroupRepository;
import cz.tacr.elza.repository.DaoFileRepository;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;

@Service
public class DaoServiceInternal {

    @Autowired
    private DaoPackageRepository daoPackageRepository;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private DaoFileRepository daoFileRepository;

    @Autowired
    private DaoFileGroupRepository daoFileGroupRepository;

    public ArrDaoPackage createDaoPackage(ArrFund fund,
                                          ArrDigitalRepository repository,
                                          String identifier,
                                          ArrDaoBatchInfo batchInfo) {
        ArrDaoPackage arrDaoPackage = new ArrDaoPackage();
        arrDaoPackage.setFund(fund);
        arrDaoPackage.setDigitalRepository(repository);
        arrDaoPackage.setDaoBatchInfo(batchInfo);
        arrDaoPackage.setCode(identifier);

        return daoPackageRepository.save(arrDaoPackage);
    }

    public ArrDao createDao(ArrDaoPackage daoPackage, String code, String label, String attrs, DaoType daoType) {
        ArrDao dao = new ArrDao();
        dao.setCode(code);
        dao.setLabel(label);
        dao.setAttributes(attrs);
        dao.setDaoType(daoType);
        dao.setDaoPackage(daoPackage);
        dao.setValid(true);
        return dao;
    }

    public ArrDao persistDao(final ArrDao dao) {
        return daoRepository.save(dao);
    }

    public List<ArrDaoFile> getFilesByDao(ArrDao dao) {
        List<ArrDaoFile> daoFiles = daoFileRepository.findByDao(dao);
        return daoFiles;
    }

    public List<ArrDaoFileGroup> getFileGroupsByDao(ArrDao dao) {
        return daoFileGroupRepository.findByDaoOrderByCodeAsc(dao);
    }

    /**
     * Delete dao files from DB
     * 
     * @param daoFiles
     */
    public void deleteDaoFiles(Collection<ArrDaoFile> daoFiles) {
        if (CollectionUtils.isEmpty(daoFiles)) {
            return;
        }
        daoFileRepository.deleteAll(daoFiles);

    }

    public void deleteDaoFileGroups(Collection<ArrDaoFileGroup> daoFileGroups) {
        if (CollectionUtils.isEmpty(daoFileGroups)) {
            return;
        }
        daoFileGroupRepository.deleteAll(daoFileGroups);
    }

    public ArrDaoFileGroup createDaoFileGroup(String code, String label, ArrDao dao) {
        ArrDaoFileGroup daoFileGroup = new ArrDaoFileGroup();
        daoFileGroup.setCode(code);
        daoFileGroup.setLabel(label);
        daoFileGroup.setDao(dao);

        return daoFileGroupRepository.save(daoFileGroup);
    }

    public ArrDaoFile createDaoFile(String code, String fileName, ArrDaoFileGroup parentFileGroup,
                                    ArrDao dao) {
        ArrDaoFile daoFile = new ArrDaoFile();
        daoFile.setCode(code);
        daoFile.setDao(dao);
        daoFile.setDaoFileGroup(parentFileGroup);
        daoFile.setFileName(fileName);
        return daoFile;
    }

    public ArrDaoFile persistDaoFile(final ArrDaoFile daoFile) {
        return daoFileRepository.save(daoFile);
    }
}
