package cz.tacr.elza.bulkaction.generator.unitid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.bulkaction.generator.unitid.SealedUnitId.LevelType;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

public class LevelGenerator {
    
    /**
     * Child node of current level
     */
    static class Node {

        final ArrLevel level;

        String oldValue;

        boolean unitIdUsable = false;

        PartSealedUnitId sealedUnitId;

        final RulItemSpec levelSpec;

        /**
         * Flag is node requires extra slash
         */
        boolean extraSlashBefore = false;

        private ArrDescItem oldItemUnitId;

        public Node(ArrLevel l, RulItemSpec levelSpec) {
            this.level = l;
            this.levelSpec = levelSpec;
        }

        public void setSealedUnitId(PartSealedUnitId sealedUnitId) {
            this.sealedUnitId = sealedUnitId;
        }

        public PartSealedUnitId getSealedUnitId() {
            return sealedUnitId;
        }

        public void setUnitIdUsable(boolean b) {
            this.unitIdUsable = b;
        }

        public boolean isUnitIdUsable() {
            return unitIdUsable;
        }

        public void setExtraSlashBefore(boolean b) {
            this.extraSlashBefore = b;
        }

        public boolean isExtraSlashBefore() {
            return extraSlashBefore;
        }

        public ArrLevel getLevel() {
            return level;
        }

        public void setOldValue(ArrDescItem itemUnidId, String oldValue) {
            this.oldItemUnitId = itemUnidId;
            this.oldValue = oldValue;
        }

        public String getOldValue() {
            return oldValue;
        }

        public ArrDescItem getOldItemUnitId() {
            return oldItemUnitId;
        }

        public RulItemSpec getLevelSpec() {
            return levelSpec;
        }

    };

    final ArrLevel parentLevel;

    /**
     * Array of child nodes
     */
    ArrayList<Node> nodes;

    final UnitIdGeneratorParams params;

    final RulItemSpec itemSpec;

    final String prefix;

    public LevelGenerator(ArrLevel level, RulItemSpec itemSpec, UnitIdGeneratorParams params, String prefix) {
        this.parentLevel = level;
        this.itemSpec = itemSpec;
        this.params = params;
        this.prefix = prefix;
    }

    static public RulItemSpec getLevelSpec(ArrLevel level, UnitIdGeneratorParams params) {
        // read level type
        ArrDescItem levelType = params.getBulkAction().loadSingleDescItem(level.getNode(), params.getLevelItemType());
        if (levelType == null) {
            throw new SystemException("Level without level type", BaseCode.INVALID_STATE)
                    .set("nodeId", level.getNodeId());
        }

        Integer specId = levelType.getItemSpecId();
        StaticDataProvider sdp = params.getBulkAction().getStaticDataProvider();
        RulItemSpec spec = sdp.getItemSpecById(specId);
        if (spec == null) {
            throw new SystemException("Level without specification", BaseCode.INVALID_STATE)
                    .set("levelId", level.getLevelId());
        }
        return spec;
    }

    public List<LevelGenerator> run() {

        // read child levels
        readChildLevels();

        if (nodes == null || nodes.size() == 0) {
            return Collections.emptyList();
        }

        // find which ids will be fixed
        TreeSet<UnitIdPart> fixedUnitIds = findFixedUnitIds(nodes);

        // find subtree for this level
        PartSealedUnitId parentSealedUnitId = params.getSealedTree().find(prefix);
        Validate.notNull(parentSealedUnitId);

        // generators for child nodes
        return generate(fixedUnitIds, parentSealedUnitId);
    }

    private List<LevelGenerator> generate(TreeSet<UnitIdPart> fixedUnitIds, PartSealedUnitId parentSealedUnitId) {

        // final collection of subsequent generators
        List<LevelGenerator> generators = new ArrayList<>(nodes.size());

        UnitIdPart loBorder = null;
        UnitIdPart hiBorder = null;
        Iterator<UnitIdPart> it = fixedUnitIds.iterator();
        if (it.hasNext()) {
            hiBorder = it.next();
        }

        for (Node n : nodes) {

            String unitIdValue;

            // Check if node is fixed
            PartSealedUnitId sealedUnitId = n.getSealedUnitId();
            if (sealedUnitId != null && fixedUnitIds.contains(sealedUnitId.getPart())) {
                // this is fixed value

                unitIdValue = n.getOldValue();

                // move borders
                loBorder = hiBorder;
                if (it.hasNext()) {
                    hiBorder = it.next();
                } else {
                    hiBorder = null;
                }
            } else {
                // item without fixed value
                unitIdValue = generateNode(n, loBorder, hiBorder, parentSealedUnitId);
            }

            // prepare subgenerators
            LevelGenerator lg = new LevelGenerator(n.getLevel(), n.getLevelSpec(), params, unitIdValue);
            generators.add(lg);

        }
        return generators;
    }

    /**
     * Generate UnitId for given node
     * 
     * 
     * @param n
     * @param loBorder
     * @param hiBorder
     * @param parentSealedUnitId
     * @return Return generated unitId
     */
    private String generateNode(Node n, UnitIdPart loBorder, UnitIdPart hiBorder, PartSealedUnitId parentSealedUnitId) {

        LevelType levelType = n.isExtraSlashBefore() ? LevelType.SLASHED : LevelType.DEFAULT;
        SealedLevel parentSealedLevel = parentSealedUnitId.getLevel(levelType);

        // Try to find greater value
        PartSealedUnitId sealedUnitId = parentSealedLevel.createSealed(loBorder, hiBorder);

        // prepare text value
        String value = sealedUnitId.getValue();

        // check if old value is same as new value
        String oldValue = n.getOldValue();
        if (value.equals(oldValue)) {
            return oldValue;
        }

        // Check if node has old unitId
        //    Check if node is frozen -> move to old data
        //    Reset old unitId
        if (oldValue != null) {
            // check if value is frozen
            PartSealedUnitId oldSealedUnitId = params.getSealedTree().find(oldValue);
            if (oldSealedUnitId != null) {
                if (oldSealedUnitId.isSealed()) {
                    // move to old values
                    ArrDataString dataPrev = new ArrDataString();
                    dataPrev.setValue(oldValue);

                    ArrDescItem descItemPrev = new ArrDescItem();
                    descItemPrev.setItemType(params.getPreviousItemType());
                    descItemPrev.setItemSpec(params.getPreviousItemSpec());
                    descItemPrev.setNode(n.getLevel().getNode());
                    descItemPrev.setData(dataPrev);

                    params.getBulkAction().saveDescItem(descItemPrev);

                }
            }
            // drop old item - mark as delete
            ArrDescItem oldDescItem = n.getOldItemUnitId();
            params.getBulkAction().deleteDescItem(oldDescItem);
        }
        // Store new value
        ArrDataUnitid dataUnitId = new ArrDataUnitid();
        dataUnitId.setValue(value);

        ArrDescItem descItemUnitId = new ArrDescItem();
        descItemUnitId.setItemType(params.getItemType());
        descItemUnitId.setNode(n.getLevel().getNode());
        descItemUnitId.setData(dataUnitId);

        params.getBulkAction().saveDescItem(descItemUnitId);

        return value;
    }

    static public TreeSet<UnitIdPart> findFixedUnitIds(List<Node> nodes) {
        // prepare fixed nodes
        ArrayList<UnitIdPart> parts = new ArrayList<>(nodes.size());
        for (Node n : nodes) {
            PartSealedUnitId sealedUnitId = n.getSealedUnitId();
            if (sealedUnitId != null && sealedUnitId.isSealed() && n.isUnitIdUsable()) {
                parts.add(sealedUnitId.getPart());
            }
        }

        return findLongestUnitIdSequence(parts);
    }

    // Binary search
    static int getCeilIndex(ArrayList<UnitIdPart> parts,
                            int tailIndices[], int l,
                            int r, UnitIdPart part) {

        while (r - l > 1) {

            int m = l + (r - l) / 2;

            UnitIdPart lPart = parts.get(tailIndices[m]);
            if (lPart.compareTo(part) >= 0)
                r = m;
            else
                l = m;
        }

        return r;
    }

    static public TreeSet<UnitIdPart> findLongestUnitIdSequence(ArrayList<UnitIdPart> parts) {

        // check empty input
        if (parts.size() == 0) {
            return new TreeSet<>();
        }

        // find the longest growing sequence
        int tailIndices[] = new int[parts.size()];
        // Initialized with 0
        Arrays.fill(tailIndices, 0);
        int prevIndices[] = new int[parts.size()];
        Arrays.fill(prevIndices, -1);

        // it will always point to empty 
        // location
        int len = 1;

        for (int i = 1; i < parts.size(); i++) {
            
            UnitIdPart part = parts.get(i);
            UnitIdPart lNodeMin = parts.get(tailIndices[0]);
            UnitIdPart lNodeMax = parts.get(tailIndices[len - 1]);
            
            // check if smallest
            if (part.compareTo(lNodeMin) < 0) {
                // new smallest value
                tailIndices[0] = i;
            } else if (part.compareTo(lNodeMax) > 0) {
                // arr[i] wants to extend 
                // largest subsequence
                prevIndices[i] = tailIndices[len - 1];
                tailIndices[len++] = i;
            } else {
                // arr[i] wants to be a potential 
                // condidate of future subsequence
                // It will replace ceil value in 
                // tailIndices
                int pos = getCeilIndex(parts,
                                       tailIndices, -1, len - 1, part);

                prevIndices[i] = tailIndices[pos - 1];
                tailIndices[pos] = i;
            }
        }
        
        TreeSet<UnitIdPart> fixedParts = new TreeSet<>();
        for (int i = tailIndices[len - 1]; i >= 0; 
                i = prevIndices[i])
        {
            UnitIdPart n = parts.get(i);
            fixedParts.add(n);
        }

        return fixedParts;
    }

    private void readChildLevels() {
        List<ArrLevel> childLevels = params.getBulkAction().getChildren(parentLevel);
        if (CollectionUtils.isEmpty(childLevels)) {
            return;
        }

        nodes = new ArrayList<>(childLevels.size());

        for (ArrLevel l : childLevels) {

            Node n = prepareNode(l);
            nodes.add(n);
        }
    }

    private Node prepareNode(ArrLevel l) {
        // read level type
        RulItemSpec nodeLevelSpec = getLevelSpec(l, params);

        Node n = new Node(l, nodeLevelSpec);

        // read current unitId
        ArrDescItem itemUnidId = params.getBulkAction().loadSingleDescItem(l.getNode(), params.getItemType());
        if (itemUnidId != null) {
            ArrData data = itemUnidId.getData();
            if (data == null) {
                throw new SystemException("UnitId without data", BaseCode.INVALID_STATE)
                        .set("levelId", l.getLevelId());
            }
            ArrDataUnitid unitId = HibernateUtils.unproxy(data);

            String value = unitId.getValue();
            // check if value is fixed
            PartSealedUnitId sealedUnitId = params.getSealedTree().find(value);
            n.setSealedUnitId(sealedUnitId);
            n.setOldValue(itemUnidId, value);

            StringBuilder expectedPrefix = new StringBuilder().append(prefix).append('/');
            
            // check if extra slash sublevel
            if (isExtraSlashBoundary(nodeLevelSpec)) {
                n.setExtraSlashBefore(true);
                expectedPrefix.append('/');
            }
            // check if value match prefix
            if (value.startsWith(expectedPrefix.toString())) {
                String remaining = value.substring(expectedPrefix.length());
                // check remaining is without slash
                if (remaining.length() > 0) {
                    if (remaining.indexOf('/') < 0) {
                        n.setUnitIdUsable(true);
                    }
                }
            }
        }
        return n;
    }

    private boolean isExtraSlashBoundary(RulItemSpec nodeLevelSpec) {
        if (this.itemSpec == params.getExtraSlashLevelType()) {
            if (nodeLevelSpec != params.getExtraSlashLevelType()) {
                return true;
            }
        }
        return false;
    }

}
