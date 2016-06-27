package cz.tacr.elza.controller;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.service.RuleService;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.xml.Encoder;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import javax.servlet.ServletOutputStream;
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
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private RegRecordRepository regRecordRepository;

    @Autowired
    private RegCoordinatesRepository regCoordinatesRepository;

    @Autowired
    private RegistryService registryService;

    private Collection<SimpleFeature> getPlacemars(SimpleFeature document) throws IllegalArgumentException {
        Collection<SimpleFeature> placemarks;
        Collection<SimpleFeature> features;
        try {
            features = (Collection<SimpleFeature>) document.getAttribute("Feature");
            if (features.size() == 1 && features.iterator().next().getAttribute("Geometry") == null) {
                placemarks = (Collection<SimpleFeature>) features.iterator().next().getAttribute("Feature");
            } else {
                placemarks = features;
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Chybná data pro import. Nepodařilo se zpracovat.");
        }
        return placemarks;
    }

    @Transactional
    @RequestMapping(value = "/api/kmlManagerV1/import/arrCoordinates", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Integer> importArrCoordinates(
            @RequestParam(required = false, value = "fundVersionId") final Integer fundVersionId,
            @RequestParam(required = false, value = "descItemTypeId") final Integer descItemTypeId,
            @RequestParam(required = false, value = "nodeId") final Integer nodeId,
            @RequestParam(required = false, value = "nodeVersion") final Integer nodeVersion,
            @RequestParam(required = true, value = "file") final MultipartFile importFile) throws IOException, ParserConfigurationException, SAXException {
        Assert.notNull(fundVersionId);
        Assert.notNull(descItemTypeId);
        Assert.notNull(nodeId);
        Assert.notNull(nodeVersion);

        RulItemType descItemType = itemTypeRepository.findOne(descItemTypeId);
        if (descItemType == null) {
            throw new IllegalStateException("Typ s ID=" + descItemTypeId + " neexistuje");
        }
        if (!"COORDINATES".equals(descItemType.getDataType().getCode())) {
            throw new UnsupportedOperationException("Pouze typ COORDINATES může být importován pomocí KML.");
        }

        Parser parser = new Parser(new KMLConfiguration());
        SimpleFeature document = (SimpleFeature) parser.parse(importFile.getInputStream());
        Collection<SimpleFeature> placemarks = getPlacemars(document);

        List<ArrDescItem> toCreate = new ArrayList<>();
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

            ArrItemCoordinates item = new ArrItemCoordinates();
            item.setValue(geometry);
            ArrDescItem<ArrItemCoordinates> descItem = new ArrDescItem<ArrItemCoordinates>(item);
            descItem.setItemType(descItemType);
            toCreate.add(descItem);
        }

        if (toCreate.isEmpty()) {
            throw new IllegalStateException("Nebyli nalezeny souřadnice.");
        }

        List<RulItemTypeExt> descriptionItemTypes = ruleService.getDescriptionItemTypes(fundVersionId, nodeId);

        RulItemTypeExt rule = null;

        for (RulItemTypeExt desc : descriptionItemTypes) {
            if (descItemType.getItemTypeId().equals(desc.getItemTypeId())) {
                rule = desc;
                break;
            }
        }

        if (rule == null) {
            throw new IllegalStateException("Pravidlo s ID=" + descItemTypeId + " neexistuje");
        }

        if (!rule.getRepeatable()) {
            if (toCreate.size() > 1) {
                throw new IllegalStateException("Do neopakovatelného prvku lze importovat pouze jeden geoobjekt.");
            }

            descriptionItemService.deleteDescriptionItemsByTypeWithoutVersion(fundVersionId, nodeId, nodeVersion, descItemTypeId);
        }

        List<Integer> ids = new ArrayList<>();
        descriptionItemService.createDescriptionItems(toCreate, nodeId, nodeVersion, fundVersionId).forEach(arrDescItem -> {
            ids.add(arrDescItem.getDescItemObjectId());
        });
        return ids;
    }

    @Transactional
    @RequestMapping(value = "/api/kmlManagerV1/import/regCoordinates", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
        Collection<SimpleFeature> placemarks = getPlacemars(document);
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


    @RequestMapping(value = "/api/kmlManagerV1/export/arrCoordinates/{fundVersionId}/{descItemObjectId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    public void exportArrCoordinates(HttpServletResponse response,
                                     @PathVariable(value = "descItemObjectId") final Integer descItemObjectId,
                                     @PathVariable(value = "fundVersionId") final Integer fundVersionId) throws IOException {
        Assert.notNull(descItemObjectId);
        Assert.notNull(fundVersionId);
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        ArrChange change = fundVersion.getLockChange();
        List<ArrDescItem> items;
        if (change == null) {
            change = fundVersion.getCreateChange();
            items = descItemRepository.findByDescItemObjectIdAndBetweenVersionChangeId(descItemObjectId, change);
        } else {
            items = descItemRepository.findByDescItemObjectIdAndLockChangeId(descItemObjectId, change);
        }

        if (items.size() < 1) {
            throw new IllegalArgumentException("Neexistují data pro verzi:" + fundVersionId);
        }

        ArrDescItem one = items.get(items.size() - 1);

        Assert.notNull(one);

        one = descItemFactory.getDescItem(one);

        ArrItemData item = one.getItem();

        if (!(item instanceof ArrItemCoordinates)) {
            throw new UnsupportedOperationException("Pouze typ COORDINATES může být exportován do KML.");
        }
        ArrItemCoordinates cords = (ArrItemCoordinates) item;

        toKml(response, cords.getValue());

    }

    @RequestMapping(value = "/api/kmlManagerV1/export/regCoordinates/{coordinatesId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    public void exportRegCoordinates(HttpServletResponse response, @PathVariable(value = "coordinatesId") final Integer coordinatesId) throws IOException {
        Assert.notNull(coordinatesId);
        RegCoordinates cords = regCoordinatesRepository.findOne(coordinatesId);

        Assert.notNull(cords);

        toKml(response, cords.getValue());
    }

    private void toKml(HttpServletResponse response, Geometry geometry) throws IOException {
        response.setHeader("Content-Disposition", "attachment;filename=export.kml");

        ServletOutputStream out = response.getOutputStream();
        Encoder encoder = new Encoder(new KMLConfiguration());
        encoder.setIndenting(false);
        encoder.setNamespaceAware(false);

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("geometry");
        String geotype = geometry.getGeometryType();
        switch (geotype) {
            case "Point":
                typeBuilder.add("geometry", Point.class, DefaultGeographicCRS.WGS84);
                break;
            case "LineString":
                typeBuilder.add("geometry", LineString.class, DefaultGeographicCRS.WGS84);
                break;
            case "Polygon":
                typeBuilder.add("geometry", Polygon.class, DefaultGeographicCRS.WGS84);
                break;
            default:
                throw new UnsupportedOperationException("Neznámý typ geodat:" + geotype + ". Nelze exportovat.");
        }

        SimpleFeatureType TYPE = typeBuilder.buildFeatureType();

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        DefaultFeatureCollection features = new DefaultFeatureCollection();

        featureBuilder.add(geometry);
        features.add(featureBuilder.buildFeature("1"));

        encoder.encode(features, KML.kml, out);
    }
}
