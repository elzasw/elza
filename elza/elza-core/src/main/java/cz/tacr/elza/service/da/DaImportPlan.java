package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * What importing a package would create below the node it is imported to: the levels and
 * attachments decided by the DA_IMPORT script for the divs of the logical structural map.
 * Nothing is stored - the plan is carried out by whoever imports the package.
 *
 * Skipped divs are not in the plan; what was below them hangs on their nearest planned ancestor,
 * or on the root of the plan.
 *
 * A plan placing a received package ({@link #placement}) starts with the chain of levels the
 * DA_MATCH script decided on; those levels stand for no div.
 */
public class DaImportPlan {

    /** One planned div. */
    public static class Node {

        private final String divId;
        private final String label;
        private final DaImportResult.Decision decision;
        private final List<DaLevelItems.MatchKey> matchBy;
        private final List<DaLevelItems.Item> items;
        private final boolean attachOwnEntity;
        private final boolean attachWholeAip;
        private final List<Node> children = new ArrayList<>();

        Node(String divId, @Nullable String label, DaImportResult.Decision decision,
             List<DaLevelItems.MatchKey> matchBy,
             List<DaLevelItems.Item> items) {
            this(divId, label, decision, matchBy, items, false);
        }

        Node(String divId, @Nullable String label, DaImportResult.Decision decision,
             List<DaLevelItems.MatchKey> matchBy,
             List<DaLevelItems.Item> items, boolean attachOwnEntity) {
            this(divId, label, decision, matchBy, items, attachOwnEntity, false);
        }

        private Node(@Nullable String divId, @Nullable String label, DaImportResult.Decision decision,
                     List<DaLevelItems.MatchKey> matchBy, List<DaLevelItems.Item> items, boolean attachOwnEntity,
                     boolean attachWholeAip) {
            this.divId = divId;
            this.label = label;
            this.decision = decision;
            this.matchBy = List.copyOf(matchBy);
            this.items = List.copyOf(items);
            this.attachOwnEntity = attachOwnEntity;
            this.attachWholeAip = attachWholeAip;
        }

        /**
         * A level of the chain a received package is placed under; it stands for no div.
         *
         * @param attachWholeAip whether the whole package is linked to the level
         */
        static Node chainLevel(String label, List<DaLevelItems.MatchKey> matchBy, List<DaLevelItems.Item> items,
                               boolean attachWholeAip) {
            return new Node(null, label, DaImportResult.Decision.LEVEL, matchBy, items, false, attachWholeAip);
        }

        /** Whether the level is a level of the chain a package is placed under, standing for no div. */
        public boolean isChainLevel() {
            return divId == null;
        }

        /** Whether the whole package is linked to the level. */
        public boolean isAttachWholeAip() {
            return attachWholeAip;
        }

        /**
         * Whether the part of the package the div stands for is attached to the level itself -
         * with everything below it, which is attached with it.
         */
        public boolean isAttachOwnEntity() {
            return attachOwnEntity;
        }

        /** ID of the div - the code of its logical digital entity; null for a level of the chain. */
        @Nullable
        public String getDivId() {
            return divId;
        }

        @Nullable
        public String getLabel() {
            return label;
        }

        /** {@link DaImportResult.Decision#LEVEL} or {@link DaImportResult.Decision#ATTACH}. */
        public DaImportResult.Decision getDecision() {
            return decision;
        }

        /** @see DaLevelItems#matchBy(String...) */
        public List<DaLevelItems.MatchKey> getMatchBy() {
            return matchBy;
        }

        /** Items of the level; empty for an attachment. */
        public List<DaLevelItems.Item> getItems() {
            return items;
        }

        public List<Node> getChildren() {
            return Collections.unmodifiableList(children);
        }

        void addChild(Node child) {
            children.add(child);
        }
    }

    private final List<Node> roots = new ArrayList<>();

    /** Planned divs hanging directly on the node the package is imported to. */
    public List<Node> getRoots() {
        return Collections.unmodifiableList(roots);
    }

    void addRoot(Node node) {
        roots.add(node);
    }

    /**
     * The plan placing a received package: the levels of the chain, each below the previous one,
     * and the plan of the package below the last of them.
     *
     * @param chain levels of the chain, top-down
     * @param below the plan of the package; null when the package is only linked
     */
    static DaImportPlan placement(List<Node> chain, @Nullable DaImportPlan below) {
        DaImportPlan plan = new DaImportPlan();
        Node last = null;
        for (Node level : chain) {
            if (last == null) {
                plan.addRoot(level);
            } else {
                last.addChild(level);
            }
            last = level;
        }
        if (below != null) {
            for (Node root : below.getRoots()) {
                if (last == null) {
                    plan.addRoot(root);
                } else {
                    last.addChild(root);
                }
            }
        }
        return plan;
    }

    /**
     * The first level of the plan only: its levels without what is below them, each with its own
     * part of the package attached - the levels below are attached with it, without being
     * described. Attachments of the first level stay as they are.
     */
    public DaImportPlan firstLevel() {
        DaImportPlan result = new DaImportPlan();
        for (Node root : roots) {
            result.addRoot(root.getDecision() == DaImportResult.Decision.LEVEL
                    ? new Node(root.getDivId(), root.getLabel(), root.getDecision(), root.getMatchBy(),
                               root.getItems(), true)
                    : root);
        }
        return result;
    }
}
