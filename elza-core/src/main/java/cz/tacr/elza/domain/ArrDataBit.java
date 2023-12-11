package cz.tacr.elza.domain;

import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name="arr_data_bit")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataBit extends ArrData {

    public static final String BIT_VALUE = "bitValue";

    @Basic
    @Column(name="bit_value", nullable=false)
    private Boolean bitValue;

    public ArrDataBit() {

    }

    public ArrDataBit(final Boolean bitValue) {
        this.bitValue = bitValue;
    }

    protected ArrDataBit(ArrDataBit src) {
        super(src);
        copyValue(src);
    }

    private void copyValue(ArrDataBit src) {
        this.bitValue = src.bitValue;
    }

    @Override
    public String getFulltextValue() {
        return Boolean.toString(bitValue);
    }

    public Boolean isBitValue() {
        return bitValue;
    }

    public void setBitValue(Boolean bitValue) {
        this.bitValue = bitValue;
    }

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataBit src = (ArrDataBit) srcData;
        return (bitValue.booleanValue() == src.bitValue.booleanValue());
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
        Validate.notNull(bitValue);
    }
}
