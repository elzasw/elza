package cz.tacr.elza.dataexchange.output;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.service.UserService;

@Service
public class IOExportWorker implements SmartLifecycle {

    static final Logger log = LoggerFactory.getLogger(IOExportWorker.class);

    private enum ThreadStatus {
        RUNNING, STOP_REQUEST, STOPPED
    }

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    private DEExportService exportService;

    @Autowired
    private UserService userService;

    private static int requestCount = 0; 

    private LinkedList<IOExportRequest> exportRequests = new LinkedList<>();

    private Map<Integer, IOExportResult> mapExportResult = new HashMap<>();
    
    private LoadingCache<Integer, IOExportResult> mapCacheResult;    

    private ThreadStatus status = ThreadStatus.STOPPED;

    private final Object lock = new Object();

    @PostConstruct
    protected void init() {
        CacheLoader<Integer, IOExportResult> loader = new CacheLoader<Integer, IOExportResult>() {
            @Override
            public IOExportResult load(Integer key) throws Exception {
                return mapExportResult.get(key);
            }
        };
        RemovalListener<Integer, IOExportResult> removalListener = new RemovalListener<Integer, IOExportResult>() {
            @Override
            public void onRemoval(RemovalNotification<Integer, IOExportResult> notification) {
                Path filePath = resourcePathResolver.getExportXmlTrasnformDir().resolve(notification.getKey() + ".xml");
                try {
                    Files.delete(filePath);
                } catch (IOException e) {
                    log.error("Error deleting a file: {}", filePath);
                }
                synchronized (lock) {
                    mapExportResult.remove(notification.getKey());
                }
            }
        };
        mapCacheResult = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES) // časový interval uchování souboru (5 min)
                .removalListener(removalListener)
                .build(loader);
    }

    public int addExportRequest(IOExportRequest exportRequest) {
        synchronized (lock) {
            exportRequest.setRequestId(++requestCount);
            exportRequests.add(exportRequest);         
            lock.notifyAll();
            return requestCount;
        }        
    }

    public IOExportResult getExportState(Integer requestId) {
        boolean isProcessing = exportRequests.stream().anyMatch(i -> i.getRequestId().equals(requestId));
        if (isProcessing) {
            return new IOExportResult(IOExportState.PROCESSING);
        }

        // activation of deletion of expired records
        mapCacheResult.cleanUp();

        synchronized (lock) {
            IOExportResult result = mapExportResult.get(requestId);
            if (result != null) {
                return result;
            }
        }

        return new IOExportResult(IOExportState.NOT_FOUND);
    }

    private void exportData(IOExportRequest request) throws IOException {

        // create security context
        SecurityContext secCtx = userService.createSecurityContext(request.getUserId());
        SecurityContextHolder.setContext(secCtx);

        exportService.exportXmlData(request);        
    }

    public void run() {

        while (status == ThreadStatus.RUNNING) {
            IOExportRequest request;

            synchronized (lock) {
                // get next request
                request = exportRequests.poll();
                if (request == null) {
                    // if no pending request -> wait and continue
                    try {
                        // wake up every minute to check for changes
                        lock.wait(1000 * 60);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage());
                    }
                    continue;
                }
            }

            IOExportResult result;
            try {
                exportData(request);
                result = new IOExportResult(IOExportState.OK);
             } catch (Exception ex) {
                 // prepare result
                 result = new IOExportResult(IOExportState.ERROR, ex);
                 log.error("Error in export process.", ex);
             }
            synchronized (lock) {
                // store result
                mapExportResult.put(request.getRequestId(), result);
                // insert item from result map to cached map
                mapCacheResult.getUnchecked(request.getRequestId());                
            }
        }
        status = ThreadStatus.STOPPED;
        lock.notifyAll();        
    }

    @Override
    public void start() {
        log.info("Starting service...");
        status = ThreadStatus.RUNNING;
        new Thread(() -> {
            run();
        }).start();
        log.info("Service started.");
    }

    @Override
    public void stop() {
        log.info("Stopping service...");
        Validate.isTrue(status == ThreadStatus.RUNNING);
        status = ThreadStatus.STOP_REQUEST;

        synchronized (lock) {
            lock.notifyAll();
            while (status != ThreadStatus.STOPPED) {
                try {
                    lock.wait(100);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                    break;
                }
            }
        }
        log.info("Service is stopped.");
    }

    @Override
    public boolean isRunning() {
        return status == ThreadStatus.RUNNING;
    }

    public static class IOExportResult {
        private final IOExportState state;
        private final Exception exception;

        public IOExportResult(IOExportState state) {
            this.state = state;
            this.exception = null;
        }

        public IOExportResult(IOExportState state, Exception exception) {
            this.state = state;
            this.exception = exception;
        }

        public IOExportState getState() {
            return state;
        }

        public Exception getException() {
            return exception;
        }
    }
}