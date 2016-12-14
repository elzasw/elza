package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrFundVersion;

import java.util.List;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 14.12.16
 */
public interface DaoPackageRepositoryCustom {

    // TODO - JavaDoc - Lebeda
    List<ArrDaoPackage> findDaoPackages(ArrFundVersion fundVersion, String search, Boolean unassigned, Integer maxResults);
}
