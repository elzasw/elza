package cz.tacr.elza.service;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.db.HibernateConfiguration;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.Item;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class DataService {
	
	private static Logger log = LoggerFactory.getLogger(DataService.class);

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    HibernateConfiguration hibernateConfiguration;

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
}
