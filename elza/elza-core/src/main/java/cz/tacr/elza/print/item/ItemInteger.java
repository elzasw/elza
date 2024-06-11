package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.integer.DisplayType;

/**
 * Integer item for print
 */
public class ItemInteger extends AbstractItem {

    private final Integer value;

    public ItemInteger(final Integer value) {
        this.value = value;
    }

    @Override
    public String getSerializedValue() {
        Object viewDef = getType().getViewDefinition();
        if (viewDef != null && viewDef == DisplayType.DURATION) {
            // format as xx:xx:xx
            int hour = value / 3600;
            int minute = (value / 60) % 60;
            int sec = value % 60;

            StringBuilder sb = new StringBuilder();
            sb.append(hour).append(":");

            if (minute < 10) {
                sb.append("0");
            }
            sb.append(minute).append(":");

            if (sec < 10) {
                sb.append("0");
            }
            sb.append(sec);
            return sb.toString();
        }
        return value.toString();
    }

    @Override
    protected Integer getValue() {
        return value;
    }

    public Integer getIntegerValue() {
        return value;
    }
}
