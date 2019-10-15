package cz.tacr.elza.packageimport.xml;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.StringUtils;

import cz.tacr.elza.domain.RulItemAptype;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.packageimport.ItemTypeUpdater;
import cz.tacr.elza.repository.ItemAptypeRepository;

/**
 * ItemSpec.
 *
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "item-spec")
public class ItemSpec {

    // --- fields ---

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "item-type", required = true)
    private String itemType;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "description", required = true)
    private String description;

    @XmlElement(name = "shortcut", required = true)
    private String shortcut;

    @XmlElement(name = "item-aptype")
    @XmlElementWrapper(name = "item-aptypes")
    private List<ItemAptype> itemAptypes;

    @XmlElement(name = "category")
    @XmlElementWrapper(name = "categories")
    private List<Category> categories;

    // --- getters/setters ---

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(final String itemType) {
        this.itemType = itemType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(final String shortcut) {
        this.shortcut = shortcut;
    }

    public List<ItemAptype> getItemAptypes() {
        return itemAptypes;
    }

    public void setItemAptypes(final List<ItemAptype> itemAptypes) {
        this.itemAptypes = itemAptypes;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(final List<Category> categories) {
        this.categories = categories;
    }

    // --- methods ---

    /**
     * Převod DAO na VO specifikace.
     *
     * @param rulDescItemSpec DAO specifikace
     */
    public static ItemSpec fromEntity(RulItemSpec rulDescItemSpec, ItemAptypeRepository itemAptypeRepository) {

        ItemSpec itemSpec = new ItemSpec();
        itemSpec.setCode(rulDescItemSpec.getCode());
        itemSpec.setName(rulDescItemSpec.getName());
        itemSpec.setDescription(rulDescItemSpec.getDescription());
        itemSpec.setItemType(rulDescItemSpec.getItemType().getCode());
        itemSpec.setShortcut(rulDescItemSpec.getShortcut());

        List<RulItemAptype> itemAptypes = itemAptypeRepository.findByItemSpec(rulDescItemSpec);
        if (!itemAptypes.isEmpty()) {
            itemSpec.setItemAptypes(itemAptypes.stream().map(ItemAptype::fromEntity).collect(Collectors.toList()));
        }

        if (StringUtils.isNotEmpty(rulDescItemSpec.getCategory())) {
            String[] categoriesString = rulDescItemSpec.getCategory().split("\\" + ItemTypeUpdater.CATEGORY_SEPARATOR);
            List<Category> categories = Arrays.stream(categoriesString)
                    .map(s -> new Category(s))
                    .collect(Collectors.toList());
            itemSpec.setCategories(categories);
        }

        return itemSpec;
    }
}
