package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.enums.ParRelationClassTypeRepeatabilityEnum;

/**
 * VO pro třídu typu vztahu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 26. 10. 2016
 */
public class ParRelationClassTypeVO {

    private Integer id;

    private String name;

    private String code;

    private ParRelationClassTypeRepeatabilityEnum repeatability;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
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
