package cz.tacr.elza.service;

import cz.tacr.elza.controller.vo.ap.item.ApUpdateItemVO;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PartService {

    private final ApPartRepository partRepository;
    private final ApItemRepository itemRepository;
    private final ApChangeRepository changeRepository;
    private final AccessPointGeneratorService apGeneratorService;
    private final AccessPointItemService apItemService;
    private final AccessPointDataService apDataService;

    @Autowired
    public PartService(final ApPartRepository partRepository,
                       final ApItemRepository itemRepository,
                       final ApChangeRepository changeRepository,
                       final AccessPointGeneratorService apGeneratorService,
                       final AccessPointItemService apItemService,
                       final AccessPointDataService apDataService) {
        this.partRepository = partRepository;
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

    public ApPart getFragment(final Integer fragmentId) {
        Validate.notNull(fragmentId);
        ApPart fragment = partRepository.findOne(fragmentId);
        if (fragment == null) {
            throw new ObjectNotFoundException("Fragment nenalezen", BaseCode.ID_NOT_EXIST)
                    .setId(fragmentId);
        }
        return fragment;
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

}
