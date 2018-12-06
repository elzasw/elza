package cz.tacr.elza.print;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.party.Party;
import net.bytebuddy.implementation.bytecode.Throw;

/**
 * Output interface to access all output related data
 *
 */
public interface Output {

    Fund getFund();

    String getInternalCode();

    String getName();

    String getType();

    String getTypeCode();

    /**
     * Getter položky items
     *
     * @return seznam items
     */
    List<Item> getItems();

    /**
     * vstupem je seznam kódu typů atributů a vrací se seznam hodnot těchto atributů
     * řazených dle rul_desc_item.view_order + arr_item.position
     *
     * @param codes seznam požadovaných kódů itemů
     * @return seznam items s odpovídajícími kódy
     */
	List<Item> getItems(final Collection<String> codes);

	/**
     * Vstupem je seznam kódu typů atributů a vrací se seznam všech hodnot atributů výstupu kromě hodnot typů uvedených ve vstupu metody;
     * řazeno dle rul_desc_item.view_order + arr_item.position.
     *
     * @param codes seznam ignorovaných kódů itemů
     * @return seznam všech items výstupu kromě hodnot typů uvedených ve vstupu metody
     */
    List<Item> getItemsWithout(final Collection<String> codes);

	/**
	 * Return list of parties from the given description items.
	 *
	 * Description items have to be party references.
	 * @param codes List of description items referencing parties
	 * @return List of referenced parties
	 */
	List<Party> getParties(final Collection<String> codes);

    /**
     * Return single item
     * @param itemTypeCode Code of item
     * @return Return single item if exists. Return null if item does not exists.
     * @throws Throw exception if there are multiple items with same type.
     */
	Item getSingleItem(final String itemTypeCode);

    /**
     * Return value of single item
     * @param itemTypeCode Code of item
     * @return Return value of single item. Return null if item does not exists.
     * @throws Throw exception if there are multiple items with same type.
     */
	String getSingleItemValue(final String itemTypeCode);

    /**
     * @return instance iterátoru, který prochází jednotky popisu do hloubky
     */
    NodeIterator createFlatNodeIterator();

    /**
     * Create iterator for structured object
     * 
     * @param structTypeCode
     *            Code of structured type
     * @return List of structured objects
     */
    List<Structured> createStructObjList(String structTypeCode);

    /**
     * @return kolekci s počtem prvků odpovídajícím počtu stran příloh,
     *         používá se jako DS v Jasperu pro placeholder stránky
     */
    List<AttPagePlaceHolder> getAttPagePlaceHolders(final String itemTypeCode);

    /**
     * vstupem je kód typu rejstříku a vrací se seznam rejstříkových hesel řazených podle názvu (record).
     *
     * @param code požadovaný kód recordu
     * @return seznam recordů v daného typu
     */
    FilteredRecords getRecordsByType(final String code);
}
