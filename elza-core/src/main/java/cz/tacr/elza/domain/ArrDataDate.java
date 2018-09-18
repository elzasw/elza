package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Hodnota atributu archivního popisu typu Datum.
 */
@Entity
@Table(name = "arr_data_date")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataDate extends ArrData {

    @Column(nullable = false)
    private LocalDate value;

    public ArrDataDate() {

    }

    protected ArrDataDate(final ArrDataDate src) {
        super(src);
        copyValue(src);
    }

    private void copyValue(final ArrDataDate src) {
        this.value = src.value;
    }

    public LocalDate getValue() {
        return value;
    }

    public void setValue(final LocalDate value) {
        this.value = value;
    }

    @Override
    public String getFulltextValue() {
        return null; // zatím není jasné, jak se bude indexovat
    }

    @Override
    public ArrDataDate makeCopy() {
        return new ArrDataDate(this);
    }

    @Override
    public Date getDate() {
        return Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Override
    protected boolean isEqualValueInternal(final ArrData srcData) {
        ArrDataDate src = (ArrDataDate) srcData;
        return value.equals(src.value);
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataDate src = (ArrDataDate) srcData;
        copyValue(src);
    }
}
