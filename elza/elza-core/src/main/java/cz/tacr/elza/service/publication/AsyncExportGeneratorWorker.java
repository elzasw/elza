package cz.tacr.elza.service.publication;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.asynchactions.AsyncRequest;
import cz.tacr.elza.asynchactions.AsyncRequestEvent;
import cz.tacr.elza.asynchactions.IAsyncRequest;
import cz.tacr.elza.asynchactions.IAsyncWorker;
import cz.tacr.elza.dataexchange.output.DEExportParams;
import cz.tacr.elza.dataexchange.output.DEExportService;
import cz.tacr.elza.dataexchange.output.writer.xml.XmlExportBuilder;
import cz.tacr.elza.domain.ArrExport;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.RulExportFilter;
import cz.tacr.elza.exception.ExceptionResponse;
import cz.tacr.elza.exception.ExceptionResponseBuilder;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.PublicationService;
import cz.tacr.elza.service.UserService;
import jakarta.persistence.EntityManager;

@Component
@Scope("prototype")
public class AsyncExportGeneratorWorker implements IAsyncWorker {

    private final static Logger logger = LoggerFactory.getLogger(AsyncExportGeneratorWorker.class);

    @Autowired
    private UserService userService;

    @Autowired
    private DmsService dmsService;

    @Autowired
    private DEExportService deExportService;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PublicationService publicationService;

    private Long beginTime;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final AsyncRequest request;

    public AsyncExportGeneratorWorker(final List<AsyncRequest> requests) {
        if (CollectionUtils.isNotEmpty(requests)) {
            Validate.isTrue(requests.size() == 1, "Only single request processing is supported by this worker");
            this.request = requests.get(0);
        } else {
            this.request = null;
        }
    }

    @Override
    public void run() {
        running.set(true);
        beginTime = System.currentTimeMillis();
        boolean success = false;
        Throwable failure = null;
        // Snapshot the thread's current security context so we can restore it
        // after we finish — worker threads are pooled, leaving our user's
        // context behind would leak permissions into the next task.
        SecurityContext originalSecCtx = SecurityContextHolder.getContext();
        try {
            // Set the security context to the user who created the publication
            // request (arr_export.user_id). DEExportService and any downstream
            // permission / scope checks read it.
            SecurityContext userCtx = userService.createSecurityContext(request.getUserId());
            SecurityContextHolder.setContext(userCtx);

            // Capture the IDs needed for the retention sweep inside the
            // transaction so we can use them after the entity is detached.
            int[] sweepIds = new TransactionTemplate(transactionManager).execute(status -> {
                ArrExport export = generateExport(request.getExportId());
                return new int[] {
                        export.getFundVersion().getFundId(),
                        export.getExportType().getExportTypeId()
                };
            });
            // Retention sweep runs in its own transaction (PublicationService
            // is @Transactional, called from outside any TX → REQUIRED opens a
            // new one). A sweep failure must not turn a successful generation
            // into PREPARE_ERROR; the freshly prepared file stays usable and
            // retention will catch up on the next successful generation.
            try {
                publicationService.sweepRetention(sweepIds[0], sweepIds[1]);
            } catch (Exception sweepEx) {
                logger.warn("Retention sweep failed for export {} (fund {} / type {})",
                        request.getExportId(), sweepIds[0], sweepIds[1], sweepEx);
            }
            success = true;
        } catch (Throwable t) {
            failure = t;
            new TransactionTemplate(transactionManager).execute(status -> {
                handleExportError(t);
                return null;
            });
        } finally {
            // Restore the thread's pre-worker security context. If nothing was
            // set previously (empty context), clear instead so we don't pin an
            // empty Authentication on the thread.
            SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
            if (emptyContext.equals(originalSecCtx)) {
                SecurityContextHolder.clearContext();
            } else {
                SecurityContextHolder.setContext(originalSecCtx);
            }
            eventPublisher.publishEvent(success ? AsyncRequestEvent.success(request, this) : AsyncRequestEvent.fail(request, this, failure));
            running.set(false);
        }
    }

	@Override
	public IAsyncRequest getRequest() {
		return request;
	}

	@Override
	public Long getBeginTime() {
		return beginTime;
	}

	@Override
	public Long getRunningTime() {
        if (beginTime != null) {
            return System.currentTimeMillis() - beginTime;
        } else {
            return null;
        }
	}

	@Override
	public void terminate() {
        while (running.get()) {
            try {
                logger.info("Čekání na dokončení generování výstupu: {}", request.getExportId());
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Nothing to do with this -> simply finish
                Thread.currentThread().interrupt();
            }
        }
	}

	@Override
	public List<? extends IAsyncRequest> getRequests() {
		return Collections.singletonList(request);
	}

	/**
     * Generate publication XML for an existing arr_export record.
     * Runs inside a write transaction opened by {@link #run()}; the security
     * context has already been set on the worker thread by {@link #run()}.
	 *
	 * @param exportId
	 * @return the persisted (now PREPARED) {@code arr_export} entity, so the
	 *         caller can read {@code fundId} / {@code exportTypeId} before
	 *         the transaction commits and the entity detaches
	 */
	private ArrExport generateExport(Integer exportId) {
	    ArrExport export = em.find(ArrExport.class, exportId);
	    if (export == null) {
	        throw new SystemException("Export not found", BaseCode.ID_NOT_EXIST)
	                .set("exportId", exportId);
	    }

	    // Security context is set by run() before this method is called — see
	    // there for the lifecycle (set on entry, restored in finally). Don't
	    // touch it here: re-setting would mask the snapshot, and we'd lose the
	    // ability to restore the original context on the worker thread.

	    ArrFundVersion fundVersion = export.getFundVersion();
	    RulExportFilter exportFilter = export.getExportFilter();

	    DEExportParams params = new DEExportParams();
	    DEExportParams.FundSections section = new DEExportParams.FundSections();
	    section.setFundVersionId(fundVersion.getFundVersionId());
	    params.setFundsSections(Collections.singletonList(section));
	    params.setIncludeAccessPoints(true);
	    params.setIncludeUUID(true);
	    if (exportFilter != null) {
	        // exportXmlData() resolves the filter yaml by name via RuleService.
	        params.setExportFilter(exportFilter.getCode());
	    }

	    // build file name
	    String fileName = "publication-" + export.getExportId() + ".xml";
	    
	    DmsFile dmsFile = new DmsFile();
	    dmsFile.setName(fileName);
	    dmsFile.setFileName(fileName);
	    dmsFile.setMimeType("application/xml");
	    dmsFile.setFileSize(0);

	    try {
	        dmsService.createFile(dmsFile, os -> {
	            try {
	                deExportService.exportXmlData(os, new XmlExportBuilder(), params);
	            } catch (RuntimeException e) {
	                throw e;
	            } catch (Exception e) {
	                // Consumer<OutputStream> doesn't allow checked exceptions —
	                // wrap and let createFile() propagate it out as IOException.
	                throw new SystemException("Failed to generate publication XML", e, BaseCode.INVALID_STATE);
	            }
	        });
	    } catch (IOException e) {
	        throw new SystemException("Failed to write publication file", e, BaseCode.INVALID_STATE);
	    }

	    // Opaque monotonic cursor for the public publication API.
	    Number seq = (Number) em.createNativeQuery("SELECT nextval('arr_export_seq')").getSingleResult();

	    export.setFile(dmsFile);
	    export.setExportSeq(seq.longValue());
	    export.setState(ArrExport.State.PREPARED);
	    export.setPreparedAt(OffsetDateTime.now());
	    return export;
	}

    /**
     * Persist error state on the export entity. Called in its own transaction
     * after {@link #generateExport(Integer, Integer)} failed.
	 */
	private void handleExportError(Throwable t) {
	    ExceptionResponseBuilder builder = ExceptionResponseBuilder.createFrom(t);
	    builder.logError(logger);

	    ArrExport export = em.find(ArrExport.class, request.getExportId());
	    if (export == null) {
	        return;
	    }
	    ExceptionResponse er = builder.build();
	    export.setState(ArrExport.State.PREPARE_ERROR);
	    export.setErrorAt(OffsetDateTime.now());
	    export.setErrorMessage(er.toJson());
	}

}