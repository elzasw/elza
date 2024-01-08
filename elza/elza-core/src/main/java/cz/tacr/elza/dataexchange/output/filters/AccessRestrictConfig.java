package cz.tacr.elza.dataexchange.output.filters;

import jakarta.persistence.EntityManager;

import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.StaticDataProvider;

public class AccessRestrictConfig extends FilterConfig implements ExportFilterConfig {

    @Override
    public ExportFilter createFilter(final EntityManager em, final StaticDataProvider sdp,
                                     final ElzaLocale elzaLocale) {
        return new AccessRestrictFilter(em, sdp, this, elzaLocale);
    }
}
