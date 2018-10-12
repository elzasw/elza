package cz.tacr.elza.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.eventbus.Subscribe;

import cz.tacr.elza.EventBusListener;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrSobjVrequest;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ArrStructuredObject.State;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.SobjVrequestRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.GroovyScriptService.GroovyScriptFile;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventStructureDataChange;

/**
 * Servisní třída pro aktualizaci hodnot strukturovaných objektů.
 *
 *
 * Pokud je do fronty zařazen smazaný uzel, tak se použije
 * pouze k validaci duplicit.
 *
 * @since 13.11.2017
 */
@Service
@EventBusListener
public class StructObjValueService {

    private static final Logger logger = LoggerFactory.getLogger(StructObjValueService.class);

    private static int QUEUE_CHECK_TIME_INTERVAL = 10000; // 10s

    private final EntityManager em;
    private final StructuredItemRepository structureItemRepository;
    private final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;
    private final StructureDefinitionRepository structureDefinitionRepository;
    private final StructuredObjectRepository structObjRepository;
    private final FundVersionRepository fundVersionRepository;
    private final RuleService ruleService;
    private final ApplicationContext applicationContext;
    private final EventNotificationService notificationService;
    private final ResourcePathResolver resourcePathResolver;
    private final SobjVrequestRepository sobjVrequestRepository;

    //private Queue<Integer> queueObjIds = new ConcurrentLinkedQueue<>();
    private final Object lock = new Object();

    private Thread generatorThread = null;
    private boolean stopGenerator = false;

    private Map<File, GroovyScriptService.GroovyScriptFile> groovyScriptMap = new HashMap<>();

    @Subscribe
    public synchronized void invalidateCache(final CacheInvalidateEvent cacheInvalidateEvent) {
        if (cacheInvalidateEvent.contains(CacheInvalidateEvent.Type.GROOVY)) {
            groovyScriptMap = new HashMap<>();
        }
    }

    @Autowired
    public StructObjValueService(final StructuredItemRepository structureItemRepository,
            final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository,
            final StructureDefinitionRepository structureDefinitionRepository,
            final StructuredObjectRepository structureDataRepository,
            final FundVersionRepository fundVersionRepository,
            final RuleService ruleService,
            final ApplicationContext applicationContext,
            final EventNotificationService notificationService,
            final ResourcePathResolver resourcePathResolver,
            final SobjVrequestRepository sobjQueueRepository,
            final EntityManager em) {
        this.structureItemRepository = structureItemRepository;
        this.structureExtensionDefinitionRepository = structureExtensionDefinitionRepository;
        this.structureDefinitionRepository = structureDefinitionRepository;
        this.structObjRepository = structureDataRepository;
        this.fundVersionRepository = fundVersionRepository;
        this.ruleService = ruleService;
        this.applicationContext = applicationContext;
        this.notificationService = notificationService;
        this.resourcePathResolver = resourcePathResolver;
        this.sobjVrequestRepository = sobjQueueRepository;
        this.em = em;
    }

    /**
     * Přidání hodnoty k validaci.
     *
     * @param structureData
     *            hodnota
     */
    public void addToValidate(final ArrStructuredObject sobj) {
        ArrSobjVrequest sobjQueue = new ArrSobjVrequest();
        sobjQueue.setStructuredObject(sobj);

        sobjVrequestRepository.save(sobjQueue);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                notifyGenerator();
                //runValidator(Collections.singletonList(structureData.getStructuredObjectId()));
            }
        });
    }

    /**
     * Přidání hodnot k validaci.
     *
     * @param structureDataList
     *            seznam hodnot
     */
    public void addToValidate(final List<ArrStructuredObject> structureDataList) {
        Iterator<ArrSobjVrequest> it = structureDataList.stream().map(sobj -> {
            ArrSobjVrequest sobjQueue = new ArrSobjVrequest();
            sobjQueue.setStructuredObject(sobj);
            return sobjQueue;
        }).iterator();

        sobjVrequestRepository.save((Iterable<ArrSobjVrequest>) (() -> it));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                notifyGenerator();
            }

        });
    }

    /**
     * Přidání hodnot k validaci.
     *
     * @param structureDataIds
     *            seznam identifikátorů hodnot
     */
    public void addIdsToValidate(final List<Integer> structureDataIds) {
        Iterator<ArrSobjVrequest> it = structureDataIds.stream().map(sobjId -> {
            ArrSobjVrequest sobjQueue = new ArrSobjVrequest();
            ArrStructuredObject sobj = em.getReference(ArrStructuredObject.class, sobjId);
            sobjQueue.setStructuredObject(sobj);
            return sobjQueue;
        }).iterator();

        sobjVrequestRepository.save((Iterable<ArrSobjVrequest>) (() -> it));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                notifyGenerator();
            }
        });
    }

    /**
     * Provede revalidaci podle strukt. typu.
     *
     * @param structureTypes
     *            revalidované typy
     */
    public void addToValidateByTypes(List<RulStructuredType> structTypes) {
        if (structTypes.isEmpty()) {
            return;
        }
        List<Integer> structureDataIds = structObjRepository.findStructuredObjectIdByStructureTypes(structTypes);
        addIdsToValidate(structureDataIds);
    }

    private void notifyGenerator() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public void startGenerator() {
        synchronized (lock) {
            // check that generator is not running
            Validate.isTrue(generatorThread == null);

            generatorThread = new Thread(() -> {
                generatorMain();
            }, "StructObjGenerator");
            generatorThread.start();
        }
    }

    public void stopGenerator() {
        while (true) {
            synchronized (lock) {
                if (generatorThread == null) {
                    Validate.isTrue(stopGenerator == false);
                    return;
                }
                if (stopGenerator == false) {
                    stopGenerator = true;
                    notifyGenerator();
                }
                // simply wait for stop
                try {
                    lock.wait(100);
                } catch (InterruptedException e) {
                    // nothing to do here
                    Thread.currentThread().interrupt();
                }
            }
        }

    }

    public void generatorMain() {
        while (true) {
            synchronized (lock) {
                if (stopGenerator) {
                    break;
                }
            }
            boolean moreData = false;
            try {
                // check queue
                StructObjValueService structureDataService = applicationContext.getBean(StructObjValueService.class);
                moreData = structureDataService.processBatch();
            } catch (Exception e) {
                logger.error("Failed to run generator", e);
            }
            if (!moreData) {
                // no more items -> sleep
                synchronized (lock) {
                    try {
                        lock.wait(QUEUE_CHECK_TIME_INTERVAL);
                    } catch (InterruptedException e) {
                        // request to stop thread -> nothing to do
                        stopGenerator = false;
                        generatorThread = null;
                        lock.notifyAll();

                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        // terminate
        synchronized (lock) {
            stopGenerator = false;
            generatorThread = null;
            lock.notifyAll();
        }
    }

    /**
     * Běh validace. NEVOLAT NA PŘÍMO!!!
     * 
     * @return Return true if has more data to process.
     *         Return false if there are no more data.
     */
    @Transactional(TxType.REQUIRED)
    public boolean processBatch() {
        Pageable pageable = new PageRequest(0, 100);
        // find first items
        Page<ArrSobjVrequest> page = sobjVrequestRepository.findAll(pageable);
        if (page == null || page.getNumberOfElements() <= 0) {
            return false;
        }

        List<ArrSobjVrequest> items = page.getContent();
        List<ArrSobjVrequest> delItems = new ArrayList<>(items.size());
        // now run validate
        items.forEach(item -> {
            final ArrStructuredObject sobj = item.getStructuredObject();
            try {
                generateAndValidate(sobj);
                delItems.add(item);
            } catch (Exception e) {
                logger.error("Nastala chyba při validaci hodnoty strukturovaného typu -> structureDataId="
                        + sobj.getStructuredObjectId(), e);
            }
        });

        // drop processed items
        sobjVrequestRepository.delete(delItems);

        return page.hasNext();
    }

    /**
     * Uložení hodnoty strukturovaného datového typu.
     * <ul>
     * <li>vygenerování textové hodnoty
     * <li>kontrola duplicity
     * <li>validace položek hodnoty
     *
     * @param structObj
     *            hodnota struktovaného datového typu
     *
     */
    private void generateAndValidate(final ArrStructuredObject structObj) {
        // do not generate for temp objects
        if (structObj.getState() == ArrStructuredObject.State.TEMP) {
            return;
        }

        generateValue(structObj);
    }

    /**
     * Internal method to generate value and save it.
     *
     * Method will only check if value is empty.
     *
     * @param structObj
     * @return Return generated value. Return null if failed
     */
    private void generateValue(ArrStructuredObject structObj) {
        // generate value
        String oldValue = structObj.getValue();
        ArrStructuredObject.State state = ArrStructuredObject.State.OK;
        ValidationErrorDescription validationErrorDescription = new ValidationErrorDescription();

        List<ArrStructuredItem> structureItems = structureItemRepository
                .findByStructuredObjectAndDeleteChangeIsNullFetchData(structObj);

        validateStructureItems(validationErrorDescription, structObj, structureItems);

        Result result = generateValue(structObj, structureItems);
        // Check if result is properly set (not empty)
        String value = result.getValue();
        String sortValue = result.getSortValue();
        if (StringUtils.isEmpty(value)) {
            validationErrorDescription.setEmptyValue(true);
        }
        // use value as sort value (if empty)
        if (StringUtils.isEmpty(sortValue)) {
            sortValue = value;
        }

        // run duplicate check only for nondeleted values
        if (structObj.getDeleteChangeId() == null) {
            boolean duplicatedValues = false;
            // check duplicates
            if (StringUtils.isNotEmpty(oldValue)) {
                if (oldValue.equals(value)) {
                    duplicatedValues |= recheckDuplicates(oldValue, structObj, 0);
                } else {
                    recheckDuplicates(oldValue, structObj, 1);
                }
            }

            if (StringUtils.isNotEmpty(value)) {
                // same values -> do only one check
                if (!value.equals(oldValue)) {
                    duplicatedValues |= recheckDuplicates(value, structObj, 0);
                }
            }
            if (duplicatedValues) {
                validationErrorDescription.setDuplicateValue(true);
            }
        }

        if (validationErrorDescription.hasSomeError()) {
            state = ArrStructuredObject.State.ERROR;
        }

        // pokud se jedná o tempová data, stav se nenastavuje
        state = structObj.getState() == ArrStructuredObject.State.TEMP ? ArrStructuredObject.State.TEMP : state;

        // check if some value has changed
        boolean change = false;
        if (!StringUtils.equals(oldValue, value)) {
            structObj.setValue(value);
            change = true;
        }
        if (!StringUtils.equals(structObj.getSortValue(), sortValue)) {
            structObj.setSortValue(sortValue);
            change = true;
        }
        if (!Objects.equals(structObj.getState(), state)) {
            structObj.setState(state);
            change = true;
        }
        String errorDescr = validationErrorDescription.asJsonString();
        if (!StringUtils.equals(structObj.getErrorDescription(), errorDescr)) {
            structObj.setErrorDescription(errorDescr);
            change = true;
        }

        if (change) {
            structObjRepository.save(structObj);
            sendNotification(structObj);
        }
    }

    /**
     * Recheck struct objects for duplicates
     *
     * @param checkedValue
     *            Value to be check
     * @param srcStructObj
     *            Object which caused / requested this check
     *            Source object is not notified.
     */
    private boolean recheckDuplicates(String checkedValue,
                                   ArrStructuredObject srcStructObj,
                                      int numPossible) {
        // check duplicates - if not empty
        Validate.isTrue(StringUtils.isNotEmpty(checkedValue));
        Validate.isTrue(srcStructObj.getDeleteChangeId() == null);

        // read current structObjs
        List<ArrStructuredObject> validStructureDataList = structObjRepository
                .findValidByStructureTypeAndFund(srcStructObj.getStructuredType(), srcStructObj.getFund(),
                                                 checkedValue, srcStructObj);
        int cnt = validStructureDataList.size();

        boolean duplicatedValues;

        if (cnt <= numPossible) {
            duplicatedValues = false;
        } else {
            duplicatedValues = true;
        }

        for (ArrStructuredObject so : validStructureDataList) {
            // reset duplicated state
            setDuplicatedState(so, duplicatedValues);
            sendNotification(so);
        }

        return duplicatedValues;
    }

    private void sendNotification(ArrStructuredObject structObj) {
        Integer structObjId = structObj.getStructuredObjectId();
        // send notifications
        if (structObj.getState() == ArrStructuredObject.State.TEMP) {
            notificationService.publishEvent(new EventStructureDataChange(structObj.getFundId(),
                    structObj.getStructuredType().getCode(),
                    Collections.singletonList(structObjId),
                    null,
                    null,
                    null));
        } else {
            notificationService.publishEvent(new EventStructureDataChange(structObj.getFundId(),
                    structObj.getStructuredType().getCode(),
                    null,
                    null,
                    Collections.singletonList(structObjId),
                    null));
        }
    }

    private void setDuplicatedState(ArrStructuredObject so, boolean duplicated) {

        // Do not check duplicates on TEMP items
        Validate.isTrue(so.getState() != State.TEMP);

        String errorDescr = so.getErrorDescription();

        ValidationErrorDescription ved = new ValidationErrorDescription();
        if (StringUtils.isNotBlank(errorDescr)) {
            try {
                ValidationErrorDescription parsed = ValidationErrorDescription.fromJson(errorDescr);
                ved = parsed;
            } catch (Exception e) {
                logger.error("Failed to parse JSON: " + errorDescr, e);
            }
        }
        ved.setDuplicateValue(duplicated);

        String value = ved.asJsonString();
        if (value != null) {
            so.setState(State.ERROR);
        } else {
            so.setState(State.OK);
        }
        so.setErrorDescription(value);

        structObjRepository.save(so);
    }

    /**
     * Validace položek pro strukturovaný datový typ.
     *
     * @param validationErrorDescription
     *            objekt pro výsledky validace
     * @param structureData
     *            hodnota struktovaného datového typu
     * @param structureItems
     *            validované položky
     */
    private void validateStructureItems(final ValidationErrorDescription validationErrorDescription,
                                        final ArrStructuredObject structureData,
                                        final List<ArrStructuredItem> structureItems) {
        ArrFundVersion fundVersion = fundVersionRepository.findByFundIdAndLockChangeIsNull(structureData.getFundId());
        List<RulItemTypeExt> structureItemTypes = ruleService
                .getStructureItemTypesInternal(structureData.getStructuredType(), fundVersion, structureItems);
        List<RulItemTypeExt> requiredItemTypes = structureItemTypes.stream()
                .filter(itemType -> RulItemType.Type.REQUIRED == itemType.getType()).collect(Collectors.toList());
        List<RulItemTypeExt> impossibleItemTypes = structureItemTypes.stream()
                .filter(itemType -> RulItemType.Type.IMPOSSIBLE == itemType.getType()).collect(Collectors.toList());

        for (RulItemTypeExt requiredItemType : requiredItemTypes) {
            boolean add = true;
            for (ArrStructuredItem structureItem : structureItems) {
                if (structureItem.getItemType().getCode().equals(requiredItemType.getCode())) {
                    add = false;
                    break;
                }
            }
            if (add) {
                validationErrorDescription.getRequiredItemTypeIds().add(requiredItemType.getItemTypeId());
            }
        }

        for (RulItemTypeExt impossibleItemType : impossibleItemTypes) {
            boolean add = false;
            for (ArrStructuredItem structureItem : structureItems) {
                if (structureItem.getItemType().getCode().equals(impossibleItemType.getCode())) {
                    add = true;
                    break;
                }
            }
            if (add) {
                validationErrorDescription.getImpossibleItemTypeIds().add(impossibleItemType.getItemTypeId());
            }
        }
    }

    /**
     * Vygenerování hodnoty pro hodnotu strukt. datového typu.
     *
     * @param structureData
     *            hodnota struktovaného datového typu
     * @return hodnota
     */
    private Result generateValue(final ArrStructuredObject structureData,
                                 final List<ArrStructuredItem> structureItems) {

        RulStructuredType structureType = structureData.getStructuredType();
        File groovyFile = findGroovyFile(structureType, structureData.getFund());

        GroovyScriptService.GroovyScriptFile groovyScriptFile = groovyScriptMap.get(groovyFile);
        if (groovyScriptFile == null) {
            groovyScriptFile = new GroovyScriptFile(groovyFile);
            groovyScriptMap.put(groovyFile, groovyScriptFile);
        }

        Result result = new Result();

        Map<String, Object> input = new HashMap<>();
        input.put("ITEMS", structureItems);
        input.put("RESULT", result);

        groovyScriptFile.evaluate(input);

        return result;
    }

    /**
     * Vyhledání groovy scriptu podle strukturovaného typu k AS.
     *
     * @param structureType
     *            strukturovaný typ
     * @param fund
     *            archivní soubor
     * @return nalezený groovy soubor
     */
    private File findGroovyFile(final RulStructuredType structureType, final ArrFund fund) {
        List<RulStructureExtensionDefinition> structureExtensionDefinitions = structureExtensionDefinitionRepository
                .findByStructureTypeAndDefTypeAndFundOrderByPriority(structureType,
                                                                     RulStructureExtensionDefinition.DefType.SERIALIZED_VALUE,
                                                                     fund);
        RulComponent component;
        RulPackage rulPackage;
        if (structureExtensionDefinitions.size() > 0) {
            RulStructureExtensionDefinition structureExtensionDefinition = structureExtensionDefinitions
                    .get(structureExtensionDefinitions.size() - 1);
            component = structureExtensionDefinition.getComponent();
            rulPackage = structureExtensionDefinition.getRulPackage();
        } else {
            List<RulStructureDefinition> structureDefinitions = structureDefinitionRepository
                    .findByStructuredTypeAndDefTypeOrderByPriority(structureType,
                                                                   RulStructureDefinition.DefType.SERIALIZED_VALUE);
            if (structureDefinitions.size() > 0) {
                RulStructureDefinition structureDefinition = structureDefinitions.get(structureDefinitions.size() - 1);
                component = structureDefinition.getComponent();
                rulPackage = structureDefinition.getRulPackage();
            } else {
                throw new SystemException(
                        "Strukturovaný typ '" + structureType.getCode() + "' nemá žádný script pro výpočet hodnoty",
                        BaseCode.INVALID_STATE);
            }
        }

        return resourcePathResolver.getGroovyDir(rulPackage)
                .resolve(component.getFilename())
                .toFile();
    }

    /**
     * Provede smazání dočasných hodnot strukt. typu.
     * 
     * Metoda se spouští jen při inicializaci
     */
    public void removeTempStructureData() {
        // Check that service is not yet running
        Validate.isTrue(generatorThread == null);

        // Quick fix: change state to error for connected temp struct objs
        // TODO: Find why it is happening and fix cause of this issue
        List<ArrStructuredObject> connTempObjs = structObjRepository.findConnectedTempObjs();
        if (CollectionUtils.isNotEmpty(connTempObjs)) {
            // Exists inconsistent objects -> change state
            logger.error("Found inconsistent structured objects, count: " + connTempObjs.size());

            for (ArrStructuredObject so : connTempObjs) {
                logger.error("Updating object with id: " + so.getStructuredObjectId());
                so.setState(State.ERROR);
                structObjRepository.save(so);
            }
            //structObjRepository.
            logger.info("Inconsistencies in structured objects fixed");

            // plan recheck
            this.addToValidate(connTempObjs);
        }

        // Delete remaining temp objects
        //List<ArrChange> changes = structObjRepository.findTempChange();
        structureItemRepository.deleteByStructuredObjectStateTemp();
        structObjRepository.deleteByStateTemp();
        // Changes cannot be easily deleted
        // We cannot be sure how many time is change used
        // changeRepository.delete(changes);
    }

    /**
     * Result of structured object validation
     *
     */
    public static class Result {
        private String value;
        private String sortValue;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getSortValue() {
            return sortValue;
        }

        public void setSortValue(String sortValue) {
            this.sortValue = sortValue;
        }
    }

    public static class ValidationErrorDescription {

        private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

        private Boolean emptyValue;
        private Boolean duplicateValue;
        private List<Integer> requiredItemTypeIds;
        private List<Integer> impossibleItemTypeIds;

        public ValidationErrorDescription() {
            emptyValue = false;
            duplicateValue = false;
            requiredItemTypeIds = new ArrayList<>();
            impossibleItemTypeIds = new ArrayList<>();
        }

        public boolean hasSomeError() {
            if (BooleanUtils.isTrue(emptyValue)) {
                return true;
            }
            if (BooleanUtils.isTrue(duplicateValue)) {
                return true;
            }
            if (CollectionUtils.isNotEmpty(requiredItemTypeIds)) {
                return true;
            }
            if (CollectionUtils.isNotEmpty(impossibleItemTypeIds)) {
                return true;
            }
            return false;
        }

        static public ValidationErrorDescription fromJson(String json) {
            ObjectReader reader = objectMapper.readerFor(ValidationErrorDescription.class);
            try {
                return reader.readValue(json);
            } catch (IOException e) {
                throw new SystemException("Failed to deserialize value").set("json", json);
            }
        }

        public String asJsonString() {
            if (hasSomeError()) {
                try {
                    return objectMapper.writeValueAsString(this);
                } catch (JsonProcessingException e) {
                    throw new SystemException("Nepodařilo se serializovat data");
                }
            }
            return null;
        }

        public Boolean getEmptyValue() {
            return emptyValue;
        }

        public void setEmptyValue(final Boolean emptyValue) {
            this.emptyValue = emptyValue;
        }

        public Boolean getDuplicateValue() {
            return duplicateValue;
        }

        public void setDuplicateValue(final Boolean duplicateValue) {
            this.duplicateValue = duplicateValue;
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
    }
}
