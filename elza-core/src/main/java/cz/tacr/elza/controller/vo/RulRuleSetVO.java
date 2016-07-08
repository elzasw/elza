package cz.tacr.elza.controller.vo;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;


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

    /** Kódy atributů pro zobrazení v gridu hromadných úprav - jaké jsou implicitní atributy a jaké je jejich pořadí. */
    private List<String> itemTypeCodes;

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

    public List<String> getItemTypeCodes() {
        return itemTypeCodes;
    }

    public void setItemTypeCodes(List<String> itemTypeCodes) {
        this.itemTypeCodes = itemTypeCodes;
    }
}
