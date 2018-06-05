package cz.tacr.elza.packageimport;

import static cz.tacr.elza.packageimport.PackageService.ITEM_TYPE_XML;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cz.tacr.elza.packageimport.xml.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecRegister;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.ItemSpecRegisterRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PackageDependencyRepository;
import cz.tacr.elza.repository.PackageRepository;

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
	private ApTypeRepository apTypeRepository;

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private PackageDependencyRepository packageDependencyRepository;

    public static final String CATEGORY_SEPARATOR = "|";

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
     *  @param itemSpecs       seznam importovaných specifikací
     * @param rulPackage          balíček
     * @param rulDescItemTypes    seznam typů atributů
     * @param rulRuleSet
     */
    private void processDescItemSpecs(final ItemSpecs itemSpecs,
                                      final RulPackage rulPackage,
                                      final List<RulItemType> rulDescItemTypes,
                                      final RulRuleSet rulRuleSet) {

        List<RulItemSpec> rulDescItemSpecs = itemSpecRepository.findByRulPackageAndRuleSet(rulPackage, rulRuleSet);
        List<RulItemSpec> rulDescItemSpecsNew = new ArrayList<>();

        // item type code -> local view order
        Map<String, Integer> viewOrderMap = new HashMap<>();

        if (itemSpecs != null && !CollectionUtils.isEmpty(itemSpecs.getItemSpecs())) {
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

                Integer nextViewOrder = viewOrderMap.computeIfAbsent(item.getItemType().getCode(), next -> 1);
                item.setViewOrder(nextViewOrder);
                viewOrderMap.put(item.getItemType().getCode(), ++nextViewOrder);

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
	 * @param itemTypes
	 * @param itemSpecs
	 * @param rulRuleSet
     * @return return list of updated types
	 */
	public List<RulItemType> update(final List<RulDataType> rulDataTypes,
                                    final List<RulStructuredType> rulStructureTypes,
                                    final RulPackage rulPackage,
                                    final ItemTypes itemTypes,
                                    final ItemSpecs itemSpecs,
                                    final RulRuleSet rulRuleSet) {
		this.rulDataTypes = rulDataTypes;
		this.rulPackage = rulPackage;

		prepareForUpdate(rulRuleSet);

        List<RulItemType> rulItemTypesUpdated = new ArrayList<>();
        if (itemTypes != null) {
            // prepare list of updated/new items
            List<ItemType> itemTypesList = itemTypes.getItemTypes();
            if (!CollectionUtils.isEmpty(itemTypesList)) {
                rulItemTypesUpdated = updateItemTypes(rulItemTypesOrig, itemTypesList, rulRuleSet, rulStructureTypes);
                // try to save updated items
                rulItemTypesUpdated = itemTypeRepository.save(rulItemTypesUpdated);
            }

        }
        List<RulItemType> rulItemTypesAllByRules = new ArrayList<>(rulItemTypesUpdated);
        rulItemTypesAllByRules.addAll(itemTypeRepository.findByRuleSet(rulRuleSet));

        // update specifications
        processDescItemSpecs(itemSpecs, rulPackage, rulItemTypesAllByRules, rulRuleSet);
        postSpecsOrder();

		// delete unused item types
		List<RulItemType> rulDescItemTypesDelete = new ArrayList<>(rulItemTypesOrig);
		rulDescItemTypesDelete.removeAll(rulItemTypesUpdated);
		itemTypeRepository.delete(rulDescItemTypesDelete);

		return rulItemTypesUpdated;
	}

    /**
     * Seřazení specifikací podle balíčků.
     */
    private void postSpecsOrder() {

        // potřeba seřadit podle typu, balíčku a "lokálnímu" ražení
        Sort sort = new Sort(
                new Sort.Order(Sort.Direction.ASC, "itemType"),
                new Sort.Order(Sort.Direction.ASC, "rulPackage"),
                new Sort.Order(Sort.Direction.ASC, "viewOrder")
        );
        List<RulItemSpec> itemSpecList = itemSpecRepository.findAll(sort);

        final List<RulPackage> sortedPackages = getSortedPackages();

        // item type id -> list spec
        Map<Integer, List<RulItemSpec>> itemSpecMap = itemSpecList.stream()
                .collect(Collectors.groupingBy(RulItemSpec::getItemTypeId));

        for (List<RulItemSpec> rulItemSpecs : itemSpecMap.values()) {

            // seřazení podle priority balíčků (lokální seřazení se změní na globální)
            rulItemSpecs.sort((o1, o2) -> {
                int i1 = sortedPackages.indexOf(o1.getPackage());
                int i2 = sortedPackages.indexOf(o2.getPackage());
                return Integer.compare(i1, i2);
            });

            // provede přečíslování
            for (int i = 0; i < rulItemSpecs.size(); i++) {
                RulItemSpec rulItemSpec = rulItemSpecs.get(i);
                rulItemSpec.setViewOrder(i + 1);
            }
        }

        itemSpecRepository.save(itemSpecList);
    }

    /**
     * Vrací všechny balíčky serazené podle topologického řazení - podle závislostí mezi sebou.
     *
     * @return seznam balíčků
     */
    private List<RulPackage> getSortedPackages() {
        List<RulPackage> packages = packageRepository.findAll();
        PackageUtils.Graph<RulPackage> g = new PackageUtils.Graph<>(packages.size());
        List<RulPackageDependency> dependencies = packageDependencyRepository.findAll();
        dependencies.forEach(d -> g.addEdge(d.getRulPackage(), d.getDependsOnPackage()));
        return g.topologicalSort();
    }

    /**
	 * Update items types
	 * @param rulItemTypesOrig
	 * @param itemTypes
	 * @param rulRuleSet
     * @return Return new list of active item types
	 */
    private List<RulItemType> updateItemTypes(List<RulItemType> rulItemTypesOrig, List<ItemType> itemTypes,
                                              final RulRuleSet rulRuleSet,
                                              final List<RulStructuredType> rulStructureTypes) {
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
						throw new SystemException("Nelze změnit použití specifikace u typu " + dbItemType.getCode()
								+ ", protože existují záznamy, které typ využívají");
					}
				}

                Object viewDefinition = dbItemType.getViewDefinition();
                if (viewDefinition != null) {
                    DataType dataType = DataType.fromId(dbItemType.getDataTypeId());
                    switch (dataType) {
                        case JSON_TABLE: {
                            if (!equalsColumns((List<ElzaColumn>) viewDefinition, itemType.getColumnsDefinition())) {
                                Long countDescItems = descItemRepository.getCountByType(dbItemType);
                                if (countDescItems != null && countDescItems > 0) {
                                    throw new SystemException("Nelze změnit definici sloupců (datový typ a kód) u typu "
                                            + dbItemType.getCode() + ", protože existují záznamy, které typ využívají");
                                }
                            }
                            break;
                        }
                    }
                }

				// update view order
				int i = dbItemType.getViewOrder();
				if(i<=lastUsedViewOrder) {
					lastUsedViewOrder = getNextViewOrderPos();
				} else {
					lastUsedViewOrder = i;
				}

			} else {
				dbItemType = new RulItemType();
				lastUsedViewOrder = getNextViewOrderPos();
			}

			convertRulDescItemType(rulPackage, itemType, dbItemType, rulDataTypes, rulStructureTypes, rulRuleSet);

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
     *  @param rulPackage      balíček
     * @param itemType    VO typu
     * @param rulDescItemType DAO typy
     * @param rulDataTypes    datové typy atributů
     * @param rulRuleSet    pravidla
     */
    private void convertRulDescItemType(final RulPackage rulPackage,
                                        final ItemType itemType,
                                        final RulItemType rulDescItemType,
                                        final List<RulDataType> rulDataTypes,
                                        final List<RulStructuredType> rulStructureTypes,
                                        final RulRuleSet rulRuleSet) {

        rulDescItemType.setCode(itemType.getCode());
        rulDescItemType.setName(itemType.getName());

        List<RulDataType> findItems = rulDataTypes.stream()
                .filter((r) -> r.getCode().equals(itemType.getDataType()))
                .collect(Collectors.toList());

        RulDataType item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new SystemException("Kód " + itemType.getDataType() + " neexistuje v RulDataType", BaseCode.ID_NOT_EXIST);
        }

        RulStructuredType rulStructureType = null;
        if (DataType.STRUCTURED == DataType.fromCode(itemType.getDataType())) {
            List<RulStructuredType> findStructureTypes = rulStructureTypes.stream()
                    .filter((r) -> r.getCode().equals(itemType.getStructureType()))
                    .collect(Collectors.toList());
            if (findStructureTypes.size() > 0) {
                rulStructureType = findStructureTypes.get(0);
            } else {
                throw new SystemException("Kód " + itemType.getStructureType() + " neexistuje v RulStructureType", BaseCode.ID_NOT_EXIST);
            }
        }

        rulDescItemType.setDataType(item);
        rulDescItemType.setShortcut(itemType.getShortcut());
        rulDescItemType.setDescription(itemType.getDescription());
        rulDescItemType.setIsValueUnique(itemType.getIsValueUnique());
        rulDescItemType.setCanBeOrdered(itemType.getCanBeOrdered());
        rulDescItemType.setUseSpecification(itemType.getUseSpecification());
        rulDescItemType.setStructuredType(rulStructureType);

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

            rulDescItemType.setViewDefinition(elzaColumns);
        }

        DisplayType displayType = itemType.getDisplayType();
        if (displayType != null) {
            rulDescItemType.setViewDefinition(cz.tacr.elza.domain.integer.DisplayType.valueOf(displayType.name()));
        }

        rulDescItemType.setRulPackage(rulPackage);
        rulDescItemType.setRuleSet(rulRuleSet);
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
            throw new BusinessException("Typ s kódem " + itemSpec.getItemType() + " nenalezen",
                    PackageCode.CODE_NOT_FOUND)
                            .set("code", itemSpec.getItemType()).set("file", ITEM_TYPE_XML);
        }

        if (CollectionUtils.isNotEmpty(itemSpec.getCategories())) {
            List<String> categories = itemSpec.getCategories().stream().map(Category::getValue).collect(Collectors.toList());
            rulDescItemSpec.setCategory(StringUtils.join(categories, CATEGORY_SEPARATOR));
        } else {
            rulDescItemSpec.setCategory(null);
        }

        rulDescItemSpec.setItemType(item);
    }


    /**
     * Zpracování napojení specifikací na ap.
     *
     * @param itemSpecs    seznam importovaných specifikací
     * @param rulDescItemSpecs seznam specifikací atributů
     */
    private void processDescItemSpecsRegister(final ItemSpecs itemSpecs,
                                              final List<RulItemSpec> rulDescItemSpecs) {

        List<ApType> apTypes = apTypeRepository.findAll();

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

            if (!CollectionUtils.isEmpty(item.getItemSpecRegisters())) {
                for (ItemSpecRegister itemSpecRegister : item.getItemSpecRegisters()) {
                    List<RulItemSpecRegister> findItems = rulItemSpecRegisters.stream()
                            .filter((r) -> r.getApType().getCode().equals(
                                    itemSpecRegister.getRegisterType())).collect(Collectors.toList());
                    RulItemSpecRegister itemRegister;
                    if (findItems.size() > 0) {
                        itemRegister = findItems.get(0);
                    } else {
                        itemRegister = new RulItemSpecRegister();
                    }

                    convertRulDescItemSpecsRegister(rulDescItemSpec, itemRegister, apTypes,
                            itemSpecRegister);

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
     * Převod VO na DAO napojení specifikací na ap.
     *
     * @param rulDescItemSpec         seznam specifikací
     * @param rulItemSpecRegister seznam DAO napojení
     * @param apTypes        seznam typů ap.
     * @param itemSpecRegister    seznam VO napojení
     */
    private void convertRulDescItemSpecsRegister(final RulItemSpec rulDescItemSpec,
                                                 final RulItemSpecRegister rulItemSpecRegister,
                                                 final List<ApType> apTypes,
                                                 final ItemSpecRegister itemSpecRegister) {

        rulItemSpecRegister.setItemSpec(rulDescItemSpec);

        List<ApType> findItems = apTypes.stream()
                .filter((r) -> r.getCode().equals(itemSpecRegister.getRegisterType()))
                .collect(Collectors.toList());

        ApType item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new SystemException(
                    "Kód " + itemSpecRegister.getRegisterType() + " neexistuje v ApType", BaseCode.ID_NOT_EXIST);
        }

        rulItemSpecRegister.setApType(item);

    }

    /**
	 * Prepare item types to be updated

     * @param rulRuleSet rule set
	 */
	private void prepareForUpdate(final RulRuleSet rulRuleSet) {

		rulItemTypesOrig = itemTypeRepository.findByRulPackageAndRuleSet(rulRuleSet.getPackage(), rulRuleSet);

		// read first free view-order id
		RulItemType itemTypeHighest = itemTypeRepository.findFirstByOrderByViewOrderDesc();
		if(itemTypeHighest!=null) {
			Integer maxValue = itemTypeHighest.getViewOrder();
			maxViewOrderPos = maxValue!=null?maxValue.intValue():0;
		}
	}

}
