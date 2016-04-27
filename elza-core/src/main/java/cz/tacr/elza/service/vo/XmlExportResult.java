package cz.tacr.elza.service.vo;

import org.springframework.util.Assert;

/**
 * Výseledek xml exportu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 22. 4. 2016
 */
public class XmlExportResult {

    private byte[] xmlData;

    private byte[] transformedData;

    private String fundName;

    public XmlExportResult(final byte[] xmlData, final String fundName) {
        Assert.notNull(xmlData);
        Assert.notNull(fundName);

        this.xmlData = xmlData;
        this.fundName = fundName;
    }

    public byte[] getXmlData() {
        return xmlData;
    }

    public byte[] getTransformedData() {
        return transformedData;
    }

    public void setTransformedData(final byte[] transformedData) {
        this.transformedData = transformedData;
    }

    public String getFundName() {
        return fundName;
    }
}
