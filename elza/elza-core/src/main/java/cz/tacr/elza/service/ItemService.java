package cz.tacr.elza.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.constraints.NotNull;

import cz.tacr.elza.domain.*;
import org.apache.commons.lang3.Validate;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.ItemAptypeRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;

/**
 * Serviska pro správu hodnot atributů.
 */
@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private StructuredObjectRepository structureDataRepository;

    @Autowired
    private FundFileRepository fundFileRepository;

    @Autowired
    private ApAccessPointRepository recordRepository;

    @Autowired
    private ItemAptypeRepository itemAptypeRepository;

    @Autowired
    private ApTypeRepository registerTypeRepository;

    @Autowired
    private ApStateRepository stateRepository;

    @Autowired
    private EntityManager em;

    /**
     * Kontrola sloupců v JSON tabulce.
     *
     * @param table   kontrolovaná tabulka
     * @param columns seznam definicí sloupců
     */
    public void checkJsonTableData(@NotNull final ElzaTable table,
                                   @NotEmpty final List<ElzaColumn> columns) {
        Map<String, ElzaColumn.DataType> typeMap = columns.stream().collect(Collectors.toMap(ElzaColumn::getCode, ElzaColumn::getDataType));
        for (ElzaRow row : table.getRows()) {
            for (Map.Entry<String, String> entry : row.getValues().entrySet()) {
                ElzaColumn.DataType dataType = typeMap.get(entry.getKey());
                if (dataType == null) {
                    throw new BusinessException("Sloupec s kódem '" + entry.getKey() +  "' neexistuje v definici tabulky", BaseCode.PROPERTY_IS_INVALID)
                    .set("property", entry.getKey());
                }

                switch (dataType) {
                    case INTEGER:
                        try {
                            Integer.parseInt(entry.getValue());
                        } catch (NumberFormatException e) {
                            throw new BusinessException("Neplatný vstup: Hodnota sloupce '" + entry.getKey() + "' musí být celé číslo", e,
                                    BaseCode.PROPERTY_IS_INVALID)
                            .set("property", entry.getKey());
                        }
                        break;

                    case TEXT:
                        if (entry.getValue() == null) {
                            throw new BusinessException("Neplatný vstup: Hodnota sloupce '" + entry.getKey() + "' nesmí být null",
                                    BaseCode.PROPERTY_IS_INVALID)
                            .set("property", entry.getKey());
                        }
                        break;

                    default:
                        throw new BusinessException("Neznámý typ sloupce '" + dataType.name() + "' ve validaci JSON tabulky",
                                BaseCode.PROPERTY_IS_INVALID)
                        .set("property", dataType.name());
                }
            }
        }
    }

    @Deprecated
    public <T extends ArrItem> T save(final T item) {
        ArrData data = item.getData();

        if (data != null) {
            if (data instanceof ArrDataJsonTable) {
                checkJsonTableData(((ArrDataJsonTable) data).getValue(), (List<ElzaColumn>) item.getItemType().getViewDefinition());
        }

            ArrData dataNew = ArrData.makeCopyWithoutId(data);
            dataRepository.save(dataNew);

            item.setData(dataNew);
    }
        return itemRepository.save(item);
    }

    @SuppressWarnings("unchecked")
    @Transactional(TxType.MANDATORY)
    public <T extends ArrItem> T copyItem(T item, ArrChange change, int position) {
        Validate.isTrue(em.contains(item));
        Validate.notNull(change);

        item.setDeleteChange(change); // save by commit

        T newItem = (T) item.makeCopy();
        newItem.setCreateChange(change);
        newItem.setPosition(position);
        newItem.setDeleteChange(null);
        newItem.setItemId(null);

        ArrData newData = ArrData.makeCopyWithoutId(item.getData());
        newItem.setData(newData);

        em.persist(newData);
        em.persist(newItem);

        return newItem;
        }

    /**
     * Kontrola typu a specifikace.
     *
     * @param descItem hodnota atributu
     */
    @Transactional(TxType.MANDATORY)
    public void checkValidTypeAndSpec(@NotNull final StaticDataProvider sdp, @NotNull final ArrItem descItem) {

        Integer itemTypeId = descItem.getItemTypeId();
        Validate.notNull(itemTypeId, "Invalid description item type: " + itemTypeId);

        ItemType itemType = sdp.getItemTypeById(itemTypeId);
        Validate.notNull(itemType, "Invalid description item type: " + itemTypeId);

        // extra check for data
        ArrData data = descItem.getData();
        RulItemType rulItemType = itemType.getEntity();

        // check if defined specification
        Integer itemSpecId = descItem.getItemSpecId();
        if (itemType.hasSpecifications()) {

            if (itemSpecId == null) {
                throw new BusinessException("Pro typ atributu je nutné specifikaci vyplnit",
                        ArrangementCode.ITEM_SPEC_NOT_FOUND).level(Level.WARNING);
            }

            RulItemSpec rulItemSpec = itemType.getItemSpecById(itemSpecId);
            if (rulItemSpec == null) {
                throw new SystemException("Specifikace neodpovídá typu hodnoty atributu");
            }

            if (data != null && !descItem.isUndefined()) {
                // check record_ref
                if (itemType.getDataType().equals(DataType.RECORD_REF)) {
                    ArrDataRecordRef recordRef = (ArrDataRecordRef) data;
                    checkRecordRef(recordRef, rulItemType, rulItemSpec);
                }
            }

        } else {
            if (itemSpecId != null) {
                throw new BusinessException("Pro typ atributu nesmí být specifikace vyplněná",
                        ArrangementCode.ITEM_SPEC_FOUND).level(Level.WARNING);
            } else {
                if (data != null && !descItem.isUndefined()) {
                    if (itemType.getDataType().equals(DataType.RECORD_REF)) {
                        ArrDataRecordRef recordRef = (ArrDataRecordRef) data;
                        checkRecordRef(recordRef, rulItemType, null);
                    }
                }
            }
        }

        if(itemType.getDataType() == DataType.STRING && itemType.getEntity().getStringLengthLimit() != null) {
            if(((ArrDataString) descItem.getData()).getValue().length() > itemType.getEntity().getStringLengthLimit()) {
                throw new BusinessException("Délka řetězce je delší než maximální povolená : " +itemType.getEntity().getStringLengthLimit(), BaseCode.INVALID_LENGTH);
            }
        }
    }

    private void checkRecordRef(ArrDataRecordRef dataRecordRef, RulItemType rulItemType, RulItemSpec rulItemSpec) {
        ApAccessPoint apAccessPoint = dataRecordRef.getRecord();
        ApState apState = stateRepository.findLastByAccessPoint(apAccessPoint);

        // TODO: refactor and use static data
        List<Integer> apTypeIds = null;
        if (rulItemSpec != null) {
            apTypeIds = itemAptypeRepository.findApTypeIdsByItemSpec(rulItemSpec);
        } else {
            apTypeIds = itemAptypeRepository.findApTypeIdsByItemType(rulItemType);
        }
        Set<Integer> apTypeIdTree = registerTypeRepository.findSubtreeIds(apTypeIds);

        if (!apTypeIdTree.contains(apState.getApTypeId())) {
            throw new BusinessException("Hodnota neodpovídá typu rejstříku podle specifikace nebo typu",
                    RegistryCode.FOREIGN_ENTITY_INVALID_SUBTYPE).level(Level.WARNING);
        }
    }

    public ApAccessPoint getApProxy(Integer apId) {
        return recordRepository.getOne(apId);
    }

    /**
     * Donačítá položky, které jsou typově jako odkaz, podle ID.
     *
     * @param dataItems seznam položek, které je potřeba donačíst podle ID návazných entit
     */
    public void refItemsLoader(final Collection<? extends ArrItem> dataItems) {

        // mapy pro naplnění ID entit
        Map<Integer, ArrDataStructureRef> structureMap = new HashMap<>();
        Map<Integer, ArrDataFileRef> fileMap = new HashMap<>();
        Map<Integer, ArrDataRecordRef> recordMap = new HashMap<>();

        // prohledávám pouze entity, které mají návazné data
        for (ArrItem dataItem : dataItems) {
            ArrData data = dataItem.getData();
            if (data != null) {
                if (data instanceof ArrDataStructureRef) {
                    ArrStructuredObject structureData = ((ArrDataStructureRef) data).getStructuredObject();
                    structureMap.put(structureData.getStructuredObjectId(), (ArrDataStructureRef) data);
                } else if (data instanceof ArrDataFileRef) {
                    ArrFile file = ((ArrDataFileRef) data).getFile();
                    fileMap.put(file.getFileId(), (ArrDataFileRef) data);
                } else if (data instanceof ArrDataRecordRef) {
                    ApAccessPoint record = ((ArrDataRecordRef) data).getRecord();
                    recordMap.put(record.getAccessPointId(), (ArrDataRecordRef) data);
                }
            }
        }

        Set<Integer> structureDataIds = structureMap.keySet();
        List<ArrStructuredObject> structureDataEntities = structureDataRepository.findAll(structureDataIds);
        for (ArrStructuredObject structureDataEntity : structureDataEntities) {
            structureMap.get(structureDataEntity.getStructuredObjectId()).setStructuredObject(structureDataEntity);
        }

        Set<Integer> fileIds = fileMap.keySet();
        List<ArrFile> fileEntities = fundFileRepository.findAll(fileIds);
        for (ArrFile fileEntity : fileEntities) {
            ArrDataFileRef ref = fileMap.get(fileEntity.getFileId());
            if (ref != null) {
                ref.setFile(fileEntity);
            }
        }

        Set<Integer> recordIds = recordMap.keySet();
        List<ApAccessPoint> recordEntities = recordRepository.findAll(recordIds);
        for (ApAccessPoint recordEntity : recordEntities) {
            recordMap.get(recordEntity.getAccessPointId()).setRecord(recordEntity);
        }
    }
}
