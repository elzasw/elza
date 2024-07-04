package cz.tacr.elza.groovy;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.domain.convertor.UnitDateConvertorConsts;

/**
 * Build string value for future Item
 * 
 * Appender is always connected with GroovyPart and thus has link
 * to StaticDataProvider and all items already existing in Part
 */
public class GroovyAppender {

    /**
     * Appender item
     * Typically static string or some conditional item
     */
    public interface AppenderItem {

        /**
         * Build item and add it to the builder
         * 
         * @param sb
         */
        void build(StringBuilder sb);
    }

    abstract public static class BaseItemAppender implements AppenderItem {
        public static final String SPEC_SEPARATOR = ": ";

        private boolean useSpec = false;
        private String specSeparator;

        protected String separator;
        protected String prefix;
        protected String postfix;
        protected final List<GroovyItem> items;

        BaseItemAppender(final List<GroovyItem> items) {
            this.items = items;
        }

        /**
         * Append single item to the result
         * 
         * @param sb
         * @param item
         */
        abstract protected void buildItem(StringBuilder sb, GroovyItem item);

        public BaseItemAppender withSpec() {
            return withSpec(SPEC_SEPARATOR);
        }

        public BaseItemAppender withSpec(@NotNull final String specSeparator) {
            this.useSpec = true;
            this.specSeparator = specSeparator;
            return this;
        }

        public BaseItemAppender withSeparator(@NotNull final String separator) {
            this.separator = separator;
            return this;
        }

        public BaseItemAppender withPostfix(@NotNull final String postfix) {
            this.postfix = postfix;
            return this;
        }

        public BaseItemAppender withPrefix(@NotNull final String prefix) {
            this.prefix = prefix;
            return this;
        }

        public void build(StringBuilder sb) {
            if (CollectionUtils.isEmpty(items)) {
                return;
            }

            if (separator != null) {
                if (sb.length() > 0) {
                    sb.append(separator);
                }
            }
            if (prefix != null) {
                sb.append(prefix);
            }

            for (GroovyItem groovyItem : items) {
                if (useSpec && groovyItem.getSpec() != null) {
                    sb.append(groovyItem.getSpec());
                    sb.append(specSeparator);
                }

                buildItem(sb, groovyItem);
                // todo: how to separate multiple same items?? 
            }

            if (postfix != null) {
                sb.append(postfix);
            }
        }

    }

    public static class StringPart implements AppenderItem {
        private String str;

        private String separator;
        private String prefix;
        private String postfix;

        public StringPart(@NotNull final String str) {
            this.str = str;
        }

        public StringPart withSeparator(@NotNull final String separator) {
            this.separator = separator;
            return this;
        }

        public StringPart withPostfix(@NotNull final String postfix) {
            this.postfix = postfix;
            return this;
        }

        public StringPart withPrefix(@NotNull final String prefix) {
            this.prefix = prefix;
            return this;
        }

        public void build(StringBuilder sb) {
            if (StringUtils.isNotEmpty(str)) {
                StringBuilder sbi = new StringBuilder();
                if (separator != null) {
                    if (sb.length() > 0) {
                        sbi.append(separator);
                    }
                }
                if (prefix != null) {
                    sbi.append(prefix);
                }
                sbi.append(str);
                if (postfix != null) {
                    sbi.append(postfix);
                }
                sb.append(sbi.toString());
            }
        }

    }

    public static class ItemPart extends BaseItemAppender {

        private ItemPart(final List<GroovyItem> items) {
            super(items);
        }

        @Override
        public void buildItem(StringBuilder sb, GroovyItem item) {
            sb.append(item.getValue());
        }

    }

    public static class BoolPart extends BaseItemAppender {
        private String trueValue;
        private String falseValue;

        private BoolPart(final List<GroovyItem> items, final String trueValue, final String falseValue) {
            super(items);
            this.trueValue = trueValue;
            this.falseValue = falseValue;
        }

        @Override
        public void buildItem(StringBuilder sb, GroovyItem item) {
            sb.append(item.getBoolValue() ? trueValue : falseValue);
        }

    }

    public static class IntPart extends BaseItemAppender {

        private IntPart(final List<GroovyItem> items) {
            super(items);
        }

        @Override
        public void buildItem(StringBuilder sb, GroovyItem item) {
            sb.append(item.getIntValue());
        }
    }

    public static class UnitdatePart extends BaseItemAppender {

        private boolean from;

        private UnitdatePart(final List<GroovyItem> items, boolean from) {
            super(items);
            this.from = from;
        }

        public boolean isFrom() {
            return from;
        }

        @Override
        public void buildItem(StringBuilder sb, GroovyItem item) {
            ArrDataUnitdate dataUnitdate = ArrDataUnitdate
                    .valueOf(UnitDateConvertor.convertToString(item.getUnitdateValue()));
            long normalizedDataFrom = dataUnitdate.getNormalizedFrom()
                    + UnitDateConvertorConsts.MAX_NEGATIVE_DATE;
            sb.append(String.format("%019d", normalizedDataFrom));
        }

        @Override
        public void build(StringBuilder sb) {
            if (CollectionUtils.isEmpty(items)) {
                sb.append(String.format("%019d", isFrom() ? 0 : Long.MAX_VALUE));
            } else {
                super.build(sb);
            }
        }

    }

    // TOOD: What is it for?
    public static class ViewOrderPart implements AppenderItem {

        private List<GroovyItem> items;

        private ViewOrderPart(final List<GroovyItem> items) {
            this.items = items;
        }

        @Override
        public void build(StringBuilder sb) {
            if (CollectionUtils.isNotEmpty(items)) {
                StringBuilder sbi = new StringBuilder();
                for (GroovyItem groovyItem : items) {
                    Integer viewOrder = groovyItem.getSpecOrder();
                    if (viewOrder == null) {
                        viewOrder = 0;
                    }
                    sbi.append(String.format("%010d", viewOrder));
                }
                sb.append(sbi.toString());
            } else {
                sb.append(String.format("%010d", 0));
            }
        }

    }

    /**
     * Part to be build
     */
    private final GroovyPart part;

    /**
     * List appender items.
     * 
     * These items will be used to build an items
     */
    private final List<AppenderItem> items = new ArrayList<>();

    /**
     * Collection of groovy items
     */
    private final GroovyItems groovyItems = new GroovyItems();

    public GroovyAppender(final GroovyPart part) {
        this.part = part;
    }

    @NotNull
    public String build() {
        if (CollectionUtils.isEmpty(items)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (AppenderItem item : items) {
            item.build(sb);
        }
        return sb.toString();
    }

    public ItemPart add(final String itemTypeCode) {
        List<GroovyItem> groovyItems = part.getItems(itemTypeCode);
        ItemPart item = new ItemPart(groovyItems);
        items.add(item);
        return item;
    }

    public BoolPart addBool(@NotNull final String itemTypeCode,
                            @NotNull final String trueValue,
                            @NotNull final String falseValue) {
        List<GroovyItem> groovyItems = part.getItems(itemTypeCode);
        BoolPart item = new BoolPart(groovyItems, trueValue, falseValue);
        items.add(item);
        return item;
    }

    public IntPart addInt(@NotNull final String itemTypeCode) {
        List<GroovyItem> groovyItems = part.getItems(itemTypeCode);
        IntPart item = new IntPart(groovyItems);
        items.add(item);
        return item;
    }

    public StringPart addStr(final String value) {
        StringPart item = new StringPart(value);
        items.add(item);
        return item;
    }

    public UnitdatePart addUnitdateFrom(@NotNull final String itemTypeCode) {
        return addUnitdate(itemTypeCode, true);
    }

    public UnitdatePart addUnitdateTo(@NotNull final String itemTypeCode) {
        return addUnitdate(itemTypeCode, false);
    }

    public UnitdatePart addUnitdate(@NotNull final String itemTypeCode, final boolean from) {
        List<GroovyItem> groovyItems = part.getItems(itemTypeCode);
        UnitdatePart item = new UnitdatePart(groovyItems, from);
        items.add(item);
        return item;
    }

    public ViewOrderPart addViewOrder(@NotNull final String itemTypeCode) {
        List<GroovyItem> groovyItems = part.getItems(itemTypeCode);
        ViewOrderPart item = new ViewOrderPart(groovyItems);
        items.add(item);
        return item;
    }
}
