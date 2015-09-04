package cz.tacr.elza.ui.components.autocomplete;


import java.util.List;


/**
 * Rozhraní pro získání položek komponenty .
 *
 *
 * @author <a href="mailto:jaroslav.pubal@marbes.cz">Jaroslav Půbal</a>
 * @since 15.5.13
 * @see Autocomplete
 * @see AutocompleteItem
 */
public interface AutocompleteItemLoader {

	/**
	 * Vrátí položky pro zadaný filtrovací text.
	 *
	 * <p>Při inicializaci komponenty {@link Autocomplete} se zavolá tato metoda s text = "".
	 * Slouží hlavně pro naplnění položky, která odpovídá databindingu.
	 * Pokud hodnotě z setPropertyDataSource() nebude odpovídat naplněná AutocompleteItem,
	 * tak se v komboboxu neukáže popisek!
	 *
	 * @param text zadaný text do komponenty Autocomplete
	 * @return položky do Autocomplete
	 */
	List<AutocompleteItem> loadItems(String text);
}
