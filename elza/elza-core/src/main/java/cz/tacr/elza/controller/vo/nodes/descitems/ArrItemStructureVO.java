package cz.tacr.elza.controller.vo.nodes.descitems;

import java.util.Objects;
import jakarta.persistence.EntityManager;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;


/**
 * VO hodnoty atributu - structure data.
 *
 * @since 16.11.2017
 */
public class ArrItemStructureVO extends ArrItemVO {

    /**
     * obal
     */
    private Integer value;

    private ArrStructureDataVO structureData;

    public ArrItemStructureVO() {
    	
    }

    public ArrItemStructureVO(ArrItem item, final Integer value, ArrStructureDataVO structureData) {
        super(item);
        this.value = value;
        this.structureData = structureData;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }

    public ArrStructureDataVO getStructureData() {
        return structureData;
    }

    public void setStructureData(final ArrStructureDataVO structureData) {
        this.structureData = structureData;
    }

    // Entity can be created only from ID and not from embedded object
    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataStructureRef data = new ArrDataStructureRef();

        if (structureData != null) {
            if (!Objects.equals(structureData.getId(), value)) {
                throw new BusinessException("Inconsistent data, structureData is not null",
                        BaseCode.PROPERTY_IS_INVALID)
                                .set("value", value).set("structureData.id", structureData.getId());
            }
        }

        // get reference
        ArrStructuredObject structObj = null;
        if (this.value != null) {
            structObj = em.getReference(ArrStructuredObject.class, value);
        }
        data.setStructuredObject(structObj);

        data.setDataType(DataType.STRUCTURED.getEntity());
        return data;
    }

    public static ArrItemStructureVO newInstance(ArrItem item) {
        ArrData data = HibernateUtils.unproxy(item.getData());
        Integer value = null;
        ArrStructureDataVO structureData = null;
        if (data != null) {
            if (!(data instanceof ArrDataStructureRef)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", item.getClass());
            }
            ArrDataStructureRef struct = (ArrDataStructureRef) data;
            value = struct.getValueInt();
            structureData = ArrStructureDataVO.newInstance(struct.getStructuredObject());
        }
        ArrItemStructureVO vo = new ArrItemStructureVO(item, value, structureData);
        return vo;
    }
}
