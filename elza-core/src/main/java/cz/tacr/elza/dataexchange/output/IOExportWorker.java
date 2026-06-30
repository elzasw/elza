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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.service.UserService;

@Service
public class IOExportWorker implements SmartLifecycle {

    static final Logger log = LoggerFactory.getLogger(IOExportWorker.class);

    /**
     * Factory used by {@link #enqueue(IOExportRequestFactory)} to build a request once its id is allocated.
     */
    @FunctionalInterface
    public interface IOExportRequestFactory {
        IOExportRequest create(int requestId);
    }

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

    // lookup by id; holds pending/processing requests and (until eviction) finished/error ones
    private Map<Integer, IOExportRequest> mapExportResult = new HashMap<>();

    // TTL cache for finished/error requests; eviction triggers file deletion + map cleanup
    private Cache<Integer, IOExportRequest> mapCacheResult;

    private ThreadStatus status = ThreadStatus.STOPPED;

    private final Object lock = new Object();

    @PostConstruct
    protected void init() {
        RemovalListener<Integer, IOExportRequest> removalListener = notification -> {
            IOExportRequest request = notification.getValue();
            if (request == null) {
                return;
            }
            Path filePath = resourcePathResolver.getExportTrasnformDir()
                    .resolve(notification.getKey() + request.getFileExt());
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.error("Error deleting a file: {}", filePath, e);
            }
            synchronized (lock) {
                mapExportResult.remove(notification.getKey());
            }
        };
        mapCacheResult = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES) // časový interval uchování souboru (5 min)
                .removalListener(removalListener)
                .build();
    }

    /**
     * Allocate a request id, build the request via the factory, store it and notify the worker.
     *
     * @return id of the newly queued request
     */
    public int enqueue(IOExportRequestFactory factory) {
        synchronized (lock) {
            IOExportRequest request = factory.create(++requestCount);
            mapExportResult.put(request.getRequestId(), request);
            exportRequests.add(request);
            lock.notifyAll();
            return request.getRequestId();
        }
    }

    public IOExportRequest getExportState(Integer requestId) {

        // trigger eviction of expired entries (file deletion + map cleanup)
        mapCacheResult.cleanUp();

        synchronized (lock) {
            return mapExportResult.get(requestId);
        }
    }

    private void exportData(IOExportRequest request) throws IOException {

        // create security context
        SecurityContext secCtx = userService.createSecurityContext(request.getUserId());
        SecurityContextHolder.setContext(secCtx);

        Path exportTrasnformDir = resourcePathResolver.getExportTrasnformDir();
        Files.createDirectories(exportTrasnformDir);
        Path exportFile = Files.createFile(exportTrasnformDir.resolve(request.getRequestId() + request.getFileExt()));
        request.exportToFile(exportFile, exportService);
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
                // set result and start TTL eviction
                if (exception == null) {
                    request.setFinished();
                } else {
                    request.setFailed(exception);
                }
                mapCacheResult.put(request.getRequestId(), request);
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
