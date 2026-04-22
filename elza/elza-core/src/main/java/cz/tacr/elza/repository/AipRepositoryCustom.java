package cz.tacr.elza.repository;

import cz.tacr.elza.controller.vo.AipFilterGen;
import cz.tacr.elza.domain.DaAip;

import java.util.List;

public interface AipRepositoryCustom {

    FilteredResult<DaAip> findAipsByFilter(List<AipFilterGen> filters, Integer firstResult, Integer maxResults);
}
