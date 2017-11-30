package cz.tacr.elza.dataexchange.input.sections;

import cz.tacr.elza.dataexchange.input.sections.context.ContextSection;

public interface SectionProcessedListener {

    void onSectionProcessed(ContextSection section);
}
