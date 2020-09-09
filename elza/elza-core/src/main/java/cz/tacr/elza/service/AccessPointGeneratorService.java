package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import cz.tacr.elza.controller.vo.ApValidationErrorsVO;
import cz.tacr.elza.service.event.AccessPointQueueEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
/**
 * Serviska pro generování.
 */
@Service
public class AccessPointGeneratorService implements ApplicationListener<AccessPointQueueEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AccessPointGeneratorService.class);

    private final AccessPointDataService apDataService;
    private final ApAccessPointRepository accessPointRepository;
    private final AccessPointService accessPointService;
    private final PartService partService;
    private final RuleService ruleService;
    private final ApAccessPointQueueItemRepository accessPointQueueItemRepository;
    private final ApItemRepository itemRepository;
    private final ApPartRepository partRepository;

    @Autowired
    public AccessPointGeneratorService(final AccessPointDataService apDataService,
                                       final ApAccessPointRepository accessPointRepository,
                                       final AccessPointService accessPointService,
                                       final PartService partService,
                                       final RuleService ruleService,
                                       final ApAccessPointQueueItemRepository accessPointQueueItemRepository,
                                       final ApItemRepository itemRepository,
                                       final ApPartRepository partRepository) {
        this.apDataService = apDataService;
        this.accessPointRepository = accessPointRepository;
        this.accessPointService = accessPointService;
        this.partService = partService;
        this.ruleService = ruleService;
        this.accessPointQueueItemRepository = accessPointQueueItemRepository;
        this.itemRepository = itemRepository;
        this.partRepository = partRepository;
    }

    /**
     * Zpracování požadavku AP.
     *
     * @param apAccessPointQueueItem item ve frontě pro generování
     */
    @Transactional
    public void processAsyncGenerate(final ApAccessPointQueueItem apAccessPointQueueItem) {
        ApAccessPoint accessPoint = apAccessPointQueueItem.getAccessPoint();
        ApState apState = accessPointService.getState(accessPoint);
        logger.info("Asynchronní zpracování AP={}", accessPoint.getAccessPointId());
        generateAndSetResult(apState);
        logger.info("Asynchronní zpracování AP={} - END", accessPoint.getAccessPointId());
        accessPointQueueItemRepository.delete(apAccessPointQueueItem);
    }

    private void generateAndSetResult(final ApState apState) {
        ApAccessPoint accessPoint = apState.getAccessPoint();
        List<ApPart> partList = partService.findPartsByAccessPoint(accessPoint);
        Map<Integer, List<ApItem>> itemMap = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(ApItem::getPartId));

        boolean successfulGeneration = accessPointService.updatePartValues(apState, partList, itemMap, true);
        ApValidationErrorsVO apValidationErrorsVO = ruleService.executeValidation(apState.getAccessPoint().getAccessPointId());
        accessPointService.updateValidationErrors(accessPoint.getAccessPointId(), apValidationErrorsVO, successfulGeneration);
    }

    /**
     * Provede přidání AP do fronty pro přegenerování/validaci.
     * V případě, že ve frontě již AP je, nepřidá se.
     *
     * @param accessPoints přístupové body
     */
    public void generateAsync(final List<ApAccessPoint> accessPoints) {
        if (CollectionUtils.isNotEmpty(accessPoints)) {
            List<ApAccessPointQueueItem> accessPointQueueItems = new ArrayList<>();
            List<ApAccessPointQueueItem> queueApList = accessPointQueueItemRepository.findAll();

            for (ApAccessPoint accessPoint : accessPoints) {
                if (!isApAlreadyInQueue(queueApList, accessPoint)) {
                    ApAccessPointQueueItem accessPointQueueItem = new ApAccessPointQueueItem();
                    accessPointQueueItem.setAccessPoint(accessPoint);
                    accessPointQueueItems.add(accessPointQueueItem);
                }
            }

            if (CollectionUtils.isNotEmpty(accessPointQueueItems)) {
                accessPointQueueItemRepository.saveAll(accessPointQueueItems);
            }
        }
    }

    private boolean isApAlreadyInQueue(List<ApAccessPointQueueItem> queueApList, ApAccessPoint accessPoint) {
        if (CollectionUtils.isNotEmpty(queueApList)) {
            for (ApAccessPointQueueItem item : queueApList) {
                if (item.getAccessPoint().getAccessPointId().equals(accessPoint.getAccessPointId())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onApplicationEvent(AccessPointQueueEvent accessPointQueueEvent) {
        generateAsync(accessPointQueueEvent.getApAccessPoints());
    }
}
