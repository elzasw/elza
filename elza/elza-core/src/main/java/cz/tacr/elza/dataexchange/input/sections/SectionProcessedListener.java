package cz.tacr.elza.dataexchange.input.sections;

import cz.tacr.elza.dataexchange.input.sections.context.SectionContext;

public interface SectionProcessedListener {

    void onSectionProcessed(SectionContext section);
}
