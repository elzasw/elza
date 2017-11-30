package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Typ kalendáře.
 *
 * @author Martin Šlapa
 * @since 20.10.2015
 */
@Entity(name = "arr_calendar_type")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrCalendarType {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer calendarTypeId;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    public Integer getCalendarTypeId() {
        return this.calendarTypeId;
    }

    public void setCalendarTypeId(final Integer calendarTypeId) {
        this.calendarTypeId = calendarTypeId;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
