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
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApFragment;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApNameItem;
import cz.tacr.elza.domain.ApRule;
import cz.tacr.elza.domain.ApRuleSystem;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBodyItemRepository;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApFragmentItemRepository;
import cz.tacr.elza.repository.ApFragmentRepository;
import cz.tacr.elza.repository.ApNameItemRepository;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.repository.ApRuleRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
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

    private final ApRuleRepository ruleRepository;
    private final ResourcePathResolver resourcePathResolver;
    private final ApFragmentItemRepository fragmentItemRepository;
    private final ApNameItemRepository nameItemRepository;
    private final ApBodyItemRepository bodyItemRepository;
    private final ApNameRepository apNameRepository;
    private final RuleService ruleService;
    private final ApFragmentRepository fragmentRepository;
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
    public AccessPointGeneratorService(final ApRuleRepository ruleRepository,
                                       final ResourcePathResolver resourcePathResolver,
                                       final ApFragmentItemRepository fragmentItemRepository,
                                       final ApNameItemRepository nameItemRepository,
                                       final ApBodyItemRepository bodyItemRepository,
                                       final ApNameRepository apNameRepository,
                                       final RuleService ruleService,
                                       final ApFragmentRepository fragmentRepository,
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
        this.ruleRepository = ruleRepository;
        this.resourcePathResolver = resourcePathResolver;
        this.fragmentItemRepository = fragmentItemRepository;
        this.nameItemRepository = nameItemRepository;
        this.bodyItemRepository = bodyItemRepository;
        this.apNameRepository = apNameRepository;
        this.ruleService = ruleService;
        this.apDataService = apDataService;
        this.fragmentRepository = fragmentRepository;
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
        ApAccessPoint accessPoint = apRepository.findOne(accessPointId);
        ApChange change;
        if (changeId == null) {
            change = apDataService.createChange(ApChange.Type.AP_REVALIDATE);
        } else {
            change = apChangeRepository.findOne(changeId);
        }
        logger.info("Asynchronní zpracování AP={} ApChache={}", accessPointId, change.getChangeId());
        generateAndSetResult(accessPoint, change);
        logger.info("Asynchronní zpracování AP={} ApChache={} - END - State={}", accessPointId, change.getChangeId(), accessPoint.getState());
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

    public void generateAndSetResult(final ApFragment fragment) {
        List<ApItem> fragmentItems = new ArrayList<>(fragmentItemRepository.findValidItemsByFragment(fragment));
        FragmentErrorDescription fragmentErrorDescription = new FragmentErrorDescription();
        ApState stateOld = fragment.getState();
        ApState state = ApState.OK;

        validateFragmentItems(fragmentErrorDescription, fragment, fragmentItems);

        String value = null;
        try {
            value = generateValue(fragment, fragmentItems);
        } catch (Exception e) {
            logger.error("Selhání groovy scriptu (fragmentId: {})", fragment.getFragmentId(), e);
            fragmentErrorDescription.setScriptFail(true);
            state = ApState.ERROR;
        }

        if (StringUtils.isEmpty(value)) {
            fragmentErrorDescription.setEmptyValue(true);
            state = ApState.ERROR;
        }
        if (CollectionUtils.isNotEmpty(fragmentErrorDescription.getImpossibleItemTypeIds())
                || CollectionUtils.isNotEmpty(fragmentErrorDescription.getRequiredItemTypeIds())) {
            state = ApState.ERROR;
        }
        fragment.setValue(value);
        fragment.setErrorDescription(fragmentErrorDescription.asJsonString());
        fragment.setState(stateOld == ApState.TEMP ? ApState.TEMP : state);
        fragmentRepository.save(fragment);

        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.FRAGMENT_UPDATE, fragment.getFragmentId()));
    }

    private String generateValue(final ApFragment fragment, final List<ApItem> items) {

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

    public void generateAndSetResult(final ApAccessPoint accessPoint, final ApChange change) {

        if (accessPoint.getRuleSystem() == null) {
            logger.warn("Přístupový bod {} nemá vazbu na pravidla a nebude se provádět script", accessPoint.getAccessPointId());
            eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.ACCESS_POINT_UPDATE, accessPoint.getAccessPointId()));
            return;
        }

        List<ApItem> apItems = new ArrayList<>(bodyItemRepository.findValidItemsByAccessPoint(accessPoint));
        List<ApName> apNames = apNameRepository.findByAccessPoint(accessPoint);

        Map<Integer, ApName> apNameMap = apNames.isEmpty()
                ? Collections.emptyMap()
                : apNames.stream().collect(Collectors.toMap(ApName::getNameId, Function.identity()));

        List<ApNameItem> nameItems = apNames.isEmpty()
                ? Collections.emptyList()
                : nameItemRepository.findValidItemsByNames(apNames);

        Map<Integer, List<ApItem>> nameItemsMap = createNameItemsMap(nameItems);

        ApErrorDescription apErrorDescription = new ApErrorDescription();
        ApState apStateOld = accessPoint.getState();
        ApState apState = ApState.OK;

        validateApItems(apErrorDescription, accessPoint, apItems);

        try {
            AccessPoint result = generateValue(accessPoint, apItems, apNames, nameItemsMap);
            boolean hasError = processResult(accessPoint, change, apNameMap, nameItemsMap, result);
            if (hasError) {
                apState = ApState.ERROR;
            }
        } catch (Exception e) {
            logger.error("Selhání groovy scriptu (accessPointId: {})", accessPoint.getAccessPointId(), e);
            apErrorDescription.setScriptFail(true);
            apState = ApState.ERROR;
        }

        if (CollectionUtils.isNotEmpty(apErrorDescription.getImpossibleItemTypeIds())
                || CollectionUtils.isNotEmpty(apErrorDescription.getRequiredItemTypeIds())) {
            apState = ApState.ERROR;
        }

        accessPoint.setErrorDescription(apErrorDescription.asJsonString());
        accessPoint.setState(apStateOld == ApState.TEMP ? ApState.TEMP : apState);
        accessPointService.saveWithLock(accessPoint);

        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.ACCESS_POINT_UPDATE, accessPoint.getAccessPointId()));

        accessPointService.reindexDescItem(accessPoint);
    }

    private boolean processResult(final ApAccessPoint accessPoint, final ApChange change, final Map<Integer, ApName> apNameMap, final Map<Integer, List<ApItem>> nameItemsMap, final AccessPoint result) {

        // zpracování změny charakteristiky
        apDataService.changeDescription(accessPoint, result.getDescription(), change);

        // zpracování jednotlivých jmen přístupového bodu
        List<NameContext> nameContexts = createNameContextsFromResult(accessPoint, change, apNameMap, nameItemsMap, result);
        return processNameContexts(accessPoint, nameContexts);
    }

    private boolean processNameContexts(final ApAccessPoint accessPoint, final List<NameContext> nameContexts) {
        boolean error = false;
        for (NameContext nameContext : nameContexts) {
            ApName name = nameContext.getName();
            NameErrorDescription errorDescription = nameContext.getErrorDescription();

            if (StringUtils.isEmpty(name.getFullName())) {
                errorDescription.setEmptyValue(true);
                nameContext.setState(ApState.ERROR);
            } else {
                boolean isUnique = apDataService.isNameUnique(accessPoint.getScope(), name.getFullName());
                if (!isUnique) {
                    errorDescription.setDuplicateValue(true);
                    nameContext.setState(ApState.ERROR);
                }
            }

            name.setErrorDescription(errorDescription.asJsonString());
            name.setState(nameContext.getStateOld() == ApState.TEMP ? ApState.TEMP : nameContext.getState());
            if (name.getState() == ApState.ERROR) {
                error = true;
            }
            apNameRepository.save(name);
        }
        return error;
    }

    private List<NameContext> createNameContextsFromResult(final ApAccessPoint accessPoint, final ApChange change, final Map<Integer, ApName> apNameMap, final Map<Integer, List<ApItem>> nameItemsMap, final AccessPoint result) {
        List<NameContext> nameContexts = new ArrayList<>();
        for (Name name : result.getNames()) {
            ApName apName = apNameMap.get(name.getId());
            List<ApItem> items = nameItemsMap.getOrDefault(apName.getNameId(), Collections.emptyList());

            if (!apDataService.equalsNames(apName, name.getName(), name.getComplement(), name.getFullName(), apName.getLanguageId())) {
                ApName apNameNew = apDataService.updateAccessPointName(accessPoint, apName, name.getName(), name.getComplement(), name.getFullName(), apName.getLanguage(), change, false);
                if (apName != apNameNew) {
                    items = apItemService.copyItems(apName, apNameNew, change);
                    apName = apNameNew;
                }
            }

            NameErrorDescription nameErrorDescription = new NameErrorDescription();
            NameContext nameContext = new NameContext(apName, apName.getState(), ApState.OK, nameErrorDescription);
            validateNameItems(nameErrorDescription, apName, items);

            if (CollectionUtils.isNotEmpty(nameErrorDescription.getImpossibleItemTypeIds())
                    || CollectionUtils.isNotEmpty(nameErrorDescription.getRequiredItemTypeIds())) {
                nameContext.setState(ApState.ERROR);
            }

            nameContexts.add(nameContext);
        }
        return nameContexts;
    }

    private Map<Integer, List<ApItem>> createNameItemsMap(final List<ApNameItem> nameItems) {
        Map<Integer, List<ApItem>> nameItemsMap = new HashMap<>();
        for (ApNameItem nameItem : nameItems) {
            Integer nameId = nameItem.getNameId();
            List<ApItem> items = nameItemsMap.computeIfAbsent(nameId, k -> new ArrayList<>());
            items.add(nameItem);
        }
        return nameItemsMap;
    }

    private AccessPoint generateValue(final ApAccessPoint accessPoint, final List<ApItem> apItems, final List<ApName> names, final Map<Integer, List<ApItem>> nameItems) {

        File groovyFile = findGroovyFile(accessPoint);

        GroovyScriptService.GroovyScriptFile groovyScriptFile = groovyScriptMap.get(groovyFile);
        if (groovyScriptFile == null) {
            groovyScriptFile = new GroovyScriptService.GroovyScriptFile(groovyFile);
            groovyScriptMap.put(groovyFile, groovyScriptFile);
        }

        Map<String, Object> input = new HashMap<>();
        input.put(AP, ModelFactory.createAp(accessPoint, apItems, names, nameItems));

        return (AccessPoint) groovyScriptFile.evaluate(input);
    }

    private File findGroovyFile(final ApFragment fragment) {
        RulStructuredType structureType = fragment.getFragmentType();
        List<RulStructureExtensionDefinition> structureExtensionDefinitions = structureExtensionDefinitionRepository
                .findByStructureTypeAndDefTypeOrderByPriority(structureType, RulStructureExtensionDefinition.DefType.SERIALIZED_VALUE);
        RulComponent component;
        RulPackage rulPackage;
        if (structureExtensionDefinitions.size() > 0) {
            RulStructureExtensionDefinition structureExtensionDefinition = structureExtensionDefinitions.get(structureExtensionDefinitions.size() - 1);
            component = structureExtensionDefinition.getComponent();
            rulPackage = structureExtensionDefinition.getRulPackage();
        } else {
            List<RulStructureDefinition> structureDefinitions = structureDefinitionRepository
                    .findByStructTypeAndDefTypeOrderByPriority(structureType, RulStructureDefinition.DefType.SERIALIZED_VALUE);
            if (structureDefinitions.size() > 0) {
                RulStructureDefinition structureDefinition = structureDefinitions.get(structureDefinitions.size() - 1);
                component = structureDefinition.getComponent();
                rulPackage = structureDefinition.getRulPackage();
            } else {
                throw new SystemException("Strukturovaný typ '" + structureType.getCode() + "' nemá žádný script pro výpočet hodnoty", BaseCode.INVALID_STATE);
            }
        }

        return resourcePathResolver.getGroovyDir(rulPackage)
                .resolve(component.getFilename())
                .toFile();
    }

    private File findGroovyFile(final ApAccessPoint accessPoint) {
        ApRuleSystem ruleSystem = accessPoint.getRuleSystem();
        ApRule rule = ruleRepository.findByRuleSystemAndRuleType(ruleSystem, ApRule.RuleType.TEXT_GENERATOR);
        if (rule == null) {
            throw new SystemException("Nebyly nalezeny pravidla generování pro přítupový bod", BaseCode.SYSTEM_ERROR);
        }
        RulComponent component = rule.getComponent();
        return resourcePathResolver.getGroovyDir(ruleSystem.getRulPackage())
                .resolve(component.getFilename())
                .toFile();
    }

    private void validateFragmentItems(final ErrorDescription errorDescription,
                                       final ApFragment fragment,
                                       final List<ApItem> items) {
        List<RulItemTypeExt> fragmentItemTypes = ruleService.getFragmentItemTypesInternal(fragment.getFragmentType(), items);
        validateItems(errorDescription, items, fragmentItemTypes);
    }

    private void validateNameItems(final ErrorDescription errorDescription,
                                   final ApName name,
                                   final List<ApItem> items) {
        List<RulItemTypeExt> nameItemTypes = ruleService.getApItemTypesInternal(name.getAccessPoint().getApType(), items, ApRule.RuleType.NAME_ITEMS);
        validateItems(errorDescription, items, nameItemTypes);
    }

    private void validateApItems(final ErrorDescription errorDescription,
                                 final ApAccessPoint accessPoint,
                                 final List<ApItem> items) {
        List<RulItemTypeExt> bodyItemTypes = ruleService.getApItemTypesInternal(accessPoint.getApType(), items, ApRule.RuleType.BODY_ITEMS);
        validateItems(errorDescription, items, bodyItemTypes);
    }

    private void validateItems(final ErrorDescription errorDescription, final List<ApItem> items, final List<RulItemTypeExt> fragmentItemTypes) {
        List<RulItemTypeExt> requiredItemTypes = fragmentItemTypes.stream().filter(itemType -> RulItemType.Type.REQUIRED == itemType.getType()).collect(Collectors.toList());
        List<RulItemTypeExt> impossibleItemTypes = fragmentItemTypes.stream().filter(itemType -> RulItemType.Type.IMPOSSIBLE == itemType.getType()).collect(Collectors.toList());

        for (RulItemTypeExt requiredItemType : requiredItemTypes) {
            boolean add = true;
            for (ApItem item : items) {
                if (item.getItemType().getCode().equals(requiredItemType.getCode())) {
                    add = false;
                    break;
                }
            }
            if (add) {
                errorDescription.getRequiredItemTypeIds().add(requiredItemType.getItemTypeId());
            }
        }

        for (RulItemTypeExt impossibleItemType : impossibleItemTypes) {
            boolean add = false;
            for (ApItem item : items) {
                if (item.getItemType().getCode().equals(impossibleItemType.getCode())) {
                    add = true;
                    break;
                }
            }
            if (add) {
                errorDescription.getImpossibleItemTypeIds().add(impossibleItemType.getItemTypeId());
            }
        }
    }

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
     * Pomocná třída při zpracování jména.
     */
    private static class NameContext {

        private ApName name;

        private ApState stateOld;

        private ApState state;

        private NameErrorDescription errorDescription;

        public NameContext(final ApName name, final ApState stateOld, final ApState state, final NameErrorDescription errorDescription) {
            this.name = name;
            this.stateOld = stateOld;
            this.state = state;
            this.errorDescription = errorDescription;
        }

        public void setState(final ApState state) {
            this.state = state;
        }

        public ApName getName() {
            return name;
        }

        public ApState getStateOld() {
            return stateOld;
        }

        public ApState getState() {
            return state;
        }

        public NameErrorDescription getErrorDescription() {
            return errorDescription;
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
