package cz.tacr.elza.service.vo;

import java.io.File;

import org.springframework.util.Assert;

/**
 * Výseledek xml exportu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 22. 4. 2016
 */
public class XmlExportResult {

    private File exportedData;

    private String fileName;

    private boolean isCompressed;

    public XmlExportResult(final File exportedData, final String fileName, final boolean isCompressed) {
        Assert.notNull(exportedData);
        Assert.notNull(fileName);

        this.exportedData = exportedData;
        this.fileName = fileName;
        this.isCompressed = isCompressed;
    }

    public File getExportedData() {
        return exportedData;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isCompressed() {
        return isCompressed;
    }
}
