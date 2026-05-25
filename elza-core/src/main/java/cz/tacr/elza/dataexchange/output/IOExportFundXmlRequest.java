package cz.tacr.elza.dataexchange.output;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.http.MediaType;

/**
 * Parameters for io export fund xml-file request.
 */
public class IOExportFundXmlRequest extends IOExportRequest {

	final private static String FILE_NAME_EXT = ".xml";

	final private DEExportParams exportParams;

    public IOExportFundXmlRequest(Integer userId, Integer requestId, String dlFileName, DEExportParams params) {
		super(userId, requestId, dlFileName, MediaType.APPLICATION_XML_VALUE, FILE_NAME_EXT);
		this.exportParams = params;
	}

    public DEExportParams getExportParams() {
        return exportParams;
    }

	@Override
	void exportToFile(Path exportFile, DEExportService exportService) throws IOException {
        exportService.exportXmlDataToFile(exportParams, exportFile);
	}
}
