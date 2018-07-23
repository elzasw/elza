package cz.tacr.elza.service;

import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.controller.vo.ap.item.ApUpdateItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.UpdateOp;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.security.UserDetail;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccessPointItemService {

    private final UserService userService;
    private final EntityManager em;
    private final ApChangeRepository apChangeRepository;
    private final StaticDataService staticDataService;
    private final ApItemRepository itemRepository;
    private final DataRepository dataRepository;

    public AccessPointItemService(final UserService userService,
                                  final EntityManager em,
                                  final ApChangeRepository apChangeRepository,
                                  final StaticDataService staticDataService,
                                  final ApItemRepository itemRepository,
                                  final DataRepository dataRepository) {
        this.userService = userService;
        this.em = em;
        this.apChangeRepository = apChangeRepository;
        this.staticDataService = staticDataService;
        this.itemRepository = itemRepository;
        this.dataRepository = dataRepository;
    }

    @FunctionalInterface
    public interface CreateFunction {
        ApItem apply(final RulItemType itemType, final RulItemSpec itemSpec, final ApChange change, final int objectId, final int position);
    }

    public void changeItems(final List<ApUpdateItemVO> items, final List<ApItem> itemsDb, final CreateFunction create) {
        Map<Integer, ApItem> objectIdItemMap = itemsDb.stream().collect(Collectors.toMap(ApItem::getObjectId, Function.identity()));
        Map<Integer, List<ApItem>> typeIdItemsMap = itemsDb.stream().collect(Collectors.groupingBy(ApItem::getItemTypeId));

        List<ApItemVO> createItems = new ArrayList<>();
        List<ApItemVO> updateItems = new ArrayList<>();
        List<ApItemVO> deleteItems = new ArrayList<>();

        for (ApUpdateItemVO item : items) {
            UpdateOp updateOp = item.getUpdateOp();
            switch (updateOp) {
                case CREATE:
                    createItems.add(item.getItem());
                    break;
                case UPDATE:
                    updateItems.add(item.getItem());
                    break;
                case DELETE:
                    deleteItems.add(item.getItem());
                    break;
                default:
                    throw new NotImplementedException("Neimplementovaná operace: " + updateOp);
            }
        }

        ApChange change = createChange(ApChange.Type.FRAGMENT_CHANGE);
        createItems(createItems, typeIdItemsMap, itemsDb, change, create);

        itemRepository.save(itemsDb);
    }

    private static Random r = new Random();

    private int nextObjectId() {
        return r.nextInt();
    }

    private void createItems(final List<ApItemVO> createItems, final Map<Integer, List<ApItem>> typeIdItemsMap, final List<ApItem> itemsDb, final ApChange change, final CreateFunction create) {
        StaticDataProvider sdp = staticDataService.getData();
        List<ArrData> dataToSave = new ArrayList<>(createItems.size());
        for (ApItemVO createItem : createItems) {
            RulItemType itemType = sdp.getItemTypeById(createItem.getTypeId()).getEntity();
            RulItemSpec itemSpec = createItem.getSpecId() == null ? null : sdp.getItemSpecById(createItem.getSpecId());
            List<ApItem> existsItems = typeIdItemsMap.computeIfAbsent(itemType.getItemTypeId(), k -> new ArrayList<>());

            Integer positionWant = createItem.getPosition();
            int position = nextPosition(existsItems);

            if (positionWant != null) {
                Validate.isTrue(positionWant > 0);
                if (position > positionWant) {

                    List<ApItem> itemsToShift = findItemsGE(existsItems, positionWant);
                    List<ApItem> newItems = shiftItems(itemsToShift, 1, change);
                    itemsDb.addAll(newItems);
                    existsItems.addAll(newItems);

                    position = positionWant;
                }
            }

            ArrData data = createItem.createDataEntity(em);
            ApItem itemCreated = create.apply(itemType, itemSpec, change, nextObjectId(), position);
            dataToSave.add(data);
            itemCreated.setData(data);

            itemsDb.add(itemCreated);
            existsItems.add(itemCreated);
        }
        dataRepository.save(dataToSave);
    }

    private List<ApItem> shiftItems(final List<ApItem> items, final int diff, final ApChange change) {
        List<ApItem> newItems = new ArrayList<>();
        for (ApItem item : items) {
            if (item.getItemId() == null) {
                item.setPosition(item.getPosition() + diff);
            } else {
                ApItem newItem = item.copy();
                newItem.setCreateChange(change);
                newItem.setPosition(item.getPosition() + diff);
                newItems.add(newItem);

                item.setDeleteChange(change);
            }
        }
        return newItems;
    }

    private List<ApItem> findItemsGE(final List<ApItem> items, final int position) {
        List<ApItem> result = new ArrayList<>();
        for (ApItem item : items) {
            if (item.getDeleteChange() == null) {
                if (item.getPosition() >= position) {
                    result.add(item);
                }
            }
        }
        return result;
    }


    private int nextPosition(final List<ApItem> existsItems) {
        if (existsItems.size() == 0) {
            return 1;
        }
        int position = 2;
        for (ApItem existsItem : existsItems) {
            if (existsItem.getDeleteChange() == null) {
                if (existsItem.getPosition() >= position) {
                    position = existsItem.getPosition() + 1;
                }
            }
        }
        return position;
    }

    public ApChange createChange(@Nullable final ApChange.Type type) {
        return createChange(type, null);
    }

    public ApChange createChange(@Nullable final ApChange.Type type, @Nullable ApExternalSystem externalSystem) {
        ApChange change = new ApChange();
        UserDetail userDetail = userService.getLoggedUserDetail();
        change.setChangeDate(LocalDateTime.now());

        if (userDetail != null && userDetail.getId() != null) {
            UsrUser user = em.getReference(UsrUser.class, userDetail.getId());
            change.setUser(user);
        }

        change.setType(type);
        change.setExternalSystem(externalSystem);

        return apChangeRepository.save(change);
    }

}
