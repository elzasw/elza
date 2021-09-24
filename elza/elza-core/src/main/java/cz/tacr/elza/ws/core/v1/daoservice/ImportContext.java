package cz.tacr.elza.ws.core.v1.daoservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrDao.DaoType;

/**
 * Context for dao import
 *
 */
public class ImportContext {

    final List<ArrDaoPackage> daoPackages = new ArrayList<>();

    final List<ArrDao> daos = new ArrayList<>();

    final Map<String, ArrDao> levelDaos = new HashMap<>();

    private ArrDigitalRepository repository;

    public ArrDigitalRepository getRepository() {
        return repository;
    }

    public ImportContext(final ArrDigitalRepository repository) {
        this.repository = repository;
    }

    public void addPackage(final ArrDaoPackage daoPackage) {
        daoPackages.add(daoPackage);
    }

    public void addDaos(final List<ArrDao> daos) {
        this.daos.addAll(daos);
        daos.forEach(dao -> {
            if (dao.getDaoType() == DaoType.LEVEL) {
                levelDaos.put(dao.getCode(), dao);
            }
        });
    }

    public List<ArrDao> getDaos() {
        return daos;
    }

}
