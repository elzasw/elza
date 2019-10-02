package cz.tacr.elza.bulkaction.generator.unitid;

import java.util.ArrayDeque;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

@Component
@Scope("prototype")
public class UnitIdGenerator {

    final ArrLevel rootLevel;

    UnitIdGeneratorParams params;

    final ArrayDeque<LevelGenerator> levelQueue = new ArrayDeque<>();

    int changeCounter = 0;

    public UnitIdGenerator(ArrLevel level, UnitIdGeneratorParams params) {
        this.rootLevel = level;
        this.params = params;
    }

    public void run() {

        // Prepare tree with sealed ids
        prepare();

        // iterate levels
        processLevels();

    }

    /**
     * Prepare generator
     * 
     * Method is called only once.
     */
    private void prepare() {
        // get unitId of root
        boolean isRoot = (rootLevel.getNodeIdParent() == null);

        RulItemSpec levelSpec = LevelGenerator.getLevelSpec(rootLevel, params);

        ArrDescItem descItem = params.getBulkAction().loadSingleDescItem(rootLevel.getNode(), params.getItemType());
        String rootUnitId;

        if (isRoot) {
            // root has to be without unit id
            if(descItem!=null) {
                throw new SystemException("Root node has unit id", BaseCode.INVALID_STATE);
            }

            rootUnitId = "";

        } else {
            // non-root node has to have exactly one unitid
            if (descItem == null) {
                throw new SystemException("Starting node without unitId", BaseCode.INVALID_STATE)
                        .set("nodeId", rootLevel.getNodeId());
            }
            ArrData data = descItem.getData();
            ArrDataUnitid unitId = HibernateUtils.unproxy(data);
            rootUnitId = unitId.getUnitId();
        }

        Validate.notNull(rootUnitId);

        LevelGenerator lg = new LevelGenerator(rootLevel, levelSpec, params, rootUnitId);
        levelQueue.addLast(lg);
    }

    private void processLevels() {

        while (!levelQueue.isEmpty()) {
            LevelGenerator lg = levelQueue.removeFirst();

            // Run generator for single level 
            List<LevelGenerator> nextGenerators = lg.run();

            if (nextGenerators != null && nextGenerators.size() > 0) {
                levelQueue.addAll(nextGenerators);
            }
        }

    }

    public int getCountChanges() {
        return changeCounter;
    }

}
