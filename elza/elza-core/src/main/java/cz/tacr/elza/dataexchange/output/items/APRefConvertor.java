package cz.tacr.elza.dataexchange.output.items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class APRefConvertor implements ItemDataConvertor {

    private static final Logger log = LoggerFactory.getLogger(APRefConvertor.class);

    @Override
    public DescriptionItemAPRef convert(ArrData data, ObjectFactory objectFactory) {
        if (data == null) {
            log.error("data is null");
            throw new SystemException("Data is null");
        }
        if (!(data instanceof ArrDataRecordRef)) {
            log.error("data is null, dataId: ", data.getDataId());
            throw new SystemException("Invalid data type, dataId: " + data.getDataId())
                    .set("dataId", data.getDataId());
        }

        ArrDataRecordRef apRef = (ArrDataRecordRef) data;

        DescriptionItemAPRef item = objectFactory.createDescriptionItemAPRef();
        if (apRef.getRecordId() == null) {
            log.debug("AccessPointRef without real data, dataId: {}", data.getDataId());
            ApBinding binding = apRef.getBinding();
            if (binding == null) {
                log.error("ArrDataRecordRef without connected recordId and binding, dataId: ", data.getDataId());
                throw new SystemException("ArrDataRecordRef without connected recordId and binding")
                        .set("dataId", data.getDataId());
            }
            item.setApid(binding.getValue());
        } else {
            item.setApid(apRef.getRecordId().toString());
        }
        return item;
    }
}
