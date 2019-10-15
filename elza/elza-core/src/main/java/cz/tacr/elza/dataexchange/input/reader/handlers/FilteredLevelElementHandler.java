package cz.tacr.elza.dataexchange.input.reader.handlers;

import java.util.HashMap;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.dataexchange.input.sections.SectionLevelProcessor;
import cz.tacr.elza.schema.v2.Level;

/**
 * Filtered handler will filter out root nodes
 *
 *
 */
public class FilteredLevelElementHandler extends SectionLevelElementHandler {

	HashMap<String, String> filter = new HashMap<>();

	public FilteredLevelElementHandler(ImportContext context) {
		super(context);
	}

	@Override
	protected void handleJaxbElement(JAXBElement<Level> element) {
		Level level = element.getValue();

		// check if root item
		String pid = level.getPid();
		if (pid == null) {
			// root node which should be filtered
			filter.put(level.getId(), null);
			// skip further processing
			return;
		} else {
			// check if filtered and update pid value
			pid = filter.getOrDefault(level.getPid(), level.getPid());
			level.setPid(pid);
		}

		ItemProcessor processor = new SectionLevelProcessor(context);
		processor.process(level);
	}
}
