package cz.tacr.elza.controller.vo.nodes.descitems;

import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.*;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EntityManager;
import java.net.URI;

public class ArrItemUriRefVO extends ArrItemVO {

    private String value;

    private String description;

    public ArrItemUriRefVO() {

    }

    public ArrItemUriRefVO(ArrItem item, String value, String description) {
        super(item);
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataUriRef data = new ArrDataUriRef();
        data.setValue(value);
        data.setDescription(description);
        data.setDataType(DataType.URI_REF.getEntity());
        data.setSchema(ArrDataUriRef.createSchema(value));
        return data;
    }

}
