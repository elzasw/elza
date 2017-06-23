package cz.tacr.elza.print;

import java.util.Collection;
import java.util.List;

import javax.validation.constraints.NotNull;

import cz.tacr.elza.print.item.Item;

public interface Output {

    /**
     * vstupem je seznam kódu typů atributů a vrací se seznam hodnot těchto atributů
     * řazených dle rul_desc_item.view_order + arr_item.position
     *
     * @param codes seznam požadovaných kódů itemů
     * @return seznam items s odpovídajícími kódy
     */
	public List<Item> getItems(@NotNull final Collection<String> codes);
	
    /**
     * Return single item
     * @param itemTypeCode Code of item
     * @return Return single item if exists. Return null if item does not exists.
     * @throws Throw exception if there are multiple items with same type.
     */
	public Item getSingleItem(final String itemTypeCode);
	
    /**
     * Return value of single item
     * @param itemTypeCode Code of item
     * @return Return value of single item. Return null if item does not exists.
     * @throws Throw exception if there are multiple items with same type.
     */
	public String getSingleItemValue(final String itemTypeCode);
	
    /**
     * Vstupem je seznam kódu typů atributů a vrací se seznam všech hodnot atributů výstupu kromě hodnot typů uvedených ve vstupu metody;
     * řazeno dle rul_desc_item.view_order + arr_item.position.
     *
     * @param codes seznam ignorovaných kódů itemů
     * @return seznam všech items výstupu kromě hodnot typů uvedených ve vstupu metody
     */
    public List<Item> getAllItems(@NotNull final Collection<String> codes);
    
    /**
     * Getter položky items
     *
     * @return seznam items
     */
    public List<Item> getItems();
    
    /**
     * @return instance iterátoru, který prochází jednotky popisu do hloubky
     */
    IteratorNodes getNodesDFS();

    /**
     * @return instance iterátoru, který prochází jednotky popisu do šířky
     */
    IteratorNodes getNodesBFS();    
    
    
    /**
     * vstupem je kód typu rejstříku a vrací se seznam rejstříkových hesel řazených podle názvu (record).
     *
     * @param code požadovaný kód recordu
     * @return seznam recordů v daného typu
     */
    public FilteredRecords getRecordsByType(final String code);
    
    public Fund getFund();
    public String getInternal_code();
    public String getName();
    public String getType();
    public String getTypeCode();
}
