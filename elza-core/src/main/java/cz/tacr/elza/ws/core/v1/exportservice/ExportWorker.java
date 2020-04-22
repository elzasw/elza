package cz.tacr.elza.ws.core.v1.exportservice;

import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.dataexchange.output.DEExportParams;
import cz.tacr.elza.dataexchange.output.DEExportService;
import cz.tacr.elza.dataexchange.output.writer.ExportBuilder;

@Component
@Scope("prototype")
public class ExportWorker implements Runnable {
	
	public interface ErrorHandler {
		void onError(Exception e);
	};

    private static final Logger logger = LoggerFactory.getLogger(ExportWorker.class);

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    DEExportService deExportService;

    final private OutputStream output;

    final private DEExportParams exportParams;

    final private Authentication authentication;

    final protected ExportBuilder exportBuilder;
    
    final protected ErrorHandler errorHandler;

    ExportWorker(final OutputStream output,
                 final ExportBuilder exportBuilder,
                 final DEExportParams params,
                       final Authentication auth,
                       final ErrorHandler errorHandler) {
        this.output = output;
        this.exportBuilder = exportBuilder;
        this.exportParams = params;
        this.authentication = auth;
        this.errorHandler = errorHandler;
    }

    @Override
    public void run() {
        try {
            new TransactionTemplate(transactionManager).execute(status -> {
                executeInTransaction();
                logger.info("EntityExportWorker succesfully finished: {}", this);
                return null;
            });
        } catch (Exception e) {
            logger.error("EntityExportWorker failed, error: ", e);
            errorHandler.onError(e);
        } finally {
            try {
                output.close();
            } catch (Exception e) {
            }        	
        }
    }

    private void executeInTransaction() {
        // prepare sec context
        SecurityContext originalSecCtx = SecurityContextHolder.getContext();
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(authentication);
        SecurityContextHolder.setContext(ctx);

        try {
            deExportService.exportXmlData(output, exportBuilder, exportParams);
        } finally {
            SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
            if (emptyContext.equals(originalSecCtx)) {
                SecurityContextHolder.clearContext();
            } else {
                SecurityContextHolder.setContext(originalSecCtx);
            }
        }

    }

}
