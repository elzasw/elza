package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaAip;

public interface AipRepositoryCustom {

    FilteredResult<DaAip> findAips(String search, Integer firstResult, Integer maxResults);
}
