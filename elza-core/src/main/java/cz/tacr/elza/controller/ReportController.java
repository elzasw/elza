package cz.tacr.elza.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.common.ResponseFactory;
import cz.tacr.elza.controller.vo.ReportReportCategory;
import cz.tacr.elza.controller.vo.ReportReportFormat;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.report.ReportRequest;
import cz.tacr.elza.service.report.ReportService;
import cz.tacr.elza.service.report.ReportWorker;

@RestController
@RequestMapping("/api/v1")
public class ReportController implements ReportApi {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private UserService userService;

    @Autowired
	ReportService reportService;

	@Autowired
	ReportWorker reportWorker;

	/**
	 * Získání seznamu statistických zpráv
	 * 
	 * @return list of report definition
	 */
	@Override
	public ResponseEntity<List<ReportReportCategory>> reportGetDefinitions() {
		return ResponseEntity.ok(reportService.getDefinitions());
	}

	/**
	 * Spuštění generování statistické zprávy
	 * 
	 * @param code
	 * @param reportReportRequest
	 * @return requestId
	 */
	@Override
	public ResponseEntity<Integer> reportGenerateReport(final String code, final ReportReportParameters reportParameters) {
        UsrUser user = userService.getLoggedUser();
        Integer userId = (user == null ? null : user.getUserId());

        return ResponseEntity.ok(reportWorker.addReportRequest(userId, code, reportParameters));
	}

	/**
	 * Získání stavu běžící statistické zprávy
	 * 
	 * @param requestId
	 * @return request process state
	 */
	@Override
	public ResponseEntity reportGetReportStatus(Integer requestId) {
		ReportRequest reportRequest = reportWorker.getReportRequest(requestId);
		if (reportRequest == null) {
            logger.error("Error reading request status by requestId={} - not found.", requestId);
            return ResponseEntity.notFound().build();
		}

        HttpStatus status = HttpStatus.OK;
        Object body = null;
        switch (reportRequest.getState()) {
        case PENDING:
        case PROCESSING:
        case FINISHED:
            body = reportRequest.getState();
            break;
        case ERROR:
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            body = ResponseFactory.createBaseException(reportRequest.getException());
            break;
        default:
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            body = ResponseFactory.createBaseException(new IllegalStateException());
            break;
        }

        return ResponseEntity.status(status).body(body);
	}

	/**
	 * Získání statistické zprávy
	 * 
	 * @param requestId
	 * @param format
	 * @return report data
	 */
	@Override
	public ResponseEntity reportGetReport(Integer requestId, ReportReportFormat format) {
		ReportRequest reportRequest = reportWorker.getReportRequest(requestId);
		if (reportRequest == null) {
            logger.error("Error reading request status by requestId={} - not found.", requestId);
            return ResponseEntity.notFound().build();
		}

        switch (reportRequest.getState()) {
        case FINISHED:
        	switch (format) {
        	case JSON:
        		return ResponseEntity.status(HttpStatus.OK).body(reportRequest.getReportData());
        	case CSV:
                String data = reportService.getCsvReport(reportRequest.getReportData());
                byte[] dataBytes = data.getBytes();
                ByteArrayResource resource = new ByteArrayResource(dataBytes);
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name());
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
                headers.add(HttpHeaders.CONTENT_LENGTH, Long.toString(dataBytes.length));
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_data.csv");
                headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
                headers.add(HttpHeaders.PRAGMA, "no-cache");
                headers.add(HttpHeaders.EXPIRES, "0");
                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        	}
        case PROCESSING:
            return ResponseEntity.status(102).build();
        case ERROR:
            return ResponseFactory.responseException(500, reportRequest.getException());
        default:
            throw new IllegalStateException();
        }
	}
}
