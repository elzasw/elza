package cz.tacr.elza.dataexchange.input.reader.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.codec.binary.Base64OutputStream;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.sections.context.SectionContext;
import cz.tacr.elza.exception.SystemException;

public class FileHandler extends ContextAwareElementHandler {

    public FileHandler(ImportContext context) {
        super(context, ImportPhase.SECTIONS);
    }

    @Override
    protected void handleStart(XMLEventReader eventReader, StartElement startElement) {
        /*try {
            XMLEvent nextEvent = eventReader..nextEvent();
            while (nextEvent.isCharacters()) {
                nextEvent = eventReader.nextEvent();
            }
            if (!nextEvent.isStartElement()) {
                throw new DEImportException("Cannot find next event.");
            }
            startElement = nextEvent.asStartElement();
        } catch (Exception e) {
            throw new DEImportException("Cannot read section start element.");
        }*/

        Attribute attrImportId = startElement.getAttributeByName(new QName("id"));
        Attribute attrName = startElement.getAttributeByName(new QName("n"));
        Attribute attrFileName = startElement.getAttributeByName(new QName("fn"));
        Attribute attrMimetype = startElement.getAttributeByName(new QName("mt"));

        // get subelement with data
        try {
            // move to the next event (peek is this element)
            eventReader.nextEvent();
            // skip whitespaces
            XMLEvent nextEvent = eventReader.nextEvent();
            while (nextEvent.isCharacters()) {
                nextEvent = eventReader.nextEvent();
            }
            if (!nextEvent.isStartElement()) {
                throw new DEImportException("Unexpected element type found, expected was <d>")
                        .set("elem", nextEvent.toString());
            }

            StartElement binDataStartElem = nextEvent.asStartElement();
            String locBinDataPart = binDataStartElem.getName().getLocalPart();
            if (!locBinDataPart.equals("d")) {
                throw new DEImportException("Unexpected element found, expected was <d>")
                        .set("elem", locBinDataPart.toString());
            }
            XMLEvent dataEvent = eventReader.nextEvent();
            if (!dataEvent.isCharacters()) {
                throw new DEImportException("Unexpected XML event, encoded content was expected")
                        .set("type", dataEvent.getEventType());
            }
            Characters dataChars = dataEvent.asCharacters();
            writeFile(attrImportId.getValue(), attrName.getValue(),
                      attrFileName.getValue(), attrMimetype.getValue(),
                      dataChars);
            
            //currSection.addFile(, )
        } catch (XMLStreamException e) {
            throw new DEImportException("Unexpected XML event or error", e);
        }


        //.setProcessingStructType(attrImportId.getValue());

    }

    private void writeFile(String importId, String name,
                           String fileName,
                           String mimetype,
                           Characters dataChars) {
        SectionContext currSection = context.getSections().getCurrentSection();

        try {
            currSection.addFile(importId, name,
                                fileName, mimetype,
                                (os) -> decodeChars(dataChars, os));
        } catch (IOException e) {
            throw new SystemException("Failed to save file", e);
        }
    }

    private void decodeChars(Characters dataChars, OutputStream os) {
        try (//Base64OutputStream b64decoder = ;
                OutputStreamWriter osw = new OutputStreamWriter(new Base64OutputStream(os, false),
                        Charset.forName("utf-8"))) {
            dataChars.writeAsEncodedUnicode(osw);

            //osw.flush();
            //b64decoder.flush();
        } catch (XMLStreamException e) {
            throw new SystemException("Failed to rewrite data as utf-8", e);
        } catch (IOException e) {
            throw new SystemException("Failed to write result", e);
        }

        /*
        Exception decException = null;
        PipedInputStream inputStream = new PipedInputStream();
        try (PipedOutputStream out = new PipedOutputStream(inputStream)) {
            Thread thread = new Thread(() -> {
                try {
                    // write the original OutputStream to the PipedOutputStream
                    // note that in order for the below method to work, you need
                    // to ensure that the data has finished writing to the
                    // ByteArrayOutputStream
                    try {
                        Base64OutputStream b64decoder = new Base64OutputStream(out, false);
                        OutputStreamWriter osw = new OutputStreamWriter(b64decoder, Charset.forName("utf-b"));
        
                        dataChars.writeAsEncodedUnicode(osw);
                        osw.close();
                        b64decoder.flush();
                        b64decoder.close();
                    } catch (javax.xml.stream.XMLStreamException e) {
                        //decException = e;
                    } finally {
                        // close the PipedOutputStream here because we're done writing data
                        // once this thread has completed its run
                        if (out != null) {
                            // close the PipedOutputStream cleanly
                            out.close();
                        }
                    }
                } catch (IOException e) {
                    // logging and exception handling should go here
                    //decException = e;
                }
            });
            thread.start();
        
            if (decException == null) {
                currSection.addFile(importId, name,
                                    fileName, mimetype,
                                    inputStream);
            }
        
            if (thread.isInterrupted() || decException != null) {
                throw new SystemException("Failed to decode binary data", decException);
            }
        } catch (IOException e) {
            throw new SystemException("Failed to save file", e);
        }*/
    }

    @Override
    protected void handleEnd() {
        // TODO Auto-generated method stub

    }


}
