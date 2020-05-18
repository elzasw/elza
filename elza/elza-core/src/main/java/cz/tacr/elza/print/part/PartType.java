package cz.tacr.elza.print.part;

import cz.tacr.elza.domain.RulPartType;

public class PartType {

    private final String name;

    private final String code;

    public PartType(RulPartType rulPartType) {
        this.name = rulPartType.getName();
        this.code = rulPartType.getCode();
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }
}
