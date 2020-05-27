package cz.tacr.elza.groovy;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroovyAppender {

    private final GroovyPart part;
    private final List<Item> items;
    private final GroovyItems groovyItems;

    public GroovyAppender(final GroovyPart part) {
        this.part = part;
        this.items = new ArrayList<>();
        this.groovyItems = new GroovyItems();
    }

    public GroovyAppender() {
        this.groovyItems = new GroovyItems();
        this.part = new GroovyPart(null, false, null, groovyItems, null);
        this.items = new ArrayList<>();
    }

    @NotNull
    public String build() {
        if (items.size() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Item item : items) {
            if (item instanceof StringPart) {
                StringPart stringPart = ((StringPart) item);
                if (StringUtils.isNotEmpty(stringPart.str)) {
                    StringBuilder sbi = new StringBuilder();
                    if (stringPart.separator != null) {
                        if (sb.length() > 0) {
                            sbi.append(stringPart.separator);
                        }
                    }
                    if (stringPart.prefix != null) {
                        sbi.append(stringPart.prefix);
                    }
                    sbi.append(stringPart.str);
                    if (stringPart.postfix != null) {
                        sbi.append(stringPart.postfix);
                    }
                    sb.append(sbi.toString());
                }
            } else if (item instanceof ItemPart) {
                ItemPart itemPart = ((ItemPart) item);
                if (CollectionUtils.isNotEmpty(itemPart.items)) {
                    StringBuilder sbi = new StringBuilder();
                    if (itemPart.separator != null) {
                        if (sb.length() > 0) {
                            sbi.append(itemPart.separator);
                        }
                    }
                    if (itemPart.prefix != null) {
                        sbi.append(itemPart.prefix);
                    }

                    for (GroovyItem groovyItem : itemPart.items) {
                        StringBuilder sgi = new StringBuilder();
                        if (itemPart.useSpec && groovyItem.getSpec() != null) {
                            sgi.append(groovyItem.getSpec());
                            sgi.append(itemPart.specSeparator);
                        }
                        sgi.append(groovyItem.getValue());
                        sbi.append(sgi.toString());
                    }

                    if (itemPart.postfix != null) {
                        sbi.append(itemPart.postfix);
                    }
                    sb.append(sbi.toString());
                }
            } else if (item instanceof BoolPart) {
                BoolPart boolPart = ((BoolPart) item);
                if (CollectionUtils.isNotEmpty(boolPart.items)) {
                    StringBuilder sbi = new StringBuilder();
                    if (boolPart.separator != null) {
                        if (sb.length() > 0) {
                            sbi.append(boolPart.separator);
                        }
                    }
                    if (boolPart.prefix != null) {
                        sbi.append(boolPart.prefix);
                    }

                    for (GroovyItem groovyItem : boolPart.items) {
                        StringBuilder sgi = new StringBuilder();
                        if (boolPart.useSpec && groovyItem.getSpec() != null) {
                            sgi.append(groovyItem.getSpec());
                            sgi.append(boolPart.specSeparator);
                        }
                        sgi.append(groovyItem.getBoolValue() ? boolPart.trueValue : boolPart.falseValue);
                        sbi.append(sgi.toString());
                    }

                    if (boolPart.postfix != null) {
                        sbi.append(boolPart.postfix);
                    }
                    sb.append(sbi.toString());
                }
            } else if (item instanceof IntPart) {
                IntPart intPart = ((IntPart) item);
                if (CollectionUtils.isNotEmpty(intPart.items)) {
                    StringBuilder sbi = new StringBuilder();
                    if (intPart.separator != null) {
                        if (sb.length() > 0) {
                            sbi.append(intPart.separator);
                        }
                    }
                    if (intPart.prefix != null) {
                        sbi.append(intPart.prefix);
                    }

                    for (GroovyItem groovyItem : intPart.items) {
                        StringBuilder sgi = new StringBuilder();
                        if (intPart.useSpec && groovyItem.getSpec() != null) {
                            sgi.append(groovyItem.getSpec());
                            sgi.append(intPart.specSeparator);
                        }
                        sgi.append(groovyItem.getIntValue());
                        sbi.append(sgi.toString());
                    }

                    if (intPart.postfix != null) {
                        sbi.append(intPart.postfix);
                    }
                    sb.append(sbi.toString());
                }
            }
        }
        return sb.toString();
    }

    public ItemPart add(final String itemTypeCode) {
        return addValidate(itemTypeCode, true);
    }

    private ItemPart addValidate(final String itemTypeCode, final boolean validate) {
        if (validate) {
            //TODO fantis
            //CodeEntityProvider.getItemType(itemTypeCode);
        }
        List<GroovyItem> groovyItems = part.getItems(itemTypeCode);
        ItemPart item = new ItemPart(groovyItems);
        items.add(item);
        return item;
    }

    public ItemPart add(@NotNull final GroovyItem groovyItem) {
        String typeCode = getUniqueCode(groovyItem.getTypeCode());
        groovyItems.addItem(typeCode, groovyItem);
        return addValidate(typeCode, false);
    }

    public BoolPart addBool(@NotNull final String itemTypeCode,
                            @NotNull final String trueValue,
                            @NotNull final String falseValue) {
        List<GroovyItem> groovyItems = part.getItems(itemTypeCode);
        BoolPart item = new BoolPart(groovyItems, trueValue, falseValue);
        items.add(item);
        return item;
    }

    public BoolPart addBool(@NotNull final GroovyItem groovyItem,
                            @NotNull final String trueValue,
                            @NotNull final String falseValue) {
        String typeCode = getUniqueCode(groovyItem.getTypeCode());
        groovyItems.addItem(typeCode, groovyItem);
        return addBool(typeCode, trueValue, falseValue);
    }

    public IntPart addInt(@NotNull final String itemTypeCode) {
        List<GroovyItem> groovyItems = part.getItems(itemTypeCode);
        IntPart item = new IntPart(groovyItems);
        items.add(item);
        return item;
    }

    public IntPart addInt(@NotNull final GroovyItem groovyItem) {
        String typeCode = getUniqueCode(groovyItem.getTypeCode());
        groovyItems.addItem(typeCode, groovyItem);
        return addInt(typeCode);
    }

    public StringPart addStr(final String value) {
        StringPart item = new StringPart(value);
        items.add(item);
        return item;
    }

    public String getUniqueCode(final String typeCode) {
        return typeCode + UUID.randomUUID().toString();
    }

    public interface Item {

    }

    public static class ItemPart implements Item {
        public static final String SPEC_SEPARATOR = ": ";

        private List<GroovyItem> items;

        private boolean useSpec = false;
        private String specSeparator;

        private String separator;
        private String prefix;
        private String postfix;

        private ItemPart(final List<GroovyItem> items) {
            this.items = items;
        }

        public ItemPart withSpec() {
            return withSpec(SPEC_SEPARATOR);
        }

        public ItemPart withSpec(@NotNull final String specSeparator) {
            this.useSpec = true;
            this.specSeparator = specSeparator;
            return this;
        }

        public ItemPart withSeparator(@NotNull final String separator) {
            this.separator = separator;
            return this;
        }

        public ItemPart withPostfix(@NotNull final String postfix) {
            this.postfix = postfix;
            return this;
        }

        public ItemPart withPrefix(@NotNull final String prefix) {
            this.prefix = prefix;
            return this;
        }

    }

    public static class StringPart implements Item {
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
    }

    public static class BoolPart implements Item {
        public static final String SPEC_SEPARATOR = ": ";

        private List<GroovyItem> items;

        private String trueValue;
        private String falseValue;

        private boolean useSpec = false;
        private String specSeparator;

        private String separator;
        private String prefix;
        private String postfix;

        private BoolPart(final List<GroovyItem> items, final String trueValue, final String falseValue) {
            this.items = items;
            this.trueValue = trueValue;
            this.falseValue = falseValue;
        }

        public BoolPart withSpec() {
            return withSpec(SPEC_SEPARATOR);
        }

        public BoolPart withSpec(@NotNull final String specSeparator) {
            this.useSpec = true;
            this.specSeparator = specSeparator;
            return this;
        }

        public BoolPart withSeparator(@NotNull final String separator) {
            this.separator = separator;
            return this;
        }

        public BoolPart withPostfix(@NotNull final String postfix) {
            this.postfix = postfix;
            return this;
        }

        public BoolPart withPrefix(@NotNull final String prefix) {
            this.prefix = prefix;
            return this;
        }

    }

    public static class IntPart implements Item {
        public static final String SPEC_SEPARATOR = ": ";

        private List<GroovyItem> items;

        private boolean useSpec = false;
        private String specSeparator;

        private String separator;
        private String prefix;
        private String postfix;

        private IntPart(final List<GroovyItem> items) {
            this.items = items;
        }

        public IntPart withSpec() {
            return withSpec(SPEC_SEPARATOR);
        }

        public IntPart withSpec(@NotNull final String specSeparator) {
            this.useSpec = true;
            this.specSeparator = specSeparator;
            return this;
        }

        public IntPart withSeparator(@NotNull final String separator) {
            this.separator = separator;
            return this;
        }

        public IntPart withPostfix(@NotNull final String postfix) {
            this.postfix = postfix;
            return this;
        }

        public IntPart withPrefix(@NotNull final String prefix) {
            this.prefix = prefix;
            return this;
        }

    }

}
