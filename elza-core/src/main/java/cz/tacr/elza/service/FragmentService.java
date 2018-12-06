package cz.tacr.elza.service;

import cz.tacr.elza.controller.vo.ap.item.ApUpdateItemVO;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApFragmentItemRepository;
import cz.tacr.elza.repository.ApFragmentRepository;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FragmentService {

    private final ApFragmentRepository fragmentRepository;
    private final ApFragmentItemRepository fragmentItemRepository;
    private final ApChangeRepository changeRepository;
    private final AccessPointGeneratorService apGeneratorService;
    private final AccessPointItemService apItemService;
    private final AccessPointDataService apDataService;

    @Autowired
    public FragmentService(final ApFragmentRepository fragmentRepository,
                           final ApFragmentItemRepository fragmentItemRepository,
                           final ApChangeRepository changeRepository,
                           final AccessPointGeneratorService apGeneratorService,
                           final AccessPointItemService apItemService,
                           final AccessPointDataService apDataService) {
        this.fragmentRepository = fragmentRepository;
        this.fragmentItemRepository = fragmentItemRepository;
        this.changeRepository = changeRepository;
        this.apGeneratorService = apGeneratorService;
        this.apItemService = apItemService;
        this.apDataService = apDataService;
    }

    public ApFragment createFragment(final RulStructuredType fragmentType) {
        Validate.notNull(fragmentType, "Typ fragmentu musí být vyplněn");

        ApFragment fragment = new ApFragment();
        fragment.setFragmentType(fragmentType);
        fragment.setState(ApState.TEMP);

        return fragmentRepository.save(fragment);
    }

    public ApFragment getFragment(final Integer fragmentId) {
        Validate.notNull(fragmentId);
        ApFragment fragment = fragmentRepository.findOne(fragmentId);
        if (fragment == null) {
            throw new ObjectNotFoundException("Fragment nenalezen", BaseCode.ID_NOT_EXIST)
                    .setId(fragmentId);
        }
        return fragment;
    }

    public void changeFragmentItems(final ApFragment fragment, final List<ApUpdateItemVO> items) {
        Validate.notNull(fragment, "Fragment musí být vyplněn");
        Validate.notEmpty(items, "Musí být alespoň jedna položka ke změně");

        List<ApFragmentItem> itemsDb = fragmentItemRepository.findValidItemsByFragment(fragment);

        ApChange change = apDataService.createChange(ApChange.Type.FRAGMENT_CHANGE);
        apItemService.changeItems(items, new ArrayList<>(itemsDb), change, (RulItemType it, RulItemSpec is, ApChange c, int objectId, int position)
                -> createFragmentItem(fragment, it, is, c, objectId, position));

        apGeneratorService.generateAndSetResult(fragment);
    }

    /**
     * Smazání hodnot fragmentu podle typu.
     *
     * @param fragment fragment
     * @param itemType typu atributu
     */
    public void deleteFragmentItemsByType(final ApFragment fragment, final RulItemType itemType) {
        Validate.notNull(fragment, "Fragment musí být vyplněn");
        Validate.notNull(itemType, "Typ musí být vyplněn");
        ApChange change = apDataService.createChange(ApChange.Type.FRAGMENT_CHANGE);

        apItemService.deleteItemsByType(fragmentItemRepository, fragment, itemType, change);
        apGeneratorService.generateAndSetResult(fragment);
    }

    private ApItem createFragmentItem(final ApFragment fragment, final RulItemType it, final RulItemSpec is, final ApChange c, final int objectId, final int position) {
        ApFragmentItem item = new ApFragmentItem();
        item.setFragment(fragment);
        item.setItemType(it);
        item.setItemSpec(is);
        item.setCreateChange(c);
        item.setObjectId(objectId);
        item.setPosition(position);
        return item;
    }

    public void deleteFragment(final ApFragment fragment) {
        if (fragment.getState() == ApState.TEMP) {
            List<ApFragmentItem> items = fragmentItemRepository.findValidItemsByFragment(fragment);
            Set<ApChange> changes = new HashSet<>();
            for (ApFragmentItem item : items) {
                changes.add(item.getCreateChange());
            }
            changeRepository.delete(changes);
            fragmentItemRepository.delete(items);
            fragmentRepository.delete(fragment);
        } else {
            throw new NotImplementedException("Mazání platného fragmentu není k dispozici");
            // zde bude potřeba zkontrolovat návazné entity (z ap_fragment_item a arr_data_apfrag_ref)
        }
    }

    public void confirmFragment(final ApFragment fragment) {
        if (fragment.getState() == ApState.TEMP) {
            fragment.setState(ApState.INIT);
            apGeneratorService.generateAndSetResult(fragment);
            fragmentRepository.save(fragment);
        } else {
            throw new BusinessException("Nelze potvrdit fragment, který není dočasný", BaseCode.INVALID_STATE);
        }
    }

}
