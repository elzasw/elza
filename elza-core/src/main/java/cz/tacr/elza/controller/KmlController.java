package cz.tacr.elza.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.xml.parsers.ParserConfigurationException;

import org.geotools.kml.KMLConfiguration;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;
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

import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.service.ArrIOService;
import cz.tacr.elza.service.RegistryService;

/**
 * Controller pro import a export KML souborů
 *
 * @author Petr Compel
 * @since 18. 4. 2016
 */
@RestController
public class KmlController {

    @Autowired
    private RegRecordRepository regRecordRepository;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private ArrIOService arrIOService;

    @Transactional
    @RequestMapping(value = "/api/kml/import/outputCoordinates", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Integer> importArrCoordinates(
            @RequestParam(required = false, value = "fundVersionId") final Integer fundVersionId,
            @RequestParam(required = false, value = "descItemTypeId") final Integer descItemTypeId,
            @RequestParam(required = false, value = "outputDefinitionId") final Integer outputDefinitionId,
            @RequestParam(required = false, value = "outputDefinitionVersion") final Integer outputDefinitionVersion,
            @RequestParam(required = true, value = "file") final MultipartFile importFile) throws IOException, ParserConfigurationException, SAXException {
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Assert.notNull(descItemTypeId, "Nebyl vyplněn identifikátor typu atributu");
        Assert.notNull(outputDefinitionId, "Identifikátor definice výstupu musí být vyplněn");
        Assert.notNull(outputDefinitionVersion, "Verze definice výstupu musí být vyplněna");

        return arrIOService.coordinatesOutputImport(fundVersionId, descItemTypeId, outputDefinitionId, outputDefinitionVersion, importFile);
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
            @RequestParam(value = "regRecordId") final Integer regRecordId,
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

}
