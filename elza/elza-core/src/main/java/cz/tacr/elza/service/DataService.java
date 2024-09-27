package cz.tacr.elza.service;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.controller.vo.FileType;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.db.HibernateConfiguration;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.Item;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DataCoordinatesRepository;

import org.geotools.kml.v22.KMLConfiguration;
import org.geotools.xsd.PullParser;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryType;
import org.opengis.geometry.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;
import javax.xml.stream.XMLStreamException;

@Service
public class DataService {
	
	private static Logger log = LoggerFactory.getLogger(DataService.class);

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    HibernateConfiguration hibernateConfiguration;

    @Autowired
    DataCoordinatesRepository dataCoordinatesRepository;

    public <ENTITY extends Item> List<ENTITY> findItemsWithData(List<ENTITY> items) {
    	StaticDataProvider sdp = staticDataService.getData();
    	Map<DataType, List<ENTITY>> entityMap = new HashMap<>();
    	for (ENTITY item : items) {
    		ItemType itemType = sdp.getItemTypeById(item.getItemTypeId());
    		DataType dataType = itemType.getDataType();
    		List<ENTITY> entities = entityMap.computeIfAbsent(dataType, k -> new ArrayList<>());
    		entities.add(item);
    		if (entities.size() == hibernateConfiguration.getBatchSize()) {
    			findAllDataByDataResults(dataType, entities);
    			entityMap.remove(dataType);
    		}
    	}
    	entityMap.keySet().forEach(d -> findAllDataByDataResults(d, entityMap.get(d)));
    	return items;
    }

    public <ENTITY extends Item> ENTITY findItemWithData(ENTITY item) {
    	List<ENTITY> items = findItemsWithData(List.of(item));
    	return items.get(0);
    }

    private <ENTITY extends Item> void findAllDataByDataResults(DataType dataType, List<ENTITY> items) {
    	List<Integer> dataIds = items.stream().filter(i -> i.getDataId() != null).map(i -> i.getDataId()).toList();
    	List<? extends ArrData> result = dataType.getRepository().findAllById(dataIds);        

        // kontrola neporušenosti dat
        if (result.size() != dataIds.size()) {
        	// Loaded IDS
        	Set<Integer> dbDataIds = result.stream().map(i -> i.getDataId()).collect(Collectors.toSet());
        	List<Integer> missingIds = dataIds.stream().filter(i -> !dbDataIds.contains(i)).collect(Collectors.toList());
        	
        	log.error("Failed to load items (dataType: {}), dataIds({}): {}, missing items in DB({}): {}",
        			dataType,
        			dataIds.size(), dataIds, 
        			missingIds.size(), missingIds);;
        	throw new SystemException("Failed to load items.", BaseCode.DB_INTEGRITY_PROBLEM)
        		.set("dataType", dataType)
        		.set("dataIds.size", dataIds.size())
        		.set("dataIds", dataIds)
        		.set("missingIds.size", missingIds.size())
        		.set("missingIds", missingIds);
        }
        
        items.forEach(i -> HibernateUtils.unproxy(i.getData()));
    }

    /**
     * Převod souřadnic na řetězec na základě formátu (KML, GML, WKT)
     * 
     * @param fileType
     * @param data
     * @return
     */
    public String convertCoordinates(FileType fileType, ArrData data) {
        switch (fileType) {
            case KML:
                return convertCoordinatesToKml(data.getDataId());
            case GML:
                return convertCoordinatesToGml(data.getDataId());
            case WKT:
                data = HibernateUtils.unproxy(data);
                return data.getFulltextValue();
            default:
                throw new IllegalStateException("Nepovolený typ souboru pro export souřadnic");
        }
    }

    /**
     * Získání souřadnic ze souboru ve formátu KML.
     * @see https://gis.stackexchange.com/questions/278570/not-able-to-parse-extendeddata-from-kml-file-using-geotools
     * 
     * Vytvořit testovací KML soubor
     * @see https://www.freemaptools.com/kml-file-creator.htm
     * 
     * @param inputStream
     * @return
     */
    public String convertCoordinatesFromKml(InputStream inputStream) {
        try {
            PullParser parser = new PullParser(new KMLConfiguration(), inputStream, SimpleFeature.class);
            SimpleFeature simpleFeature = (SimpleFeature) parser.parse();
            // if there are no <Placemark>...</Placemark> tags, we will get null
            if (simpleFeature != null) {
            	GeometryAttribute geometry = simpleFeature.getDefaultGeometryProperty();
            	return geometry.getValue().toString();
            }
        } catch (IOException | SAXException | XMLStreamException e) {
            log.error("Error importing coordinates from file", e);
            throw new IllegalStateException("Chyba při importu souřadnic ze souboru", e);
        }
        return null;
    }

    private String convertCoordinatesToKml(Integer dataId) {
        return String.format("<Placemark>%s</Placemark>", dataCoordinatesRepository.convertCoordinatesToKml(dataId));
    }

    public String convertCoordinatesFromGml(String coordinates) {
        return dataCoordinatesRepository.convertCoordinatesFromGml(coordinates);
    }

    private String convertCoordinatesToGml(Integer dataId) {
        return dataCoordinatesRepository.convertCoordinatesToGml(dataId);
    }

    public byte[] convertGeometryToWKB(org.locationtech.jts.geom.Geometry geometry) {
        return dataCoordinatesRepository.convertGeometryToWKB(geometry);
    }
}
