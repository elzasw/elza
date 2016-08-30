package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RegRecord;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Rozšířené rozhraní repozitáře {@link NodeRegisterRepository}.
 *
 * @author Martin Šlapa
 * @since 30.08.2016
 */
public interface NodeRegisterRepositoryCustom {

    /**
     * Vyhledání rejstříkových hesel k požadovaným jednotkám popisu.
     *
     * @param nodeIds identifikátory jednotky popisu
     * @return mapa - klíč identifikátor jed. popisu, hodnota - seznam rejstříkových hesel
     */
    Map<Integer,List<RegRecord>> findByNodes(Collection<Integer> nodeIds);

}
