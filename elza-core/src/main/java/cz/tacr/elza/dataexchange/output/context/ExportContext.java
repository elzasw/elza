package cz.tacr.elza.dataexchange.output.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.DEExportParams.FundSections;
import cz.tacr.elza.dataexchange.output.filters.ExportFilter;
import cz.tacr.elza.dataexchange.output.writer.ExportBuilder;

public class ExportContext {

    private final Set<Integer> apIds = new HashSet<>();

    private Collection<FundSections> fundsSections = new ArrayList<>();

    private final ExportBuilder builder;

    private final StaticDataProvider staticData;

    private final int batchSize;

    private ExportFilter exportFilter;

    private boolean includeAccessPoints;

    private boolean includeUUID;

    public ExportContext(ExportBuilder builder, StaticDataProvider staticData, int batchSize) {
        this.builder = Validate.notNull(builder);
        this.staticData = Validate.notNull(staticData);
        this.batchSize = batchSize;
    }

    public ExportBuilder getBuilder() {
        return builder;
    }

    public StaticDataProvider getStaticData() {
        return staticData;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public Set<Integer> getApIds() {
        return Collections.unmodifiableSet(apIds);
    }

    public void addApId(Integer apId) {
        Validate.notNull(apId);
        apIds.add(apId);
    }

    public Collection<FundSections> getFundsSections() {
        return fundsSections;
    }

    public void setFundsSections(Collection<FundSections> fundsSections) {
        this.fundsSections = fundsSections;
    }

    public void setExportFilter(final ExportFilter expFilter) {
        this.exportFilter = expFilter;
    }

    public ExportFilter getExportFilter() {
        return exportFilter;
    }

    /**
     * Return true if deleted/invalidated APs can be exported
     * 
     * @return
     */
    public boolean canExportDeletedAPs() {
        return builder.canExportDeletedAPs();
    }

    public boolean isIncludeUUID() {
        return includeUUID;
    }

    public void setIncludeUUID(boolean includeUUID) {
        this.includeUUID = includeUUID;
    }

    public boolean isIncludeAccessPoints() {
        return includeAccessPoints;
    }

    public void setIncludeAccessPoints(boolean includeAccessPoints) {
        this.includeAccessPoints = includeAccessPoints;
    }
}