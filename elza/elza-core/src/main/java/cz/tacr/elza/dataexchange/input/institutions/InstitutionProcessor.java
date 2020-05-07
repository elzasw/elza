package cz.tacr.elza.dataexchange.input.institutions;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.institutions.context.InstitutionsContext;
import cz.tacr.elza.dataexchange.input.parts.context.PartInfo;
import cz.tacr.elza.dataexchange.input.parts.context.PartsContext;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParInstitutionType;
import cz.tacr.elza.schema.v2.Institution;

public class InstitutionProcessor implements ItemProcessor {

    private final PartsContext partsContext;

    private final InstitutionsContext context;

    private final AccessPointsContext apContext;

    public InstitutionProcessor(ImportContext context) {
        this.partsContext = context.getParts();
        this.apContext = context.getAccessPoints();
        this.context = context.getInstitutions();

    }

    @Override
    public void process(Object item) {
        Institution institution = (Institution) item;
        processInstitution(institution);
    }

    private void processInstitution(Institution item) {
       /* PartInfo partInfo = partsContext.getPartInfo(item.getPaid());
        AccessPointInfo apInfo = partInfo.getApInfo();*/

        AccessPointInfo apInfo = apContext.getApInfo(item.getPaid());

        ParInstitutionType instType = context.getInstitutionTypeByCode(item.getT());
        if (instType == null) {
            throw new DEImportException("Institution type not found, code=" + item.getT());
        }
        if (apInfo.getSaveMethod().equals(SaveMethod.IGNORE)) {
            return;
        }
        ParInstitution institution = new ParInstitution();
        institution.setInstitutionType(instType);
        institution.setInternalCode(item.getC());
        //institution.setAccessPoint();
        context.addInstitution(institution, apInfo);
    }
}
