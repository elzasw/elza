package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.parties.PartyProcessor;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.schema.v2.Person;

public class PersonElementHandler extends AbstractPartyElementHandler<Person> {

    public PersonElementHandler(ImportContext context) {
        super(context);
    }

    @Override
    protected void handlePartyElement(JAXBElement<Person> element) {
        ItemProcessor processor = new PartyProcessor<Person, ParPerson>(context, ParPerson.class);
        processor.process(element);
    }

    @Override
    public Class<Person> getType() {
        return Person.class;
    }
}
