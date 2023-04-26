package cz.tacr.elza.service;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
/**
 * Serviska pro generování.
 */
@Service
public class AccessPointGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(AccessPointGeneratorService.class);

    private final AccessPointService accessPointService;
    private final PartService partService;
    private final ApItemRepository itemRepository;
    private final AccessPointCacheService accessPointCacheService;

    @Autowired
    public AccessPointGeneratorService(final AccessPointService accessPointService,
                                       final PartService partService,
                                       final AccessPointCacheService accessPointCacheService,
                                       final ApItemRepository itemRepository) {
        this.accessPointService = accessPointService;
        this.partService = partService;
        this.accessPointCacheService = accessPointCacheService;
        this.itemRepository = itemRepository;
    }

    /**
     * Zpracování požadavku AP.
     *
     * @param accessPointId identifikátor ap
     */
    public void processRequest(final Integer accessPointId) {
        logger.info("Asynchronní zpracování AP={}", accessPointId);
        CachedAccessPoint cap = accessPointCacheService.findCachedAccessPoint(accessPointId);
        Validate.notNull(cap);
        generateAndSetResult(cap);
        logger.info("Asynchronní zpracování AP={} - END", accessPointId);
    }

    private void generateAndSetResult(final CachedAccessPoint cap) {
        ApState apState = cap.getApState();
        ApAccessPoint accessPoint = apState.getAccessPoint();
        accessPointService.lockWrite(accessPoint);

        List<ApPart> partList = cap.getApParts();
        Map<Integer, List<ApItem>> itemMap = cap.getApItemMap();

        /*
        // old implementation based on raw DB data        
        List<ApPart> partList = partService.findPartsByAccessPoint(accessPoint);
        Map<Integer, List<ApItem>> itemMap = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(ApItem::getPartId));
         */
        accessPoint = accessPointService.updateAndValidate(accessPoint, cap.getApState(),
                                                           partList, itemMap, true);
                
        accessPointCacheService.createApCachedAccessPoint(cap.getAccessPointId());
    }
}
