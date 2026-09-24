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
 */
public class DaImportPlan {

    /** One planned div. */
    public static class Node {

        private final String divId;
        private final String label;
        private final DaImportResult.Decision decision;
        private final String matchKey;
        private final List<DaImportResult.Item> items;
        private final List<Node> children = new ArrayList<>();

        Node(String divId, @Nullable String label, DaImportResult.Decision decision, @Nullable String matchKey,
             List<DaImportResult.Item> items) {
            this.divId = divId;
            this.label = label;
            this.decision = decision;
            this.matchKey = matchKey;
            this.items = List.copyOf(items);
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

        /** @see DaImportResult#matchKey(String) */
        @Nullable
        public String getMatchKey() {
            return matchKey;
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
}
