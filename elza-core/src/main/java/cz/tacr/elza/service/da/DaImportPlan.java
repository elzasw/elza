package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import cz.tacr.elza.domain.RulItemType;

/**
 * What importing a package would create below the node it is imported to: the levels and
 * attachments decided by the DA_IMPORT script for the divs of the logical structural map.
 * Nothing is stored - the plan is carried out by whoever imports the package.
 *
 * Skipped divs are not in the plan; what was below them hangs on their nearest planned ancestor,
 * or on the root of the plan.
 */
public class DaImportPlan {

    /** One planned div. */
    public static class Node {

        private final String divId;
        private final String label;
        private final DaImportResult.Decision decision;
        private final List<RulItemType> matchBy;
        private final List<DaImportResult.Item> items;
        private final boolean attachOwnEntity;
        private final List<Node> children = new ArrayList<>();

        Node(String divId, @Nullable String label, DaImportResult.Decision decision, List<RulItemType> matchBy,
             List<DaImportResult.Item> items) {
            this(divId, label, decision, matchBy, items, false);
        }

        Node(String divId, @Nullable String label, DaImportResult.Decision decision, List<RulItemType> matchBy,
             List<DaImportResult.Item> items, boolean attachOwnEntity) {
            this.divId = divId;
            this.label = label;
            this.decision = decision;
            this.matchBy = List.copyOf(matchBy);
            this.items = List.copyOf(items);
            this.attachOwnEntity = attachOwnEntity;
        }

        /**
         * Whether the part of the package the div stands for is attached to the level itself -
         * with everything below it, which is attached with it.
         */
        public boolean isAttachOwnEntity() {
            return attachOwnEntity;
        }

        /** ID of the div - the code of its logical digital entity. */
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

        /** @see DaImportResult#matchBy(String...) */
        public List<RulItemType> getMatchBy() {
            return matchBy;
        }

        /** Items of the level; empty for an attachment. */
        public List<DaImportResult.Item> getItems() {
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
