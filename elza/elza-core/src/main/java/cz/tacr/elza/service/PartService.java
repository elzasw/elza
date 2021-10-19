package cz.tacr.elza.service;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;
import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME_LOWER;
import static cz.tacr.elza.groovy.GroovyResult.PT_PREFER_NAME;
import static cz.tacr.elza.repository.ExceptionThrow.part;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApKeyValue;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.groovy.GroovyKeyValue;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApKeyValueRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.PartTypeRepository;
import cz.tacr.elza.service.AccessPointItemService.DeletedItems;
import cz.tacr.elza.service.AccessPointItemService.ReferencedEntities;
import cz.tacr.elza.service.vo.DataRef;

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
    private final AsyncRequestService asyncRequestService;
    private final ApBindingItemRepository bindingItemRepository;

    private static final Logger logger = LoggerFactory.getLogger(PartService.class);

    private static final String DUPLICITA = " duplicitní key value ";

    private static final KeyValueLock keyValueLock = new KeyValueLock();

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
                       final AsyncRequestService asyncRequestService,
                       final ApBindingItemRepository bindingItemRepository) {
        this.partRepository = partRepository;
        this.partTypeRepository = partTypeRepository;
        this.itemRepository = itemRepository;
        this.apItemService = apItemService;
        this.apDataService = apDataService;
        this.keyValueRepository = keyValueRepository;
        this.indexRepository = indexRepository;
        this.dataRecordRefRepository = dataRecordRefRepository;
        this.accessPointRepository = apAccessPointRepository;
        this.asyncRequestService = asyncRequestService;
        this.bindingItemRepository = bindingItemRepository;
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
        part.setLastChange(createChange);
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
        part.setLastChange(createChange);
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
        apItemService.createItems(newPart, apPartFormVO.getItems(), apChange, null, null);
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
     * Odstraní část přímo z DB
     * 
     * Pozor: Neodstraňuje bindings
     * 
     * Pro vymazání partu včetně biding je nutné použít jiné metody
     *
     * @param apPart
     *            část
     * @param apChange
     *            změna
     */
    private void deletePart(ApPart part, ApChange apChange) {
        ApKeyValue keyValue = part.getKeyValue();
        if (keyValue != null) {
            keyValueRepository.delete(keyValue);
        }
        part.setDeleteChange(apChange);
        part.setLastChange(apChange);
        part.setKeyValue(null);
        partRepository.save(part);
    }

    /**
     * Odstraní části
     * 
     * Odstraní jen samotné části bez prvků popisu
     *
     * @param partList
     *            seznam částí
     * @param apChange
     *            změna
     */
    public void deleteParts(List<ApPart> partList, ApChange apChange) {
        if (CollectionUtils.isEmpty(partList)) {
            return;
        }
        List<ApKeyValue> keyValues = new ArrayList<>();
        for (ApPart part : partList) {
            if (part.getKeyValue() != null) {
                keyValues.add(part.getKeyValue());
            }
            part.setDeleteChange(apChange);
            part.setKeyValue(null);
        }
        partRepository.saveAll(partList);
        if (CollectionUtils.isNotEmpty(keyValues)) {
            keyValueRepository.deleteAll(keyValues);
        }
    }

    public void deleteParts(final ApAccessPoint accessPoint, final ApChange apChange) {
        List<ApPart> partList = partRepository.findValidPartByAccessPoint(accessPoint);
        if (CollectionUtils.isNotEmpty(partList)) {
            for (ApPart part : partList) {
                apItemService.deletePartItems(part, apChange);
                deletePart(part, apChange);
            }
        }
    }

    /**
     * Odstraní část
     *
     * @param accessPoint přístupový bod
     * @param partId identifikátor části
     */
    public void deletePart(final ApAccessPoint accessPoint, final Integer partId) {
        if (accessPoint.getPreferredPartId().equals(partId)) {
            throw new IllegalArgumentException("Preferované jméno nemůže být odstraněno");
        }
        ApPart apPart = getPart(partId);

        if (partRepository.countApPartsByParentPartAndDeleteChangeIsNull(apPart) > 0) {
            throw new IllegalArgumentException("Nelze smazat part, který má aktivní návazné party");
        }

        ApChange apChange = apDataService.createChange(ApChange.Type.AP_DELETE);
        ApKeyValue keyValue = apPart.getKeyValue();
        apItemService.deletePartItems(apPart, apChange);
        
        // Delete bindings
        List<ApBindingItem> bindingParts = this.bindingItemRepository.findByPart(apPart);
        if (bindingParts.size() > 0) {
            for (ApBindingItem bindingItem : bindingParts) {
                bindingItem.setDeleteChange(apChange);
            }
            bindingParts = bindingItemRepository.saveAll(bindingParts);
        }

        deletePart(apPart, apChange);
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

    public ApPart findFirstPartByCode(String code, List<ApPart> parts) {
        for (ApPart part : parts) {
            if (part.getPartType().getCode().equals(code)) {
                return part;
            }
        }
        return null;
    }

    /**
     * Update part key value
     * 
     * @param apPart
     *            should be with fetched key value
     * @param result
     * @param state
     * @param scope
     *            Scope of accesspoint
     * @param async
     * @param preferredPart
     *            Flag if part is preferred name
     * @return
     */
    public boolean updatePartValue(ApPart apPart,
                                   GroovyResult result,
                                   ApState state,
                                   ApScope scope,
                                   boolean async,
                                   boolean preferredPart) {
        Integer accessPointId = state.getAccessPointId();
        ApAccessPoint accessPoint = state.getAccessPoint();
        Validate.notNull(accessPoint);

        boolean oldPreferredPart = false;
        if(apPart.getKeyValue() != null && apPart.getKeyValue().getKeyType().equals(PT_PREFER_NAME)) {
            oldPreferredPart = true;
        }

        boolean success = true;
        Map<String, String> indexMap = result.getIndexes();

        String displayName = indexMap != null ? indexMap.get(DISPLAY_NAME) : null;
        if (displayName == null) {
            throw new SystemException("Povinný index typu [" + DISPLAY_NAME + "] není vyplněn");
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
                throw new SystemException("Prázdná hodnota pro ApKeyValue").set("keyType", keyType);
            }
            if (value.length() > StringLength.LENGTH_4000) {
                value = value.substring(0, StringLength.LENGTH_4000 - 1);
                logger.warn("Hodnota keyValue byla příliš dlouhá, byla oříznuta: partId={}, keyValue={}", apPart.getPartId(), value);
            }
            value = value.toLowerCase();

            try {
                if (apPart.getKeyValue() != null) {
                    ApKeyValue apKeyValue = apPart.getKeyValue();

                    if ((!apKeyValue.getKeyType().equals(keyType) ||
                            !apKeyValue.getValue().equals(value) ||
                            !apKeyValue.getScope().getScopeId().equals(scope.getScopeId()))
                            && (!checkKeyValueUnique(keyType, value, scope, async) ||
                            !keyValueLock.addIfExists(keyType, value, scope.getScopeId()))) {
                        value = value + DUPLICITA + accessPointId;
                        success = false;
                    }

                    apKeyValue.setKeyType(keyType);
                    apKeyValue.setValue(value);
                    apKeyValue.setScope(scope);
                    keyValueRepository.save(apKeyValue);
                } else {
                    if (!checkKeyValueUnique(keyType, value, scope, async) ||
                            !keyValueLock.addIfExists(keyType, value, scope.getScopeId())) {
                        value = value + DUPLICITA + accessPointId;
                        success = false;
                    }

                    ApKeyValue apKeyValue = new ApKeyValue();
                    apKeyValue.setKeyType(keyType);
                    apKeyValue.setValue(value);
                    apKeyValue.setScope(scope);
                    apKeyValue = keyValueRepository.save(apKeyValue);

                    apPart.setKeyValue(apKeyValue);
                    partRepository.save(apPart);
                }
            } finally {
                keyValueLock.delete(keyType, value, scope.getScopeId());
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

            if (indexType.equals(DISPLAY_NAME)) {
                if ((oldPreferredPart && !value.equals(apIndex.getValue()))
                        || (preferredPart && (apIndex == null || !value.equals(apIndex.getValue())))
                        || (preferredPart && !oldPreferredPart)) {
                    //přegenerování entit, které odkazují na entitu, které se mění preferované jméno
                    checkReferredRecords(accessPoint);
                }
            }

            if (!success && keyType.equals(PT_PREFER_NAME) && (indexType.equals(DISPLAY_NAME) || indexType.equals(DISPLAY_NAME_LOWER))) {
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

    private void checkReferredRecords(ApAccessPoint accessPoint) {
        List<Integer> dataIdsList = dataRecordRefRepository.findIdsByRecord(accessPoint);

        if (CollectionUtils.isNotEmpty(dataIdsList)) {
            List<Integer> accessPointIds = ObjectListIterator.findIterable(dataIdsList, accessPointRepository::findAccessPointIdsByRefDataId);
            if (accessPointIds.remove(accessPoint.getAccessPointId())) {
                logger.warn("Archivní entita id " + accessPoint.getAccessPointId() + " má referenci sama na sebe!");
            }
            if (CollectionUtils.isNotEmpty(accessPointIds)) {
                ObjectListIterator.forEachPage(accessPointIds, accessPointRepository::updateToInit);
                asyncRequestService.enqueue(accessPointIds);
            }
        }
    }

    private boolean checkKeyValueUnique(String keyType, String value, ApScope scope, boolean async) {
        ApKeyValue apKeyValue = keyValueRepository.findByKeyTypeAndValueAndScope(keyType, value, scope);

        if (!async && apKeyValue != null) {
            throw new BusinessException("ApKeyValue s tímto typem a hodnotou a scopeId už existuje.", RegistryCode.NOT_UNIQUE_FULL_NAME)
                    .set("keyType", keyType)
                    .set("value", value)
                    .set("scopeId", scope.getScopeId());
        }

        return apKeyValue == null;
    }

    /**
     * Třída pro kontrolu keyValue v případě souběhu
     */
    private static class KeyValueLock {

        private Set<String> uniqueIds;
        private final Object lock = new Object();

        public KeyValueLock() {
            this.uniqueIds = new HashSet<>();
        }

        /**
         * Přidání keyValue do setu
         * @param keyType typ klíče
         * @param value hodnota klíče
         * @param scopeId identifikátor scope
         * @return true pokud keyValue v setu ještě neexistovala
         */
        public boolean addIfExists(String keyType, String value, Integer scopeId) {
           synchronized (lock) {
               return uniqueIds.add(keyType + value + scopeId);
           }
        }

        /**
         * Smazání keyValue z setu
         * @param keyType typ klíče
         * @param value hodnota klíče
         * @param scopeId identifikátor scope
         */
        public void delete(String keyType, String value, Integer scopeId) {
            synchronized (lock) {
                uniqueIds.remove(keyType + value + scopeId);
            }
        }
    }
}
