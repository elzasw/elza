package cz.tacr.elza.dataexchange.output;

/**
 * Parameters for io export fund xml-file request.
 */
public class IOExportFundXmlRequest extends IOExportRequest {

	final public static String FILE_NAME_EXT = ".xml";

	final private DEExportParams exportParams;

    public IOExportFundXmlRequest(Integer userId, Integer requestId, String downloadFileName, DEExportParams exportParams) {
		super(userId, requestId, downloadFileName, FILE_NAME_EXT);
		this.exportParams = exportParams;
	}

    public DEExportParams getExportParams() {
        return exportParams;
    }
}