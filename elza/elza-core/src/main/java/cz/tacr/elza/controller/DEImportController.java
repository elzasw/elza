package cz.tacr.elza.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.common.ZipUtils;
import cz.tacr.elza.dataexchange.input.DEImportParams;
import cz.tacr.elza.dataexchange.input.DEImportParams.ImportPositionParams;
import cz.tacr.elza.dataexchange.input.DEImportService;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.context.ImportPhaseChangeListener;
import cz.tacr.elza.dataexchange.input.sections.context.ImportPosition;
import cz.tacr.elza.dataexchange.input.sections.context.SectionsContext;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Data exchange import controller.
 */
@RestController
@RequestMapping(value = "/api/import/")
public class DEImportController {

    private final DEImportService importService;

    private final IEventNotificationService eventNotificationService;

    @Autowired
    public DEImportController(DEImportService importService, IEventNotificationService eventNotificationService) {
        this.importService = importService;
        this.eventNotificationService = eventNotificationService;
    }

    @RequestMapping(value = "transformations", method = RequestMethod.GET)
    public List<String> getTransformations() {
        try {
            return importService.getTransformationNames();
        } catch (IOException e) {
            throw new SystemException("Failed to list import transformations", e, BaseCode.SYSTEM_ERROR);
        }
    }

    @RequestMapping(value = "import", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void importData(@RequestPart(name = "importPositionParams", required = false) final ImportPositionParams importPositionParams,
                           @RequestParam(name = "transformationName", required = false) final String transformationName,
                           @RequestParam("scopeId") final int scopeId,
	        @RequestParam("xmlFile") final MultipartFile xmlFile,
	        @RequestParam(name = "ignoreRootNodes", required = false) final Boolean ignoreRootNodes) {

        // TODO: XML transformation
        if (StringUtils.isNotEmpty(transformationName)) {
            throw new UnsupportedOperationException("Import transformation not implemented");
        }

        MultipartFile srcFile = xmlFile;

        // unzipped if zip file
        File unzipFile = ZipUtils.unzipFile(xmlFile);
        if (unzipFile != null) {
            // convert File -> MultipartFile (https://stackoverflow.com/questions/16648549/converting-file-to-multipartfile)
            try {
                srcFile = new MockMultipartFile("file", unzipFile.getName(), "text/plain", Files.readAllBytes(unzipFile.toPath()));
            } catch (IOException e) {
                throw new SystemException("Error reading from file=" + unzipFile.getAbsolutePath(), e);
            }
        }

        // prepare import parameters
        DEImportParams params = new DEImportParams(scopeId, 1000, 10000, importPositionParams, ignoreRootNodes);
        params.addImportPhaseChangeListeners(new SectionNotifications(eventNotificationService));

        // validate
        try (InputStream is = srcFile.getInputStream()) {
            importService.validateData(is);
        } catch (IOException e) {
            throw new SystemException("Failed to read import source", e);
        }

        // import
        try (InputStream is = srcFile.getInputStream()) {
            importService.importData(is, params);
        } catch (IOException e) {
            throw new SystemException("Failed to read import source", e);
        }
    }

    public static class SectionNotifications implements ImportPhaseChangeListener {

        private final IEventNotificationService eventNotificationService;

        public SectionNotifications(IEventNotificationService eventNotificationService) {
            this.eventNotificationService = eventNotificationService;
        }

        @Override
        public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context) {
            SectionsContext sections = context.getSections();
            ImportPosition importPosition = sections.getImportPostition();

            if (nextPhase == ImportPhase.SECTIONS && importPosition == null) {
                sections.registerSectionProcessedListener(s -> eventNotificationService
                        .publishEvent(EventFactory.createIdEvent(EventType.FUND_CREATE, s.getFund().getFundId())));
                return false;

            }
            if (previousPhase == ImportPhase.SECTIONS && importPosition != null) {
                // TODO: consider using other notification fo resetting node structure,
                //       e.g.: ADD_LEVEL_UNDER
                eventNotificationService.publishEvent(new EventIdsInVersion(EventType.NODES_CHANGE,
                        importPosition.getFundVersion().getFundVersionId(), importPosition.getParentLevel().getNodeId()));
                return false;
            }
            return !ImportPhase.SECTIONS.isSubsequent(nextPhase);
        }
    }
}
