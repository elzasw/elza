package cz.tacr.elza.service.imp;

import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Reads just enough of an EDX2 XML payload to identify the fund it declares. Used to look for an
 * existing archival file before the payload itself is handed to {@code DEImportService}.
 */
public final class EdxHeader {

    /**
     * @param institutionInternalCode value of the &lt;fi ic="…"&gt; attribute
     * @param fundNumber              value of the &lt;fi num="…"&gt; attribute, or null
     */
    public record Header(String institutionInternalCode, Integer fundNumber) { }

    private EdxHeader() { }

    /**
     * Reads the first &lt;fi&gt; element and returns its identifying attributes. The stream is
     * consumed only as far as needed; the caller opens a fresh stream for the real import.
     */
    public static Header readHeader(InputStream in) {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.START_ELEMENT && "fi".equals(reader.getLocalName())) {
                        String ic = reader.getAttributeValue(null, "ic");
                        String num = reader.getAttributeValue(null, "num");
                        Integer fundNumber = num == null ? null : Integer.parseInt(num);
                        return new Header(ic, fundNumber);
                    }
                }
            } finally {
                reader.close();
            }
        } catch (XMLStreamException | NumberFormatException e) {
            throw new BusinessException("Failed to read EDX2 header: " + e.getMessage(), BaseCode.INVALID_STATE);
        }
        throw new BusinessException("EDX2 file has no <fi> element", BaseCode.INVALID_STATE);
    }
}
