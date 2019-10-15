package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.reader.XmlElementReader;

public class SectionElementHandler extends ContextAwareElementHandler {

	private final XmlElementReader reader;

	private final boolean ignoreRootNodes;

	public SectionElementHandler(ImportContext context, XmlElementReader reader, boolean ignoreRootNodes) {
        super(context, ImportPhase.SECTIONS);
		this.reader = reader;
		this.ignoreRootNodes = ignoreRootNodes;
    }

    @Override
    public void handleStart(XMLEventReader eventReader) {
        StartElement startElement;
        try {
            startElement = eventReader.peek().asStartElement();
        } catch (XMLStreamException e) {
            throw new DEImportException("Cannot read section start element");
        }

		installSectionHandlers();

        Attribute ruleSetCode = startElement.getAttributeByName(new QName("rule"));
        context.getSections().beginSection(ruleSetCode.getValue());
    }

	/**
	 * Install all section specific handlers
	 */
	private void installSectionHandlers() {
		reader.addElementHandler("/edx/fs/s/fi", new FundInfoElementHandler(context));
        reader.addElementHandler("/edx/fs/s/sts", new StructTypesHandler(context));
		reader.addElementHandler("/edx/fs/s/sts/st", new StructTypeElementHandler(context));
		reader.addElementHandler("/edx/fs/s/sts/st/sos/so", new StructObjectElementHandler(context));

		SectionLevelElementHandler levelHandler;
		if (ignoreRootNodes) {
			levelHandler = new FilteredLevelElementHandler(context);
		} else {
			levelHandler = new SectionLevelElementHandler(context);
		}
		reader.addElementHandler("/edx/fs/s/lvls/lvl", levelHandler);
	}

	@Override
    public void handleEnd() {
		uninstallSectionHandlers();
        context.getSections().endSection();
    }

	private void uninstallSectionHandlers() {
		reader.removeElementHandler("/edx/fs/s/lvls/lvl");
		reader.removeElementHandler("/edx/fs/s/sts/st");
        reader.removeElementHandler("/edx/fs/s/sts/st/sos/so");
		reader.removeElementHandler("/edx/fs/s/fi");
	}
}
