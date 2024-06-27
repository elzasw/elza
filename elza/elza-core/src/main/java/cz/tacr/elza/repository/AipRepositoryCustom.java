package cz.tacr.elza.repository;

import cz.tacr.elza.controller.vo.AipFilterVO;
import cz.tacr.elza.domain.DaAip;

public interface AipRepositoryCustom {

    FilteredResult<DaAip> findAipsByFilter(AipFilterVO[] filters, Integer firstResult, Integer maxResults);
}
