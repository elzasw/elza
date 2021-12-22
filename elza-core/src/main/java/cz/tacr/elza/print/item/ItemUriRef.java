package cz.tacr.elza.print.item;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.print.Node;

public class ItemUriRef extends AbstractItem {

    private final String schema;

    private final String value;

    private final String description;

    private final Node node;

    public ItemUriRef(String schema, String value, String description, Node node) {
        this.schema = schema;
        this.value = value;
        this.description = description;
        this.node = node;
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
