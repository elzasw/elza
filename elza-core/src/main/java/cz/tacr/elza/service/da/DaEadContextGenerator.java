package cz.tacr.elza.service.da;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.xml.sax.SAXException;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.schema.SchemaManager;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.print.OutputModel;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.TemplateRepository;
import cz.tacr.elza.service.output.OutputParams;
import cz.tacr.elza.service.output.generator.FreemarkerOutputGenerator;
import cz.tacr.elza.service.output.generator.OutputGeneratorFactory;
import freemarker.template.TemplateException;

/**
 * Contextual archival description of an AIP ({@code EAD-CONTEXT.xml} of a change package for the
 * digital archive).
 *
 * <p>The description is rendered by the EAD template of the ZP2015 rules in its contextual mode
 * ({@code eadContext}): the fund root, the path to every unit linked to the AIP and the subtrees of
 * the linked units. Linked units carry a {@code dao} with the METS ID of the linked part (or the
 * AIP code for a link to the whole AIP). The EAD namespace is written as the default namespace, as
 * the Czech EAD3 profile requires for packages of the digital archive (E-ARK).
 *
 * <p>See {@code docs/design/ead3-export.md}.
 */
@Service
public class DaEadContextGenerator {

    /** EAD template of the ZP2015 rules, rendered in the contextual mode. */
    static final String TEMPLATE_CODE = "ZP2015_INVENTAR_EAD";

    static final String EAD_NS = "http://ead3.archivists.org/schema/";

    private final OutputGeneratorFactory outputGeneratorFactory;
    private final TemplateRepository templateRepository;
    private final ResourcePathResolver resourcePathResolver;
    private final ArrDaLinkRepository daLinkRepository;
    private final FundVersionRepository fundVersionRepository;
    private final SchemaManager schemaManager;

    public DaEadContextGenerator(OutputGeneratorFactory outputGeneratorFactory,
                                 TemplateRepository templateRepository,
                                 ResourcePathResolver resourcePathResolver,
                                 ArrDaLinkRepository daLinkRepository,
                                 FundVersionRepository fundVersionRepository,
                                 SchemaManager schemaManager) {
        this.outputGeneratorFactory = outputGeneratorFactory;
        this.templateRepository = templateRepository;
        this.resourcePathResolver = resourcePathResolver;
        this.daLinkRepository = daLinkRepository;
        this.fundVersionRepository = fundVersionRepository;
        this.schemaManager = schemaManager;
    }

    /**
     * Writes the contextual description of the AIP. Must be called inside a transaction.
     *
     * @throws IOException when the description cannot be created; the message is meant for the user
     */
    public void write(DaAip aip, Path file) throws IOException {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Contextual EAD needs a transaction");
        }
        List<ArrDaLink> links = daLinkRepository.findByAipIdAndDeleteChangeIsNull(aip.getAipId());
        if (links.isEmpty()) {
            throw new IOException("AIP není napojený na jednotku popisu.");
        }
        Set<Integer> fundIds = links.stream().map(l -> l.getNode().getFundId()).collect(Collectors.toSet());
        if (fundIds.size() > 1) {
            throw new IOException("AIP je napojený na jednotky popisu ve více archivních souborech.");
        }
        ArrFundVersion version = fundVersionRepository.findByFundIdAndLockChangeIsNull(fundIds.iterator().next());
        RulTemplate template = templateRepository.findByCode(TEMPLATE_CODE);
        if (template == null) {
            throw new IOException("Chybí šablona " + TEMPLATE_CODE + " pro kontextový popis (balíček pravidel ZP2015).");
        }

        // the model needs an output only for its fund and type, nothing is stored
        ArrOutput output = new ArrOutput();
        output.setFund(version.getFund());
        output.setOutputType(template.getOutputType());
        output.setName("Kontextový popis AIP " + aip.getCode());
        ArrChange change = new ArrChange();
        change.setChangeDate(OffsetDateTime.now());
        List<Integer> nodeIds = new ArrayList<>(links.stream().map(ArrDaLink::getNodeId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        OutputModel model = outputGeneratorFactory.createOutputModel();
        model.init(new OutputParams(output, change, version, nodeIds, List.of()));

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("output", model);
        dataModel.put("eadContext", true);
        StringWriter ead = new StringWriter();
        try {
            FreemarkerOutputGenerator.process(resourcePathResolver.getTemplateDir(template), dataModel, ead);
        } catch (TemplateException e) {
            throw new IOException("Kontextový popis AIP se nepodařilo vytvořit: " + e.getMessage(), e);
        }

        byte[] result = toDefaultNamespace(ead.toString());
        try {
            schemaManager.getSchema(EAD_NS).newValidator()
                    .validate(new StreamSource(new ByteArrayInputStream(result)));
        } catch (SAXException e) {
            throw new IOException("Kontextový popis AIP neodpovídá schématu EAD3: " + e.getMessage(), e);
        }
        Files.write(file, result);
    }

    /**
     * Rewrites the EAD elements from the {@code ead:} prefix to the default namespace; other
     * namespaces (CAM) keep their prefixes.
     */
    static byte[] toDefaultNamespace(String xml) throws IOException {
        XMLEventFactory events = XMLEventFactory.newInstance();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(new StringReader(xml));
            XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(os, "UTF-8");
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isStartDocument()) {
                    event = events.createStartDocument("UTF-8", "1.0");
                } else if (event.isStartElement()) {
                    StartElement start = event.asStartElement();
                    List<Namespace> namespaces = new ArrayList<>();
                    for (Iterator<Namespace> it = start.getNamespaces(); it.hasNext();) {
                        Namespace ns = it.next();
                        namespaces.add(EAD_NS.equals(ns.getNamespaceURI())
                                ? events.createNamespace(EAD_NS)
                                : ns);
                    }
                    QName name = start.getName();
                    String prefix = EAD_NS.equals(name.getNamespaceURI()) ? "" : name.getPrefix();
                    event = events.createStartElement(prefix, name.getNamespaceURI(), name.getLocalPart(),
                            start.getAttributes(), namespaces.iterator());
                } else if (event.isEndElement()) {
                    EndElement end = event.asEndElement();
                    QName name = end.getName();
                    if (EAD_NS.equals(name.getNamespaceURI())) {
                        event = events.createEndElement("", EAD_NS, name.getLocalPart());
                    }
                }
                writer.add(event);
            }
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Kontextový popis AIP se nepodařilo vytvořit: " + e.getMessage(), e);
        }
        return os.toByteArray();
    }
}
