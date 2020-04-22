package cz.tacr.elza.ws.core.v1.items;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("String")
public class ItemString extends Item {
    String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
