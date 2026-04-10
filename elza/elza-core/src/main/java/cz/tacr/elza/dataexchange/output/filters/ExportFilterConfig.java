package cz.tacr.elza.dataexchange.output.filters;

import jakarta.persistence.EntityManager;

import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.service.DataService;
import cz.tacr.elza.service.cache.AccessPointCacheProvider;
import cz.tacr.elza.service.cache.AccessPointCacheService;

public interface ExportFilterConfig {

    ExportFilter createFilter(final EntityManager em, final StaticDataProvider sdp, 
    		final ElzaLocale elzaLocale, final DataService dataService, 
    		final AccessPointCacheProvider apcProvider);

}
