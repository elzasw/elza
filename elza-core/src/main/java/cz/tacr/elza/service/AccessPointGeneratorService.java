package cz.tacr.elza.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.eventbus.Subscribe;
import cz.tacr.elza.common.TaskExecutor;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.vo.AccessPoint;
import cz.tacr.elza.service.vo.Name;

/**
 * Serviska pro generování.
 */
@Service
public class AccessPointGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(AccessPointGeneratorService.class);

    public static final String ITEMS = "ITEMS";
    public static final String AP = "AP";

    private final ResourcePathResolver resourcePathResolver;
    private final ApItemRepository itemRepository;
    private final RuleService ruleService;
    private final ApPartRepository partRepository;
    private final AccessPointDataService apDataService;
    private final ApAccessPointRepository apRepository;
    private final AccessPointItemService apItemService;
    private final ApplicationContext appCtx;
    private final ApChangeRepository apChangeRepository;
    private final IEventNotificationService eventNotificationService;
    private final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;
    private final StructureDefinitionRepository structureDefinitionRepository;
    private final AccessPointService accessPointService;
    private final EntityManager em;

    private Map<File, GroovyScriptService.GroovyScriptFile> groovyScriptMap = new HashMap<>();

    private final TaskExecutor taskExecutor = new TaskExecutor(1);
    private final BlockingQueue<ApQueueItem> queue = new LinkedBlockingQueue<>();

    @Autowired
    public AccessPointGeneratorService(final ResourcePathResolver resourcePathResolver,
                                       final ApItemRepository itemRepository,
                                       final RuleService ruleService,
                                       final ApPartRepository partRepository,
                                       final AccessPointDataService apDataService,
                                       final ApAccessPointRepository apRepository,
                                       final AccessPointItemService apItemService,
                                       final ApplicationContext appCtx,
                                       final ApChangeRepository apChangeRepository,
                                       final IEventNotificationService eventNotificationService,
                                       final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository,
                                       final StructureDefinitionRepository structureDefinitionRepository,
                                       final AccessPointService accessPointService,
                                       final EntityManager em) {
        this.resourcePathResolver = resourcePathResolver;
        this.itemRepository = itemRepository;
        this.ruleService = ruleService;
        this.apDataService = apDataService;
        this.partRepository = partRepository;
        this.apRepository = apRepository;
        this.apItemService = apItemService;
        this.appCtx = appCtx;
        this.apChangeRepository = apChangeRepository;
        this.eventNotificationService = eventNotificationService;
        this.structureExtensionDefinitionRepository = structureExtensionDefinitionRepository;
        this.structureDefinitionRepository = structureDefinitionRepository;
        this.accessPointService = accessPointService;
        this.em = em;
        this.taskExecutor.addTask(new AccessPointGeneratorThread());
        this.taskExecutor.start();
    }

    @Subscribe
    public synchronized void invalidateCache(final CacheInvalidateEvent cacheInvalidateEvent) {
        if (cacheInvalidateEvent.contains(CacheInvalidateEvent.Type.GROOVY)) {
            groovyScriptMap = new HashMap<>();
        }
    }

    /**
     * Provede revalidaci AP, které nebyly dokončeny před restartem serveru.
     */
    public void restartQueuedAccessPoints() {
        Set<Integer> accessPointIds = apRepository.findInitAccessPointIds();
        for (Integer accessPointId : accessPointIds) {
            generateAsyncAfterCommit(accessPointId, null);
        }
    }

    /**
     * Třída pro zpracování požadavků pro asynchronní zpracování.
     */
    private class AccessPointGeneratorThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                ApQueueItem item = null;
                try {
                    item = queue.take();
                    appCtx.getBean(AccessPointGeneratorService.class).processAsyncGenerate(item.getAccessPointId(), item.getChangeId());
                } catch (InterruptedException e) {
                    logger.info("Closing generator", e);
                    break;
                } catch (Exception e) {
                    logger.error("Process generate fail on accessPointId: {}", item.getAccessPointId(), e);
                }
            }
        }
    }

    /**
     * Zpracování požadavku AP.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param changeId      identifikátor změny
     */
    @Transactional
    public void processAsyncGenerate(final Integer accessPointId, final Integer changeId) {
        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        ApChange apChange = /*changeId != null
                            ? apChangeRepository.findOne(changeId)
                            :*/ apDataService.createChange(ApChange.Type.AP_REVALIDATE);
        logger.info("Asynchronní zpracování AP={} ApChache={}", accessPointId, apChange.getChangeId());
        generateAndSetResult(apState, apChange);
        logger.info("Asynchronní zpracování AP={} ApChache={} - END - State={}", accessPointId, apChange.getChangeId(), accessPoint.getState());
    }

    /**
     * Provede přidání AP do fronty pro přegenerování/validaci po dokončení aktuální transakce.
     * V případě, že ve frontě již AP je, nepřidá se.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param changeId      identifikátor změny
     */
    public void generateAsyncAfterCommit(final Integer accessPointId,
                                         @Nullable final Integer changeId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                ApQueueItem item = new ApQueueItem(accessPointId, changeId);
                synchronized (queue) {
                    if (!queue.contains(item)) {
                        try {
                            queue.put(item);
                        } catch (InterruptedException e) {
                            logger.error("Fail insert AP to queue", e);
                        }
                    }
                }
            }
        });
    }

    public void generateAndSetResult(final ApPart fragment) {
        List<ApItem> fragmentItems = new ArrayList<>(itemRepository.findValidItemsByPart(fragment));
        FragmentErrorDescription fragmentErrorDescription = new FragmentErrorDescription();
        ApStateEnum stateOld = fragment.getState();
        ApStateEnum state = ApStateEnum.OK;

        //TODO fantis: smazat nebo prepsat na novou strukturu
//        validateFragmentItems(fragmentErrorDescription, fragment, fragmentItems);

        String value = null;
        try {
            value = generateValue(fragment, fragmentItems);
        } catch (Exception e) {
            logger.error("Selhání groovy scriptu (fragmentId: {})", fragment.getPartId(), e);
            fragmentErrorDescription.setScriptFail(true);
            state = ApStateEnum.ERROR;
        }

        if (StringUtils.isEmpty(value)) {
            fragmentErrorDescription.setEmptyValue(true);
            state = ApStateEnum.ERROR;
        }
        if (CollectionUtils.isNotEmpty(fragmentErrorDescription.getImpossibleItemTypeIds())
                || CollectionUtils.isNotEmpty(fragmentErrorDescription.getRequiredItemTypeIds())) {
            state = ApStateEnum.ERROR;
        }
        fragment.setValue(value);
        fragment.setErrorDescription(fragmentErrorDescription.asJsonString());
        fragment.setState(stateOld == ApStateEnum.TEMP ? ApStateEnum.TEMP : state);
        partRepository.save(fragment);

        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.FRAGMENT_UPDATE, fragment.getPartId()));
    }

    private String generateValue(final ApPart fragment, final List<ApItem> items) {

        File groovyFile = findGroovyFile(fragment);

        GroovyScriptService.GroovyScriptFile groovyScriptFile = groovyScriptMap.get(groovyFile);
        if (groovyScriptFile == null) {
            groovyScriptFile = new GroovyScriptService.GroovyScriptFile(groovyFile);
            groovyScriptMap.put(groovyFile, groovyScriptFile);
        }

        Map<String, Object> input = new HashMap<>();
        input.put(ITEMS, ModelFactory.createApItems(items));

        return (String) groovyScriptFile.evaluate(input);
    }

    public void generateAndSetResult(final ApState apState, final ApChange apChange) {

        ApAccessPoint accessPoint = apState.getAccessPoint();

        logger.warn("Přístupový bod {} nemá vazbu na pravidla a nebude se provádět script", accessPoint.getAccessPointId());
        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.ACCESS_POINT_UPDATE, accessPoint.getAccessPointId()));


        /*if (accessPoint.getRuleSystem() == null) {
            logger.warn("Přístupový bod {} nemá vazbu na pravidla a nebude se provádět script", accessPoint.getAccessPointId());
            eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.ACCESS_POINT_UPDATE, accessPoint.getAccessPointId()));
            return;
        }

        List<ApItem> apItems = new ArrayList<>(itemRepository.findValidItemsByAccessPoint(accessPoint));

        ApErrorDescription apErrorDescription = new ApErrorDescription();
        ApStateEnum apStateEnumOld = accessPoint.getState();
        ApStateEnum apStateEnum = ApStateEnum.OK;

        validateApItems(apErrorDescription, apState, apItems);

        if (CollectionUtils.isNotEmpty(apErrorDescription.getImpossibleItemTypeIds())
                || CollectionUtils.isNotEmpty(apErrorDescription.getRequiredItemTypeIds())) {
            apStateEnum = ApStateEnum.ERROR;
        }

        accessPoint.setErrorDescription(apErrorDescription.asJsonString());
        accessPoint.setState(apStateEnumOld == ApStateEnum.TEMP ? ApStateEnum.TEMP : apStateEnum);
        accessPointService.saveWithLock(accessPoint);

        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.ACCESS_POINT_UPDATE, accessPoint.getAccessPointId()));

        accessPointService.reindexDescItem(accessPoint);*/
    }

    private File findGroovyFile(final ApPart fragment) {
        //TODO fantis: prepsat na novy zpusob
        return null;
//        RulStructuredType structureType = fragment.getFragmentType();
//        List<RulStructureExtensionDefinition> structureExtensionDefinitions = structureExtensionDefinitionRepository
//                .findByStructureTypeAndDefTypeOrderByPriority(structureType, RulStructureExtensionDefinition.DefType.SERIALIZED_VALUE);
//        RulComponent component;
//        RulPackage rulPackage;
//        if (structureExtensionDefinitions.size() > 0) {
//            RulStructureExtensionDefinition structureExtensionDefinition = structureExtensionDefinitions.get(structureExtensionDefinitions.size() - 1);
//            component = structureExtensionDefinition.getComponent();
//            rulPackage = structureExtensionDefinition.getRulPackage();
//        } else {
//            List<RulStructureDefinition> structureDefinitions = structureDefinitionRepository
//                    .findByStructTypeAndDefTypeOrderByPriority(structureType, RulStructureDefinition.DefType.SERIALIZED_VALUE);
//            if (structureDefinitions.size() > 0) {
//                RulStructureDefinition structureDefinition = structureDefinitions.get(structureDefinitions.size() - 1);
//                component = structureDefinition.getComponent();
//                rulPackage = structureDefinition.getRulPackage();
//            } else {
//                throw new SystemException("Strukturovaný typ '" + structureType.getCode() + "' nemá žádný script pro výpočet hodnoty", BaseCode.INVALID_STATE);
//            }
//        }
//
//        return resourcePathResolver.getGroovyDir(rulPackage)
//                .resolve(component.getFilename())
//                .toFile();
    }

    private File findGroovyFile(final ApAccessPoint accessPoint) {
       /* ApRuleSystem ruleSystem = accessPoint.getRuleSystem();
        ApRule rule = ruleRepository.findByRuleSystemAndRuleType(ruleSystem, ApRule.RuleType.TEXT_GENERATOR);
        if (rule == null) {
            throw new SystemException("Nebyly nalezeny pravidla generování pro přítupový bod", BaseCode.SYSTEM_ERROR);
        }
        RulComponent component = rule.getComponent();
        return resourcePathResolver.getGroovyDir(ruleSystem.getRulPackage())
                .resolve(component.getFilename())
                .toFile();*/
       return null;
    }

    //TODO fantis: smazat nebo prepsat na novou strukturu
    /*private void validateFragmentItems(final ErrorDescription errorDescription,
                                       final ApPart fragment,
                                       final List<ApItem> items) {
        List<RulItemTypeExt> fragmentItemTypes = ruleService.getFragmentItemTypesInternal(fragment.getFragmentType(), items);
        validateItems(errorDescription, items, fragmentItemTypes);
    }*/

    private static class ErrorDescription {

        protected static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

        protected Boolean emptyValue;
        protected Boolean scriptFail;
        protected List<Integer> requiredItemTypeIds;
        protected List<Integer> impossibleItemTypeIds;

        public ErrorDescription() {
            emptyValue = false;
            scriptFail = false;
            requiredItemTypeIds = new ArrayList<>();
            impossibleItemTypeIds = new ArrayList<>();
        }

        public String asJsonString() {
            if (generate()) {
                try {
                    return objectMapper.writeValueAsString(this);
                } catch (JsonProcessingException e) {
                    throw new SystemException("Nepodařilo se serializovat data");
                }
            }
            return null;
        }

        protected boolean generate() {
            return BooleanUtils.isTrue(emptyValue)
                    || BooleanUtils.isTrue(scriptFail)
                    || CollectionUtils.isNotEmpty(requiredItemTypeIds)
                    || CollectionUtils.isNotEmpty(impossibleItemTypeIds);
        }

        public Boolean getEmptyValue() {
            return emptyValue;
        }

        public void setEmptyValue(final Boolean emptyValue) {
            this.emptyValue = emptyValue;
        }

        public List<Integer> getRequiredItemTypeIds() {
            return requiredItemTypeIds;
        }

        public void setRequiredItemTypeIds(final List<Integer> requiredItemTypeIds) {
            this.requiredItemTypeIds = requiredItemTypeIds;
        }

        public List<Integer> getImpossibleItemTypeIds() {
            return impossibleItemTypeIds;
        }

        public void setImpossibleItemTypeIds(final List<Integer> impossibleItemTypeIds) {
            this.impossibleItemTypeIds = impossibleItemTypeIds;
        }

        public Boolean getScriptFail() {
            return scriptFail;
        }

        public void setScriptFail(final Boolean scriptFail) {
            this.scriptFail = scriptFail;
        }
    }

    public static class FragmentErrorDescription extends ErrorDescription {
        public static FragmentErrorDescription fromJson(String json) {
            ObjectReader reader = objectMapper.readerFor(FragmentErrorDescription.class);
            try {
                return reader.readValue(json);
            } catch (IOException e) {
                throw new SystemException("Failed to deserialize value").set("json", json);
            }
        }
    }

    public static class NameErrorDescription extends ErrorDescription {

        protected Boolean duplicateValue;

        public NameErrorDescription() {
            duplicateValue = false;
        }

        protected boolean generate() {
            return super.generate() || BooleanUtils.isTrue(duplicateValue);
        }

        public static NameErrorDescription fromJson(String json) {
            ObjectReader reader = objectMapper.readerFor(NameErrorDescription.class);
            try {
                return reader.readValue(json);
            } catch (IOException e) {
                throw new SystemException("Failed to deserialize value").set("json", json);
            }
        }

        public Boolean getDuplicateValue() {
            return duplicateValue;
        }

        public void setDuplicateValue(final Boolean duplicateValue) {
            this.duplicateValue = duplicateValue;
        }
    }

    public static class ApErrorDescription extends ErrorDescription {
        public static ApErrorDescription fromJson(String json) {
            ObjectReader reader = objectMapper.readerFor(ApErrorDescription.class);
            try {
                return reader.readValue(json);
            } catch (IOException e) {
                throw new SystemException("Failed to deserialize value").set("json", json);
            }
        }
    }

    /**
     * Položka ve frontě na zpracování.
     */
    private class ApQueueItem {

        private Integer accessPointId;
        private Integer changeId;

        public ApQueueItem(final Integer accessPointId, final Integer changeId) {
            this.accessPointId = accessPointId;
            this.changeId = changeId;
        }

        public Integer getAccessPointId() {
            return accessPointId;
        }

        public Integer getChangeId() {
            return changeId;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ApQueueItem that = (ApQueueItem) o;
            return Objects.equals(accessPointId, that.accessPointId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accessPointId);
        }
    }

}
