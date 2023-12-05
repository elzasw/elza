package cz.tacr.elza.controller.vo.nodes.descitems;

import com.fasterxml.jackson.annotation.JsonFormat;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDate;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;

/**
 * VO hodnoty atributu - date.
 *
 * @since 05.06.2018
 */
public class ArrItemDateVO extends ArrItemVO {

    /**
     * datum
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate value;

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
