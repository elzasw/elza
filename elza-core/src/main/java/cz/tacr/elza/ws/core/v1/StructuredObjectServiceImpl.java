package cz.tacr.elza.ws.core.v1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ArrStructuredObject.State;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.StructObjService;
import cz.tacr.elza.ws.types.v1.ErrorDescription;
import cz.tacr.elza.ws.types.v1.Items;
import cz.tacr.elza.ws.types.v1.StructuredObject;
import cz.tacr.elza.ws.types.v1.StructuredObjectIdentifiers;

@Component
@javax.jws.WebService(serviceName = "CoreService", portName = "StructuredObjectService", targetNamespace = "http://elza.tacr.cz/ws/core/v1",
        //                      wsdlLocation = "file:elza-core-v1.wsdl",
        endpointInterface = "cz.tacr.elza.ws.core.v1.StructuredObjectService")
public class StructuredObjectServiceImpl implements StructuredObjectService {

    final private static Logger logger = LoggerFactory.getLogger(StructuredObjectServiceImpl.class);

    @Autowired
    StructObjService structObjService;

    @Autowired
    ArrangementInternalService arrangementInternalService;

    @Autowired
    StaticDataService staticDataService;

    @Autowired
    WSHelper wsHelper;

    private ArrStructuredObject findStructObj(String id, String uuid) {
        if (StringUtils.isNotEmpty(id)) {
            Integer stuctObjId = Integer.valueOf(id);
            return structObjService.getStructObjById(stuctObjId);
        }
        if (StringUtils.isNotEmpty(uuid)) {
            return structObjService.getExistingStructObjByUUID(uuid);
        }
        // missing ID
        throw new ObjectNotFoundException("Structured object identifier is missing (id nor uuid provided).",
                BaseCode.ID_NOT_EXIST);
    }

    @Override
    @Transactional
    public void deleteStructuredObject(StructuredObjectIdentifiers deleteStructuredObj)
            throws DeleteStructuredObjectFailed {
        try {
            ArrStructuredObject structObj = findStructObj(deleteStructuredObj.getId(), deleteStructuredObj.getUuid());
            structObjService.deleteStructObj(Collections.singletonList(structObj));
        } catch (Exception e)
        {
            logger.error("Failed to delete structured object: {}", e.getMessage(), e);
            throw prepareDeleteException("Failed to delete structured object.", deleteStructuredObj, e);
        }
    }

    DeleteStructuredObjectFailed prepareDeleteException(String msg, StructuredObjectIdentifiers deleteStructuredObj,
                                                        Exception e) {
        ErrorDescription ed = new ErrorDescription();
        ed.setUserMessage(msg);
        if (e != null) {
            ed.setDetail(e.getMessage());
        }
        
        List<String> ids = new ArrayList<>(2);
        if (StringUtils.isNotEmpty(deleteStructuredObj.getId())) {
            ids.add("id: " + deleteStructuredObj.getId());
        }
        if (StringUtils.isNotEmpty(deleteStructuredObj.getUuid())) {
            ids.add("uuid: " + deleteStructuredObj.getUuid());
        }
        ed.setDetail(String.join(", ", ids));

        return new DeleteStructuredObjectFailed(msg, ed, e);
    }

    @Override
    @Transactional
    public StructuredObjectIdentifiers createStructuredObject(StructuredObject createStructuredObject)
            throws CreateStructuredObjectFailed {
        String structObjTypeCode = createStructuredObject.getType();
        StaticDataProvider sdp = staticDataService.getData();
        StructType structObjType = sdp.getStructuredTypeByCode(structObjTypeCode);
        if (structObjType == null) {
            ErrorDescription errorDesc = new ErrorDescription();
            errorDesc.setUserMessage("Structured object not found: " + structObjTypeCode);
            throw new CreateStructuredObjectFailed(errorDesc.getUserMessage(), errorDesc);
        }
        // get fund
        ArrFund fund = wsHelper.getFund(createStructuredObject.getFund());

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.ADD_STRUCTURE_DATA);
        List<ArrStructuredItem> items = prepareItems(createStructuredObject.getItems());
        ArrStructuredObject structObj = structObjService.createStructObj(fund, change,
                                                                         structObjType.getStructuredType(),
                                                                         State.OK,
                                                                         createStructuredObject.getUuid(),
                                                                         items);

        StructuredObjectIdentifiers sois = new StructuredObjectIdentifiers();
        Validate.notNull(structObj.getStructuredObjectId());
        sois.setId(structObj.getStructuredObjectId().toString());
        sois.setUuid(structObj.getUuid());
        return sois;
    }

    private List<ArrStructuredItem> prepareItems(Items items) {
        if (items == null) {
            return null;
        }
        List<Object> itemList = items.getStrOrLongOrEnm();

        List<ArrStructuredItem> result = new ArrayList<>(itemList.size());
        for (Object srcItem : itemList) {
            result.add(prepareItem(srcItem));
        }
        wsHelper.countPositions(result);
        return result;
    }

    private ArrStructuredItem prepareItem(Object srcItem) {
        ArrStructuredItem si = new ArrStructuredItem();
        wsHelper.convertItem(si, srcItem);
        return si;
    }

    @Override
    @Transactional
    public void updateStructuredObject(StructuredObject updateStructuredObject) throws UpdateStructuredObjectFailed {
        try {
            ArrStructuredObject structObj = findStructObj(updateStructuredObject.getId(), updateStructuredObject
                    .getUuid());

            ArrChange change = arrangementInternalService.createChange(ArrChange.Type.UPDATE_STRUCT_DATA_BATCH);
            List<ArrStructuredItem> items = prepareItems(updateStructuredObject.getItems());

            structObjService.updateStructObj(change, structObj, items);
        } catch (Exception e) {
            logger.error("Failed to update structured object: {}", e.getMessage(), e);
            throw prepareUpdateException("Failed to update structured object.", updateStructuredObject, e);
        }

    }

    private UpdateStructuredObjectFailed prepareUpdateException(String msg, StructuredObject updateStructuredObject,
                                                                Exception e) {
        ErrorDescription ed = new ErrorDescription();
        ed.setUserMessage(msg);
        if (e != null) {
            ed.setDetail(e.getMessage());
        }

        List<String> ids = new ArrayList<>(2);
        if (StringUtils.isNotEmpty(updateStructuredObject.getId())) {
            ids.add("id: " + updateStructuredObject.getId());
        }
        if (StringUtils.isNotEmpty(updateStructuredObject.getUuid())) {
            ids.add("uuid: " + updateStructuredObject.getUuid());
        }
        ed.setDetail(String.join(", ", ids));

        return new UpdateStructuredObjectFailed(msg, ed, e);
    }

}
