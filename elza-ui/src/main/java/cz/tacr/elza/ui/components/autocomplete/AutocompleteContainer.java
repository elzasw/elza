package cz.tacr.elza.ui.components.autocomplete;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.vaadin.data.Container;
import com.vaadin.data.ContainerHelpers;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.data.util.filter.UnsupportedFilterException;
import com.vaadin.server.Resource;


/**
 * Speciální kontainer pro komponentu {@link Autocomplete} umožnující načítání položek podle zadaného textu.
 *
 * @author <a href="mailto:jaroslav.pubal@marbes.cz">Jaroslav Půbal</a>
 * @see Autocomplete
 * @see AutocompleteItemLoader
 * @since 15.5.13
 */
class AutocompleteContainer implements Container.Filterable, Container.Indexed, Container.ItemSetChangeNotifier {

	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Implementace plnění itemů.
	 */
	private AutocompleteItemLoader itemLoader;

	/**
	 * Načtené Itemy pro aktuální filter.
	 */
	List<AutocompleteItem> lastLoadedItems = new ArrayList<AutocompleteItem>();

	/**
	 * Event ItemSetChangeListener.
	 */
	private List<ItemSetChangeListener> itemSetChangeListeners = new ArrayList<ItemSetChangeListener>();


	/**
	 * Konstuktor.
	 *
	 * @param itemLoader implementace plniče itemů
	 */
	public AutocompleteContainer(final AutocompleteItemLoader itemLoader) {

		Assert.notNull(itemLoader);
		this.itemLoader = itemLoader;
	}


	@Override
	public void addContainerFilter(final Filter filter) throws UnsupportedFilterException {
		try {
			lastLoadedItems = itemLoader.loadItems(((SimpleStringFilter) filter).getFilterString());
		} catch (Exception ex) {
			//pokud v asynchronim nacitani polozek dojde k vyjimce, UI zobrazi nekonecny indikator cekani
			logger.error(ex.getMessage(), ex);
		}
		fireItemSetChange();
	}

	@Override
	public void removeContainerFilter(final Filter filter) {
		//zde je chyba pri odmazani posledniho znaku z filtru se nezavola
	}

	@Override
	public void removeAllContainerFilters() {
		//nikdy se nevola
		try {
			lastLoadedItems = itemLoader.loadItems("");
		} catch (Exception ex) {
			//pokud v asynchronim nacitani polozek dojde k vyjimce, UI zobrazi nekonecny indikator cekani
			logger.error(ex.getMessage(), ex);
		}
		fireItemSetChange();
	}

	@Override
	public Collection<Filter> getContainerFilters() {
		return Collections.emptyList();
	}

	@Override
	public Item getItem(final Object itemId) {
		for (AutocompleteItem item : lastLoadedItems) {
			if (item.getId().equals(itemId)) {
				return new BeanItem<Object>(item);
			}
		}
		return null;
	}


	@Override
	public Collection<?> getContainerPropertyIds() {
		return Arrays.asList(AutocompleteItem.ID, AutocompleteItem.CAPTION, AutocompleteItem.ICON);
	}

	@Override
	public Collection<?> getItemIds() {
		List<Object> ids = new ArrayList<Object>();
		for (AutocompleteItem item : lastLoadedItems) {
			ids.add(item.getId());
		}

		return ids;
	}

	@Override
	public Property getContainerProperty(final Object itemId, final Object propertyId) {
		for (AutocompleteItem item : lastLoadedItems) {
			if (item.getId().equals(itemId)) {
				if (propertyId.equals(AutocompleteItem.ID)) {
					return new ObjectProperty<Object>(item.getId());
				} else if (propertyId.equals(AutocompleteItem.CAPTION)) {
					return new ObjectProperty<String>(item.getCaption(), String.class);
				} else if (propertyId.equals(AutocompleteItem.ICON)) {
					return new ObjectProperty<Resource>(item.getIcon(), Resource.class);
				} else {
					return null;
				}
			}
		}
		return null;
	}

	@Override
	public Class<?> getType(final Object propertyId) {
		if (propertyId.equals(AutocompleteItem.CAPTION)) {
			return String.class;
		}
		if (propertyId.equals(AutocompleteItem.ICON)) {
			return Resource.class;
		}
		return Object.class;
	}

	@Override
	public int size() {
		return lastLoadedItems.size();
	}

	@Override
	public boolean containsId(final Object itemId) {
		for (AutocompleteItem item : lastLoadedItems) {
			if (ObjectUtils.equals(item.getId(), itemId)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Item addItem(final Object itemId) throws UnsupportedOperationException {
		AutocompleteItem item = new AutocompleteItem(itemId, "");
		lastLoadedItems.add(0, item);
		fireItemSetChange();
		return new BeanItem<Object>(item);
	}

	@Override
	public Object addItem() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeItem(final Object itemId) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addContainerProperty(final Object propertyId, final Class<?> type, final Object defaultValue)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeContainerProperty(final Object propertyId) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAllItems() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int indexOfId(final Object itemId) {
		int index = -1;
		for (int i = 0; i < lastLoadedItems.size(); i++) {
			AutocompleteItem item = lastLoadedItems.get(i);
			if (item.getId().equals(itemId)) {
				index = i;
				break;
			}
		}
		return index;
	}

	@Override
	public Object getIdByIndex(final int index) {
		return lastLoadedItems.get(index).getId();
	}

	@Override
	public List<?> getItemIds(final int startIndex, final int numberOfItems) {
		return ContainerHelpers.getItemIdsUsingGetIdByIndex(startIndex, numberOfItems, this);
	}

	@Override
	public Object addItemAt(final int index) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Item addItemAt(final int index, final Object newItemId) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object nextItemId(final Object itemId) {
		return (Integer) itemId < lastLoadedItems.size() - 1 ? (Integer) itemId + 1 : null;
	}

	@Override
	public Object prevItemId(final Object itemId) {
		return (Integer) itemId > 0 ? (Integer) itemId - 1 : null;
	}

	@Override
	public Object firstItemId() {
		return 0;
	}

	@Override
	public Object lastItemId() {
		return lastLoadedItems.size() - 1;
	}

	@Override
	public boolean isFirstId(final Object itemId) {
		return (Integer) itemId == 0;
	}

	@Override
	public boolean isLastId(final Object itemId) {
		return (Integer) itemId == lastLoadedItems.size() - 1;
	}

	@Override
	public Object addItemAfter(final Object previousItemId) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Item addItemAfter(final Object previousItemId, final Object newItemId) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}


	//-------------- events --------------------

	/**
	 * @see ItemSetChangeNotifier#addItemSetChangeListener(ItemSetChangeListener)
	 */
	public void addItemSetChangeListener(Container.ItemSetChangeListener listener) {
		itemSetChangeListeners.add(listener);
	}

	/**
	 * @see ItemSetChangeNotifier#addListener(com.vaadin.data.Container.ItemSetChangeListener)
	 */
	@Override
	public void addListener(ItemSetChangeListener listener) {
		itemSetChangeListeners.add(listener);
	}

	/**
	 * @see ItemSetChangeNotifier#removeItemSetChangeListener(ItemSetChangeListener)
	 */
	@Override
	public void removeItemSetChangeListener(ItemSetChangeListener listener) {
		itemSetChangeListeners.remove(listener);
	}

	/**
	 * @see ItemSetChangeNotifier#removeListener(ItemSetChangeListener)
	 */
	@Override
	public void removeListener(ItemSetChangeListener listener) {
		itemSetChangeListeners.remove(listener);
	}

	/**
	 * Pošle event o změně položek v kontaineru.
	 */
	protected void fireItemSetChange() {
		final Object[] l = itemSetChangeListeners.toArray();
		for (int i = 0; i < l.length; i++) {
			((Container.ItemSetChangeListener) l[i]).containerItemSetChange(new ItemSetChangeEvent(this));
		}
	}


	/**
	 * Event o změně položek v konteineru.
	 */
	public static class ItemSetChangeEvent extends EventObject implements
			Container.ItemSetChangeEvent, Serializable {

		protected ItemSetChangeEvent(Container source) {
			super(source);
		}

		@Override
		public Container getContainer() {
			return (Container) getSource();
		}
	}
}
