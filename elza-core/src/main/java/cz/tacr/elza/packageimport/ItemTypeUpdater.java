package cz.tacr.elza.packageimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecRegister;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.packageimport.xml.Column;
import cz.tacr.elza.packageimport.xml.DescItemSpecRegister;
import cz.tacr.elza.packageimport.xml.ItemSpec;
import cz.tacr.elza.packageimport.xml.ItemSpecs;
import cz.tacr.elza.packageimport.xml.ItemType;
import cz.tacr.elza.packageimport.xml.ItemTypes;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.ItemSpecRegisterRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;

/**
 * Class to update item types in DB
 * 
 * Class will use types from XML and try to synchronize them in DB
 * 
 * @author Petr Pytelka
 *
 */
@Component
@Scope("prototype")
public class ItemTypeUpdater {
	
	private RulPackage rulPackage;
	
	private List<RulDataType> rulDataTypes;
	
	List<RulItemType> rulItemTypesOrig;
	
	@Autowired
	private ItemTypeRepository itemTypeRepository;
	
	@Autowired
	private DescItemRepository descItemRepository;
	
    @Autowired
    private ItemSpecRepository itemSpecRepository;	

    @Autowired
    private ItemSpecRegisterRepository itemSpecRegisterRepository;

    @Autowired
	private RegisterTypeRepository registerTypeRepository;
    
    /**
     * Max used view order
     */
    int maxViewOrderPos = 0;
    
	public ItemTypeUpdater() {
	}
	

    /**
     * Porovnávání typů sloupců.
     *
     * @param elzaColumnList porovnávaný list ElzaColumn
     * @param columnList     porovnávaný list Column
     * @return jsou změněný neměnitelný položky?
     */
    private boolean equalsColumns(final List<ElzaColumn> elzaColumnList, final List<Column> columnList) {
        if (elzaColumnList.size() != columnList.size()) {
            return false;
        }

        for (int i = 0; i < elzaColumnList.size(); i++) {
            ElzaColumn elzaColumn = elzaColumnList.get(i);
            Column column = columnList.get(i);
            if (!elzaColumn.getCode().equals(column.getCode())
                    || !elzaColumn.getDataType().toString().equals(column.getDataType())) {
                return false;
            }
        }

        return true;
    }
    
    /**
     * Zpracování specifikací atributů.
     *
     * @param itemSpecs       seznam importovaných specifikací
     * @param rulPackage          balíček
     * @param rulDescItemTypes    seznam typů atributů
     */
    private void processDescItemSpecs(final ItemSpecs itemSpecs,
                                      final RulPackage rulPackage, final List<RulItemType> rulDescItemTypes) {

        List<RulItemSpec> rulDescItemSpecs = itemSpecRepository.findByRulPackage(rulPackage);
        List<RulItemSpec> rulDescItemSpecsNew = new ArrayList<>();

        if (!CollectionUtils.isEmpty(itemSpecs.getItemSpecs())) {
            for (ItemSpec itemSpec : itemSpecs.getItemSpecs()) {
                List<RulItemSpec> findItems = rulDescItemSpecs.stream()
                        .filter((r) -> r.getCode().equals(itemSpec.getCode())).collect(
                                Collectors.toList());
                RulItemSpec item;
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                } else {
                    item = new RulItemSpec();
                }

                convertRulDescItemSpec(rulPackage, itemSpec, item, rulDescItemTypes);
                rulDescItemSpecsNew.add(item);
            }
        }

        rulDescItemSpecsNew = itemSpecRepository.save(rulDescItemSpecsNew);

        processDescItemSpecsRegister(itemSpecs, rulDescItemSpecsNew);

        List<RulItemSpec> rulDescItemSpecsDelete = new ArrayList<>(rulDescItemSpecs);
        rulDescItemSpecsDelete.removeAll(rulDescItemSpecsNew);
        for (RulItemSpec descItemSpec : rulDescItemSpecsDelete) {
            itemSpecRegisterRepository.deleteByItemSpec(descItemSpec);
        }
        itemSpecRepository.delete(rulDescItemSpecsDelete);
    }    

	/**
	 * Do the update
	 * @param itemSpecs 
	 * @param itemTypes 
	 * @return return list of updated types
	 */
	public List<RulItemType> update(final List<RulDataType> rulDataTypes, final RulPackage rulPackage, 
			final ItemTypes itemTypes, final ItemSpecs itemSpecs) {
		this.rulDataTypes = rulDataTypes; 
		this.rulPackage = rulPackage;

		prepareForUpdate();

		// prepare list of updated/new items
		List<ItemType> itemTypesList = itemTypes.getItemTypes();
		List<RulItemType> rulItemTypesUpdated;
		if(CollectionUtils.isEmpty(itemTypesList)) {
			rulItemTypesUpdated = Collections.emptyList();
		} else {
			rulItemTypesUpdated = updateItemTypes(rulItemTypesOrig, itemTypesList);
			// try to save updated items
			rulItemTypesUpdated = itemTypeRepository.save(rulItemTypesUpdated);
		}

		// update specifications
		processDescItemSpecs(itemSpecs, rulPackage, rulItemTypesUpdated);

		// delete unused item types
		List<RulItemType> rulDescItemTypesDelete = new ArrayList<>(rulItemTypesOrig);
		rulDescItemTypesDelete.removeAll(rulItemTypesUpdated);
		itemTypeRepository.delete(rulDescItemTypesDelete);

		return rulItemTypesUpdated;
	}
	

	/**
	 * Update items types
	 * @param rulItemTypesOrig 
	 * @param itemTypes
	 * @return Return new list of active item types
	 */
    private List<RulItemType> updateItemTypes(List<RulItemType> rulItemTypesOrig, List<ItemType> itemTypes) {
    	List<RulItemType> rulItemTypesUpdated = new ArrayList<>();
    	int lastUsedViewOrder = -1;
		for (ItemType itemType : itemTypes) {
			RulItemType dbItemType = getItemTypeByCode(rulItemTypesOrig, itemType.getCode());
			if (dbItemType!=null) {				

				// provedla se změna pro použití specifikace?
				if (!dbItemType.getUseSpecification().equals(itemType.getUseSpecification())) {
					// je nutné zkontrolovat, jestli neexistuje nějaký záznam

					Long countDescItems = descItemRepository.getCountByType(dbItemType);
					if (countDescItems != null && countDescItems > 0) {
						throw new IllegalStateException("Nelze změnit použití specifikace u typu " + dbItemType.getCode()
								+ ", protože existují záznamy, které typ využívají");
					}
				}

				if (dbItemType.getColumnsDefinition() != null
						&& !equalsColumns(dbItemType.getColumnsDefinition(), itemType.getColumnsDefinition())) {
					Long countDescItems = descItemRepository.getCountByType(dbItemType);
					if (countDescItems != null && countDescItems > 0) {
						throw new IllegalStateException("Nelze změnit definici sloupců (datový typ a kód) u typu "
								+ dbItemType.getCode() + ", protože existují záznamy, které typ využívají");
					}
				}
				
				// update view order
				int i = dbItemType.getViewOrder().intValue();
				if(i<=lastUsedViewOrder) {
					lastUsedViewOrder = getNextViewOrderPos();
				} else {
					lastUsedViewOrder = i;
				}

			} else {
				dbItemType = new RulItemType();
				lastUsedViewOrder = getNextViewOrderPos();
			}

			convertRulDescItemType(rulPackage, itemType, dbItemType, rulDataTypes);
			
			// update view order
			dbItemType.setViewOrder(lastUsedViewOrder);
			
			rulItemTypesUpdated.add(dbItemType);
		}
		return rulItemTypesUpdated;
	}


    /**
     * Return next view_order position
     * @return next value
     */
    private int getNextViewOrderPos() {
    	return ++this.maxViewOrderPos;
	}


	/**
     * Get item type by code
     * @param rulItemTypes list of item types
     * @param code Item type code
     * @return Return first item type with given code or null if does not exists
     */
	private RulItemType getItemTypeByCode(List<RulItemType> rulItemTypes, String code) {
		for(RulItemType itemType: rulItemTypes) {
			if(code.equals(itemType.getCode())) {
				return itemType;
			}
		}
		return null;
	}


	/**
     * Převod VO na DAO typu atributu.
     *
     * @param rulPackage      balíček
     * @param itemType    VO typu
     * @param rulDescItemType DAO typy
     * @param rulDataTypes    datové typy atributů
     */
    private void convertRulDescItemType(final RulPackage rulPackage,
                                        final ItemType itemType,
                                        final RulItemType rulDescItemType,
                                        final List<RulDataType> rulDataTypes) {

        rulDescItemType.setCode(itemType.getCode());
        rulDescItemType.setName(itemType.getName());

        List<RulDataType> findItems = rulDataTypes.stream()
                .filter((r) -> r.getCode().equals(itemType.getDataType()))
                .collect(Collectors.toList());

        RulDataType item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new IllegalStateException("Kód " + itemType.getDataType() + " neexistuje v RulDataType");
        }

        rulDescItemType.setDataType(item);
        rulDescItemType.setShortcut(itemType.getShortcut());
        rulDescItemType.setDescription(itemType.getDescription());
        rulDescItemType.setIsValueUnique(itemType.getIsValueUnique());
        rulDescItemType.setCanBeOrdered(itemType.getCanBeOrdered());
        rulDescItemType.setUseSpecification(itemType.getUseSpecification());

        if (itemType.getColumnsDefinition() != null) {
            List<ElzaColumn> elzaColumns = new ArrayList<>(itemType.getColumnsDefinition().size());
            for (Column column : itemType.getColumnsDefinition()) {
                ElzaColumn elzaColumn = new ElzaColumn();
                elzaColumn.setCode(column.getCode());
                elzaColumn.setName(column.getName());
                elzaColumn.setDataType(ElzaColumn.DataType.valueOf(column.getDataType()));
                elzaColumn.setWidth(column.getWidth());
                elzaColumns.add(elzaColumn);
            }

            rulDescItemType.setColumnsDefinition(elzaColumns);
        }

        rulDescItemType.setPackage(rulPackage);
    }
    
    /**
     * Převod VO na DAO specifikace atributu.
     *
     * @param rulPackage       balíček
     * @param itemSpec     VO specifikace
     * @param rulDescItemSpec  DAO specifikace
     * @param rulDescItemTypes seznam typů atributů
     */
    private void convertRulDescItemSpec(final RulPackage rulPackage,
                                        final ItemSpec itemSpec,
                                        final RulItemSpec rulDescItemSpec,
                                        final List<RulItemType> rulDescItemTypes) {

        rulDescItemSpec.setName(itemSpec.getName());
        rulDescItemSpec.setCode(itemSpec.getCode());
        rulDescItemSpec.setViewOrder(itemSpec.getViewOrder());
        rulDescItemSpec.setDescription(itemSpec.getDescription());
        rulDescItemSpec.setShortcut(itemSpec.getShortcut());
        rulDescItemSpec.setPackage(rulPackage);

        List<RulItemType> findItems = rulDescItemTypes.stream()
                .filter((r) -> r.getCode().equals(itemSpec.getItemType()))
                .collect(Collectors.toList());

        RulItemType item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new IllegalStateException("Kód " + itemSpec.getItemType() + " neexistuje v RulItemType");
        }

        rulDescItemSpec.setItemType(item);
    }    
	

    /**
     * Zpracování napojení specifikací na reg.
     *
     * @param itemSpecs    seznam importovaných specifikací
     * @param rulDescItemSpecs seznam specifikací atributů
     */
    private void processDescItemSpecsRegister(final ItemSpecs itemSpecs,
                                              final List<RulItemSpec> rulDescItemSpecs) {

        List<RegRegisterType> regRegisterTypes = registerTypeRepository.findAll();

        for (RulItemSpec rulDescItemSpec : rulDescItemSpecs) {
            List<ItemSpec> findItemsSpec = itemSpecs.getItemSpecs().stream().filter(
                    (r) -> r.getCode().equals(rulDescItemSpec.getCode())).collect(Collectors.toList());
            ItemSpec item;
            if (findItemsSpec.size() > 0) {
                item = findItemsSpec.get(0);
            } else {
                throw new IllegalStateException("Kód " + rulDescItemSpec.getCode() + " neexistuje v ItemSpecs");
            }

            List<RulItemSpecRegister> rulItemSpecRegisters = itemSpecRegisterRepository
                    .findByDescItemSpecId(rulDescItemSpec);
            List<RulItemSpecRegister> rulItemSpecRegistersNew = new ArrayList<>();

            if (!CollectionUtils.isEmpty(item.getDescItemSpecRegisters())) {
                for (DescItemSpecRegister descItemSpecRegister : item.getDescItemSpecRegisters()) {
                    List<RulItemSpecRegister> findItems = rulItemSpecRegisters.stream()
                            .filter((r) -> r.getRegisterType().getCode().equals(
                                    descItemSpecRegister.getRegisterType())).collect(Collectors.toList());
                    RulItemSpecRegister itemRegister;
                    if (findItems.size() > 0) {
                        itemRegister = findItems.get(0);
                    } else {
                        itemRegister = new RulItemSpecRegister();
                    }

                    convertRulDescItemSpecsRegister(rulDescItemSpec, itemRegister, regRegisterTypes,
                            descItemSpecRegister);

                    rulItemSpecRegistersNew.add(itemRegister);
                }
            }

            rulItemSpecRegistersNew = itemSpecRegisterRepository.save(rulItemSpecRegistersNew);

            List<RulItemSpecRegister> rulItemSpecRegistersDelete = new ArrayList<>(rulItemSpecRegisters);
            rulItemSpecRegistersDelete.removeAll(rulItemSpecRegistersNew);
            itemSpecRegisterRepository.delete(rulItemSpecRegistersDelete);

        }

    }


    /**
     * Převod VO na DAO napojení specifikací na reg.
     *
     * @param rulDescItemSpec         seznam specifikací
     * @param rulItemSpecRegister seznam DAO napojení
     * @param regRegisterTypes        seznam typů reg.
     * @param descItemSpecRegister    seznam VO napojení
     */
    private void convertRulDescItemSpecsRegister(final RulItemSpec rulDescItemSpec,
                                                 final RulItemSpecRegister rulItemSpecRegister,
                                                 final List<RegRegisterType> regRegisterTypes,
                                                 final DescItemSpecRegister descItemSpecRegister) {

        rulItemSpecRegister.setItemSpec(rulDescItemSpec);

        List<RegRegisterType> findItems = regRegisterTypes.stream()
                .filter((r) -> r.getCode().equals(descItemSpecRegister.getRegisterType()))
                .collect(Collectors.toList());

        RegRegisterType item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new IllegalStateException(
                    "Kód " + descItemSpecRegister.getRegisterType() + " neexistuje v RegRegisterType");
        }

        rulItemSpecRegister.setRegisterType(item);

    }

    /**
	 * Prepare item types to be updated
	 * @return 
	 */
	private void prepareForUpdate() {
		
		rulItemTypesOrig = itemTypeRepository.findByRulPackage(rulPackage);
		
		// read first free view-order id 
		RulItemType itemTypeHighest = itemTypeRepository.findFirstByOrderByViewOrderDesc();
		if(itemTypeHighest!=null) {
			Integer maxValue = itemTypeHighest.getViewOrder();
			maxViewOrderPos = maxValue!=null?maxValue.intValue():0;
		}
	}

}