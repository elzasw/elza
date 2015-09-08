package cz.tacr.elza.ui.components.autocomplete;

import javax.annotation.Nullable;

import org.springframework.util.Assert;

import com.vaadin.server.Resource;


/**
 * Položka pro komponentu {@link Autocomplete}.
 *
 * Obsahuje ID pro databinding a caption pro zobrazení v komponentě.
 *
 * @author <a href="mailto:jaroslav.pubal@marbes.cz">Jaroslav Půbal</a>
 * @since 15.5.13
 * @see AutocompleteItemLoader
 */
public class AutocompleteItem {

	static final String ID = "id";
	static final String CAPTION = "caption";
	static final String ICON = "icon";

	/**
	 * Identifikator položky pro databinding.
	 */
	private Object id;

	/**
	 * Vizuální reprezentace položky.
	 */
	private String caption;

	/**
	 * Ikona položky.
	 */
	private Resource icon;

	/**
	 * Konstuktor.
	 *
	 * @param id klíč položky
	 * @param caption popis položky
	 */
	public AutocompleteItem(Object id, @Nullable String caption) {
		this(id, caption, null);
	}

	/**
	 * Konstuktor.
	 *
	 * @param id klíč položky
	 * @param caption popis položky
     * @param icon ikona položky
	 */
	public AutocompleteItem(Object id, @Nullable String caption, @Nullable Resource icon) {
        Assert.notNull(id);

		this.id = id;
		this.caption = caption;
		this.icon = icon;
	}

	/**
	 * Klíč položky.
	 *
	 * @return key
	 */
	public Object getId() {
		return id;
	}

	/**
	 * Nastaví klíč položky
	 *
	 * @param id klíč položky
	 */
	public void setId(Object id) {
		this.id = id;
	}

	/**
	 * Popis položky.
	 *
	 * @return popis položky
	 */
	public String getCaption() {
		return caption;
	}

	/**
	 * Nastaví popis položky.
	 *
	 * @param caption popis položky
	 */
	public void setCaption(String caption) {
		this.caption = caption;
	}

	/**
	 * Ikona položky.
	 *
	 * @return ikona položky
	 */
	@Nullable
	public Resource getIcon() {
		return icon;
	}

	/**
	 * Nastaví ikonu položky, nebo NULL pro zádnou ikonu.
	 *
	 * @param icon ikona
	 */
	public void setIcon(@Nullable Resource icon) {
		this.icon = icon;
	}
}
