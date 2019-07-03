package cz.tacr.elza.daoimport;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.dspace.content.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import cz.tacr.elza.daoimport.service.DaoImportService;

@Service
public class DaoImportScheduler {

    private static Logger log = Logger.getLogger(DaoImportScheduler.class);

    @Autowired
    private DaoImportService daoImportService;

    @Scheduled(initialDelay = 5000, fixedDelay = 5000)
    public void runImport() throws IOException {
        daoImportService.importDAOs();
    }
}
