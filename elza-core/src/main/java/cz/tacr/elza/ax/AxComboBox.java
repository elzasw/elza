package cz.tacr.elza.ax;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.ComboBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 16.4.15
 */
public class AxComboBox<T> extends ComboBox {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    Function<T, String> itemCaptionFunction;

    public AxComboBox(String caption, Container dataSource, Function<T, String> itemCaption) {
        super(caption, dataSource);
        this.itemCaptionFunction = itemCaption;
    }

    public void setItemCaptionFunction(Function<T, String> itemCaptionFunction) {
        this.itemCaptionFunction = itemCaptionFunction;
    }

    @Override
    public String getItemCaption(Object itemId) {
        if (itemCaptionFunction == null) return super.getItemCaption(itemId);
        //TODO Burianek Null item text
        if (itemId == null) return null;
        try {
            Item item = getItem(itemId);
            if (item == null) return null;
            if (item instanceof BeanItem) {
                T bean = ((BeanItem<T>) item).getBean();
                return itemCaptionFunction.apply(bean);
            } else {
                throw new IllegalArgumentException("Invalid item " + item);
            }
        } catch (Exception ex) {
            logger.error("ComboBox itemCaptionFunction error", ex);
            return "#";
        }
    }
}
