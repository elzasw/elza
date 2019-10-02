package cz.tacr.elza.controller.vo.ap.item;

import com.fasterxml.jackson.annotation.JsonFormat;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDate;

import javax.persistence.EntityManager;
import java.time.LocalDate;

/**
 * @since 18.07.2018
 */
public class ApItemDateVO extends ApItemVO {

    /**
     * datum
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate value;

    public ApItemDateVO() {
    }

    public ApItemDateVO(final ApItem item) {
        super(item);
        ArrDataDate data = (ArrDataDate) item.getData();
        value = data == null ? null : data.getValue();
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
}
