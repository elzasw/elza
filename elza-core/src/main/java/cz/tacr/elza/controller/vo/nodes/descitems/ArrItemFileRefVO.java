package cz.tacr.elza.controller.vo.nodes.descitems;


import java.util.Objects;

import cz.tacr.elza.common.db.HibernateUtils;
import jakarta.persistence.EntityManager;

import cz.tacr.elza.controller.vo.ArrFileVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.attachment.AttachmentService;


/**
 * VO hodnoty atributu - file.
 *
 * @since 27.6.16
 */
public class ArrItemFileRefVO extends ArrItemVO {

    /**
     * obal
     */
    private Integer value;

    private ArrFileVO file;

    public ArrItemFileRefVO() {

    }

    public ArrItemFileRefVO(ArrItem item, ArrFileVO file) {
        super(item);

        this.file = file;
        if (file != null) {
            value = file.getId();
        }
    }

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
            if (!Objects.equals(file.getId(), value)) {
                throw new BusinessException("Inconsistent data, file is not found", BaseCode.PROPERTY_IS_INVALID)
                        .set("value", value).set("file.id", file.getId());
            }
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

    public static ArrItemFileRefVO newInstance(ArrItem item, AttachmentService attachmentService) {
        ArrData data = HibernateUtils.unproxy(item.getData());
        ArrFileVO fileVO = null;
        if (data != null) {
            if (!(data instanceof ArrDataFileRef)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", item.getClass());
            }
            ArrDataFileRef dataFileRef = (ArrDataFileRef) data;
            ArrFile file = dataFileRef.getFile();
            fileVO = ArrFileVO.newInstance(file, attachmentService);
        }
        ArrItemFileRefVO vo = new ArrItemFileRefVO(item, fileVO);
        return vo;
    }
}
