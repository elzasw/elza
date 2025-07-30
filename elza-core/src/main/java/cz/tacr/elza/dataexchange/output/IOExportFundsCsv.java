package cz.tacr.elza.dataexchange.output;

import cz.tacr.elza.controller.vo.SearchParams;

/**
 * Parameters for io export funds list csv-file request.
 */
public class IOExportFundsCsv extends IOExportRequest {

	final private static String FILE_NAME_EXT = ".csv";

	final private SearchParams searchParams;

	public IOExportFundsCsv(Integer userId, Integer requestId, String downloadFileName, SearchParams searchParams) {
		super(userId, requestId, downloadFileName, FILE_NAME_EXT);
		this.searchParams = searchParams;
	}

	public SearchParams getSearchParams() {
		return searchParams;
	}
}