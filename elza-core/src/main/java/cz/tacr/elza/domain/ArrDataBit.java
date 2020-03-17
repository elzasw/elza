package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.Validate;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="arr_data_bit")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataBit extends ArrData {

    @Basic
    @Column(name="value", nullable=false)
    private Boolean value;

    public ArrDataBit() {

    }

    public ArrDataBit(final Boolean value) {
        this.value = value;
    }

    public ArrDataBit(ArrDataBit src) {
        super(src);
        copyValue(src);
    }

    private void copyValue(ArrDataBit src) {
        this.value = src.value;
    }

    @Override
    public String getFulltextValue() {
        return Boolean.toString(value);
    }

    public Boolean isValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataBit src = (ArrDataBit) srcData;
        return (value.booleanValue() == src.value.booleanValue());
    }

    @Override
    public ArrDataBit makeCopy() {
        return new ArrDataBit(this);
    }

    @Override
    protected void mergeInternal(final ArrData srcData) {
        ArrDataBit src = (ArrDataBit) srcData;
        copyValue(src);
    }

    @Override
    protected void validateInternal() {
        Validate.notNull(value);
    }
}
