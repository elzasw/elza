package cz.tacr.elza.service.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.vo.AiDisplayBlockVO;

/**
 * Routes task outputs to their {@link AiBlockMapper}. Implements the contract's
 * server-side fallback rule: an output with no mapper renders as one markdown
 * block containing the output JSON in a fenced code block — nothing ever
 * renders as blank.
 */
@Component
public class AiBlockMapperRegistry {

    private final Map<String, AiBlockMapper> byTaskType = new HashMap<>();

    private final ObjectMapper objectMapper;

    public AiBlockMapperRegistry(List<AiBlockMapper> mappers, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        for (AiBlockMapper mapper : mappers) {
            for (String taskType : mapper.taskTypes()) {
                byTaskType.put(taskType, mapper);
            }
        }
    }

    public List<AiDisplayBlockVO> map(String taskType, String outputJson) {
        AiBlockMapper mapper = byTaskType.get(taskType);
        if (mapper != null) {
            return mapper.map(outputJson);
        }
        return fallback(outputJson);
    }

    private List<AiDisplayBlockVO> fallback(String outputJson) {
        String pretty;
        try {
            pretty = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(outputJson));
        } catch (Exception e) {
            pretty = outputJson;
        }
        AiDisplayBlockVO block = new AiDisplayBlockVO();
        block.setType("markdown");
        block.setContent("```json\n" + pretty + "\n```");
        return List.of(block);
    }
}
