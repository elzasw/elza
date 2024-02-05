package cz.tacr.elza.controller.vo.nodes.descitems;

import com.fasterxml.jackson.annotation.JsonFormat;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
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

    public ArrItemDateVO() {
    	
    }

    public ArrItemDateVO(ArrItem item, final LocalDate value) {
        super(item);
        this.value = value;
    }
    
    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataDate data = new ArrDataDate();
        data.setValue(value);
        data.setDataType(DataType.DATE.getEntity());
        return data;
    }

    public static ArrItemDateVO newInstance(ArrItem item) {
        ArrData data = HibernateUtils.unproxy(item.getData());
        LocalDate value = null;
        if (data != null) {
            if (!(data instanceof ArrDataDate)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", item.getClass());
            }
            ArrDataDate dataDate = (ArrDataDate) data;
            value = dataDate.getValue();
        }
        ArrItemDateVO vo = new ArrItemDateVO(item, value);
        return vo;
    }
}
