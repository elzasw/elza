package cz.tacr.elza.dataexchange.output;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

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

    // queue of export requests
    private LinkedList<IOExportRequest> exportRequests = new LinkedList<>();

    // lookup by id
    private Map<Integer, IOExportRequest> mapExportResult = new HashMap<>();
    
    // cach for timeout removal
    private LoadingCache<Integer, IOExportRequest> mapCacheResult;

    private ThreadStatus status = ThreadStatus.STOPPED;

    private final Object lock = new Object();

    @PostConstruct
    protected void init() {
        CacheLoader<Integer, IOExportRequest> loader = new CacheLoader<Integer, IOExportRequest>() {
            @Override
            public IOExportRequest load(Integer key) throws Exception {
                IOExportRequest r = mapExportResult.get(key);
                if (r != null) {
                    if (r.getState() == IOExportState.FINISHED || r.getState() == IOExportState.ERROR) {
                        return r;
                    }
                }
                return null;
            }
        };
        RemovalListener<Integer, IOExportRequest> removalListener = new RemovalListener<Integer, IOExportRequest>() {
            @Override
            public void onRemoval(RemovalNotification<Integer, IOExportRequest> notification) {
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

    public int addExportRequest(final Integer userId, final String downloadFileName, DEExportParams exportParams) {
        synchronized (lock) {
            IOExportRequest exportRequest = new IOExportRequest(userId, ++requestCount, downloadFileName, exportParams);

            // store result
            mapExportResult.put(exportRequest.getRequestId(), exportRequest);
            exportRequests.add(exportRequest);         
            lock.notifyAll();
            return exportRequest.getRequestId();
        }        
    }

    public IOExportRequest getExportState(Integer requestId) {

        // activation of deletion of expired records
        mapCacheResult.cleanUp();

        synchronized (lock) {
            IOExportRequest result = mapExportResult.get(requestId);
            return result;
        }
    }

    private void exportData(IOExportRequest request) throws IOException {

        // create security context
        SecurityContext secCtx = userService.createSecurityContext(request.getUserId());
        SecurityContextHolder.setContext(secCtx);

        Path exportXmlTrasnformDir = resourcePathResolver.getExportXmlTrasnformDir();
        Files.createDirectories(exportXmlTrasnformDir);

        Path xmlFile = Files.createFile(exportXmlTrasnformDir.resolve(request.getRequestId() + ".xml"));

        exportService.exportXmlDataToFile(request.getExportParams(), xmlFile);
    }

    public void run() {
        while (true) {
            IOExportRequest request = null;

            synchronized (lock) {
                if (status != ThreadStatus.RUNNING) {
                    break;
                }

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

                // mark as processing
                request.setStateProcessing();
            }

            Exception exception = null;
            try {
                exportData(request);
            } catch (Exception ex) {
                log.error("Error in export process.", ex);
                exception = ex;
            }

            synchronized (lock) {
                // set result
                if (exception == null) {
                    request.setFinished();
                } else {
                    request.setFailed(exception);
                }
            }
        }

        synchronized (lock) {
            status = ThreadStatus.STOPPED;
            lock.notifyAll();
        }
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

}