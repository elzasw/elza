package cz.tacr.elza.service;

import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ap.item.ApUpdateItemVO;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
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

@Service
public class PartService {

    private final ApPartRepository partRepository;
    private final PartTypeRepository partTypeRepository;
    private final ApItemRepository itemRepository;
    private final ApChangeRepository changeRepository;
    private final AccessPointGeneratorService apGeneratorService;
    private final AccessPointItemService apItemService;
    private final AccessPointDataService apDataService;

    @Autowired
    public PartService(final ApPartRepository partRepository,
                       final PartTypeRepository partTypeRepository,
                       final ApItemRepository itemRepository,
                       final ApChangeRepository changeRepository,
                       final AccessPointGeneratorService apGeneratorService,
                       final AccessPointItemService apItemService,
                       final AccessPointDataService apDataService) {
        this.partRepository = partRepository;
        this.partTypeRepository = partTypeRepository;
        this.itemRepository = itemRepository;
        this.changeRepository = changeRepository;
        this.apGeneratorService = apGeneratorService;
        this.apItemService = apItemService;
        this.apDataService = apDataService;
    }

    public ApPart createPart(final RulPartType partType) {
        Validate.notNull(partType, "Typ fragmentu musí být vyplněn");

        ApPart part = new ApPart();
        part.setPartType(partType);
        part.setState(ApStateEnum.TEMP);

        return partRepository.save(part);
    }

    public ApPart createPart(final RulPartType partType,
                             final ApAccessPoint accessPoint,
                             final ApChange createChange,
                             final ApPart parentPart) {
        Validate.notNull(partType, "Typ fragmentu musí být vyplněn");

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
        partRepository.save(partList);
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
        createPartItems(apChange, newPart, apPartFormVO, null);
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
        ApPart part = partRepository.findOne(partId);
        if (part == null) {
            throw new ObjectNotFoundException("Part nenalezen", BaseCode.ID_NOT_EXIST)
                    .setId(partId);
        }
        return part;
    }

    public void changeFragmentItems(final ApPart part, final List<ApUpdateItemVO> items) {
        Validate.notNull(part, "Fragment musí být vyplněn");
        Validate.notEmpty(items, "Musí být alespoň jedna položka ke změně");

        List<ApItem> itemsDb = itemRepository.findValidItemsByPart(part);

        ApChange change = apDataService.createChange(ApChange.Type.FRAGMENT_CHANGE);
        apItemService.changeItems(items, new ArrayList<>(itemsDb), change, (RulItemType it, RulItemSpec is, ApChange c, int objectId, int position)
                -> createPartItem(part, it, is, c, objectId, position));

        apGeneratorService.generateAndSetResult(part);
    }

    /**
     * Smazání hodnot fragmentu podle typu.
     *
     * @param fragment fragment
     * @param itemType typu atributu
     */
    public void deleteFragmentItemsByType(final ApPart fragment, final RulItemType itemType) {
        Validate.notNull(fragment, "Fragment musí být vyplněn");
        Validate.notNull(itemType, "Typ musí být vyplněn");
        ApChange change = apDataService.createChange(ApChange.Type.FRAGMENT_CHANGE);

        //TODO fantis
        //apItemService.deleteItemsByType(itemRepository, fragment, itemType, change);
        apGeneratorService.generateAndSetResult(fragment);
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
                                        final List<ApBindingItem> bindingItemList) {
        List<ApItem> itemsDb = new ArrayList<>();
        Map<Integer, List<ApItem>> typeIdItemsMap = new HashMap<>();
        List<ApItem> items = apItemService.createItems(apPartFormVO.getItems(), typeIdItemsMap, itemsDb, apChange, bindingItemList, (RulItemType it, RulItemSpec is, ApChange c, int objectId, int position)
                -> createPartItem(apPart, it, is, c, objectId, position));
        return itemRepository.save(items);
    }

    public List<ApItem> createPartItems(final ApChange apChange,
                                        final ApPart apPart,
                                        final List<Object> itemList,
                                        final ApBinding binding,
                                        final List<DataRef> dataRefList) {
        List<ApItem> items = apItemService.createItems(itemList, apChange, binding, dataRefList, (RulItemType it, RulItemSpec is, ApChange c, int objectId, int position)
                -> createPartItem(apPart, it, is, c, objectId, position));
        return itemRepository.save(items);
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
        partRepository.save(partList);
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

    public void deleteFragment(final ApPart part) {
        if (part.getState() == ApStateEnum.TEMP) {
            List<ApItem> items = itemRepository.findValidItemsByPart(part);
            Set<ApChange> changes = new HashSet<>();
            for (ApItem item : items) {
                changes.add(item.getCreateChange());
            }
            changeRepository.delete(changes);
            itemRepository.delete(items);
            partRepository.delete(part);
        } else {
            throw new NotImplementedException("Mazání platného fragmentu není k dispozici");
            // zde bude potřeba zkontrolovat návazné entity (z ap_fragment_item a arr_data_apfrag_ref)
        }
    }

    public void confirmFragment(final ApPart fragment) {
        if (fragment.getState() == ApStateEnum.TEMP) {
            fragment.setState(ApStateEnum.INIT);
            apGeneratorService.generateAndSetResult(fragment);
            partRepository.save(fragment);
        } else {
            throw new BusinessException("Nelze potvrdit fragment, který není dočasný", BaseCode.INVALID_STATE);
        }
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
}
