package cz.tacr.elza.controller.vo.ap.item;

import com.fasterxml.jackson.annotation.JsonFormat;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDate;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Objects;

public class ApItemDateVO extends ApItemVO {

    /**
     * Datum
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate value;

    public ApItemDateVO() {
    }

    public ApItemDateVO(final ApItem item) {
        super(item);
        value = getLocalDateValue(item);
    }

    final public LocalDate getLocalDateValue(final ApItem item) {
        ArrDataDate data = (ArrDataDate) item.getData();
        return data == null ? null : data.getValue();
    }

    public LocalDate getValue() {
        return value;
    }

    public void setValue(final LocalDate value) {
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataDate data = new ArrDataDate();
        data.setValue(value);
        data.setDataType(DataType.DATE.getEntity());
        return data;
    }

    @Override
    public boolean equalsValue(ApItem item) {
        return equalsBase(item) && Objects.equals(value, getLocalDateValue(item));
    }
}
