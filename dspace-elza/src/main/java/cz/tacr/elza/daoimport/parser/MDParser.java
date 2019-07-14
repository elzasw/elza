package cz.tacr.elza.daoimport.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser pro technická metadata.
 */
public class MDParser extends DefaultHandler {

    private static Logger log = Logger.getLogger(MDParser.class);

    private String processedElement;

    private Map<String, String> md = new HashMap<>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        processedElement = qName;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (processedElement != null && length > 0) {
            String data = "";
            for (int i = start; i < start + length; i++) {
                data += ch[i];
            }
            md.put(processedElement, data);
            processedElement = null;
        }
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        processedElement = null;
        log.error(e.getLocalizedMessage());
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        processedElement = null;
        log.error(e.getLocalizedMessage());
    }

    public Map<String, String> getMd() {
        return md;
    }
}
