package cz.tacr.elza.dataexchange.input.institutions;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.institutions.context.InstitutionsContext;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParInstitutionType;
import cz.tacr.elza.schema.v2.Institution;

public class InstitutionProcessor implements ItemProcessor {

    private final InstitutionsContext context;

    public InstitutionProcessor(ImportContext context) {
        this.context = context.getInstitutions();
    }

    @Override
    public void process(Object item) {
        Institution institution = (Institution) item;
        processInstitution(institution);
    }

    private void processInstitution(Institution item) {

        ParInstitutionType instType = context.getInstitutionTypeByCode(item.getT());
        if (instType == null) {
            throw new DEImportException("Institution type not found, code=" + item.getT());
        }
        ParInstitution institution = new ParInstitution();
        institution.setInstitutionType(instType);
        institution.setInternalCode(item.getC());
        context.addInstitution(institution);
    }
}
