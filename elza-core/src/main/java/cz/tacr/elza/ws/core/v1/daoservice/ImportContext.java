package cz.tacr.elza.ws.core.v1.daoservice;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoPackage;

/**
 * Context for dao import
 *
 */
public class ImportContext {

    final List<ArrDaoPackage> daoPackages = new ArrayList<>();

    final List<ArrDao> daos = new ArrayList<>();

    public void addPackage(final ArrDaoPackage daoPackage) {
        daoPackages.add(daoPackage);
    }

    public void addDaos(final List<ArrDao> daos) {
        this.daos.addAll(daos);
    }

    public List<ArrDao> getDaos() {
        return daos;
    }

}
