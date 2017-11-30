package cz.tacr.elza.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.net.HttpHeaders;

import cz.tacr.elza.dataexchange.output.DEExportParams;
import cz.tacr.elza.dataexchange.output.DEExportParams.FundSections;
import cz.tacr.elza.dataexchange.output.DEExportService;

@RestController
@RequestMapping(value = "/api/export/")
public class DEExportController {

    private final DEExportService exportService;

    @Autowired
    public DEExportController(DEExportService exportService) {
        this.exportService = exportService;
    }

    @RequestMapping(value = "transformations", method = RequestMethod.GET)
    public List<String> getTransformations() {
        return exportService.getTransformationNames();
    }

    /**
     * Legacy request mapping. TODO: Remove after UI update.
     */
    @Deprecated
    @RequestMapping(value = "fund/{versionId}", method = RequestMethod.GET)
    public void exportFund(final HttpServletResponse response,
                           @PathVariable(value = "versionId") final int fundVersionId,
                           @RequestParam(value = "transformationName") final String transformationName)
            throws IOException {
        FundSections fundParams = new FundSections();
        fundParams.setFundVersionId(fundVersionId);
        DEExportParamsVO params = new DEExportParamsVO();
        params.setFundsSections(Collections.singleton(fundParams));
        params.setTransformationName(transformationName);
        exportFund(response, params);
    }

    @RequestMapping(value = "create", method = RequestMethod.POST)
    public void exportFund(final HttpServletResponse response, final @RequestBody DEExportParamsVO params) throws IOException {
        // TODO: XML transformation
        if (StringUtils.isNotEmpty(params.getTransformationName())) {
            throw new UnsupportedOperationException("Export transformation not implemented");
        }

        // file headers
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_XML_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=elza-data.xml");

        // cache headers
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setDateHeader(HttpHeaders.EXPIRES, 0);

        // write response
        try (ServletOutputStream os = response.getOutputStream()) {
            response.flushBuffer();
            exportService.exportXmlData(os, params);
            response.flushBuffer();
        }
    }

    public static class DEExportParamsVO extends DEExportParams {

        private String transformationName;

        public String getTransformationName() {
            return transformationName;
        }

        public void setTransformationName(String transformationName) {
            this.transformationName = transformationName;
        }
    }
}
