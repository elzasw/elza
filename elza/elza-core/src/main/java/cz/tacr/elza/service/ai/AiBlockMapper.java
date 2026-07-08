package cz.tacr.elza.service.ai;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import cz.tacr.elza.controller.vo.AiDisplayBlockVO;

/**
 * Maps one typed result block of a provider task output to render-ready display
 * blocks — the server half of the rendering contract (see the AiDisplayBlock
 * model in elza-development/typespec/main.tsp). The provider returns a flat list
 * of typed objects (AiObject: {@code objectType} + {@code data}); each object is
 * turned into display block(s) here: structured data becomes typed blocks
 * (table, …), free text/markdown becomes markdown (CommonMark + GFM, never raw
 * HTML). One mapper per result {@code ObjectType}; registration is automatic via
 * {@link AiBlockMapperRegistry}. An object type with no mapper falls back to a
 * markdown block containing its JSON (the registry guarantees nothing renders
 * blank), so unimplemented result types degrade gracefully.
 */
public interface AiBlockMapper {

    /** Result object types this mapper renders, e.g. {@code elza.markdown}. */
    Set<String> objectTypes();

    /**
     * @param data the {@code data} payload of one result block (the object's
     *            type-specific content)
     * @return ordered display blocks for that object
     */
    List<AiDisplayBlockVO> map(JsonNode data);
}
