package cz.tacr.elza.controller;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.common.ResponseFactory;
import cz.tacr.elza.controller.vo.ExportParams;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.dataexchange.output.IOExportWorker;
import cz.tacr.elza.dataexchange.output.DEExportParams.FundSections;
import cz.tacr.elza.dataexchange.output.IOExportRequest;
import cz.tacr.elza.dataexchange.output.IOExportWorker.IOExportResult;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.service.UserService;

@RestController
@RequestMapping("/api/v1")
public class IOController implements IoApi {

    @Autowired
    private UserService userService;

    @Autowired
    private IOExportWorker ioExportWorker;

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Override
    public ResponseEntity<Integer> exportRequest(@RequestBody ExportParams exportParams) {
        UsrUser user = userService.getLoggedUser();

        // convert ExportParams to IOExportRequest
        IOExportRequest request = new IOExportRequest();
        if (exportParams.getFundsSections() != null) {
            for (cz.tacr.elza.controller.vo.FundSections fs : exportParams.getFundsSections()) {
                FundSections fundSection = new FundSections();
                fundSection.setFundVersionId(fs.getFundVersionId());
                fundSection.setMergeSections(fs.getMergeSections());
                if (fs.getRootNodeIds() != null) {
                    fundSection.setRootNodeIds(fs.getRootNodeIds());
                }
                request.addFundsSection(fundSection);
            }
        }
        request.setUserId(user == null? null : user.getUserId());

        int id = ioExportWorker.addExportRequest(request);
        return ResponseEntity.ok(id);
    }

    @Override
    public ResponseEntity<Resource> getExportFile(@PathVariable Integer requestId) {

        IOExportResult result = ioExportWorker.getExportState(requestId);
        switch (result.getState()) {
            case OK: 
                Path filePath = resourcePathResolver.getExportXmlTrasnformDir().resolve(requestId + ".xml");
                return ResponseFactory.responseFile(filePath);
            case PROCESSING:
                return ResponseEntity.status(102).build();
            case NOT_FOUND:
                return ResponseEntity.notFound().build();
            case ERROR:
                return ResponseFactory.responseException(500, result.getException());
            default:
                throw new IllegalStateException();
        }
    }
}
