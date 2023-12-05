package cz.tacr.elza.controller;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.controller.vo.GisExternalSystemVO;
import cz.tacr.elza.domain.GisExternalSystem;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.ArrIOService;
import cz.tacr.elza.service.ExternalSystemService;

/**
 * Controller pro import a export KML souborů
 *
 * @author Petr Compel
 * @since 18. 4. 2016
 */
@RestController
public class KmlController {

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private ArrIOService arrIOService;

    @Transactional
    @RequestMapping(value = "/api/kml/import/outputCoordinates", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Integer> importArrCoordinates(
            @RequestParam(required = false, value = "fundVersionId") final Integer fundVersionId,
            @RequestParam(required = false, value = "descItemTypeId") final Integer descItemTypeId,
            @RequestParam(required = false, value = "outputId") final Integer outputId,
            @RequestParam(required = false, value = "outputVersion") final Integer outputVersion,
            @RequestParam(required = true, value = "file") final MultipartFile importFile) throws IOException, ParserConfigurationException, SAXException {
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Assert.notNull(descItemTypeId, "Nebyl vyplněn identifikátor typu atributu");
        Assert.notNull(outputId, "Identifikátor výstupu musí být vyplněn");
        Assert.notNull(outputVersion, "Verze výstupu musí být vyplněna");

        return arrIOService.coordinatesOutputImport(fundVersionId, descItemTypeId, outputId, outputVersion, importFile);
    }

    @Transactional
    @RequestMapping(value = "/api/kml/import/descCoordinates", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Integer> coordinatesDescImport(
            @RequestParam(required = false, value = "fundVersionId") final Integer fundVersionId,
            @RequestParam(required = false, value = "descItemTypeId") final Integer descItemTypeId,
            @RequestParam(required = false, value = "nodeId") final Integer nodeId,
            @RequestParam(required = false, value = "nodeVersion") final Integer nodeVersion,
            @RequestParam(required = true, value = "file") final MultipartFile importFile) throws IOException, ParserConfigurationException, SAXException {
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Assert.notNull(descItemTypeId, "Nebyl vyplněn identifikátor typu atributu");
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");
        Assert.notNull(nodeVersion, "Nebyla vyplněna verze JP");

        return arrIOService.coordinatesDescImport(fundVersionId, descItemTypeId, nodeId, nodeVersion, importFile);
    }

    @Transactional
    @RequestMapping(value = "/api/kml/import/regCoordinates", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Integer> importRegCoordinates(
            @RequestParam(value = "apRecordId") final Integer apRecordId,
            @RequestParam(value = "file") final MultipartFile importFile) throws IOException, ParserConfigurationException, SAXException {
        throw new SystemException("import koordinat neni podporovan", BaseCode.SYSTEM_ERROR);
    }


    @RequestMapping(value = "/api/kml/export/descCoordinates/{fundVersionId}/{descItemObjectId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    public void coordinatesDescExport(final HttpServletResponse response,
                                     @PathVariable(value = "descItemObjectId") final Integer descItemObjectId,
                                     @PathVariable(value = "fundVersionId") final Integer fundVersionId) throws IOException {
        Assert.notNull(descItemObjectId, "Nebyl vyplněn jednoznačný identifikátor descItem");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");

        arrIOService.coordinatesExport(response, descItemObjectId, fundVersionId);
    }


    @RequestMapping(value = "/api/kml/export/outputCoordinates/{fundVersionId}/{descItemObjectId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    public void coordinatesOutputExport(final HttpServletResponse response,
                                     @PathVariable(value = "descItemObjectId") final Integer descItemObjectId,
                                     @PathVariable(value = "fundVersionId") final Integer fundVersionId) throws IOException {
        Assert.notNull(descItemObjectId, "Nebyl vyplněn jednoznačný identifikátor descItem");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");

        arrIOService.coordinatesExport(response, descItemObjectId, fundVersionId);
    }

    @RequestMapping(value = "/api/kml/export/regCoordinates/{coordinatesId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    public void exportRegCoordinates(final HttpServletResponse response, @PathVariable(value = "coordinatesId") final Integer coordinatesId) throws IOException {
        throw new SystemException("export koordinat neni podporovan", BaseCode.SYSTEM_ERROR);
    }

    @RequestMapping(value = "/api/kml/externalSystems", method = RequestMethod.GET)
    @Transactional
    public List<GisExternalSystemVO> findAllExternalSystems() {
        List<GisExternalSystem> extSystems = externalSystemService.findAllGisSystem();
        return FactoryUtils.transformList(extSystems, GisExternalSystemVO::newInstance);
    }
}
