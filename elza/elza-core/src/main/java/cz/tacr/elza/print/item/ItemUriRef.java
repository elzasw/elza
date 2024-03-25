package cz.tacr.elza.print.item;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.item.convertors.ItemConvertorContext;

public class ItemUriRef extends AbstractItem {

    private final String schema;

    private final String value;

    private final String description;

    private Node node = null;

    private final ArrNode linkedNode;

    private final ItemConvertorContext context;

    public ItemUriRef(final String schema,
                      final String value,
                      final String description,
                      final ArrNode linkedNode,
                      final ItemConvertorContext context) {
        this.schema = schema;
        this.value = value;
        this.description = description;
        this.linkedNode = linkedNode;
        this.context = context;
    }

    public String getSchema() {
        return schema;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public Node getNode() {
        if (node == null) {
            if (linkedNode != null) {
                node = context.getNode(linkedNode);
            }
        }
        return node;

    }

    @Override
    public String getSerializedValue() {
    	StringBuilder sb = new StringBuilder();
    	if(StringUtils.isNotEmpty(description)) {
    		sb.append(description).append(": ");
    	}
    	if(StringUtils.isNotEmpty(value)) {
    		sb.append(value);
    	}
        return sb.toString();
    }
}
