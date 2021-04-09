package cz.tacr.elza.print.format;

import java.util.Iterator;

import cz.tacr.elza.print.item.Item;

/**
 * Static class with helper methods for print
 *
 */
abstract public class Helper {
    private Helper() {

    }

    /**
     * Return first real object
     * 
     * Collection are iterate to find first object
     * 
     * @param object
     * @return
     */
    @SuppressWarnings("rawtypes")
    static public Object getFirstObject(Object object) {
        if (object == null) {
            return null;
        }
        Class<? extends Object> clz = object.getClass();
        if (Iterable.class.isAssignableFrom(clz)) {
            Iterable itbl = (Iterable) object;
            return getFirstObject(itbl.iterator());
        }
        if (Iterator.class.isAssignableFrom(clz)) {
            Iterator it = (Iterator) object;
            while (it.hasNext()) {
                Object itObj = it.next();
                Object result = getFirstObject(itObj);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
        return object;
    }

    static public String getFirstStringOrDefault(Object object, String defaultValue) {
        Object realObj = getFirstObject(object);
        if (realObj == null) {
            return defaultValue;
        }
        Class<? extends Object> realObjClz = realObj.getClass();
        if (Item.class.isAssignableFrom(realObjClz)) {
            Item item = (Item) realObj;
            return item.getSerializedValue();
        }
        return realObj.toString();
    }
}
