package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.controller.vo.ap.ApFragmentVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataApFragRef;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

import javax.persistence.EntityManager;

/**
 * @since 18.07.2018
 */
public class ApItemAPFragmentRefVO extends ApItemVO {

    /**
     * fragment
     */
    private ApFragmentVO fragment;

    private Integer value;

    public ApFragmentVO getFragment() {
        return fragment;
    }

    public void setFragment(final ApFragmentVO fragment) {
        this.fragment = fragment;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }

    public ApItemAPFragmentRefVO() {
    }

    public ApItemAPFragmentRefVO(final ApItem item) {
        super(item);
        ArrDataApFragRef data = (ArrDataApFragRef) item.getData();
        value = data == null ? null : data.getFragmentId();
    }

    // Entity can be created only from ID and not from embedded object
    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataApFragRef data = new ArrDataApFragRef();

        if (fragment != null) {
            throw new BusinessException("Inconsistent data, fragment is not null", BaseCode.PROPERTY_IS_INVALID);
        }

        // try to map fragment
        ApPart fragment = null;
        if (this.value != null) {
            fragment = em.getReference(ApPart.class, value);
        }
        data.setFragment(fragment);

        data.setDataType(DataType.APFRAG_REF.getEntity());
        return data;
    }

}
