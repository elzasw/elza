package cz.tacr.elza.service;

import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ap.item.ApUpdateItemVO;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.PartTypeRepository;
import cz.tacr.elza.service.vo.DataRef;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;
import static cz.tacr.elza.repository.ExceptionThrow.part;

@Service
public class PartService {

    private final ApPartRepository partRepository;
    private final PartTypeRepository partTypeRepository;
    private final ApItemRepository itemRepository;
    private final AccessPointItemService apItemService;
    private final AccessPointDataService apDataService;

    @Autowired
    public PartService(final ApPartRepository partRepository,
                       final PartTypeRepository partTypeRepository,
                       final ApItemRepository itemRepository,
                       final AccessPointItemService apItemService,
                       final AccessPointDataService apDataService) {
        this.partRepository = partRepository;
        this.partTypeRepository = partTypeRepository;
        this.itemRepository = itemRepository;
        this.apItemService = apItemService;
        this.apDataService = apDataService;
    }

    public ApPart createPart(final RulPartType partType,
                             final ApAccessPoint accessPoint,
                             final ApChange createChange,
                             final ApPart parentPart) {
        Validate.notNull(partType, "Typ partu musí být vyplněn");

        ApPart part = new ApPart();
        part.setPartType(partType);
        part.setState(ApStateEnum.TEMP);
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

    public void updatePartValue(ApPart apPart, GroovyResult result) {
        Map<String, String> indexMap = result.getIndexes();

        String displayName = indexMap != null ? indexMap.get(DISPLAY_NAME) : null;
        if (displayName == null) {
            throw new SystemException("Povinný index typu [" + DISPLAY_NAME + "] není vyplněn");
        }

        if (!displayName.equals(apPart.getValue())) {
            apPart.setValue(displayName);
            partRepository.save(apPart);
        }
    }

    /**
     * Validace unikátnosti jména v daném scope.
     *
     * @param scope    třída
     * @param fullName validované jméno
     */
    public void validationNameUnique(final ApScope scope, final String fullName) {
        if (!isNameUnique(scope, fullName)) {
            throw new BusinessException("Celé jméno není unikátní v rámci oblasti", RegistryCode.NOT_UNIQUE_FULL_NAME)
                    .set("fullName", fullName)
                    .set("scopeId", scope.getScopeId());
        }
    }

    /**
     * Kontrola, zdali je jméno unikátní v daném scope.
     *
     * @param scope    třída
     * @param fullName validované jméno
     * @return true pokud je
     */
    public boolean isNameUnique(final ApScope scope, final String fullName) {
        Validate.notNull(scope, "Přístupový bod musí být vyplněn");
        Validate.notNull(fullName, "Plné jméno musí být vyplněno");
        int count = partRepository.countUniqueName(fullName, scope);
        return count <= 1;
    }
}
