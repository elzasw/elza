package cz.tacr.elza.ws.core.v1;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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

    @Autowired
    StructObjService structObjService;

    @Autowired
    ArrangementService arrangementService;

    @Autowired
    StaticDataService staticDataService;

    @Autowired
    WSHelper wsHelper;

    @Override
    @Transactional
    public void deleteStructuredObject(StructuredObjectIdentifiers deleteStructuredObj)
            throws DeleteStructuredObjectFailed {

        try {
            ArrStructuredObject structObj;
            if (deleteStructuredObj.getId() != null) {
                Integer stuctObjId = Integer.valueOf(deleteStructuredObj.getId());
                structObj = structObjService.getStructObjById(stuctObjId);

                Validate.notNull(structObj, "Structured object not found, id: %i", deleteStructuredObj.getId());
            }
            else {
                // lookup with uuid
                Validate.isTrue(deleteStructuredObj.getUuid()!=null, "Structured object identifier is missing (id nor uuid provided).");

                structObj = structObjService.getExistingStructObjByUUID(deleteStructuredObj.getUuid());
            }
            structObjService.deleteStructObj(structObj);
        } catch (Exception e)
        {
            throw prepareDeleteException("Failed to delete structured object.", deleteStructuredObj);
        }
    }

    DeleteStructuredObjectFailed prepareDeleteException(String msg, StructuredObjectIdentifiers deleteStructuredObj) {
        ErrorDescription ed = new ErrorDescription();
        ed.setUserMessage(msg);
        
        List<String> ids = new ArrayList<>(2);
        if (StringUtils.isNotEmpty(deleteStructuredObj.getId())) {
            ids.add("id: " + deleteStructuredObj.getId());
        }
        if (StringUtils.isNotEmpty(deleteStructuredObj.getUuid())) {
            ids.add("uuid: " + deleteStructuredObj.getUuid());
        }
        ed.setDetail(String.join(", ", ids));

        return new DeleteStructuredObjectFailed(msg, ed);
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

        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_STRUCTURE_DATA);
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
    public void updateStructuredObject(StructuredObject updateStructuredObject) throws UpdateStructuredObjectFailed {
        // TODO Auto-generated method stub

    }

}
