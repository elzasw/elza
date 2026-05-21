package cz.tacr.elza.dataexchange.output;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.RevStateApproval;

/**
 * Request that exports access points matching a user filter to CSV.
 *
 * The filter shape mirrors the scalar parameters of /registry/search. Results are streamed
 * ordered by accessPointId ascending.
 */
public class IOExportAccessPointsCsv extends IOExportRequest {

    private static final String FILE_NAME_EXT = ".csv";

    private final SearchFilterVO searchFilter;
    private final Collection<Integer> apTypeIds;
    private final Collection<Integer> scopeIds;
    private final ApState.StateApproval state;
    private final RevStateApproval revState;

    public IOExportAccessPointsCsv(Integer userId,
                                   Integer requestId,
                                   String downloadFileName,
                                   SearchFilterVO searchFilter,
                                   Collection<Integer> apTypeIds,
                                   Collection<Integer> scopeIds,
                                   ApState.StateApproval state,
                                   RevStateApproval revState) {
        super(userId, requestId, downloadFileName, "text/csv", FILE_NAME_EXT);
        this.searchFilter = searchFilter;
        this.apTypeIds = apTypeIds == null ? null : List.copyOf(apTypeIds);
        this.scopeIds = scopeIds == null ? null : List.copyOf(scopeIds);
        this.state = state;
        this.revState = revState;
    }

    public SearchFilterVO getSearchFilter() {
        return searchFilter;
    }

    public Collection<Integer> getApTypeIds() {
        return apTypeIds == null ? Collections.emptyList() : apTypeIds;
    }

    public Collection<Integer> getScopeIds() {
        return scopeIds == null ? Collections.emptyList() : scopeIds;
    }

    public ApState.StateApproval getStateApproval() {
        return state;
    }

    public RevStateApproval getRevState() {
        return revState;
    }

    @Override
    void exportToFile(Path exportFile, DEExportService exportService) throws IOException {
        exportService.exportAccessPointsCsv(searchFilter, apTypeIds, scopeIds, state, revState,
                                            exportFile, this::setProgress);
    }
}
