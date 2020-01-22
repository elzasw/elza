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

//import com.bea.xml.stream.XMLEventWriterBase;

import cz.tacr.elza.exception.SystemException;

public class FileNode implements XmlNode {

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    private final Path xmlFile;

    public FileNode(Path xmlFile) {
        this.xmlFile = Validate.notNull(xmlFile);
    }

    @Override
    public void write(XMLStreamWriter streamWriter) throws XMLStreamException {
        try (InputStream is = Files.newInputStream(xmlFile, StandardOpenOption.READ)) {
            XMLEventReader eventReader = INPUT_FACTORY.createXMLEventReader(is);
            copyXml(eventReader, streamWriter);
            eventReader.close();
        } catch (IOException e) {
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

        //TODO Burianek vyresit bea??
        /*
        XMLEventWriterBase eventWriter = new XMLEventWriterBase(streamWriter);
        // read until end document or EOF
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            if (event.isEndDocument()) {
                break;
            }
            eventWriter.add(event);
        }
        */
    }

    @Override
    public void clear() {
        try {
            Files.deleteIfExists(xmlFile);
        } catch (IOException e) {
            throw new SystemException(e);
        }
    }
}
