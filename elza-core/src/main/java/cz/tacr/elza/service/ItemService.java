package cz.tacr.elza.service;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.domain.ApRecord;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.ApRecordRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.ItemSpecRegisterRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import org.apache.commons.lang3.Validate;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private PartyRepository partyRepository;

    @Autowired
    private StructuredObjectRepository structureDataRepository;

    @Autowired
    private FundFileRepository fundFileRepository;

    @Autowired
    private ApRecordRepository recordRepository;

    @Autowired
    private ItemSpecRegisterRepository itemSpecRegisterRepository;

    @Autowired
    private ApTypeRepository registerTypeRepository;

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
    public void checkValidTypeAndSpec(final RuleSystem ruleSystem, final ArrItem descItem) {
        RuleSystemItemType descItemType = ruleSystem.getItemTypeById(descItem.getItemTypeId());
        Validate.notNull(descItemType, "Invalid description item type");

        Integer descItemSpecId = descItem.getItemSpecId();

        // check if defined specification
        if (descItemType.hasSpecifications()) {
            if (descItemSpecId == null) {
                throw new BusinessException("Pro typ atributu je nutné specifikaci vyplnit",
                        ArrangementCode.ITEM_SPEC_NOT_FOUND).level(Level.WARNING);
            }

            RulItemSpec descItemSpec = descItemType.getItemSpecById(descItemSpecId);
            if (descItemSpec == null) {
                throw new SystemException("Specifikace neodpovídá typu hodnoty atributu");
            }

            // extra check for data
            ArrData data = descItem.getData();
            if (data != null) {
                // check record_ref
                if (descItemType.getDataType().equals(DataType.RECORD_REF)) {
                    ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) data;

                    // TODO: refactor and use static data
                    Set<Integer> registerTypeIds = itemSpecRegisterRepository.findIdsByItemSpecId(descItemSpec);
                    Set<Integer> registerTypeIdTree = registerTypeRepository.findSubtreeIds(registerTypeIds);

                    if (!registerTypeIdTree.contains(dataRecordRef.getRecord().getApTypeId())) {
                        throw new BusinessException("Hodnota neodpovídá typu rejstříku podle specifikace",
                                RegistryCode.FOREIGN_ENTITY_INVALID_SUBTYPE).level(Level.WARNING);
                    }
                }
            }

        } else {
            if (descItemSpecId != null) {
                throw new BusinessException("Pro typ atributu nesmí být specifikace vyplněná",
                        ArrangementCode.ITEM_SPEC_FOUND).level(Level.WARNING);
            }
        }
    }

    /**
     * Donačítá položky, které jsou typově jako odkaz, podle ID.
     *
     * @param dataItems seznam položek, které je potřeba donačíst podle ID návazných entit
     */
    public void refItemsLoader(final Collection<ArrItem> dataItems) {

        // mapy pro naplnění ID entit
        Map<Integer, ArrDataPartyRef> partyMap = new HashMap<>();
        Map<Integer, ArrDataStructureRef> structureMap = new HashMap<>();
        Map<Integer, ArrDataFileRef> fileMap = new HashMap<>();
        Map<Integer, ArrDataRecordRef> recordMap = new HashMap<>();

        // prohledávám pouze entity, které mají návazné data
        for (ArrItem dataItem : dataItems) {
            ArrData data = dataItem.getData();
            if (data != null) {
                if (data instanceof ArrDataPartyRef) {
                    ParParty party = ((ArrDataPartyRef) data).getParty();
                    partyMap.put(party.getPartyId(), (ArrDataPartyRef) data);
                } else if (data instanceof ArrDataStructureRef) {
                    ArrStructuredObject structureData = ((ArrDataStructureRef) data).getStructuredObject();
                    structureMap.put(structureData.getStructuredObjectId(), (ArrDataStructureRef) data);
                } else if (data instanceof ArrDataFileRef) {
                    ArrFile file = ((ArrDataFileRef) data).getFile();
                    fileMap.put(file.getFileId(), (ArrDataFileRef) data);
                } else if (data instanceof ArrDataRecordRef) {
                    ApRecord record = ((ArrDataRecordRef) data).getRecord();
                    recordMap.put(record.getRecordId(), (ArrDataRecordRef) data);
                }
            }
        }

        Set<Integer> structureDataIds = structureMap.keySet();
        List<ArrStructuredObject> structureDataEntities = structureDataRepository.findAll(structureDataIds);
        for (ArrStructuredObject structureDataEntity : structureDataEntities) {
            structureMap.get(structureDataEntity.getStructuredObjectId()).setStructuredObject(structureDataEntity);
        }

        Set<Integer> partyIds = partyMap.keySet();
        List<ParParty> partyEntities = partyRepository.findAll(partyIds);
        for (ParParty partyEntity : partyEntities) {
            partyMap.get(partyEntity.getPartyId()).setParty(partyEntity);
        }

        Set<Integer> fileIds = partyMap.keySet();
        List<ArrFile> fileEntities = fundFileRepository.findAll(fileIds);
        for (ArrFile fileEntity : fileEntities) {
            ArrDataFileRef ref = fileMap.get(fileEntity.getFileId());
            if (ref != null) {
                ref.setFile(fileEntity);
            }
        }

        Set<Integer> recordIds = recordMap.keySet();
        List<ApRecord> recordEntities = recordRepository.findAll(recordIds);
        for (ApRecord recordEntity : recordEntities) {
            recordMap.get(recordEntity.getRecordId()).setRecord(recordEntity);
        }
    }
}
