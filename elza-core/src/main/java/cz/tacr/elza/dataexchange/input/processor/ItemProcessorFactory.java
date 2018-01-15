package cz.tacr.elza.dataexchange.input.processor;

import cz.tacr.elza.dataexchange.input.aps.AccessPointEntryProcessor;
import cz.tacr.elza.dataexchange.input.aps.AccessPointProcessor;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.institutions.InstitutionProcessor;
import cz.tacr.elza.dataexchange.input.parties.FamilyProcessor;
import cz.tacr.elza.dataexchange.input.parties.PartyGroupProcessor;
import cz.tacr.elza.dataexchange.input.parties.PartyProcessor;
import cz.tacr.elza.dataexchange.input.sections.SectionLevelProcessor;
import cz.tacr.elza.dataexchange.input.sections.SectionPacketProcessor;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.schema.v2.Event;
import cz.tacr.elza.schema.v2.Person;

/**
 * Factory for item processors. New processor should be created for each item.
 */
public final class ItemProcessorFactory {

    public static ItemProcessor createAccessPointProcessor(ImportContext context) {
        return new AccessPointProcessor(context);
    }

    public static ItemProcessor createPartyAccessPointEntryProcessor(ImportContext context) {
        return new AccessPointEntryProcessor(context, true);
    }

    public static ItemProcessor createPersonProcessor(ImportContext context) {
        return new PartyProcessor<Person, ParPerson>(context, ParPerson.class);
    }

    public static ItemProcessor createPartyGroupProcessor(ImportContext context) {
        return new PartyGroupProcessor(context);
    }

    public static ItemProcessor createFamilyProcessor(ImportContext context) {
        return new FamilyProcessor(context);
    }

    public static ItemProcessor createEventProcessor(ImportContext context) {
        return new PartyProcessor<Event, ParEvent>(context, ParEvent.class);
    }

    public static ItemProcessor createSectionLevelProcessor(ImportContext context) {
        return new SectionLevelProcessor(context);
    }

    public static ItemProcessor createInstitutionProcessor(ImportContext context) {
        return new InstitutionProcessor(context);
    }
}
