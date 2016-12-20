package cz.tacr.elza.service;

import cz.tacr.elza.domain.ArrChange;

/**
 * Interface pro možnost vytvářet změnu až když je poprvé třeba.
 *
 * @author Martin Šlapa
 * @since 19.12.2016
 */
public interface ArrChangeLazy {

    /**
     * Získat změnu.
     *
     * @return změna
     */
    ArrChange getOrCreateChange();

}
