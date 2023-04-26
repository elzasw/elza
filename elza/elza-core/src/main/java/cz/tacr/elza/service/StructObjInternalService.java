package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ArrStructuredObject.State;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.RulStructuredType;
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

/**
 * Interní servisní třída pro práci se strukturovanými datovými typy.
 *
 * @since 14.04.2020
 */
@Service
public class StructObjInternalService {

    private final static Logger logger = LoggerFactory.getLogger(StructObjInternalService.class);

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
     * Smazání hodnot strukturovaného datového typu.
     *
     * @param structObjs seznam hodnot struktovaného datového typu
     * @param changeOverride přetížená změna
     * @return List<Integer>
     */
    public List<Integer> deleteStructObj(final List<ArrStructuredObject> structObjs, @Nullable final ArrChange changeOverride) {
        List<ArrStructuredObject> tempStructObj = new ArrayList<>(); 
        List<ArrStructuredObject> permStructObj = new ArrayList<>();
        List<String> sortValues = new ArrayList<>();
        List<Integer> deletedIds = new ArrayList<>();

        ArrStructuredObject firstStructObj = structObjs.get(0);
        ArrFund fund = firstStructObj.getFund();
        RulStructuredType structuredType = firstStructObj.getStructuredType();

        logger.debug("Creating two lists according to objects status");

        // vytvoření 2 seznamů podle stavu objektu
        for (ArrStructuredObject structObj : structObjs) {
            if (structObj.getDeleteChange() != null) {
                throw new BusinessException("Nelze odstranit již smazaná strukturovaná data", BaseCode.INVALID_STATE);
            }
            if (!structObj.getFundId().equals(fund.getFundId())) {
                throw new BusinessException("All structured objects have to be from same fund", BaseCode.INVALID_STATE)
                        .set("fundId", fund.getFundId())
                        .set("structuredObjectId", structObj.getStructuredObjectId())
                        .set("structObjFundId", structObj.getFundId());
            }
            if (!structObj.getStructuredTypeId().equals(structuredType.getStructuredTypeId())) {
                throw new BusinessException("All structured object have to have same type", BaseCode.INVALID_STATE)
                        .set("structuredTypeId", structuredType.getStructuredTypeId())
                        .set("structuredObjectId", structObj.getStructuredObjectId())
                        .set("otherStructuredTypeId", structObj.getStructuredTypeId());
            }
            if (structObj.getState() == State.TEMP) {
                tempStructObj.add(structObj);
            } else {
                sortValues.add(structObj.getSortValue());
                permStructObj.add(structObj);
            }
        }

        logger.debug("Two lists were created: temp.size={}, permanent.size={}", tempStructObj.size(), permStructObj.size());

        // vymazání 'temporary' objektů
        if (!tempStructObj.isEmpty()) {
            for (ArrStructuredObject structObj : tempStructObj) {
                structureItemRepository.deleteByStructuredObject(structObj);
                dataRepository.deleteByStructuredObject(structObj);
                ArrChange change = structObjRepository.findTempChangeByStructuredObject(structObj);
                structObjRepository.delete(structObj);
                changeRepository.delete(change);
                deletedIds.add(structObj.getStructuredObjectId());
            }

            logger.debug("Removed {} temporary objects", tempStructObj.size());
        }

        // vymazání 'permanent' objektů
        if (!permStructObj.isEmpty()) {
            ArrChange change = changeOverride == null
                    ? arrangementInternalService.createChange(ArrChange.Type.DELETE_STRUCTURE_DATA)
                    : changeOverride;

            // kontrolujeme použití
            List<Integer> userStructObjIds = new ArrayList<>(); 
            ObjectListIterator.forEachPage(permStructObj, 
                                           page -> userStructObjIds.addAll(structureItemRepository.findUsedStructuredObjectIds(page)));
            if (!userStructObjIds.isEmpty()) {
                throw new BusinessException("Existují návazné jednotky popisu, objekt(y) nelze smazat.", ArrangementCode.STRUCTURE_DATA_DELETE_ERROR)
                        .level(Level.WARNING)
                        .set("count", userStructObjIds.size())
                        .set("ids", userStructObjIds);
            }

            for (ArrStructuredObject structObj : permStructObj) {
                structObj.setDeleteChange(change);
                deletedIds.add(structObj.getStructuredObjectId());
            }
            structObjRepository.saveAll(permStructObj);

            logger.debug("Removed {} permanent objects", permStructObj.size());

            // hledáme duplikáty
            List<ArrStructuredObject> structuredObjectsDup = new ArrayList<>();
            ObjectListIterator
                .forEachPage(sortValues, 
                             page -> structuredObjectsDup.addAll(structObjRepository.findErrorByStructureTypeAndFund(structuredType, fund, page)));
            structuredObjectsDup.forEach(structObj -> structObjService.addToValidate(structObj));

            logger.debug("Processed {} duplicates objects", structuredObjectsDup.size());

            notificationService.publishEvent(new EventStructureDataChange(fund.getFundId(),
                                                                          structuredType.getCode(),
                                                                          null, null, null,
                                                                          deletedIds));
        }
        return deletedIds;
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
