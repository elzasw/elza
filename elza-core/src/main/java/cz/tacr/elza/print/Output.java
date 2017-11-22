package cz.tacr.elza.print;

import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.party.Party;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;

/**
 * Output interface to access all output related data
 *
 */
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
	 * Return list of parties from the given description items.
	 *
	 * Description items have to be party references.
	 * @param codes List of description items referencing parties
	 * @return List of referenced parties
	 */
	public List<Party> getParties(@NotNull final Collection<String> codes);

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
     * @return kolekci s počtem prvků odpovídajícím počtu stran příloh, používá se jako DS v Jasperu pro placeholder stránky
     */
    List<JRAttPagePlaceHolder> getAttPagePlaceHolders();

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
