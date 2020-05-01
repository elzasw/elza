package cz.tacr.elza.service;

import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.controller.vo.ap.item.ApUpdateItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.UpdateOp;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApNameItemRepository;
import cz.tacr.elza.repository.DataRepository;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccessPointItemService {

    private static final String OBJECT_ID_SEQUENCE_NAME = "ap_item|object_id";

    private final EntityManager em;
    private final StaticDataService staticDataService;
    private final ApItemRepository itemRepository;
    private final DataRepository dataRepository;
    private final ApNameItemRepository nameItemRepository;
    private final SequenceService sequenceService;

    public AccessPointItemService(final EntityManager em,
                                  final StaticDataService staticDataService,
                                  final ApItemRepository itemRepository,
                                  final DataRepository dataRepository,
                                  final ApNameItemRepository nameItemRepository,
                                  final SequenceService sequenceService) {
        this.em = em;
        this.staticDataService = staticDataService;
        this.itemRepository = itemRepository;
        this.dataRepository = dataRepository;
        this.nameItemRepository = nameItemRepository;
        this.sequenceService = sequenceService;
    }

    /**
     * Odstranění prvků popisů u dočasných jmen a AP.
     */
    public void removeTempItems() {
        nameItemRepository.removeTempItems();
        //TODO fantis
//        itemRepository.removeTempItems();
    }

    /**
     * Odstranění prvůk popisů u dočasných jmen a AP.
     */
    public void removeTempItems(final ApAccessPoint ap) {
        nameItemRepository.removeTempItems(ap);
        //TODO fantis
//        itemRepository.removeTempItems(ap);
    }

    @FunctionalInterface
    public interface CreateFunction {
        ApItem apply(final RulItemType itemType, final RulItemSpec itemSpec, final ApChange change, final int objectId, final int position);
    }

    /**
     * Zkopírování itemů mezi jmény AP.
     *
     * @param nameSource zdrojové jméno
     * @param nameTarget cílové jméno
     * @param change     změna pod kterou se provede kopírování
     * @return nové itemy
     */
    public List<ApItem> copyItems(final ApName nameSource, final ApName nameTarget, final ApChange change) {
        Validate.notNull(nameSource, "Zdrojové jméno musí být vyplněno");
        Validate.notNull(nameTarget, "Cílové jméno musí být vyplněno");
        Validate.notNull(change, "Změna musí být vyplněna");

        List<ApNameItem> items = nameItemRepository.findValidItemsByName(nameSource);

        List<ApNameItem> newItems = new ArrayList<>(items.size());
        for (ApNameItem item : items) {
            ApNameItem newItem;
            if (item.getCreateChange().getChangeId().equals(change.getChangeId())) {
                // optimalizace: není třeba odverzovat item, který v této změně již byl odverzován,
                //               pouze změníme odkazující name
                newItem = item;
                newItem.setName(nameTarget);
            } else {
                newItem = new ApNameItem(item);
                newItem.setCreateChange(change);
                newItem.setName(nameTarget);
                item.setDeleteChange(change);
            }
            newItems.add(newItem);
        }

        items.addAll(newItems);
        nameItemRepository.save(items);
        return new ArrayList<>(newItems);
    }

    /**
     * Provede změnu položek.
     *
     * @param items   položky k úpravě
     * @param itemsDb aktuální položky v DB
     * @param change  změna
     * @param create  funkce pro založené nové položky
     * @return nové položky, které ze vytvořili při změně
     */
    public List<ApItem> changeItems(final List<ApUpdateItemVO> items, final List<ApItem> itemsDb, final ApChange change, final CreateFunction create) {
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

        // TODO: optimalizace při úpravě se stejným change id (bez odverzování) - pro deleteItems a updateItems
        deleteItems(deleteItems, typeIdItemsMap, itemsDb, objectIdItemMap, change);
        List<ApItem> itemsCreated = createItems(createItems, typeIdItemsMap, itemsDb, change, create);
        updateItems(updateItems, typeIdItemsMap, itemsDb, objectIdItemMap, change);

        itemRepository.save(itemsDb);

        return itemsCreated;
    }

    /**
     * Provede odstranění všech aktivních položek podle typu.
     *
     * @param repository repository pro nalezení platný položek
     * @param joinItem   vazební položka (podle které se vyhledávají položky)
     * @param itemType   typ
     * @param change     změna
     * @param <T>        generický typ vazební položky
     */
    public <T> void deleteItemsByType(ByType<T> repository, T joinItem, RulItemType itemType, ApChange change) {
        List<ApItem> items = repository.findValidItemsByType(joinItem, itemType);
        for (ApItem item : items) {
            item.setDeleteChange(change);
        }
        itemRepository.save(items);
    }

    /**
     * Odstraní všechny atributy části.
     *
     * @param part část
     * @param change změna
     */
    public void deletePartItems(ApPart part, ApChange change) {
        List<ApItem> items = itemRepository.findValidItemsByPart(part);
        for (ApItem item : items) {
            item.setDeleteChange(change);
        }
        itemRepository.save(items);
    }

    private void updateItems(final List<ApItemVO> updateItems,
                             final Map<Integer, List<ApItem>> typeIdItemsMap,
                             final List<ApItem> itemsDb,
                             final Map<Integer, ApItem> objectIdItemMap,
                             final ApChange change) {
        StaticDataProvider sdp = staticDataService.getData();
        List<ArrData> dataToSave = new ArrayList<>();
        for (ApItemVO updateItem : updateItems) {
            Integer objectId = updateItem.getObjectId();
            ApItem item = objectIdItemMap.get(objectId);
            if (item == null) {
                throw new ObjectNotFoundException("Položka neexistuje", BaseCode.ID_NOT_EXIST).setId(objectId);
            }

            List<ApItem> existsItems = typeIdItemsMap.computeIfAbsent(item.getItemTypeId(), k -> new ArrayList<>());
            ArrData data = updateItem.createDataEntity(em);
            ApItem newItem = item.copy();
            item.setDeleteChange(change);

            newItem.setCreateChange(change);
            newItem.setData(data);
            newItem.setItemSpec(updateItem.getSpecId() == null ? null : sdp.getItemSpecById(updateItem.getSpecId()));

            dataToSave.add(data);

            itemsDb.add(newItem);
            existsItems.add(newItem);

            Integer oldPosition = item.getPosition();
            Integer newPosition = updateItem.getPosition();
            Validate.notNull(newPosition);
            if (!Objects.equals(oldPosition, newPosition)) {
                Validate.isTrue(newPosition > 0);
                newPosition = Math.min(newPosition, findMaxPosition(existsItems));
                List<ApItem> itemsToShift = findItemsBetween(existsItems, oldPosition, newPosition);
                newItem.setPosition(newPosition);
                itemsToShift.remove(newItem);
                List<ApItem> newItems = shiftItems(itemsToShift, oldPosition.compareTo(newPosition), change);
                itemsDb.addAll(newItems);
                existsItems.addAll(newItems);
            }
        }
        dataRepository.save(dataToSave);
    }

    private int findMaxPosition(final List<ApItem> items) {
        int max = 1;
        for (ApItem item : items) {
            if (item.getDeleteChange() == null) {
                if (item.getPosition() > max) {
                    max = item.getPosition();
                }
            }
        }
        return max;
    }

    private void deleteItems(final List<ApItemVO> deleteItems,
                             final Map<Integer, List<ApItem>> typeIdItemsMap,
                             final List<ApItem> itemsDb,
                             final Map<Integer, ApItem> objectIdItemMap,
                             final ApChange change) {
        for (ApItemVO deleteItem : deleteItems) {
            Integer objectId = deleteItem.getObjectId();
            ApItem item = objectIdItemMap.get(objectId);
            if (item == null) {
                throw new ObjectNotFoundException("Položka neexistuje", BaseCode.ID_NOT_EXIST).setId(objectId);
            }

            item.setDeleteChange(change);
            List<ApItem> existsItems = typeIdItemsMap.computeIfAbsent(item.getItemTypeId(), k -> new ArrayList<>());
            List<ApItem> itemsToShift = findItemsGE(existsItems, item.getPosition());
            List<ApItem> newItems = shiftItems(itemsToShift, -1, change);
            itemsDb.addAll(newItems);
            existsItems.addAll(newItems);
        }
    }

    /**
     * @return identifikátor pro nový item AP
     */
    private int nextItemObjectId() {
        return sequenceService.getNext(OBJECT_ID_SEQUENCE_NAME);
    }

    public List<ApItem> createItems(final List<ApItemVO> createItems, final Map<Integer, List<ApItem>> typeIdItemsMap, final List<ApItem> itemsDb, final ApChange change, final CreateFunction create) {
        StaticDataProvider sdp = staticDataService.getData();
        List<ArrData> dataToSave = new ArrayList<>(createItems.size());
        List<ApItem> itemsCreated = new ArrayList<>();
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
            ApItem itemCreated = create.apply(itemType, itemSpec, change, nextItemObjectId(), position);
            dataToSave.add(data);
            itemCreated.setData(data);
            itemsCreated.add(itemCreated);

            itemsDb.add(itemCreated);
            existsItems.add(itemCreated);
        }
        dataRepository.save(dataToSave);
        return itemsCreated;
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

    private List<ApItem> findItemsBetween(final List<ApItem> items, final int aPosition, final int bPosition) {
        int minPosition = Math.min(aPosition, bPosition);
        int maxPosition = Math.max(aPosition, bPosition);
        List<ApItem> result = new ArrayList<>();
        for (ApItem item : items) {
            if (item.getDeleteChange() == null) {
                if (item.getPosition() >= minPosition && item.getPosition() <= maxPosition) {
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

}
