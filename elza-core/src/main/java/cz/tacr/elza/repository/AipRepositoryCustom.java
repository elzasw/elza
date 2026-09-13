package cz.tacr.elza.repository;

import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.domain.DaAip;

public interface AipRepositoryCustom {

    FilteredResult<DaAip> findAipsByFilter(SearchParams params);

    /**
     * Offset stránky, na které leží daný AIP, ve stejném filtru a řazení jako {@link #findAipsByFilter}.
     *
     * @return offset zarovnaný na velikost stránky, nebo null, pokud AIP filtru neodpovídá
     */
    Integer findAipPageOffset(SearchParams params, Integer aipId);
}
