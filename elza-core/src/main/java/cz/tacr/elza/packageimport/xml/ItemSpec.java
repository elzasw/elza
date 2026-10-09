package cz.tacr.elza.packageimport.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.domain.RulItemAptype;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecAssignDeclaration;
import cz.tacr.elza.domain.RulItemSpecDeclaration;
import cz.tacr.elza.domain.RulItemTypeSpecAssign;
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

    @XmlElement(name = "item-type-assign")
    private List<ItemTypeAssign> itemTypeAssigns;

    // --- getters/setters ---

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
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

    public List<ItemTypeAssign> getItemTypeAssigns() {
        return itemTypeAssigns;
    }

    public void setItemTypeAssigns(final List<ItemTypeAssign> itemTypeAssigns) {
        this.itemTypeAssigns = itemTypeAssigns;
    }

    // --- methods ---

    /**
     * Převod DAO na VO specifikace.
     *
     * @param rulDescItemSpec
     *            DAO specifikace
     * @param assignments
     *            assignments of the specification to item types
     */
    /**
     * The specification as the package declares it: texts, category and assignments of the declaration;
     * RECORD_REF classes for the owner of the specification only.
     */
    public static ItemSpec fromDeclaration(final RulItemSpecDeclaration declaration,
                                           final List<RulItemSpecAssignDeclaration> assignments,
                                           final List<RulItemAptype> itemAptypes) {
        ItemSpec itemSpec = new ItemSpec();
        itemSpec.setCode(declaration.getItemSpec().getCode());
        itemSpec.setName(declaration.getName());
        itemSpec.setDescription(declaration.getDescription());
        itemSpec.setShortcut(declaration.getShortcut());
        if (!itemAptypes.isEmpty()) {
            itemSpec.setItemAptypes(itemAptypes.stream().map(ItemAptype::fromEntity).collect(Collectors.toList()));
        }
        if (StringUtils.isNotEmpty(declaration.getCategory())) {
            itemSpec.setCategories(Arrays.stream(declaration.getCategory().split("\\" + ItemTypeUpdater.CATEGORY_SEPARATOR))
                    .map(Category::new)
                    .collect(Collectors.toList()));
        }
        if (!assignments.isEmpty()) {
            List<ItemTypeAssign> itemTypesAssigns = new ArrayList<>();
            for (RulItemSpecAssignDeclaration assignment : assignments) {
                ItemTypeAssign assign = new ItemTypeAssign();
                assign.setCode(assignment.getItemType().getCode());
                assign.setViewAfter(assignment.getViewAfterSpecCode());
                itemTypesAssigns.add(assign);
            }
            itemSpec.setItemTypeAssigns(itemTypesAssigns);
        }
        return itemSpec;
    }

    public static ItemSpec fromEntity(RulItemSpec rulDescItemSpec,
                                      final List<RulItemTypeSpecAssign> assignments,
                                      ItemAptypeRepository itemAptypeRepository) {

        ItemSpec itemSpec = new ItemSpec();
        itemSpec.setCode(rulDescItemSpec.getCode());
        itemSpec.setName(rulDescItemSpec.getName());
        itemSpec.setDescription(rulDescItemSpec.getDescription());
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

        if (CollectionUtils.isNotEmpty(assignments)) {
            List<ItemTypeAssign> itemTypesAssigns = new ArrayList<>();
            for (RulItemTypeSpecAssign assignment : assignments) {
                itemTypesAssigns.add(ItemTypeAssign.fromEntity(assignment));
            }
            itemSpec.setItemTypeAssigns(itemTypesAssigns);
        }
        return itemSpec;
    }
}
