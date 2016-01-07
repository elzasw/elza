package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.List;


/**
 * Hodnoty datace.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_unitdate")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParUnitdate implements cz.tacr.elza.api.ParUnitdate<ArrCalendarType> {

    /* Konstanty pro vazby a fieldy. */
    public static final String UNITDATE_ID = "unitdateId";
    public static final String CALENDAR_TYPE = "calendarType";
    public static final String VALUE_FROM = "valueFrom";
    public static final String VALUE_FROM_ESTIMATED = "valueFromEstimated";
    public static final String VALUE_TO = "valueTo";
    public static final String VALUE_TO_ESTIMATED = "valueToEstimated";
    public static final String FORMAT = "format";
    public static final String TEXT_DATE = "textDate";

    @Id
    @GeneratedValue
    private Integer unitdateId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrCalendarType.class)
    @JoinColumn(name = "calendarTypeId")
    private ArrCalendarType calendarType;

    @Column(length = 19)
    private String valueFrom;

    @Column()
    private Boolean valueFromEstimated;

    @Column(length = 19)
    private String valueTo;

    @Column()
    private Boolean valueToEstimated;

    @Column(length = 50)
    private String format;

    @Column(length = 250)
    private String textDate;

    @RestResource(exported = false)
    @OneToMany(mappedBy = "from", fetch = FetchType.LAZY)
    private List<ParPartyTimeRange> fromTimeRanges;

    @RestResource(exported = false)
    @OneToMany(mappedBy = "to", fetch = FetchType.LAZY)
    private List<ParPartyTimeRange> toTimeRanges;

    @RestResource(exported = false)
    @OneToMany(mappedBy = "validFrom", fetch = FetchType.LAZY)
    private List<ParPartyName> validFromPartyNames;

    @RestResource(exported = false)
    @OneToMany(mappedBy = "validTo", fetch = FetchType.LAZY)
    private List<ParPartyName> validToPartyNames;


    @Override
    public Integer getUnitdateId() {
        return unitdateId;
    }

    @Override
    public void setUnitdateId(final Integer unitdateId) {
        this.unitdateId = unitdateId;
    }

    @Override
    public ArrCalendarType getCalendarType() {
        return calendarType;
    }

    @Override
    public void setCalendarType(final ArrCalendarType calendarType) {
        this.calendarType = calendarType;
    }

    @Override
    public String getValueFrom() {
        return valueFrom;
    }

    @Override
    public void setValueFrom(final String valueFrom) {
        this.valueFrom = valueFrom;
    }

    @Override
    public Boolean getValueFromEstimated() {
        return valueFromEstimated;
    }

    @Override
    public void setValueFromEstimated(final Boolean valueFromEstimated) {
        this.valueFromEstimated = valueFromEstimated;
    }

    @Override
    public String getValueTo() {
        return valueTo;
    }

    @Override
    public void setValueTo(final String valueTo) {
        this.valueTo = valueTo;
    }

    @Override
    public Boolean getValueToEstimated() {
        return valueToEstimated;
    }

    @Override
    public void setValueToEstimated(final Boolean valueToEstimated) {
        this.valueToEstimated = valueToEstimated;
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public void setFormat(final String format) {
        this.format = format;
    }

    @Override
    public String getTextDate() {
        return textDate;
    }

    @Override
    public void setTextDate(final String textDate) {
        this.textDate = textDate;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParUnitdate)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.api.ParUnitdate<ArrCalendarType> other = (cz.tacr.elza.api.ParUnitdate<ArrCalendarType>) obj;

        return new EqualsBuilder().append(unitdateId, other.getUnitdateId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(unitdateId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParUnitdate pk=" + unitdateId;
    }
}
