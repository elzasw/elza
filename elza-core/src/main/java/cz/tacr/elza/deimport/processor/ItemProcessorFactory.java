package cz.tacr.elza.deimport.processor;

import cz.tacr.elza.deimport.aps.AccessPointEntryProcessor;
import cz.tacr.elza.deimport.aps.AccessPointProcessor;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.institutions.InstitutionProcessor;
import cz.tacr.elza.deimport.parties.FamilyProcessor;
import cz.tacr.elza.deimport.parties.PartyGroupProcessor;
import cz.tacr.elza.deimport.parties.PartyProcessor;
import cz.tacr.elza.deimport.sections.SectionLevelProcessor;
import cz.tacr.elza.deimport.sections.SectionPacketProcessor;
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

    public static ItemProcessor createSectionPacketProcessor(ImportContext context) {
        return new SectionPacketProcessor(context.getSections());
    }

    public static ItemProcessor createSectionLevelProcessor(ImportContext context) {
        return new SectionLevelProcessor(context);
    }

    public static ItemProcessor createInstitutionProcessor(ImportContext context) {
        return new InstitutionProcessor(context);
    }
}
