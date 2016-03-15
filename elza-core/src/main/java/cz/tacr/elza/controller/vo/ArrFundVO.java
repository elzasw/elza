package cz.tacr.elza.controller.vo;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * VO pro archivní pomůcku.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 7. 1. 2016
 */
public class ArrFundVO {

    private Integer id;

    private String name;

    private Date createDate;

    private List<ArrFundVersionVO> versions = new LinkedList<>();

    private List<RegScopeVO> regScopes;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public List<ArrFundVersionVO> getVersions() {
        return versions;
    }

    public void setVersions(List<ArrFundVersionVO> versions) {
        this.versions = versions;
    }

    public List<RegScopeVO> getRegScopes() {
        return regScopes;
    }

    public void setRegScopes(List<RegScopeVO> regScopes) {
        this.regScopes = regScopes;
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
