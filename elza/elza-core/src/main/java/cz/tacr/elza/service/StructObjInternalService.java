package cz.tacr.elza.service;

import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ArrStructuredObject.State;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.PartTypeRepository;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventStructureDataChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Interní servisní třída pro práci se strukturovanými datovými typy.
 *
 * @since 14.04.2020
 */
@Service
public class StructObjInternalService {

    private final StructuredItemRepository structureItemRepository;
    private final StructuredObjectRepository structObjRepository;
    private final ArrangementInternalService arrangementInternalService;
    private final DataRepository dataRepository;
    private final StructObjValueService structObjService;
    private final ChangeRepository changeRepository;
    private final EventNotificationService notificationService;
    private final PartTypeRepository partTypeRepository;

    @Autowired
    public StructObjInternalService(final StructuredItemRepository structureItemRepository,
                                    final StructuredObjectRepository structureDataRepository,
                                    final ArrangementInternalService arrangementInternalService,
                                    final DataRepository dataRepository,
                                    final StructObjValueService structureDataService,
                                    final ChangeRepository changeRepository,
                                    final EventNotificationService notificationService,
                                    final PartTypeRepository partTypeRepository) {
        this.structureItemRepository = structureItemRepository;
        this.structObjRepository = structureDataRepository;
        this.arrangementInternalService = arrangementInternalService;
        this.dataRepository = dataRepository;
        this.structObjService = structureDataService;
        this.changeRepository = changeRepository;
        this.notificationService = notificationService;
        this.partTypeRepository = partTypeRepository;
    }

    /**
     * Smazání hodnoty strukturovaného datového typu.
     *
     * @param structObj hodnota struktovaného datového typu
     */
    public void deleteStructObj(@AuthParam(type = AuthParam.Type.FUND) final ArrStructuredObject structObj) {
        if (structObj.getDeleteChange() != null) {
            throw new BusinessException("Nelze odstranit již smazaná strukturovaná data", BaseCode.INVALID_STATE);
        }

        if (structObj.getState() == State.TEMP) {

            // remove temporary object
            structureItemRepository.deleteByStructuredObject(structObj);
            dataRepository.deleteByStructuredObject(structObj);
            ArrChange change = structObjRepository.findTempChangeByStructuredObject(structObj);
            structObjRepository.delete(structObj);
            changeRepository.delete(change);

        } else {

            // drop permanent object

            // check usage
            Integer count = structureItemRepository.countItemsUsingStructObj(structObj);
            if (count > 0) {
                throw new BusinessException("Existují návazné entity, položka nelze smazat", ArrangementCode.STRUCTURE_DATA_DELETE_ERROR)
                        .level(Level.WARNING)
                        .set("count", count)
                        .set("id", structObj.getStructuredObjectId());
            }

            ArrChange change = arrangementInternalService.createChange(ArrChange.Type.DELETE_STRUCTURE_DATA);
            structObj.setDeleteChange(change);

            structObjRepository.save(structObj);

            // check duplicates for deleted item
            // find potentially duplicated items
            List<ArrStructuredObject> potentialDuplicates = structObjRepository
                    .findValidByStructureTypeAndFund(structObj.getStructuredType(),
                            structObj.getFund(),
                            structObj.getSortValue(),
                            structObj);
            for (ArrStructuredObject pd : potentialDuplicates) {
                if (pd.getState().equals(State.ERROR)) {
                    structObjService.addToValidate(pd);
                }
            }

            notificationService.publishEvent(new EventStructureDataChange(structObj.getFundId(),
                    structObj.getStructuredType().getCode(),
                    null,
                    null,
                    null,
                    Collections.singletonList(structObj.getStructuredObjectId())));
        }
    }

    public RulPartType getPartTypeByCode(final String partTypeCode) {
        RulPartType partType = partTypeRepository.findByCode(partTypeCode);
        if (partType == null) {
            throw new ObjectNotFoundException("Typ části neexistuje: " + partTypeCode, BaseCode.ID_NOT_EXIST).setId(partTypeCode);
        }
        return partType;
    }

    public ArrStructuredObject deepCopy(final ArrStructuredObject structuredObject) {
        ArrStructuredObject copyStructuredObject = structObjRepository.save(structuredObject.makeCopyWithoutId());
        copyItems(structuredObject, copyStructuredObject);
        return copyStructuredObject;
    }

    private void copyItems(final ArrStructuredObject sourceStructuredObject, final ArrStructuredObject targetStructuredObject) {
        List<ArrStructuredItem> items = structureItemRepository.findByStructuredObjectAndDeleteChangeIsNullFetchData(sourceStructuredObject);
        List<ArrStructuredItem> copyItems = new ArrayList<>(items.size());
        for (ArrStructuredItem item : items) {
            ArrData newData = copyData(item);
            ArrStructuredItem arrStructuredItem = item.makeCopy();
            arrStructuredItem.setItemId(null);
            arrStructuredItem.setData(newData);
            arrStructuredItem.setStructuredObject(targetStructuredObject);
            arrStructuredItem.setDescItemObjectId(arrangementInternalService.getNextDescItemObjectId());
            copyItems.add(arrStructuredItem);
        }
        if (copyItems.size() > 0) {
            structureItemRepository.saveAll(copyItems);
        }
    }

    private ArrData copyData(final ArrStructuredItem item) {
        ArrData data = item.getData();
        ArrData newData = data;
        if (data != null) {
            newData = ArrData.makeCopyWithoutId(data);
            newData = dataRepository.save(newData);
        }
        return newData;
    }
}
