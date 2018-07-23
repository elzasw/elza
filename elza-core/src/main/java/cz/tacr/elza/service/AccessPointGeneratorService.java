package cz.tacr.elza.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.eventbus.Subscribe;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApFragmentItemRepository;
import cz.tacr.elza.repository.ApFragmentRuleRepository;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviska pro generování.
 */
@Service
public class AccessPointGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(AccessPointGeneratorService.class);
    public static final String ITEMS = "ITEMS";

    private final ApFragmentRuleRepository fragmentRuleRepository;
    private final ResourcePathResolver resourcePathResolver;
    private final ApFragmentItemRepository fragmentItemRepository;
    private final RuleService ruleService;

    private Map<File, GroovyScriptService.GroovyScriptFile> groovyScriptMap = new HashMap<>();

    @Autowired
    public AccessPointGeneratorService(final ApFragmentRuleRepository fragmentRuleRepository,
                                       final ResourcePathResolver resourcePathResolver,
                                       final ApFragmentItemRepository fragmentItemRepository,
                                       final RuleService ruleService) {
        this.fragmentRuleRepository = fragmentRuleRepository;
        this.resourcePathResolver = resourcePathResolver;
        this.fragmentItemRepository = fragmentItemRepository;
        this.ruleService = ruleService;
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
            state = ApState.ERROR;
        }

        if (StringUtils.isEmpty(value)) {
            fragmentErrorDescription.setEmptyValue(true);
            state = ApState.ERROR;
        }
        fragment.setValue(value);
        fragment.setErrorDescription(fragmentErrorDescription.asJsonString());
        fragment.setState(stateOld == ApState.TEMP ? ApState.TEMP : state);
    }

    private String generateValue(final ApFragment fragment, final List<ApItem> items) {

        File groovyFile = findGroovyFile(fragment);

        GroovyScriptService.GroovyScriptFile groovyScriptFile = groovyScriptMap.get(groovyFile);
        if (groovyScriptFile == null) {
            groovyScriptFile = new GroovyScriptService.GroovyScriptFile(groovyFile);
            groovyScriptMap.put(groovyFile, groovyScriptFile);
        }

        Map<String, Object> input = new HashMap<>();
        input.put(ITEMS, ModelFactory.createFragmentItems(items));

        return (String) groovyScriptFile.evaluate(input);
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

    private void validateFragmentItems(final FragmentErrorDescription fragmentErrorDescription,
                                       final ApFragment fragment,
                                       final List<ApItem> items) {
        List<RulItemTypeExt> fragmentItemTypes = ruleService.getFragmentItemTypesInternal(fragment.getFragmentType(), items);
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
                fragmentErrorDescription.getRequiredItemTypeIds().add(requiredItemType.getItemTypeId());
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
                fragmentErrorDescription.getImpossibleItemTypeIds().add(impossibleItemType.getItemTypeId());
            }
        }
    }

    private static class AbstractErrorDescription {

    }

    public static class FragmentErrorDescription extends AbstractErrorDescription {

        private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

        private Boolean emptyValue;
        private Boolean scriptFail;
        private List<Integer> requiredItemTypeIds;
        private List<Integer> impossibleItemTypeIds;

        public FragmentErrorDescription() {
            emptyValue = false;
            scriptFail = false;
            requiredItemTypeIds = new ArrayList<>();
            impossibleItemTypeIds = new ArrayList<>();
        }

        static public FragmentErrorDescription fromJson(String json) {
            ObjectReader reader = objectMapper.readerFor(FragmentErrorDescription.class);
            try {
                return reader.readValue(json);
            } catch (IOException e) {
                throw new SystemException("Failed to deserialize value").set("json", json);
            }
        }

        public String asJsonString() {
            if (BooleanUtils.isTrue(emptyValue)
                    || BooleanUtils.isTrue(scriptFail)
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
}
