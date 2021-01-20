package cz.tacr.elza.dataexchange.output.writer.xml.nodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.common.xml.XMLEventWriterBase;
import cz.tacr.elza.exception.SystemException;

public class FileNode implements XmlNode {

    final static private Logger log = LoggerFactory.getLogger(FileNode.class);

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    private final Path xmlFile;

    public FileNode(Path xmlFile) {
        this.xmlFile = Validate.notNull(xmlFile);

        log.debug("Creating FileNode, path: {}", xmlFile);
    }

    @Override
    public void write(XMLStreamWriter streamWriter) throws XMLStreamException {
        try (InputStream is = Files.newInputStream(xmlFile, StandardOpenOption.READ)) {
            XMLEventReader eventReader = INPUT_FACTORY.createXMLEventReader(is);
            copyXml(eventReader, streamWriter);
            eventReader.close();
        } catch (Exception e) {
            log.error("Failed to write file: {}", xmlFile, e);
            throw new XMLStreamException(e);
        }
    }

    private static void copyXml(XMLEventReader eventReader, XMLStreamWriter streamWriter) throws XMLStreamException {
        // read until first start element (ignore start document)
        while (true) {
            if (!eventReader.hasNext()) {
                return; // empty document
            }
            XMLEvent peek = eventReader.peek();
            if (peek.isStartElement()) {
                break;
            }
            eventReader.nextEvent();
        }

        XMLEventWriterBase eventWriter = new XMLEventWriterBase(streamWriter);
        // read until end document or EOF
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            if (event.isEndDocument()) {
                break;
            }
            eventWriter.add(event);
        }
    }

    @Override
    public void clear() {
        try {
            log.debug("Deleting FileNode, path: {}", xmlFile);

            Files.deleteIfExists(xmlFile);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", xmlFile.toString(), e);
            throw new SystemException(e);
        }
    }
}
