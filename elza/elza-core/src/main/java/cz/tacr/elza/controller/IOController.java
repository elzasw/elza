package cz.tacr.elza.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.common.FileDownload;
import cz.tacr.elza.common.ResponseFactory;
import cz.tacr.elza.controller.vo.ExportParams;
import cz.tacr.elza.controller.vo.RequestProcessState;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.dataexchange.output.DEExportParams;
import cz.tacr.elza.dataexchange.output.DEExportParams.FundSections;
import cz.tacr.elza.dataexchange.output.IOExportFundXmlRequest;
import cz.tacr.elza.dataexchange.output.IOExportRequest;
import cz.tacr.elza.dataexchange.output.IOExportWorker;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.UserService;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/v1")
public class IOController implements IoApi {

    private static final Logger logger = LoggerFactory.getLogger(IOController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private IOExportWorker ioExportWorker;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    /**
     * POST /io/export
     * Create export file of funds or access points
     *
     * @param exportParams Export request parameters (required)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    public ResponseEntity<Integer> ioExportRequest(@RequestBody ExportParams exportParams) {
        UsrUser user = userService.getLoggedUser();

        // convert ExportParams -> IOExportRequest
        DEExportParams deExportParams = new DEExportParams();
        deExportParams.setExportFilter(exportParams.getExportFilter());
        deExportParams.setIncludeUUID(exportParams.getIncludeUUID());
        deExportParams.setIncludeAccessPoints(exportParams.getIncludeAccessPoints());

        String fileName = null;

        if (exportParams.getFundsSections() != null) {
            for (cz.tacr.elza.controller.vo.FundSections fs : exportParams.getFundsSections()) {
                FundSections fundSection = new FundSections();

                ArrFundVersion fundVersion = arrangementService.getFundVersionById(fs.getFundVersionId());
                if (!userService.hasPermission(Permission.FUND_EXPORT_ALL) &&
                        !userService.hasPermission(Permission.FUND_EXPORT, fundVersion.getFundId())) {
                    throw new SystemException("Nedostatečné oprávnění pro export",
                            BaseCode.INSUFFICIENT_PERMISSIONS)
                                    .set("fundVersionId", fundVersion.getFundVersionId())
                                    .set("fundId", fundVersion.getFundId());
                }
                if (fileName == null) {
                    ArrFund fund = fundVersion.getFund();
                    fileName = prepareFileName(fund);
                }

                fundSection.setFundVersionId(fs.getFundVersionId());
                fundSection.setMergeSections(fs.getMergeSections());
                if (fs.getRootNodeIds() != null) {
                    fundSection.setRootNodeIds(fs.getRootNodeIds());
                }
                deExportParams.addFundsSection(fundSection);
            }
        }

        Integer userId = (user == null ? null : user.getUserId());

        final String dlFileName = fileName;
        final DEExportParams deParams = deExportParams;
        int id = ioExportWorker.enqueue(requestId -> new IOExportFundXmlRequest(userId, requestId, dlFileName, deParams));
        return ResponseEntity.ok(id);
    }

    private String prepareFileName(ArrFund fund) {
        StringBuilder sb = new StringBuilder();

        boolean appendSep = false;
        if (StringUtils.isNotEmpty(fund.getMark())) {
            sb.append(fund.getMark());
            appendSep = true;
        }
        if (fund.getFundNumber() != null) {
            if (appendSep) {
                sb.append("-");
            }
            sb.append(fund.getFundNumber());
            appendSep = true;
        }
        if (!appendSep) {
            sb.append("fundId-").append(fund.getFundId());
        }
        sb.append(".xml");
        String fileName = sb.toString().replaceAll("[\\\\/:*?\"<>|]", "_");

        return fileName;
    }

    /**
     * GET /io/export-status/{requestId}
     * Getting the export status by request id
     *
     * @param requestId  (required)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     *         or Server error (status code 500)
     */
    @Override
    public ResponseEntity ioGetExportStatus(@PathVariable Integer requestId) {

        logger.debug("Get export status: {}", requestId);

        IOExportRequest result = ioExportWorker.getExportState(requestId);
        if (result == null) {
            logger.error("Get export status: {} - request not found!!!", requestId);
            return ResponseEntity.notFound().build();
        }

        HttpStatus status = HttpStatus.OK;
        Object body = null;
        switch (result.getState()) {
        case PENDING:
            body = ResponseFactory.createExportRequestStatus(RequestProcessState.PENDING);
            break;
        case PROCESSING:
            body = ResponseFactory.createExportRequestStatus(RequestProcessState.PROCESSING, result.getProgress());
            break;
        case FINISHED:
            body = ResponseFactory.createExportRequestStatus(RequestProcessState.FINISHED, result.getProgress());
            break;
        case ERROR:
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            body = ResponseFactory.createBaseException(result.getException());
            break;
        default:
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            body = ResponseFactory.createBaseException(new IllegalStateException());
            break;
        }

        logger.debug("Get export status: {}, response: {}", requestId, status);

        BodyBuilder resp = ResponseEntity.status(status);
        ResponseEntity<Object> ret = resp.body(body);
        return ret;
    }

    /**
     * GET /io/file/{requestId}
     * Getting the generated file by request id
     *
     * @param requestId  (required)
     * @return Informational (status code 102)
     *         or The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     *         or Server error (status code 500)
     */
    @Override
    public ResponseEntity<Resource> ioGetExportFile(@PathVariable Integer requestId) {

        IOExportRequest request = ioExportWorker.getExportState(requestId);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        switch (request.getState()) {
        case FINISHED:
            Path filePath = resourcePathResolver.getExportTrasnformDir().resolve(request.getRequestId() + request.getFileExt());
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name());
            headers.add(HttpHeaders.CONTENT_TYPE, request.getMediaType());
            try {
                long fileSize = Files.size(filePath);
                headers.add(HttpHeaders.CONTENT_LENGTH, Long.toString(fileSize));
            } catch (IOException e) {
                throw new BusinessException("Failed to get file size", e, BaseCode.EXPORT_FAILED);
            }

            // Content-Disposition: attachment; filename="filename.jpg"
            String fileName = request.getDownloadFileName();
            if (StringUtils.isBlank(fileName)) {
                fileName = "elzaData"  + request.getFileExt();
            }
            FileDownload.addContentDispositionAsAttachment(headers, fileName);

            // cache headers
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            return ResponseFactory.responseFile(filePath, headers);
        case PROCESSING:
            return ResponseEntity.status(102).build();
        case ERROR:
            return ResponseFactory.responseException(500, request.getException());
        default:
            throw new IllegalStateException();
        }
    }
}
