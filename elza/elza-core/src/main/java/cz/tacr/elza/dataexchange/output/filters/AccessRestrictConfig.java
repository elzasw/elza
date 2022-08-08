package cz.tacr.elza.dataexchange.output.filters;

import javax.persistence.EntityManager;

import cz.tacr.elza.core.data.StaticDataProvider;

public class AccessRestrictConfig extends FilterConfig implements ExportFilterConfig {

    @Override
    public ExportFilter createFilter(final EntityManager em, final StaticDataProvider sdp) {
        return new AccessRestrictFilter(em, sdp, this);
    }
}
