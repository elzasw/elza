package cz.tacr.elza.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.BatchState;
import cz.tacr.elza.domain.FundImportStrategy;
import cz.tacr.elza.domain.ImpBatchEdx;
import cz.tacr.elza.domain.ImpItem;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.ImpBatchService;
import cz.tacr.elza.service.ImpItemService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Data exchange import controller.
 */
@RestController
@RequestMapping(value = "/api/import")
public class DEImportController {

    private final DEImportService importService;

    private final IEventNotificationService eventNotificationService;

    @Autowired
    private ImpBatchService batchService;
    @Autowired
    private ImpItemService itemService;
    @Autowired
    private AsyncRequestService asyncRequestService;
    @Autowired
    private ScopeRepository scopeRepository;
    @Autowired
    private UserService userService;

    @Autowired
    public DEImportController(DEImportService importService, IEventNotificationService eventNotificationService) {
        this.importService = importService;
        this.eventNotificationService = eventNotificationService;
    }

    @RequestMapping(value = "/transformations", method = RequestMethod.GET)
    public List<String> getTransformations() {
        try {
            return importService.getTransformationNames();
        } catch (IOException e) {
            throw new SystemException("Failed to list import transformations", e, BaseCode.SYSTEM_ERROR);
        }
    }

    @RequestMapping(value = "/import", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importData(@RequestPart(name = "importPositionParams", required = false) final ImportPositionParams importPositionParams,
                                        @RequestParam(name = "transformationName", required = false) final String transformationName,
                                        @RequestParam("scopeId") final int scopeId,
                                        @RequestParam("xmlFile") final MultipartFile xmlFile,
                                        @RequestParam(name = "ignoreRootNodes", required = false) final Boolean ignoreRootNodes,
                                        @RequestParam(name = "asBatch", required = false) final Boolean asBatch) {

        // TODO: XML transformation
        if (StringUtils.isNotEmpty(transformationName)) {
            throw new UnsupportedOperationException("Import transformation not implemented");
        }

        MultipartFile srcFile = xmlFile;

        // unzipped if zip file
        ZipUtils.UnzippedFile unzipped = ZipUtils.unzipFirstFile(xmlFile);
        if (unzipped != null) {
            // convert File -> MultipartFile (https://stackoverflow.com/questions/16648549/converting-file-to-multipartfile)
            try {
                srcFile = new MockMultipartFile("file", unzipped.originalName(), "text/plain",
                        Files.readAllBytes(unzipped.file().toPath()));
            } catch (IOException e) {
                throw new SystemException("Error reading from file=" + unzipped.file().getAbsolutePath(), e);
            }
        }

        // validate XSD up front regardless of the execution mode
        try (InputStream is = srcFile.getInputStream()) {
            importService.validateData(is);
        } catch (IOException e) {
            throw new SystemException("Failed to read import source", e);
        }

        // New-fund imports run through the batch pipeline (async, tracked, retryable);
        // callers that need the old synchronous behaviour (integration tests) opt out
        // with asBatch=false. Position-based imports are always synchronous.
        if (importPositionParams == null && !Boolean.FALSE.equals(asBatch)) {
            return runAsBatch(scopeId, srcFile, ignoreRootNodes, xmlFile.getOriginalFilename());
        }

        DEImportParams params = new DEImportParams(scopeId, 1000, 10000, importPositionParams, ignoreRootNodes);
        params.addImportPhaseChangeListeners(new SectionNotifications(eventNotificationService));
        try (InputStream is = srcFile.getInputStream()) {
            importService.importData(is, params);
        } catch (IOException e) {
            throw new SystemException("Failed to read import source", e);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Wraps the incoming EDX2 payload in an import batch (ALWAYS_NEW), enqueues it and returns
     * the batch id so the caller can watch the outcome asynchronously.
     */
    private ResponseEntity<?> runAsBatch(int scopeId,
                                         MultipartFile srcFile,
                                         Boolean ignoreRootNodes,
                                         String batchName) {
        ApScope scope = scopeRepository.findById(scopeId)
                .orElseThrow(() -> new SystemException("Scope not found: " + scopeId, BaseCode.SYSTEM_ERROR));
        // Batch is named after what the operator actually picked in the file dialog (zip or xml);
        // the item inside keeps the unpacked file name so the run can be traced.
        String name = batchName != null && !batchName.isBlank()
                ? batchName
                : (srcFile.getOriginalFilename() != null
                        ? srcFile.getOriginalFilename()
                        : "import-" + System.currentTimeMillis());
        String itemName = srcFile.getOriginalFilename() != null ? srcFile.getOriginalFilename() : name;

        ImpBatchEdx batch = batchService.createEdxBatch(
                name,
                FundImportStrategy.ALWAYS_NEW,
                null,
                Boolean.TRUE.equals(ignoreRootNodes),
                scope,
                false,
                false);

        ImpItem item;
        try (InputStream in = srcFile.getInputStream()) {
            item = itemService.uploadItem(batch, itemName, srcFile.getContentType(),
                    (int) Math.min(srcFile.getSize(), Integer.MAX_VALUE), in);
        } catch (IOException e) {
            throw new SystemException("Failed to read import source", e);
        }

        batchService.changeState(batch, BatchState.IN_PROGRESS);
        UserDetail userDetail = userService.getLoggedUserDetail();
        Integer userId = userDetail == null ? null : userDetail.getId();
        asyncRequestService.enqueue(batch, userId);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(Map.of("batchId", batch.getBatchId(), "itemId", item.getItemId()));
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
