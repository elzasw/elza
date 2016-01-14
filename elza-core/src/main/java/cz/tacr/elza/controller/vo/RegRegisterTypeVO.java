package cz.tacr.elza.controller.vo;

import java.util.LinkedList;
import java.util.List;


/**
 * VO pro Číselník typů rejstříkových hesel.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public class RegRegisterTypeVO {

    /**
     * Id.
     */
    private Integer id;
    /**
     * Kód typu.
     */
    private String code;
    /**
     * Název typu.
     */
    private String name;
    /**
     * Příznak, zda rejstříková hesla tohoto typu rejstříku tvoří hierarchii.
     */
    private Boolean hierarchical;
    /**
     * Příznak, zda může daný typ rejstříku obsahovat hesla nebo se jedná jen o "nadtyp".
     */
    private Boolean addRecord;
    /**
     * Odkaz na sebe sama (hierarchie typů rejstříků).
     */
    private Integer parentRegisterTypeId;
    /**
     * Určení, zda hesla daného typu mohou být "abstraktní" osobou/původcem a jakého typu.
     */
    private ParPartyTypeVO partyType;
    /**
     * Seznam potomků.
     */
    private List<RegRegisterTypeVO> children;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

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

    public Boolean getHierarchical() {
        return hierarchical;
    }

    public void setHierarchical(final Boolean hierarchical) {
        this.hierarchical = hierarchical;
    }

    public Boolean getAddRecord() {
        return addRecord;
    }

    public void setAddRecord(final Boolean addRecord) {
        this.addRecord = addRecord;
    }

    public ParPartyTypeVO getPartyType() {
        return partyType;
    }

    public void setPartyType(final ParPartyTypeVO partyType) {
        this.partyType = partyType;
    }

    public Integer getParentRegisterTypeId() {
        return parentRegisterTypeId;
    }

    public void setParentRegisterTypeId(final Integer parentRegisterTypeId) {
        this.parentRegisterTypeId = parentRegisterTypeId;
    }

    public List<RegRegisterTypeVO> getChildren() {
        return children;
    }

    public void setChildren(final List<RegRegisterTypeVO> children) {
        this.children = children;
    }

    public void addChild(final RegRegisterTypeVO child) {
        if (children == null) {
            children = new LinkedList<>();
        }
        children.add(child);
    }
}
