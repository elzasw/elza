package cz.tacr.elza.controller.vo;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * VO pro pravidla s typy výstupů.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 7. 1. 2016
 */
public class RulRuleSetVO {

    private Integer id;

    private String code;

    private String name;

    private List<RulArrangementTypeVO> arrangementTypes = new LinkedList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RulArrangementTypeVO> getArrangementTypes() {
        return arrangementTypes;
    }

    public void setArrangementTypes(List<RulArrangementTypeVO> arrangementTypes) {
        this.arrangementTypes = arrangementTypes;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
