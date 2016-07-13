package cz.tacr.elza.controller;

import com.vividsolutions.jts.geom.Geometry;
import cz.tacr.elza.domain.RegCoordinates;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.RegCoordinatesRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.service.ArrIOService;
import cz.tacr.elza.service.RegistryService;
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

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    private RegCoordinatesRepository regCoordinatesRepository;

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
        Assert.notNull(fundVersionId);
        Assert.notNull(descItemTypeId);
        Assert.notNull(outputDefinitionId);
        Assert.notNull(outputDefinitionVersion);

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
        Assert.notNull(fundVersionId);
        Assert.notNull(descItemTypeId);
        Assert.notNull(nodeId);
        Assert.notNull(nodeVersion);

        return arrIOService.coordinatesDescImport(fundVersionId, descItemTypeId, nodeId, nodeVersion, importFile);
    }

    @Transactional
    @RequestMapping(value = "/api/kml/import/regCoordinates", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Integer> importRegCoordinates(
            @RequestParam(value = "regRecordId") final Integer regRecordId,
            @RequestParam(value = "file") final MultipartFile importFile) throws IOException, ParserConfigurationException, SAXException {
        Assert.notNull(regRecordId);

        RegRecord record = regRecordRepository.findOne(regRecordId);
        if (record == null) {
            throw new IllegalStateException("Typ s ID=" + regRecordId + " neexistuje");
        }

        Parser parser = new Parser(new KMLConfiguration());
        SimpleFeature document = (SimpleFeature) parser.parse(importFile.getInputStream());
        Collection<SimpleFeature> placemarks = arrIOService.getPlacemars(document);
        List<RegCoordinates> toCreate = new ArrayList<>();
        for (SimpleFeature placemark : placemarks) {
            Geometry geometry = (Geometry) placemark.getAttribute("Geometry");
            switch (geometry.getGeometryType()) {
                case "Point":
                case "LineString":
                case "Polygon":
                    break;
                default:
                    continue;
            }
            String decription = (String) placemark.getAttribute("Description");
            RegCoordinates coordinates = new RegCoordinates();
            coordinates.setValue(geometry);
            coordinates.setRegRecord(record);
            coordinates.setDescription(decription);
            toCreate.add(coordinates);
        }

        List<Integer> ids = new ArrayList<>();
        registryService.saveRegCoordinates(toCreate).forEach(regCoordinates -> {
            ids.add(regCoordinates.getCoordinatesId());
        });
        return ids;
    }


    @RequestMapping(value = "/api/kml/export/descCoordinates/{fundVersionId}/{descItemObjectId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    public void coordinatesDescExport(HttpServletResponse response,
                                     @PathVariable(value = "descItemObjectId") final Integer descItemObjectId,
                                     @PathVariable(value = "fundVersionId") final Integer fundVersionId) throws IOException {
        Assert.notNull(descItemObjectId);
        Assert.notNull(fundVersionId);

        arrIOService.coordinatesDescExport(response, descItemObjectId, fundVersionId);
    }


    @RequestMapping(value = "/api/kml/export/outputCoordinates/{fundVersionId}/{descItemObjectId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    public void coordinatesOutputExport(HttpServletResponse response,
                                     @PathVariable(value = "descItemObjectId") final Integer descItemObjectId,
                                     @PathVariable(value = "fundVersionId") final Integer fundVersionId) throws IOException {
        Assert.notNull(descItemObjectId);
        Assert.notNull(fundVersionId);

        arrIOService.coordinatesOutputExport(response, descItemObjectId, fundVersionId);
    }

    @RequestMapping(value = "/api/kml/export/regCoordinates/{coordinatesId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    public void exportRegCoordinates(HttpServletResponse response, @PathVariable(value = "coordinatesId") final Integer coordinatesId) throws IOException {
        Assert.notNull(coordinatesId);
        RegCoordinates cords = regCoordinatesRepository.findOne(coordinatesId);

        Assert.notNull(cords);

       arrIOService.toKml(response, cords.getValue());
    }

}
