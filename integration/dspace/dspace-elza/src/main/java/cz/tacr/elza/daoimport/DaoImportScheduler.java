package cz.tacr.elza.daoimport;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import cz.tacr.elza.context.ContextUtils;
import cz.tacr.elza.daoimport.service.DaoImportService;
import cz.tacr.elza.daoimport.service.vo.ImportBatch;

@Service
public class DaoImportScheduler {

    private static Logger log = Logger.getLogger(DaoImportScheduler.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    @Autowired
    private DaoImportService daoImportService;

    @Scheduled(initialDelay = 5000, fixedDelay = 5000)
    public void runImport() throws SQLException {

        Context context = ContextUtils.createContext();
        context.setCurrentUser(getEPerson(context));

        try {
            context.turnOffAuthorisationSystem();
            List<ImportBatch> importBatches = daoImportService.prepareImport(context);
            daoImportService.importBatches(importBatches, context);
            log.info("Import skončil úspěšně");
        } catch (Exception e) {
            log.error("Nastala chyba importu", e);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private EPerson getEPerson(Context context) {
        String email = configurationService.getProperty("elza.daoimport.email");
        if (StringUtils.isBlank(email)) {
            throw new IllegalArgumentException("Není vyplněna hodnota elza.daoimport.email v souboru elza.cfg.");
        }

        EPerson ePerson = null;
        try {
            ePerson = ePersonService.findByEmail(context, email);
        } catch (SQLException e) {
            throw new IllegalStateException("Chyba při načítání osoby pod kterou bude spuštěn import");
        }

        if (ePerson == null) {
            throw new IllegalStateException("Nebyla nalezena osoba s e-mailem " + email);
        }

        return ePerson;
    }
}
