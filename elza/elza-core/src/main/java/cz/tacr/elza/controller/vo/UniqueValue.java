package cz.tacr.elza.controller.vo;

import jakarta.annotation.Nullable;

public class UniqueValue {

    @Nullable
    public Integer id;

    public String value;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
