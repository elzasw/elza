package cz.tacr.elza.controller.vo.nodes.descitems;


import javax.persistence.EntityManager;

import cz.tacr.elza.controller.vo.ArrFileVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;


/**
 * VO hodnoty atributu - file.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 27.6.16
 */
public class ArrItemFileRefVO extends ArrItemVO {

    /**
     * obal
     */
    private Integer value;

    private ArrFileVO file;

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }

    public ArrFileVO getFile() {
        return file;
    }

    public void setFile(final ArrFileVO file) {
        this.file = file;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataFileRef data = new ArrDataFileRef();

        if (file != null) {
            throw new BusinessException("Inconsistent data, file is not null", BaseCode.PROPERTY_IS_INVALID);
        }

        // try to map file
        ArrFile file = null;
        if (this.value != null) {
            file = em.getReference(ArrFile.class, value);
        }
        data.setFile(file);

        data.setDataType(DataType.FILE_REF.getEntity());
        return data;
    }
}