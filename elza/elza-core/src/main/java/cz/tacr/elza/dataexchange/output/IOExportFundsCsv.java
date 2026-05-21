package cz.tacr.elza.dataexchange.output;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.http.MediaType;
import cz.tacr.elza.controller.vo.SearchParams;

/**
 * Parameters for io export funds list csv-file request.
 */
public class IOExportFundsCsv extends IOExportRequest {

	final private static String FILE_NAME_EXT = ".csv";

	final private SearchParams searchParams;

	public IOExportFundsCsv(Integer userId, Integer requestId, String dlFileName, SearchParams params) {
		super(userId, requestId, dlFileName, MediaType.TEXT_PLAIN_VALUE, FILE_NAME_EXT);
		this.searchParams = params;
	}

	public SearchParams getSearchParams() {
		return searchParams;
	}

	@Override
	void exportToFile(Path exportFile, DEExportService exportService) throws IOException {
        exportService.exportCsvDataToFile(searchParams, exportFile);
	}
}
