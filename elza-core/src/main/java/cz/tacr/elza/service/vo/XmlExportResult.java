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

    private File xmlData;

    private File transformedData;

    private String fundName;

    public XmlExportResult(final File xmlData, final String fundName) {
        Assert.notNull(xmlData);
        Assert.notNull(fundName);

        this.xmlData = xmlData;
        this.fundName = fundName;
    }

    public File getXmlData() {
        return xmlData;
    }

    public File getTransformedData() {
        return transformedData;
    }

    public void setTransformedData(final File transformedData) {
        this.transformedData = transformedData;
    }

    public String getFundName() {
        return fundName;
    }
}
