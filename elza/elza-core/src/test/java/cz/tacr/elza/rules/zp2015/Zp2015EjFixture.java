package cz.tacr.elza.rules.zp2015;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulItemSpec;

/**
 * Builds a level tree for the evidence-unit tests in memory.
 *
 * <p>A fund is not imported: the counters read the levels through {@link LevelWithItems}, which is
 * an ordinary object, so the tree can be written out in the test that uses it. What a scenario says
 * is then visible next to what it asserts, and a scenario costs no database round trip.
 */
class Zp2015EjFixture {

    /** One node of the fixture tree, before it is turned into levels. */
    static class Node {
        private final List<Node> children = new ArrayList<>();
        private final List<ArrDescItem> items = new ArrayList<>();
        private final String name;

        Node(String name) {
            this.name = name;
        }

        Node with(ArrDescItem item) {
            items.add(item);
            return this;
        }

        Node child(Node child) {
            children.add(child);
            return this;
        }

        String getName() {
            return name;
        }
    }

    private final StaticDataProvider sdp;
    private int nextNodeId = 1;
    private int nextPosition = 1;

    Zp2015EjFixture(StaticDataProvider sdp) {
        this.sdp = sdp;
    }

    static Node node(String name) {
        return new Node(name);
    }

    /**
     * An enumerated description item, the form most of the arrangement rules are written against.
     * Enumerated items carry no data - the specification is the value.
     */
    ArrDescItem enumItem(String itemTypeCode, String itemSpecCode) {
        ItemType itemType = sdp.getItemTypeByCode(itemTypeCode);
        if (itemType == null) {
            throw new IllegalArgumentException("Unknown item type: " + itemTypeCode);
        }
        RulItemSpec spec = itemType.getItemSpecByCode(itemSpecCode);
        if (spec == null) {
            throw new IllegalArgumentException("Unknown specification " + itemSpecCode
                    + " of item type " + itemTypeCode);
        }
        ArrDescItem item = new ArrDescItem();
        item.setItemTypeId(itemType.getItemTypeId());
        item.setItemSpecId(spec.getItemSpecId());
        item.setPosition(nextPosition++);
        return item;
    }

    /**
     * An enumerated item that carries no specification - the rules match it by its presence
     * alone, as with the flag marking a record invalid.
     */
    ArrDescItem flagItem(String itemTypeCode) {
        ItemType itemType = sdp.getItemTypeByCode(itemTypeCode);
        if (itemType == null) {
            throw new IllegalArgumentException("Unknown item type: " + itemTypeCode);
        }
        ArrDescItem item = new ArrDescItem();
        item.setItemTypeId(itemType.getItemTypeId());
        item.setPosition(nextPosition++);
        return item;
    }

    /**
     * A reference to a storage unit. The unit itself lives in a structured object; the test supplies
     * its content through the stubbed StructObjService, so only the id matters here.
     */
    ArrDescItem storageRef(String itemTypeCode, int structObjId) {
        ItemType itemType = sdp.getItemTypeByCode(itemTypeCode);
        if (itemType == null) {
            throw new IllegalArgumentException("Unknown item type: " + itemTypeCode);
        }
        ArrStructuredObject so = new ArrStructuredObject();
        so.setStructuredObjectId(structObjId);
        ArrDataStructureRef data = new ArrDataStructureRef();
        data.setStructuredObject(so);

        ArrDescItem item = new ArrDescItem();
        item.setItemTypeId(itemType.getItemTypeId());
        item.setPosition(nextPosition++);
        item.setData(data);
        return item;
    }

    /**
     * Turns the tree into levels in the order a bulk action walks them: a node before its children.
     */
    List<LevelWithItems> levels(Node root) {
        List<LevelWithItems> out = new ArrayList<>();
        append(root, null, out);
        return out;
    }

    private void append(Node node, LevelWithItems parent, List<LevelWithItems> out) {
        TreeNode treeNode = new TreeNode(nextNodeId++, out.size());
        LevelWithItems level = new LevelWithItems(treeNode, parent, node.items);
        out.add(level);
        for (Node child : node.children) {
            append(child, level, out);
        }
    }
}
