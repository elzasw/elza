package cz.tacr.elza.service.report;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import cz.tacr.elza.controller.vo.RequestProcessState;

@Service
public class ReportWorker implements SmartLifecycle {

	static final Logger log = LoggerFactory.getLogger(ReportWorker.class);

    private enum ThreadStatus {
        RUNNING, STOP_REQUEST, STOPPED
    }

    @Autowired
    ReportService reportService;

    private static int requestCount = 0; 

    // queue of export requests
    private LinkedList<ReportRequest> reportRequests = new LinkedList<>();

    // lookup by id
    private Map<Integer, ReportRequest> mapReportResult = new HashMap<>();

    private ThreadStatus status = ThreadStatus.STOPPED;

    private final Object lock = new Object();

    public int addReportRequest(final Integer userId, final ReportProcessor processor, final ReportReportParameters reportParams) {
        synchronized (lock) {
            ReportRequest reportRequest = new ReportRequest(userId, ++requestCount, processor, reportParams);

            // store result
            mapReportResult.put(reportRequest.getRequestId(), reportRequest);
            reportRequests.add(reportRequest);         
            lock.notifyAll();
            return reportRequest.getRequestId();
        }        
    }

    public ReportRequest getReportRequest(final Integer requestId) {
        synchronized (lock) {
        	ReportRequest result = mapReportResult.get(requestId);
            return result;
        }
    }

    private void createReport(ReportRequest request) throws IOException {
    	ReportProcessor report = request.getProcessor();
    	ReportReportData reportData = report.createReport(request.getReportParameters());
    	request.setReportData(reportData);
    }

    public void run() {
        while (true) {
        	ReportRequest request = null;

            synchronized (lock) {
                if (status != ThreadStatus.RUNNING) {
                    break;
                }

                // get next request
                request = reportRequests.poll();
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
                request.setState(RequestProcessState.PROCESSING);
            }

            Exception exception = null;
            try {
            	createReport(request);
            } catch (Exception ex) {
                log.error("Error in export process.", ex);
                exception = ex;
            }

            synchronized (lock) {
                // set result
                if (exception == null) {
                    request.setState(RequestProcessState.FINISHED);
                } else {
                	request.setState(RequestProcessState.ERROR);
                    request.setException(exception);
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
