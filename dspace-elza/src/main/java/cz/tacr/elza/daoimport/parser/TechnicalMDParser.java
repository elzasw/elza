package cz.tacr.elza.daoimport.parser;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import cz.tacr.elza.metadataconstants.MetadataEnum;

/**
 * Parser pro technická metadata.
 */
public class TechnicalMDParser extends DefaultHandler {

    private MetadataEnum processedElement;
    private String data = "";
    private List<String> acceptedElements = new LinkedList<>();

    private Map<MetadataEnum, String> md = new HashMap<>();

    public TechnicalMDParser() {

        for (MetadataEnum mt : MetadataEnum.values()) {
            acceptedElements.add(mt.name());
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        String element = qName;
        if (element.startsWith("mix:")) {
            element = element.substring(4);
        }

        if (acceptedElements.contains(element.toUpperCase())) {
            processedElement = MetadataEnum.valueOf(element.toUpperCase());
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (processedElement != null && length > 0) {
            for (int i = start; i < start + length; i++) {
                data += ch[i];
            }

            if (StringUtils.isNotBlank(data)) {
                md.put(processedElement, StringUtils.trim(data));
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        processedElement = null;
        data = "";
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        processedElement = null;
        throw e;
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        processedElement = null;
        throw e;
    }

    public Map<MetadataEnum, String> getMd() {
        return md;
    }
}
