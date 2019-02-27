package cz.tacr.elza.packageimport.xml;

import java.util.ArrayList;
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

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecRegister;
import cz.tacr.elza.packageimport.ItemTypeUpdater;
import cz.tacr.elza.repository.ItemSpecRegisterRepository;


/**
 * ItemSpec.
 *
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "item-spec")
public class ItemSpec {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "item-type", required = true)
    private String itemType;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "shortcut", required = true)
    private String shortcut;

    @XmlElement(name = "description", required = true)
    private String description;

    @XmlElement(name = "item-spec-register")
    @XmlElementWrapper(name = "item-spec-registers")
    private List<ItemSpecRegister> itemSpecRegisters;

    @XmlElement(name = "category")
    @XmlElementWrapper(name = "categories")
    private List<Category> categories;

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

    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(final String shortcut) {
        this.shortcut = shortcut;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<ItemSpecRegister> getItemSpecRegisters() {
        return itemSpecRegisters;
    }

    public void setItemSpecRegisters(final List<ItemSpecRegister> itemSpecRegisters) {
        this.itemSpecRegisters = itemSpecRegisters;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(final List<Category> categories) {
        this.categories = categories;
    }

    /**
     * Převod DAO na VO specifikace.
     *
     * @param rulDescItemSpec
     *            DAO specifikace
     * @return
     */
    public static ItemSpec fromEntity(RulItemSpec rulDescItemSpec,
                                          ItemSpecRegisterRepository itemSpecRegisterRepository) {
        ItemSpec itemSpec = new ItemSpec();
        itemSpec.setCode(rulDescItemSpec.getCode());
        itemSpec.setName(rulDescItemSpec.getName());
        itemSpec.setDescription(rulDescItemSpec.getDescription());
        itemSpec.setItemType(rulDescItemSpec.getItemType().getCode());
        itemSpec.setShortcut(rulDescItemSpec.getShortcut());

        List<RulItemSpecRegister> rulItemSpecRegisters = itemSpecRegisterRepository
                .findByDescItemSpecId(rulDescItemSpec);

        List<ItemSpecRegister> itemSpecRegisterList = new ArrayList<>(rulItemSpecRegisters.size());

        for (RulItemSpecRegister rulItemSpecRegister : rulItemSpecRegisters) {
            ItemSpecRegister itemSpecRegister = ItemSpecRegister.fromEntity(rulItemSpecRegister);
            itemSpecRegisterList.add(itemSpecRegister);
        }

        itemSpec.setItemSpecRegisters(itemSpecRegisterList);

        if (StringUtils.isNotEmpty(rulDescItemSpec.getCategory())) {
            String[] categoriesString = rulDescItemSpec.getCategory().split("\\" + ItemTypeUpdater.CATEGORY_SEPARATOR);
            List<Category> categories = Arrays.asList(categoriesString).stream()
                    .map(s -> new Category(s))
                    .collect(Collectors.toList());
            itemSpec.setCategories(categories);
        }

        return itemSpec;
    }
}
