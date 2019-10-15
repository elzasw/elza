package cz.tacr.elza.domain;

import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.IUnitdate;


/**
 * Hodnoty datace.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_unitdate")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParUnitdate implements IUnitdate {

    /* Konstanty pro vazby a fieldy. */
    public static final String FIELD_UNITDATE_ID = "unitdateId";
    public static final String FIELD_CALENDAR_TYPE = "calendarType";
    public static final String FIELD_VALUE_FROM = "valueFrom";
    public static final String FIELD_VALUE_FROM_ESTIMATED = "valueFromEstimated";
    public static final String FIELD_VALUE_TO = "valueTo";
    public static final String FIELD_VALUE_TO_ESTIMATED = "valueToEstimated";
    public static final String FIELD_FORMAT = "format";
    public static final String FIELD_TEXT_DATE = "textDate";
    public static final String FIELD_NOTE = "note";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer unitdateId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrCalendarType.class)
    @JoinColumn(name = "calendarTypeId")
    private ArrCalendarType calendarType;

    @Column(updatable = false, insertable = false)
    private Integer calendarTypeId;

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
    @OneToMany(mappedBy = "validFrom", fetch = FetchType.LAZY)
    private List<ParPartyName> validFromPartyNames;

    @RestResource(exported = false)
    @OneToMany(mappedBy = "validTo", fetch = FetchType.LAZY)
    private List<ParPartyName> validToPartyNames;

    @Column
    @Lob
    @org.hibernate.annotations.Type(type = "org.hibernate.type.TextType")
    private String note;

    public Integer getUnitdateId() {
        return unitdateId;
    }

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
        this.calendarTypeId = calendarType != null ? calendarType.getCalendarTypeId() : null;
    }

    public Integer getCalendarTypeId() {
        return calendarTypeId;
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

    /**
     * @return Text pokud není validní datace.
     */
    public String getTextDate() {
        return textDate;
    }

    /**
     * @param textDate Text pokud není validní datace.
     */
    public void setTextDate(final String textDate) {
        this.textDate = textDate;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(unitdateId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParUnitdate pk=" + unitdateId;
    }

    /** @return poznámka */
    public String getNote() {
        return note;
    }

    /**
     * Poznámka
     *
     * @param note poznámka
     */
    public void setNote(final String note) {
        this.note = note;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParUnitdate)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParUnitdate other = (ParUnitdate) obj;

        return new EqualsBuilder().append(unitdateId, other.getUnitdateId()).isEquals();
    }

    @Override
    public void formatAppend(final String format) {
        if (this.format == null) {
            this.format = "";
        }
        this.format += format;
    }
}
