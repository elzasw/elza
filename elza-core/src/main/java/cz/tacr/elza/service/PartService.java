package cz.tacr.elza.service;

import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.groovy.GroovyKeyValue;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApKeyValueRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.PartTypeRepository;
import cz.tacr.elza.service.event.AccessPointQueueEvent;
import cz.tacr.elza.service.vo.DataRef;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;
import static cz.tacr.elza.groovy.GroovyResult.PT_PREFER_NAME;
import static cz.tacr.elza.repository.ExceptionThrow.part;

@Service
public class PartService {

    private final ApPartRepository partRepository;
    private final PartTypeRepository partTypeRepository;
    private final ApItemRepository itemRepository;
    private final AccessPointItemService apItemService;
    private final AccessPointDataService apDataService;
    private final ApKeyValueRepository keyValueRepository;
    private final ApIndexRepository indexRepository;
    private final DataRecordRefRepository dataRecordRefRepository;
    private final ApAccessPointRepository accessPointRepository;
    private ApplicationEventPublisher eventPublisher;

    private static final Logger logger = LoggerFactory.getLogger(PartService.class);

    private final String DUPLICITA = " duplicitní key value ";

    @Autowired
    public PartService(final ApPartRepository partRepository,
                       final PartTypeRepository partTypeRepository,
                       final ApItemRepository itemRepository,
                       final AccessPointItemService apItemService,
                       final AccessPointDataService apDataService,
                       final ApKeyValueRepository keyValueRepository,
                       final ApIndexRepository indexRepository,
                       final DataRecordRefRepository dataRecordRefRepository,
                       final ApAccessPointRepository apAccessPointRepository,
                       final ApplicationEventPublisher eventPublisher) {
        this.partRepository = partRepository;
        this.partTypeRepository = partTypeRepository;
        this.itemRepository = itemRepository;
        this.apItemService = apItemService;
        this.apDataService = apDataService;
        this.keyValueRepository = keyValueRepository;
        this.indexRepository = indexRepository;
        this.dataRecordRefRepository = dataRecordRefRepository;
        this.accessPointRepository = apAccessPointRepository;
        this.eventPublisher = eventPublisher;
    }

    public ApPart createPart(final RulPartType partType,
                             final ApAccessPoint accessPoint,
                             final ApChange createChange,
                             final ApPart parentPart) {
        Validate.notNull(partType, "Typ partu musí být vyplněn");

        ApPart part = new ApPart();
        part.setPartType(partType);
        part.setState(ApStateEnum.OK);
        part.setAccessPoint(accessPoint);
        part.setCreateChange(createChange);
        part.setParentPart(parentPart);

        return partRepository.save(part);
    }

    public ApPart createPart(final ApPart oldPart,
                             final ApChange createChange) {
        ApPart part = new ApPart();
        part.setPartType(oldPart.getPartType());
        part.setState(oldPart.getState());
        part.setAccessPoint(oldPart.getAccessPoint());
        part.setCreateChange(createChange);
        part.setParentPart(oldPart.getParentPart());
        part.setKeyValue(oldPart.getKeyValue());

        return partRepository.save(part);
    }

    public void changeParentPart(final ApPart oldPart,
                                 final ApPart newPart) {
        List<ApPart> partList = findPartsByParentPart(oldPart);
        for (ApPart part : partList) {
            part.setParentPart(newPart);
        }
        partRepository.saveAll(partList);
    }

    /**
     * Provede založení části.
     *
     * @param accessPoint přístupový bod
     * @param apPartFormVO data k založení
     */
    public ApPart createPart(final ApAccessPoint accessPoint,
                             final ApPartFormVO apPartFormVO) {
        ApPart parentPart = apPartFormVO.getParentPartId() == null ? null : getPart(apPartFormVO.getParentPartId());

        if (parentPart != null && parentPart.getParentPart() != null) {
            throw new IllegalArgumentException("Nadřazená část nesmí zároveň být podřazená část");
        }

        if (CollectionUtils.isEmpty(apPartFormVO.getItems())) {
            throw new IllegalArgumentException("Část musí mít alespoň jeden prvek popisu");
        }

        RulPartType partType = getPartTypeByCode(apPartFormVO.getPartTypeCode());
        ApChange apChange = apDataService.createChange(ApChange.Type.AP_CREATE);

        ApPart newPart = createPart(partType, accessPoint, apChange, parentPart);
        createPartItems(apChange, newPart, apPartFormVO, null, null);
        return newPart;
    }

    private RulPartType getPartTypeByCode(final String partTypeCode) {
        RulPartType partType = partTypeRepository.findByCode(partTypeCode);
        if (partType == null) {
            throw new ObjectNotFoundException("Typ části neexistuje: " + partTypeCode, BaseCode.ID_NOT_EXIST).setId(partTypeCode);
        }
        return partType;
    }

    public ApPart getPart(final Integer partId) {
        Validate.notNull(partId);
        return partRepository.findById(partId)
                .orElseThrow(part(partId));
    }

    /**
     * Založí atributy části.
     *
     * @param apChange změna
     * @param apPart část
     * @param apPartFormVO data k založení
     * @return
     */
    public List<ApItem> createPartItems(final ApChange apChange,
                                        final ApPart apPart,
                                        final ApPartFormVO apPartFormVO,
                                        final List<ApBindingItem> bindingItemList,
                                        final List<DataRef> dataRefList) {
        List<ApItem> itemsDb = new ArrayList<>();
        Map<Integer, List<ApItem>> typeIdItemsMap = new HashMap<>();
        List<ApItem> items = apItemService.createItems(apPartFormVO.getItems(), typeIdItemsMap, itemsDb, apChange, bindingItemList, dataRefList, (RulItemType it, RulItemSpec is, ApChange c, int objectId, int position)
                -> createPartItem(apPart, it, is, c, objectId, position));
        return itemRepository.saveAll(items);
    }

    public List<ApItem> createPartItems(final ApChange apChange,
                                        final ApPart apPart,
                                        final List<Object> itemList,
                                        final ApBinding binding,
                                        final List<DataRef> dataRefList) {
        List<ApItem> items = apItemService.createItems(itemList, apChange, binding, dataRefList, (RulItemType it, RulItemSpec is, ApChange c, int objectId, int position)
                -> createPartItem(apPart, it, is, c, objectId, position));
        return itemRepository.saveAll(items);
    }

    private ApItem createPartItem(final ApPart part, final RulItemType it, final RulItemSpec is, final ApChange c, final int objectId, final int position) {
        ApItem item = new ApItem();
        item.setPart(part);
        item.setItemType(it);
        item.setItemSpec(is);
        item.setCreateChange(c);
        item.setObjectId(objectId);
        item.setPosition(position);
        return item;
    }

    /**
     * Odstraní část
     *
     * @param apPart část
     * @param apChange změna
     */
    public void deletePart(ApPart apPart, ApChange apChange) {
        apPart.setDeleteChange(apChange);
        partRepository.save(apPart);
    }

    /**
     * Odstraní části
     *
     * @param partList seznam částí
     * @param apChange změna
     */
    public void deleteParts(List<ApPart> partList, ApChange apChange) {
        for (ApPart part : partList) {
            part.setDeleteChange(apChange);
        }
        partRepository.saveAll(partList);
    }

    public void deleteParts(final ApAccessPoint accessPoint, final ApChange apChange) {
        List<ApPart> partList = partRepository.findValidPartByAccessPoint(accessPoint);
        for (ApPart part : partList) {
            apItemService.deletePartItems(part, apChange);
            deletePart(part, apChange);
        }
    }

    /**
     * Odstraní část
     *
     * @param accessPoint přístupový bod
     * @param partId identifikátor části
     */
    public void deletePart(final ApAccessPoint accessPoint, final Integer partId) {
        if (accessPoint.getPreferredPart().getPartId().equals(partId)) {
            throw new IllegalArgumentException("Preferované jméno nemůže být odstraněno");
        }
        ApPart apPart = getPart(partId);

        if (partRepository.countApPartsByParentPartAndDeleteChangeIsNull(apPart) > 0) {
            throw new IllegalArgumentException("Nelze smazat part, který má aktivní návazné party");
        }

        ApChange apChange = apDataService.createChange(ApChange.Type.AP_DELETE);
        apItemService.deletePartItems(apPart, apChange);
        apPart.setDeleteChange(apChange);
        partRepository.save(apPart);
    }

    public List<ApPart> findPartsByAccessPoint(ApAccessPoint accessPoint) {
        return partRepository.findValidPartByAccessPoint(accessPoint);
    }

    public List<ApPart> findPartsByParentPart(ApPart part) {
        return partRepository.findPartsByParentPartAndDeleteChangeIsNull(part);
    }

    public List<ApPart> findNewerPartsByAccessPoint(ApAccessPoint accessPoint, Integer changeId) {
        return partRepository.findNewerValidPartsByAccessPoint(accessPoint, changeId);
    }

    public boolean updatePartValue(ApPart apPart, GroovyResult result, ApState state, boolean async) {
        ApScope scope = state.getScope();
        Integer accessPointId = state.getAccessPoint().getAccessPointId();
        boolean preferredPart = false;
        if(apPart.getKeyValue() != null && apPart.getKeyValue().getKeyType().equals("PT_PREFER_NAME")) {
            preferredPart = true;
        }

        boolean success = true;
        Map<String, String> indexMap = result.getIndexes();

        String displayName = indexMap != null ? indexMap.get(DISPLAY_NAME) : null;
        if (displayName == null) {
            throw new SystemException("Povinný index typu [" + DISPLAY_NAME + "] není vyplněn");
        }

        if (!displayName.equals(apPart.getValue())) {
            apPart.setValue(displayName);
            partRepository.save(apPart);
        }

        GroovyKeyValue keyValue = result.getKeyValue();
        String keyType = null;

        if (keyValue != null) {

            keyType = StringUtils.stripToNull(keyValue.getKey());
            if (keyType == null) {
                throw new SystemException("Neplatný typ ApKeyValue").set("keyType", keyType);
            }
            String value = StringUtils.stripToNull(keyValue.getValue());
            if (value == null) {
                throw new SystemException("Neplatná hodnota ApKeyValue").set("keyType", keyType).set("value", value);
            }
            if (value.length() > StringLength.LENGTH_4000) {
                value = value.substring(0, StringLength.LENGTH_4000 - 1);
                logger.warn("Hodnota keyValue byla příliš dlouhá, byla oříznuta: partId={}, keyValue={}", apPart.getPartId(), value);
            }
            value = value.toLowerCase();

            if (apPart.getKeyValue() != null) {
                ApKeyValue apKeyValue = apPart.getKeyValue();

                if ((!apKeyValue.getKeyType().equals(keyType) ||
                        !apKeyValue.getValue().equals(value) ||
                        !apKeyValue.getScope().getScopeId().equals(scope.getScopeId()))
                        && !checkKeyValueUnique(keyType, value, scope, async)) {
                    value = value + DUPLICITA + accessPointId;
                    success = false;
                }

                apKeyValue.setKeyType(keyType);
                apKeyValue.setValue(value);
                apKeyValue.setScope(scope);
                keyValueRepository.save(apKeyValue);
            } else {
                if (!checkKeyValueUnique(keyType, value, scope, async)) {
                    value = value + DUPLICITA + accessPointId;
                    success = false;
                }

                ApKeyValue apKeyValue = new ApKeyValue();
                apKeyValue.setKeyType(keyType);
                apKeyValue.setValue(value);
                apKeyValue.setScope(scope);
                keyValueRepository.save(apKeyValue);

                apPart.setKeyValue(apKeyValue);
                partRepository.save(apPart);
            }

        } else {
            ApKeyValue apKeyValue = apPart.getKeyValue();
            if (apKeyValue != null) {
                apPart.setKeyValue(null);
                partRepository.save(apPart);
                keyValueRepository.delete(apKeyValue);
            }
        }

        Map<String, ApIndex> apIndexMapByType = indexRepository.findByPartId(apPart.getPartId()).stream()
                .collect(Collectors.toMap(ApIndex::getIndexType, Function.identity()));

        for (Map.Entry<String, String> entry : indexMap.entrySet()) {

            String indexType = StringUtils.stripToNull(entry.getKey());
            if (indexType == null) {
                throw new SystemException("Neplatný typ indexu ApIndex").set("indexType", indexType);
            }

            String value = entry.getValue();
            if (value == null) {
                throw new SystemException("Neplatná hodnota indexu ApIndex").set("indexType", indexType).set("value", value);
            }

            if (value.length() > StringLength.LENGTH_4000) {
                value = value.substring(0, StringLength.LENGTH_4000 - 1);
                logger.warn("Hodnota indexu byla příliš dlouhá, byla oříznuta: partId={}, indexType={}, value={}", apPart.getPartId(), indexType, value);
            }

            ApIndex apIndex = apIndexMapByType.remove(indexType);

            if (preferredPart && indexType.equals(DISPLAY_NAME)) {
                if(!value.equals(apIndex.getValue())) {
                    //přegenerování entit, které odkazují na entitu, které se mění preferované jméno
                    checkReferredRecords(apPart);
                }
            }

            if (!success && keyType.equals(PT_PREFER_NAME) && indexType.equals(DISPLAY_NAME)) {
                value = value + DUPLICITA + accessPointId;
            }

            if (apIndex == null) {
                apIndex = new ApIndex();
                apIndex.setPart(apPart);
                apIndex.setIndexType(indexType);
                apIndex.setValue(value);
                indexRepository.save(apIndex);
            } else {
                if (!value.equals(apIndex.getValue())) {
                    apIndex.setValue(value);
                    indexRepository.save(apIndex);
                }
            }
        }

        // smazat to, co zbylo
        if (!apIndexMapByType.isEmpty()) {
            indexRepository.deleteAll(apIndexMapByType.values());
        }
        return success;
    }

    private void checkReferredRecords(ApPart apPart) {
        ApAccessPoint accessPoint = apPart.getAccessPoint();
        List<Integer> dataIdsList = dataRecordRefRepository.findIdsByRecord(accessPoint);

        if(CollectionUtils.isNotEmpty(dataIdsList)) {
            List<ApAccessPoint> accessPoints = accessPointRepository.findAccessPointsByRefDataId(dataIdsList);
            if (CollectionUtils.isNotEmpty(accessPoints)) {
                AccessPointQueueEvent accessPointQueueEvent = new AccessPointQueueEvent(accessPoints);
                eventPublisher.publishEvent(accessPointQueueEvent);
            }
        }
    }

    private boolean checkKeyValueUnique(String keyType, String value, ApScope scope, boolean async) {
        ApKeyValue apKeyValue = keyValueRepository.findByKeyTypeAndValueAndScope(keyType, value, scope);

        if (!async && apKeyValue != null) {
            throw new BusinessException("ApKeyValue s tímto typem a hodnotou a scope už existuje.", BaseCode.PROPERTY_IS_INVALID)
                    .set("keyType", keyType)
                    .set("value", value)
                    .set("scope", scope);
        }

        return apKeyValue == null;
    }
}
