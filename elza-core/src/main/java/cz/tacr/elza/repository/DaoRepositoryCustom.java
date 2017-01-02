package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.16
 */
public interface DaoRepositoryCustom {

    /**
     * Poskytuje seznam digitálních entit (DAO), které jsou napojené na konkrétní jednotku popisu (JP) nebo nemá žádné napojení (pouze pod archivní souborem (AS)).
     *
     * @param fundVersion archivní souboru
     * @param node        node, pokud je null, najde entity bez napojení
     * @param index       počáteční pozice pro načtení
     * @param maxResults  počet načítaných výsledků
     * @return seznam digitálních entit (DAO)
     */
    List<ArrDao> findByFundAndNodePaginating(ArrFundVersion fundVersion, @Nullable ArrNode node, Integer index, Integer maxResults);

}
