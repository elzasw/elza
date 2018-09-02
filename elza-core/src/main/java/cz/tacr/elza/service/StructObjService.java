package cz.tacr.elza.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.eventbus.Subscribe;

import cz.tacr.elza.EventBusListener;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
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
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.SettingsRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
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
public class StructObjService {

    private static final Logger logger = LoggerFactory.getLogger(StructObjService.class);

    private final StructuredItemRepository structureItemRepository;
    private final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;
    private final StructureDefinitionRepository structureDefinitionRepository;
    private final StructuredObjectRepository structObjRepository;
    private final FundVersionRepository fundVersionRepository;
    private final RuleService ruleService;
    private final ApplicationContext applicationContext;
    private final ChangeRepository changeRepository;
    private final EventNotificationService notificationService;
    private final SettingsRepository settingsRepository;
    private final ResourcePathResolver resourcePathResolver;

    private Queue<Integer> queueObjIds = new ConcurrentLinkedQueue<>();
    private final Object lock = new Object();

    private Map<File, GroovyScriptService.GroovyScriptFile> groovyScriptMap = new HashMap<>();

    @Subscribe
    public synchronized void invalidateCache(final CacheInvalidateEvent cacheInvalidateEvent) {
        if (cacheInvalidateEvent.contains(CacheInvalidateEvent.Type.GROOVY)) {
            groovyScriptMap = new HashMap<>();
        }
    }

    @Autowired
    public StructObjService(final StructuredItemRepository structureItemRepository,
                                final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository,
                                final StructureDefinitionRepository structureDefinitionRepository,
                                final StructuredObjectRepository structureDataRepository,
                                final FundVersionRepository fundVersionRepository,
                                final RuleService ruleService,
                                final ApplicationContext applicationContext,
                                final ChangeRepository changeRepository,
                                final EventNotificationService notificationService,
                                final SettingsRepository settingsRepository,
                                final ResourcePathResolver resourcePathResolver) {
        this.structureItemRepository = structureItemRepository;
        this.structureExtensionDefinitionRepository = structureExtensionDefinitionRepository;
        this.structureDefinitionRepository = structureDefinitionRepository;
        this.structObjRepository = structureDataRepository;
        this.fundVersionRepository = fundVersionRepository;
        this.ruleService = ruleService;
        this.applicationContext = applicationContext;
        this.changeRepository = changeRepository;
        this.notificationService = notificationService;
        this.settingsRepository = settingsRepository;
        this.resourcePathResolver = resourcePathResolver;
    }

    /**
     * Přidání hodnoty k validaci.
     *
     * @param structureData hodnota
     */
    public void addToValidate(final ArrStructuredObject structureData) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                runValidator(Collections.singletonList(structureData.getStructuredObjectId()));
            }
        });
    }

    /**
     * Přidání hodnot k validaci.
     *
     * @param structureDataList seznam hodnot
     */
    public void addToValidate(final List<ArrStructuredObject> structureDataList) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                List<Integer> structureDataIds = structureDataList.stream()
                        .map(ArrStructuredObject::getStructuredObjectId).collect(Collectors.toList());
                runValidator(structureDataIds);
            }
        });
    }

    /**
     * Přidání hodnot k validaci.
     *
     * @param structureDataIds seznam identifikátorů hodnot
     */
    public void addIdsToValidate(final List<Integer> structureDataIds) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                runValidator(structureDataIds);
            }
        });
    }

    /**
     * Přidání položek k validaci, případně založení vlákna pro asynchronní chod.
     *
     * @param structObjIds identifikátory hodnot
     */
    private void runValidator(final List<Integer> structObjIds) {
        if (structObjIds.isEmpty()) {
            return;
        }
        synchronized (lock) {
            boolean createThread = queueObjIds.size() == 0;
            queueObjIds.addAll(structObjIds);
            if (createThread) {
                StructObjService structureDataService = applicationContext.getBean(StructObjService.class);
                structureDataService.run();
            }
        }
    }

    /**
     * Běh validace. NEVOLAT NA PŘÍMO!!!
     */
    @Async
    public void run() {
        boolean hasNext = true;
        while (hasNext) {
            final Integer structureDataId = queueObjIds.poll();
            try {
                applicationContext.getBean(StructObjService.class).generateAndValidate(structureDataId);
            } catch (Exception e) {
                logger.error("Nastala chyba při validaci hodnoty strukturovaného typu -> structureDataId=" + structureDataId, e);
            }
            synchronized (lock) {
                hasNext = !queueObjIds.isEmpty();
            }
        }
    }

    /**
     * Valiadce hodnoty strukt. typu podle id.
     *
     * @param structObjId identifikátor hodnoty strukt. datového typu
     */
    @Transactional
    public void generateAndValidate(final Integer structObjId) {
        ArrStructuredObject structObj = structObjRepository.findOne(structObjId);
        if (structObj == null) {
            throw new ObjectNotFoundException("Nenalezena hodnota strukturovaného typu", BaseCode.ID_NOT_EXIST).setId(structObjId);
        }
        generateAndValidate(structObj);
        // do not send notification for deleted items
        if (structObj.getDeleteChangeId() != null) {
            return;
        }
        sendNotification(structObj);
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
    @Transactional
    public void generateAndValidate(final ArrStructuredObject structObj) {
        // get old value (skip for temp items)
        String oldValue = null;
        if (structObj.getState().equals(ArrStructuredObject.State.OK)
                || structObj.getState().equals(ArrStructuredObject.State.ERROR)) {
            // get last value (if object was ok)
            oldValue = structObj.getValue();
        }
        String newValue = null;
        if (structObj.getDeleteChange() == null) {
            // generate value
            newValue = generateValue(structObj);
        }

        // do not check duplicates for temp objects
        if (structObj.getState() == ArrStructuredObject.State.TEMP) {
            return;
        }

        // Method is called also after ext update etc -> can be called 
        // with same values

        if (StringUtils.isNotEmpty(oldValue)) {
            recheckDuplicates(oldValue, structObj);
        }

        if (StringUtils.isNotEmpty(newValue)) {
            // same values -> do only one check
            if (!newValue.equals(oldValue)) {
                recheckDuplicates(newValue, structObj);
            }
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
    private void recheckDuplicates(String checkedValue, 
                                   ArrStructuredObject srcStructObj) {

        // check duplicates - if not empty
        if (StringUtils.isEmpty(checkedValue)) {
            return;
        }

        // read current structObjs
        List<ArrStructuredObject> validStructureDataList = structObjRepository
                .findValidByStructureTypeAndFund(srcStructObj.getStructuredType(), srcStructObj.getFund(),
                                                 checkedValue);
        int cnt = validStructureDataList.size();
        //validStructureDataList.remove(structObj);
        if (cnt == 1) {
            ArrStructuredObject so = validStructureDataList.get(0);
            // reset duplicated state
            setDuplicatedState(so, false);
            if (!Objects.equals(so.getStructuredObjectId(),
                               srcStructObj.getStructuredObjectId())) {
                sendNotification(so);
            }
        } else if (cnt > 1) {
            // set state and send notifications
            for (ArrStructuredObject so : validStructureDataList) {
                setDuplicatedState(so, true);
                if (!Objects.equals(so.getStructuredObjectId(), srcStructObj.getStructuredObjectId())) {
                    sendNotification(so);
                }
            }
        }
        // TODO: call notifications
    }

    public void sendNotification(ArrStructuredObject structObj) {
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
     * Internal method to generate value and save it.
     * 
     * Method will only check if value is empty.
     * 
     * @param structObj
     * @return Return generated value. Return null if failed
     */
    private String generateValue(ArrStructuredObject structObj) {
        // generate value 
        ArrStructuredObject.State state = ArrStructuredObject.State.OK;
        ValidationErrorDescription validationErrorDescription = new ValidationErrorDescription();

        List<ArrStructuredItem> structureItems = structureItemRepository
                .findByStructuredObjectAndDeleteChangeIsNullFetchData(structObj);

        validateStructureItems(validationErrorDescription, structObj, structureItems);

        String value = generateValue(structObj, structureItems);
        if (StringUtils.isEmpty(value)) {
            state = ArrStructuredObject.State.ERROR;
            validationErrorDescription.setEmptyValue(true);
        }

        // pokud se jedná o tempová data, stav se nenastavuje
        state = structObj.getState() == ArrStructuredObject.State.TEMP ? ArrStructuredObject.State.TEMP : state;

        structObj.setValue(value);
        structObj.setState(state);
        structObj.setErrorDescription(validationErrorDescription.asJsonString());

        structObjRepository.save(structObj);
        return value;
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
        List<RulItemTypeExt> structureItemTypes = ruleService.getStructureItemTypesInternal(structureData.getStructuredType(), fundVersion, structureItems);
        List<RulItemTypeExt> requiredItemTypes = structureItemTypes.stream().filter(itemType -> RulItemType.Type.REQUIRED == itemType.getType()).collect(Collectors.toList());
        List<RulItemTypeExt> impossibleItemTypes = structureItemTypes.stream().filter(itemType -> RulItemType.Type.IMPOSSIBLE == itemType.getType()).collect(Collectors.toList());

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
     * @param structureData hodnota struktovaného datového typu
     * @return hodnota
     */
    private String generateValue(final ArrStructuredObject structureData, final List<ArrStructuredItem> structureItems) {

        RulStructuredType structureType = structureData.getStructuredType();
        File groovyFile = findGroovyFile(structureType, structureData.getFund());

        GroovyScriptService.GroovyScriptFile groovyScriptFile;
        try {
            groovyScriptFile = groovyScriptMap.get(groovyFile);
            if (groovyScriptFile == null) {
                groovyScriptFile = GroovyScriptService.GroovyScriptFile.createFromFile(groovyFile);
                groovyScriptMap.put(groovyFile, groovyScriptFile);
            }
        } catch (IOException e) {
            throw new SystemException("Problém při zpracování groovy scriptu", e);
        }

        Map<String, Object> input = new HashMap<>();
        input.put("ITEMS", structureItems);
        input.put("PACKET_LEADING_ZEROS", getPacketLeadingZeros());

        return (String) groovyScriptFile.evaluate(input);
    }

    /**
     * Získání počtu předřadných nul v názvu obalu.
     *
     * @return počet předřadných nul
     */
    private int getPacketLeadingZeros() {
        List<UISettings> uiSettingsList = settingsRepository.findBySettingsType(UISettings.SettingsType.PACKET_LEADING_ZEROS);
        if (CollectionUtils.isNotEmpty(uiSettingsList)) {
            if (uiSettingsList.size() > 1) {
                logger.warn("Existuje více nastavení PACKET_LEADING_ZEROS, používá se první nalezená!");
            }
            UISettings uiSettings = uiSettingsList.get(0);
            try {
                int result = Integer.parseInt(uiSettings.getValue());
                if (result < 1) {
                    throw new InvalidPropertiesFormatException("Hodnota musí být kladné číslo");
                }
                return result;
            } catch (InvalidPropertiesFormatException | NumberFormatException e) {
                logger.warn("Hodnota nastavení PACKET_LEADING_ZEROS není platné číslo: " + uiSettings.getValue() + ", je použita výchozí hodnota", e);
            }
        }
        return 4; // výchozí hodnota
    }

    /**
     * Vyhledání groovy scriptu podle strukturovaného typu k AS.
     *
     * @param structureType strukturovaný typ
     * @param fund          archivní soubor
     * @return nalezený groovy soubor
     */
    private File findGroovyFile(final RulStructuredType structureType, final ArrFund fund) {
        List<RulStructureExtensionDefinition> structureExtensionDefinitions = structureExtensionDefinitionRepository
                .findByStructureTypeAndDefTypeAndFundOrderByPriority(structureType, RulStructureExtensionDefinition.DefType.SERIALIZED_VALUE, fund);
        RulComponent component;
        RulPackage rulPackage;
        if (structureExtensionDefinitions.size() > 0) {
            RulStructureExtensionDefinition structureExtensionDefinition = structureExtensionDefinitions.get(structureExtensionDefinitions.size() - 1);
            component = structureExtensionDefinition.getComponent();
            rulPackage = structureExtensionDefinition.getRulPackage();
        } else {
            List<RulStructureDefinition> structureDefinitions = structureDefinitionRepository
                    .findByStructuredTypeAndDefTypeOrderByPriority(structureType, RulStructureDefinition.DefType.SERIALIZED_VALUE);
            if (structureDefinitions.size() > 0) {
                RulStructureDefinition structureDefinition = structureDefinitions.get(structureDefinitions.size() - 1);
                component = structureDefinition.getComponent();
                rulPackage = structureDefinition.getRulPackage();
            } else {
                throw new SystemException("Strukturovaný typ '" + structureType.getCode() + "' nemá žádný script pro výpočet hodnoty", BaseCode.INVALID_STATE);
            }
        }

        return resourcePathResolver.getGroovyDir(rulPackage, structureType.getRuleSet())
                .resolve(component.getFilename())
                .toFile();
    }

    /**
     * Provede smazání dočasných hodnot strukt. typu.
     */
    public void removeTempStructureData() {
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
        }

        // Delete remaining temp objects
        List<ArrChange> changes = structObjRepository.findTempChange();
        structureItemRepository.deleteByStructuredObjectStateTemp();
        structObjRepository.deleteByStateTemp();
        // Changes cannot be easily deleted
        // We cannot be sure how many time is change used
        // changeRepository.delete(changes);

        // Update state for changed objects
        if (CollectionUtils.isNotEmpty(connTempObjs)) {
            for (ArrStructuredObject so : connTempObjs) {
                generateAndValidate(so);
            }
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

        static public ValidationErrorDescription fromJson(String json) {
            ObjectReader reader = objectMapper.readerFor(ValidationErrorDescription.class);
            try {
                return reader.readValue(json);
            } catch (IOException e) {
                throw new SystemException("Failed to deserialize value").set("json", json);
            }
        }

        public String asJsonString() {
            if (BooleanUtils.isTrue(emptyValue)
                    || BooleanUtils.isTrue(duplicateValue)
                    || CollectionUtils.isNotEmpty(requiredItemTypeIds)
                    || CollectionUtils.isNotEmpty(impossibleItemTypeIds)) {
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
