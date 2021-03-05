package cz.tacr.elza.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import cz.tacr.elza.controller.vo.ApValidationErrorsVO;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
/**
 * Serviska pro generování.
 */
@Service
public class AccessPointGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(AccessPointGeneratorService.class);

    private final AccessPointService accessPointService;
    private final PartService partService;
    private final RuleService ruleService;
    private final ApItemRepository itemRepository;
    private final AccessPointCacheService accessPointCacheService;

    @Autowired
    public AccessPointGeneratorService(final AccessPointService accessPointService,
                                       final PartService partService,
                                       final RuleService ruleService,
                                       final AccessPointCacheService accessPointCacheService,
                                       final ApItemRepository itemRepository) {
        this.accessPointService = accessPointService;
        this.partService = partService;
        this.ruleService = ruleService;
        this.accessPointCacheService = accessPointCacheService;
        this.itemRepository = itemRepository;
    }

    /**
     * Zpracování požadavku AP.
     *
     * @param accessPointId identifikátor ap
     */
    public void processRequest(final Integer accessPointId) {
        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getStateInternal(accessPoint);
        logger.info("Asynchronní zpracování AP={}", accessPoint.getAccessPointId());
        generateAndSetResult(apState);
        logger.info("Asynchronní zpracování AP={} - END", accessPoint.getAccessPointId());
    }

    private void generateAndSetResult(final ApState apState) {
        ApAccessPoint accessPoint = apState.getAccessPoint();
        List<ApPart> partList = partService.findPartsByAccessPoint(accessPoint);
        Map<Integer, List<ApItem>> itemMap = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(ApItem::getPartId));

        boolean successfulGeneration = accessPointService.updatePartValues(apState, partList, itemMap, true);
        ApValidationErrorsVO apValidationErrorsVO = ruleService.executeValidation(apState.getAccessPoint().getAccessPointId());
        accessPointService.updateValidationErrors(accessPoint.getAccessPointId(), apValidationErrorsVO, successfulGeneration);
        accessPointCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
    }
}
