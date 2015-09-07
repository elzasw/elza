package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.*;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */

@Entity(name = "arr_data")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public abstract class ArrData extends AbstractVersionableEntity implements cz.tacr.elza.api.ArrData<RulDataType, ArrDescItem> {

    @Id
    @GeneratedValue
    private Integer dataId;


    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDataType.class)
    @JoinColumn(name = "dataTypeId", nullable = false)
    private RulDataType dataType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDescItem.class)
    @JoinColumn(name = "descItemId", nullable = true)
    private ArrDescItem descItem;

    @Override
    public Integer getDataId() {
        return dataId;
    }

    @Override
    public void setDataId(final Integer dataId) {
        this.dataId = dataId;
    }

    @Override
    public RulDataType getDataType() {
        return dataType;
    }

    @Override
    public void setDataType(final RulDataType dataType) {
        this.dataType = dataType;
    }

    @Override
    public ArrDescItem getDescItem() {
        return descItem;
    }

    @Override
    public void setDescItem(final ArrDescItem descItem) {
        this.descItem = descItem;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ArrData)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ArrData other = (ArrData) obj;

        return new EqualsBuilder().append(dataId, other.getDataId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(dataId).toHashCode();
    }

    public abstract String getData();

}
