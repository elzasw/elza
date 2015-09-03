package cz.tacr.elza.ui.components.autocomplete;


import java.util.Map;

import org.springframework.util.Assert;

import com.vaadin.data.Container;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.event.FieldEvents;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.ComboBox;


/**
 * Komponenta Autocomplete umožnující načítání položek až na základě zadaného textu.
 *
 * <p> Na zakladě zadaného textu do komponenty na klientu se zavolá funkce na serveru, která vrátí list položek {@link
 * AutocompleteItem}.
 *
 * Položka {@link AutocompleteItem} obsahuje property "id" a property "caption". ID se použije pro databinding a caption
 * se použije pro text položky.
 *
 * <p><b>Komponenta nedokáže zobrazit text, který není v items! Pokud má komponenty zobrazit text při vytvoření, je
 * potřeba ho dodat do items a nastavit value na ID items. </b>
 *
 * <p>Pomocí metody {@link #setNewItemsAllowed(boolean)} lze zadat i neověřené položky, které nejsou v nabídce.</p>
 *
 * @author <a href="mailto:jaroslav.pubal@marbes.cz">Jaroslav Půbal</a>
 * @see AutocompleteItemLoader
 * @see AutocompleteItem
 * @since 15.5.13
 */
public class Autocomplete extends ComboBox {

	/**
	 * Výchozí ID položky v datasource, která je vložená uživatelem jako neověřený text.
	 * Vytváření nové položky lze přepsat v metodě {@link #createNewItem(String)}.
	 *
	 * @see #setNewItemsAllowed(boolean)
	 */
	public static final int NEW_ITEM_ID = Integer.MIN_VALUE;

	/**
	 * Velikost stránky rozbalovátka v comboboxu.
	 */
	private static final int DEFAULT_PAGE_LENGHT = 15;

	/**
	 * Konstuktor vyžadující itemLoader.
	 *
	 * @param itemLoader implementace plniče itemů
	 */
	public Autocomplete(AutocompleteItemLoader itemLoader) {
		this(null, itemLoader);
	}

	/**
	 * Konstuktor vyžadující itemLoader.
	 *
	 * @param caption    název
	 * @param itemLoader implementace plniče itemů
	 */
	public Autocomplete(String caption, AutocompleteItemLoader itemLoader) {
		super(caption, new AutocompleteContainer(itemLoader));
		init();
	}

	/**
	 * Nastavení comboboxu, aby se choval jako autocomplete. Pokud tyto nastavení někdo přenastaví, tak komponenta přestane
	 * fungovat. Patrně to chce přetížit set metody a vyhodit výjimku.
	 */
	private void init() {
		//aby se neukazal scrollbar
		setPageLength(DEFAULT_PAGE_LENGHT);

		//jako text polozky pouzit caption z AutocompleteItem
		setItemCaptionMode(AbstractSelect.ItemCaptionMode.PROPERTY);
		setItemCaptionPropertyId(AutocompleteItem.CAPTION);
		setItemIconPropertyId(AutocompleteItem.ICON);

		//filtrovano na serveru, nefiltrovat na klientu
		setFilteringMode(FilteringMode.OFF);

		//bez toho to nebude fungovat
		setTextInputAllowed(true);
		setInvalidAllowed(false);

		//ovlivnuje datasource
		setScrollToSelectedItem(false);

		setNullSelectionAllowed(false);
		setImmediate(true);
	}

	/**
	 * Provede načtení položek podle filtru, stejně jako by filter byl zadán do komponenty.
	 *
	 * @param filter filter pro load položek
	 */
	public void reloadItems(String filter) {
		((AutocompleteContainer) getContainerDataSource())
				.addContainerFilter(new SimpleStringFilter(null, filter, false, false));
	}

	@Override
	protected Container.Filter buildFilter(String filterString, FilteringMode filteringMode) {
		//pro klient side musi byt filtering OFF, ale pro data container musi byt alespon nejaky filter definovany
		return super.buildFilter(filterString, FilteringMode.CONTAINS);
	}

	/**
	 * Povolí zadání neověřené hodnoty (jiné než z výběru).
	 *
	 * Po zadání neověřené hodnoty bude v datasource Item typu {@link AutocompleteItem}
	 * s ID {@link #NEW_ITEM_ID} a zadaným textem.
	 *
	 * @param allowNewOptions true - povolit
	 */
	@Override
	public void setNewItemsAllowed(boolean allowNewOptions) {
		super.setNewItemsAllowed(allowNewOptions);
		setInvalidAllowed(true);

		setNewItemHandler(new AbstractSelect.NewItemHandler() {
			@Override
			public void addNewItem(String newItemCaption) {

				AutocompleteItem newItem = createNewItem(newItemCaption);

				BeanItem<AutocompleteItem> item = containsId(newItem.getId())
						? (BeanItem<AutocompleteItem>) getItem(newItem.getId())
						: (BeanItem<AutocompleteItem>) addItem(newItem.getId());

				item.getBean().setCaption(newItem.getCaption());
				item.getBean().setIcon(newItem.getIcon());

				setValue(newItem.getId());
			}
		});
	}


	/**
	 * Vrátí naplněnou Item, která reprezentuje nově přidaný záznam.
	 *
	 * <p>Pozor! Vytvořená Item se nepřidá přímo do kontaineru, pouze se z ní kopírují hodnoty</p>
	 *
	 * @param newItemCaption text zadaný uživatelem
	 * @return nová vyplněná Item do kontaineru
	 */
	public AutocompleteItem createNewItem(String newItemCaption) {
		return new AutocompleteItem(NEW_ITEM_ID, newItemCaption);
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeVariables(final Object source, final Map<String, Object> variables) {
        if (variables.containsKey("filter")) {
            final String text = variables.get("filter").toString();
            fireEvent(new FieldEvents.TextChangeEvent(this) {

                @Override
                public String getText() {
                    return text;
                }

                @Override
                public int getCursorPosition() {
                    return text.length();
                }
            });
        }
        super.changeVariables(source, variables);
    }

    /**
     * Přidá listener který zachytí změnu textu.
     *
     * @param listener listner který zachytí změnu textu
     */
    public void addTextChangeListener(final FieldEvents.TextChangeListener listener) {
        Assert.notNull(listener);

        addListener(FieldEvents.TextChangeListener.EVENT_ID, FieldEvents.TextChangeEvent.class,
                listener, FieldEvents.TextChangeListener.EVENT_METHOD);
    }

    /**
     * Odebere listener který zachytí změnu textu.
     *
     * @param listener listener který bude odebrán
     */
    public void removeTextChangeListener(final FieldEvents.TextChangeListener listener) {
        Assert.notNull(listener);

        removeListener(FieldEvents.TextChangeListener.EVENT_ID, FieldEvents.TextChangeEvent.class,
                listener);
    }
}
