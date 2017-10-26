package cz.tacr.elza.dataexchange.input.institutions;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.institutions.context.InstitutionsContext;
import cz.tacr.elza.dataexchange.input.parties.context.PartiesContext;
import cz.tacr.elza.dataexchange.input.parties.context.PartyInfo;
import cz.tacr.elza.dataexchange.input.processor.ItemProcessor;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParInstitutionType;
import cz.tacr.elza.schema.v2.Institution;

public class InstitutionProcessor implements ItemProcessor {

    private final PartiesContext partiesContext;

    private final InstitutionsContext institutionsContext;

    private PartyInfo partyInfo;

    private ParInstitutionType institutionType;

    public InstitutionProcessor(ImportContext context) {
        this.partiesContext = context.getParties();
        this.institutionsContext = context.getInstitutions();
    }

    @Override
    public void process(Object item) {
        Institution institution = (Institution) item;
        prepareCachedReferences(institution);
        validateInstitution(institution);
        processInstitution(institution);
    }

    private void prepareCachedReferences(Institution item) {
        partyInfo = partiesContext.getPartyInfo(item.getPaid());
        institutionType = institutionsContext.getInstitutionTypeByCode(item.getT());
    }

    private void validateInstitution(Institution item) {
        if (partyInfo == null) {
            throw new DEImportException("Institution party not found, partyId:" + item.getPaid());
        }
        if (institutionType == null) {
            throw new DEImportException("Institution type not found, code:" + item.getT());
        }
    }

    private void processInstitution(Institution item) {
        ParInstitution institution = new ParInstitution();
        institution.setInstitutionType(institutionType);
        institution.setInternalCode(item.getC());
        institutionsContext.addInstitution(institution, partyInfo);
    }
}
