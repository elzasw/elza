package cz.tacr.elza.dataexchange.output.writer.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.Validate;

import com.ctc.wstx.api.WstxInputProperties;
import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

import jakarta.xml.bind.JAXBElement;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.sections.SectionContext;
import cz.tacr.elza.dataexchange.output.writer.ApOutputStream;
import cz.tacr.elza.dataexchange.output.writer.ExportBuilder;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.InternalNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.JaxbNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode.ChildNodeType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.schema.v2.SourceApp;

/**
 * XML export builder.
 */
public class XmlExportBuilder implements ExportBuilder {

    private final Path tempDirectory = createTempDirectory();

    private final RootNode rootNode = new RootNode();

    /**
     * Sets the source application info written as the root {@code info} element.
     */
    public void setSourceApp(SourceApp sourceApp) {
        Validate.notNull(sourceApp);
        JAXBElement<SourceApp> element = XmlUtils.wrapElement(XmlNameConsts.INFO, sourceApp);
        rootNode.setNode(ChildNodeType.INFO, new JaxbNode(element));
    }

    @Override
    public SectionOutputStream openSectionOutputStream(SectionContext sectionContext) {
        InternalNode fsNode = (InternalNode) rootNode.getNode(ChildNodeType.SECTIONS);
        if (fsNode == null) {
            fsNode = new InternalNode(XmlNameConsts.SECTIONS);
            rootNode.setNode(ChildNodeType.SECTIONS, fsNode);
        }
        return new XmlSectionOutputStream(fsNode, tempDirectory, sectionContext);
    }

    @Override
    public ApOutputStream openAccessPointsOutputStream(ExportContext context) {
        return new XmlApOutputStream(rootNode, tempDirectory, context);
    }

    @Override
    public void build(OutputStream os) throws XMLStreamException {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        outputFactory.setProperty(WstxInputProperties.P_RETURN_NULL_FOR_DEFAULT_NAMESPACE, Boolean.TRUE);

        XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(os);
        IndentingXMLStreamWriter indentingStreamWriter = new IndentingXMLStreamWriter(streamWriter);
        indentingStreamWriter.setIndentStep("    ");

        rootNode.write(indentingStreamWriter);

        streamWriter.close();
    }

    @Override
    public void clear() {
        rootNode.clear();
        try {
            Files.delete(tempDirectory);
        } catch (IOException e) {
            throw new SystemException(e);
        }
    }

    private static Path createTempDirectory() {
        String tempDir = System.getProperty("java.io.tmpdir");
        Validate.notEmpty(tempDir);
        try {
            return Files.createTempDirectory(Paths.get(tempDir), "elza-export-");
        } catch (IOException e) {
            throw new SystemException(e);
        }
    }

    @Override
    public boolean canExportDeletedAPs() {
        // Deleted APs are unsupported
        return false;
    }
}
