package cz.tacr.elza.repository;

import javax.annotation.Nullable;

import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.domain.DaAip;

public interface AipRepositoryCustom {

    /**
     * AIPs a user may see, by the fund they are attached to.
     *
     * @param unattached the AIPs attached to no fund are visible too
     * @param userId     only the funds the user holds a read or arrange permission to; null for all funds
     */
    record AipVisibility(boolean unattached, @Nullable Integer userId) {
        public static final AipVisibility ALL = new AipVisibility(true, null);
    }

    default FilteredResult<DaAip> findAipsByFilter(SearchParams params) {
        return findAipsByFilter(params, AipVisibility.ALL);
    }

    FilteredResult<DaAip> findAipsByFilter(SearchParams params, AipVisibility visibility);

    default Integer findAipPageOffset(SearchParams params, Integer aipId) {
        return findAipPageOffset(params, aipId, AipVisibility.ALL);
    }

    /**
     * Offset stránky, na které leží daný AIP, ve stejném filtru a řazení jako {@link #findAipsByFilter}.
     *
     * @return offset zarovnaný na velikost stránky, nebo null, pokud AIP filtru neodpovídá
     */
    Integer findAipPageOffset(SearchParams params, Integer aipId, AipVisibility visibility);
}
