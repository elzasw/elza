package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.ParRelationClassTypeRepeatabilityEnum;

/**
 * VO pro třídu typu vztahu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 26. 10. 2016
 */
public class ParRelationClassTypeVO {

    private Integer relationClassTypeId;

    private String name;

    private String code;

    private ParRelationClassTypeRepeatabilityEnum repeatability;

    public Integer getRelationClassTypeId() {
        return relationClassTypeId;
    }

    public void setRelationClassTypeId(final Integer relationClassTypeId) {
        this.relationClassTypeId = relationClassTypeId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public ParRelationClassTypeRepeatabilityEnum getRepeatability() {
        return repeatability;
    }

    public void setRepeatability(final ParRelationClassTypeRepeatabilityEnum repeatability) {
        this.repeatability = repeatability;
    }
}
