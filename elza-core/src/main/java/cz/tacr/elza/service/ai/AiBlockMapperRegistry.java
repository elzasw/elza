package cz.tacr.elza.service.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.vo.AiDisplayBlockVO;
import cz.tacr.elza.controller.vo.AiMarkdownBlockVO;

/**
 * Turns a provider task output — a flat JSON array of typed result blocks
 * (AiObject: {@code objectType} + {@code data}) — into render-ready display
 * blocks, routing each block to its {@link AiBlockMapper} by object type.
 * Implements the contract's server-side fallback rule: a block whose object type
 * has no mapper (an unimplemented result type) renders as one markdown block
 * containing its JSON in a fenced code block — nothing ever renders blank, and
 * new result types degrade gracefully until their mapper is added.
 */
@Component
public class AiBlockMapperRegistry {

    private static final Logger logger = LoggerFactory.getLogger(AiBlockMapperRegistry.class);

    private final Map<String, AiBlockMapper> byObjectType = new HashMap<>();

    private final ObjectMapper objectMapper;

    public AiBlockMapperRegistry(List<AiBlockMapper> mappers, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        for (AiBlockMapper mapper : mappers) {
            for (String objectType : mapper.objectTypes()) {
                byObjectType.put(objectType, mapper);
            }
        }
    }

    /**
     * Maps the stored output (a JSON array of typed blocks) to display blocks.
     *
     * @param aiRequestId id of the request the output belongs to; passed to the
     *            mappers as {@link AiBlockContext} together with each block's
     *            index within the output array
     */
    public List<AiDisplayBlockVO> map(String outputJson, Integer aiRequestId) {
        JsonNode root;
        try {
            root = objectMapper.readTree(outputJson);
        } catch (Exception e) {
            return List.of(fenced(outputJson));
        }
        List<AiDisplayBlockVO> blocks = new ArrayList<>();
        if (root.isArray()) {
            int blockIndex = 0;
            for (JsonNode block : root) {
                if (block.isNull()) {
                    // An object type this build does not implement: the provider
                    // client deserializes it to null (ApiClientAiProvider), so the
                    // rest of the output still renders. Logged, never silent.
                    logger.warn("AI request {}: output block {} has an object type unknown to this build "
                            + "— skipped (update Elza to render it)", aiRequestId, blockIndex);
                    blockIndex++;
                    continue;
                }
                String objectType = block.path("objectType").asText(null);
                AiBlockMapper mapper = objectType == null ? null : byObjectType.get(objectType);
                if (mapper != null) {
                    blocks.addAll(mapper.map(block.path("data"), new AiBlockContext(aiRequestId, blockIndex)));
                } else {
                    blocks.add(fenced(block));
                }
                blockIndex++;
            }
        } else {
            // Not the expected block array (e.g. a legacy or malformed output).
            blocks.add(fenced(root));
        }
        return blocks;
    }

    /** Fallback rendering: the value as pretty JSON in a fenced markdown code block. */
    private AiDisplayBlockVO fenced(Object value) {
        String pretty;
        try {
            JsonNode node = value instanceof JsonNode jn ? jn : objectMapper.readTree(String.valueOf(value));
            pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) {
            pretty = String.valueOf(value);
        }
        return new AiMarkdownBlockVO().content("```json\n" + pretty + "\n```");
    }
}
