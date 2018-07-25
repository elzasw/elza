package cz.tacr.elza.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.eventbus.Subscribe;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import cz.tacr.elza.service.vo.AccessPoint;
import cz.tacr.elza.service.vo.Name;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serviska pro generování.
 */
@Service
public class AccessPointGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(AccessPointGeneratorService.class);

    public static final String ITEMS = "ITEMS";
    public static final String AP = "AP";

    private final ApFragmentRuleRepository fragmentRuleRepository;
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

    private Map<File, GroovyScriptService.GroovyScriptFile> groovyScriptMap = new HashMap<>();

    @Autowired
    public AccessPointGeneratorService(final ApFragmentRuleRepository fragmentRuleRepository,
                                       final ApRuleRepository ruleRepository,
                                       final ResourcePathResolver resourcePathResolver,
                                       final ApFragmentItemRepository fragmentItemRepository,
                                       final ApNameItemRepository nameItemRepository,
                                       final ApBodyItemRepository bodyItemRepository,
                                       final ApNameRepository apNameRepository,
                                       final RuleService ruleService,
                                       final ApFragmentRepository fragmentRepository,
                                       final AccessPointDataService apDataService,
                                       final ApAccessPointRepository apRepository,
                                       final AccessPointItemService apItemService) {
        this.fragmentRuleRepository = fragmentRuleRepository;
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
    }

    @Subscribe
    public synchronized void invalidateCache(final CacheInvalidateEvent cacheInvalidateEvent) {
        if (cacheInvalidateEvent.contains(CacheInvalidateEvent.Type.GROOVY)) {
            groovyScriptMap = new HashMap<>();
        }
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

        List<ApItem> apItems = new ArrayList<>(bodyItemRepository.findValidItemsByAccessPoint(accessPoint));
        List<ApName> apNames = apNameRepository.findByAccessPoint(accessPoint);
        Map<Integer, ApName> apNameMap = apNames.stream().collect(Collectors.toMap(ApName::getNameId, Function.identity()));
        List<ApNameItem> nameItems = nameItemRepository.findValidItemsByNames(apNames);

        Map<Integer, List<ApItem>> nameItemsMap = new HashMap<>();
        for (ApNameItem nameItem : nameItems) {
            Integer nameId = nameItem.getNameId();
            List<ApItem> items = nameItemsMap.computeIfAbsent(nameId, k -> new ArrayList<>());
            items.add(nameItem);
        }

        ApErrorDescription apErrorDescription = new ApErrorDescription();
        ApState apStateOld = accessPoint.getState();
        ApState apState = ApState.OK;

        validateApItems(apErrorDescription, accessPoint, apItems);

        AccessPoint result = null;
        try {
            result = generateValue(accessPoint, apItems, apNames, nameItemsMap);
        } catch (Exception e) {
            logger.error("Selhání groovy scriptu (accessPointId: {})", accessPoint.getAccessPointId(), e);
            apErrorDescription.setScriptFail(true);
            apState = ApState.ERROR;
        }

        if (result != null) {
            apDataService.changeDescription(accessPoint, result.getDescription(), change);

            List<NameContext> nameContexts = new ArrayList<>();

            for (Name name : result.getNames()) {
                ApName apName = apNameMap.get(name.getId());
                List<ApItem> items = nameItemsMap.get(apName.getNameId());

                if (!apDataService.equalsNames(apName, name.getName(), name.getComplement(), name.getFullName(), apName.getLanguageId())) { // TODO: jak s jazykem?
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

            for (NameContext nameContext : nameContexts) {
                ApName name = nameContext.getName();
                NameErrorDescription errorDescription = nameContext.getErrorDescription();
                boolean isUnique = apDataService.isNameUnique(accessPoint.getScope(), name.getFullName());
                if (!isUnique) {
                    errorDescription.setDuplicateValue(true);
                    nameContext.setState(ApState.ERROR);
                }

                name.setErrorDescription(errorDescription.asJsonString());
                name.setState(nameContext.getStateOld() == ApState.TEMP ? ApState.TEMP : nameContext.getState());
                apNameRepository.save(name);
            }
        }

        if (CollectionUtils.isNotEmpty(apErrorDescription.getImpossibleItemTypeIds())
                || CollectionUtils.isNotEmpty(apErrorDescription.getRequiredItemTypeIds())) {
            apState = ApState.ERROR;
        }

        accessPoint.setErrorDescription(apErrorDescription.asJsonString());
        accessPoint.setState(apStateOld == ApState.TEMP ? ApState.TEMP : apState);
        apRepository.save(accessPoint);
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
        ApFragmentType fragmentType = fragment.getFragmentType();
        ApFragmentRule rule = fragmentRuleRepository.findByFragmentTypeAndRuleType(fragmentType, ApFragmentRule.RuleType.TEXT_GENERATOR);
        if (rule == null) {
            throw new SystemException("Nebyly nalezeny pravidla generování pro typ fragmentu", BaseCode.SYSTEM_ERROR);
        }
        RulComponent component = rule.getComponent();
        return resourcePathResolver.getGroovyDir(fragmentType.getRulPackage())
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

}
