package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrFundVersion;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 14.12.16
 */
public interface DaoPackageRepositoryCustom {

    /**
     *  Poskytuje seznam digitálních entit (DAO), které jsou napojené na konkrétní jednotku popisu (JP) nebo nemá žádné napojení (pouze pod archivní souborem (AS)).
     *
     * @param fundVersion   id archivního souboru
     * @param search   vyhledává (použití LIKE) nad kódem balíčku, kódem a labelem arr_dao (přirazený k balíčku), kódem a labelem arr_dao_batch_info
     * @param unassigned mají-li se získávat pouze balíčky, které obsahují DAO, které nejsou nikam přirazené (unassigned = true), a nebo úplně všechny (unassigned = false)
     * @param maxResults  maximální počet vyhledaných balíčků
     * @return  seznam balíčků, seřazení je podle ID balíčku sestupně (tzn. poslední vytvořené budou na začátku seznamu)
     */
    List<ArrDaoPackage> findDaoPackages(ArrFundVersion fundVersion, String search, Boolean unassigned, Integer maxResults);
}
