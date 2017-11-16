package cz.tacr.elza.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import cz.tacr.elza.EventBusListener;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.ArrStructureItem;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructureType;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.StructureDataRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructureItemRepository;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventStructureDataChange;
import org.apache.commons.collections4.CollectionUtils;
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

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Servisní třída pro aktualizaci hodnot strukturovaných dat.
 *
 * @since 13.11.2017
 */
@Service
@EventBusListener
public class StructureDataService {

    private static final Logger logger = LoggerFactory.getLogger(StructureDataService.class);

    private final StructureItemRepository structureItemRepository;
    private final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;
    private final StructureDefinitionRepository structureDefinitionRepository;
    private final StructureDataRepository structureDataRepository;
    private final RulesExecutor rulesExecutor;
    private final ArrangementService arrangementService;
    private final RuleService ruleService;
    private final ApplicationContext applicationContext;
    private final ChangeRepository changeRepository;
    private final EventNotificationService notificationService;

    private Queue<Integer> queueStructureDataIds = new ConcurrentLinkedQueue<>();
    private final Object lock = new Object();

    private Map<File, GroovyScriptService.GroovyScriptFile> groovyScriptMap = new HashMap<>();

    @Subscribe
    public synchronized void invalidateCache(final CacheInvalidateEvent cacheInvalidateEvent) {
        if (cacheInvalidateEvent.contains(CacheInvalidateEvent.Type.GROOVY)) {
            groovyScriptMap = new HashMap<>();
        }
    }

    @Autowired
    public StructureDataService(final StructureItemRepository structureItemRepository,
                                final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository,
                                final StructureDefinitionRepository structureDefinitionRepository,
                                final StructureDataRepository structureDataRepository,
                                final RulesExecutor rulesExecutor,
                                final ArrangementService arrangementService,
                                final RuleService ruleService,
                                final ApplicationContext applicationContext,
                                final ChangeRepository changeRepository,
                                final EventNotificationService notificationService) {
        this.structureItemRepository = structureItemRepository;
        this.structureExtensionDefinitionRepository = structureExtensionDefinitionRepository;
        this.structureDefinitionRepository = structureDefinitionRepository;
        this.structureDataRepository = structureDataRepository;
        this.rulesExecutor = rulesExecutor;
        this.arrangementService = arrangementService;
        this.ruleService = ruleService;
        this.applicationContext = applicationContext;
        this.changeRepository = changeRepository;
        this.notificationService = notificationService;
    }

    /**
     * Přidání hodnoty k validaci.
     *
     * @param structureData hodnota
     */
    public void addToValidate(final ArrStructureData structureData) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                runValidator(Collections.singletonList(structureData.getStructureDataId()));
            }
        });
    }

    /**
     * Přidání hodnot k validaci.
     *
     * @param structureDataList seznam hodnot
     */
    public void addToValidate(final List<ArrStructureData> structureDataList) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                List<Integer> structureDataIds = structureDataList.stream()
                        .map(ArrStructureData::getStructureDataId).collect(Collectors.toList());
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
     * @param structureDataIds identifikátory hodnot
     */
    private void runValidator(final List<Integer> structureDataIds) {
        if (structureDataIds.isEmpty()) {
            return;
        }
        synchronized (lock) {
            boolean createThread = queueStructureDataIds.size() == 0;
            queueStructureDataIds.addAll(structureDataIds);
            if (createThread) {
                StructureDataService structureDataService = applicationContext.getBean(StructureDataService.class);
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
            final Integer structureDataId = queueStructureDataIds.poll();
            try {
                applicationContext.getBean(StructureDataService.class).validate(structureDataId);
            } catch (Exception e) {
                logger.error("Nastala chyba při validaci hodnoty strukturovaného typu -> structureDataId=" + structureDataId, e);
            }
            synchronized (lock) {
                hasNext = !queueStructureDataIds.isEmpty();
            }
        }
    }

    /**
     * Valiadce hodnoty strukt. typu podle id.
     *
     * @param structureDataId identifikátor hodnoty strukt. datového typu
     */
    @Transactional
    public void validate(final Integer structureDataId) {
        ArrStructureData structureData = structureDataRepository.findOne(structureDataId);
        if (structureData == null) {
            throw new ObjectNotFoundException("Nenalezena hodnota strukturovaného typu", BaseCode.ID_NOT_EXIST).setId(structureDataId);
        }
        validate(structureData);
        if (structureData.getState() == ArrStructureData.State.TEMP) {
            notificationService.publishEvent(new EventStructureDataChange(structureData.getFundId(),
                    structureData.getStructureType().getCode(),
                    Collections.singletonList(structureDataId),
                    null,
                    null,
                    null));
        } else {
            notificationService.publishEvent(new EventStructureDataChange(structureData.getFundId(),
                    structureData.getStructureType().getCode(),
                    null,
                    null,
                    Collections.singletonList(structureDataId),
                    null));
        }
    }

    /**
     * Validace hodnoty strukturovaného datového typu.
     *
     * @param structureData hodnota struktovaného datového typu
     * @return zvalidovaná hodnota
     */
    @Transactional
    public ArrStructureData validate(final ArrStructureData structureData) {
        if (structureData.getDeleteChange() != null) {
            throw new BusinessException("Nelze validovat smazanou hodnotu", BaseCode.INVALID_STATE);
        }
        List<ArrStructureData> validStructureDataList = structureDataRepository.findValidByStructureTypeAndFund(structureData.getStructureType(), structureData.getFund());
        validStructureDataList.remove(structureData);

        List<String> values = validStructureDataList.stream()
                .map(ArrStructureData::getValue)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        ArrStructureData.State state = ArrStructureData.State.OK;
        ValidationErrorDescription validationErrorDescription = new ValidationErrorDescription();

        List<ArrStructureItem> structureItems = structureItemRepository.findByStructureDataAndDeleteChangeIsNull(structureData);

        validateStructureItems(validationErrorDescription, structureData, structureItems);

        String value = generateValue(structureData, structureItems);
        if (StringUtils.isEmpty(value)) {
            state = ArrStructureData.State.ERROR;
            validationErrorDescription.setEmptyValue(true);
        }

        if (values.contains(value.toLowerCase())) {
            state = ArrStructureData.State.ERROR;
            validationErrorDescription.setDuplicateValue(true);
        }

        // pokud se jedná o tempová data, stav se nenastavuje
        state = structureData.getState() == ArrStructureData.State.TEMP ? ArrStructureData.State.TEMP : state;

        structureData.setValue(value);
        structureData.setState(state);
        structureData.setErrorDescription(validationErrorDescription.asJsonString());

        return structureDataRepository.save(structureData);
    }

    /**
     * Validace položek pro strukturovaný datový typ.
     *
     * @param validationErrorDescription objekt pro výsledky validace
     * @param structureData              hodnota struktovaného datového typu
     * @param structureItems             validované položky
     */
    private void validateStructureItems(final ValidationErrorDescription validationErrorDescription,
                                        final ArrStructureData structureData,
                                        final List<ArrStructureItem> structureItems) {
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(structureData.getFundId());
        List<RulItemTypeExt> structureItemTypes = ruleService.getStructureItemTypes(structureData.getStructureType(), fundVersion);
        List<RulItemTypeExt> requiredItemTypes = structureItemTypes.stream().filter(itemType -> RulItemType.Type.REQUIRED == itemType.getType()).collect(Collectors.toList());
        List<RulItemTypeExt> impossibleItemTypes = structureItemTypes.stream().filter(itemType -> RulItemType.Type.IMPOSSIBLE == itemType.getType()).collect(Collectors.toList());

        for (RulItemTypeExt requiredItemType : requiredItemTypes) {
            boolean add = true;
            for (ArrStructureItem structureItem : structureItems) {
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
            for (ArrStructureItem structureItem : structureItems) {
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
    private String generateValue(final ArrStructureData structureData, final List<ArrStructureItem> structureItems) {

        RulStructureType structureType = structureData.getStructureType();
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
        input.put("PACKET_LEADING_ZEROS", 4);

        return (String) groovyScriptFile.evaluate(input);
    }

    /**
     * Vyhledání groovy scriptu podle strukturovaného typu k AS.
     *
     * @param structureType strukturovaný typ
     * @param fund          archivní soubor
     * @return nalezený groovy soubor
     */
    private File findGroovyFile(final RulStructureType structureType, final ArrFund fund) {
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
                    .findByStructureTypeAndDefTypeOrderByPriority(structureType, RulStructureDefinition.DefType.SERIALIZED_VALUE);
            if (structureDefinitions.size() > 0) {
                RulStructureDefinition structureDefinition = structureDefinitions.get(structureDefinitions.size() - 1);
                component = structureDefinition.getComponent();
                rulPackage = structureDefinition.getRulPackage();
            } else {
                throw new SystemException("Strukturovaný typ '" + structureType.getCode() + "' nemá žádný script pro výpočet hodnoty", BaseCode.INVALID_STATE);
            }
        }
        return new File(rulesExecutor.getGroovyDir(rulPackage.getCode(), structureType.getRuleSet().getCode())
                + File.separator + component.getFilename());
    }

    /**
     * Provede smazání dočasných hodnot strukt. typu.
     */
    public void removeTempStructureData() {
        List<ArrChange> changes = structureDataRepository.findTempChange();
        structureItemRepository.deleteByStructureDataStateTemp();
        structureDataRepository.deleteByStateTemp();
        changeRepository.delete(changes);
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
