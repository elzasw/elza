package cz.tacr.elza.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.deimport.DEImportParams;
import cz.tacr.elza.deimport.DEImportParams.ImportPositionParams;
import cz.tacr.elza.deimport.DEImportService;
import cz.tacr.elza.exception.SystemException;

/**
 * Data exchange import controller.
 */
@RestController
@RequestMapping(value = "/api/import/")
public class DEImportController {

    private final DEImportService importService;

    @Autowired
    public DEImportController(DEImportService importService) {
        this.importService = importService;
    }

    @RequestMapping(value = "transformations", method = RequestMethod.GET)
    public List<String> getTransformations() {
        return importService.getTransformationNames();
    }

    @RequestMapping(value = "import", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void importData(@RequestPart(name = "importPositionParams", required = false) final ImportPositionParams importPositionParams,
                           @RequestParam(name = "transformationName", required = false) final String transformationName,
                           @RequestParam("scopeId") final int scopeId,
                           @RequestParam("xmlFile") final MultipartFile xmlFile) {

        // prepare import parameters
        DEImportParams params = new DEImportParams(scopeId, 1000, 10000, importPositionParams);

        // TODO: XML transformation

        // validate
        try (InputStream is = xmlFile.getInputStream()) {
            importService.validateData(is);
        } catch (IOException e) {
            throw new SystemException("Failed to read import source", e);
        }

        // import
        try (InputStream is = xmlFile.getInputStream()) {
            importService.importData(is, params);
        } catch (IOException e) {
            throw new SystemException("Failed to read import source", e);
        }
    }
}
