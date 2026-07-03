package cz.tacr.elza.service.ai;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.controller.vo.AiDisplayBlockVO;

/**
 * Maps a provider task output (schema-validated JSON) to render-ready display
 * blocks — the server half of the rendering contract (see the AiDisplayBlock
 * model in elza-development/typespec/main.tsp): structured data becomes typed
 * blocks (table, …), free text becomes markdown (CommonMark + GFM, never raw
 * HTML). One mapper per task-type family; registration is automatic via
 * {@link AiBlockMapperRegistry}.
 */
public interface AiBlockMapper {

    /** Task types this mapper renders. */
    Set<String> taskTypes();

    /**
     * @param outputJson the task output as stored (JSON)
     * @return ordered display blocks
     */
    List<AiDisplayBlockVO> map(String outputJson);
}
