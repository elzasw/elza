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
    private Integer partyTypeId;
    /**
     * Seznam potomků.
     */
    private List<RegRegisterTypeVO> children;

    private List<Integer> relationRoleTypIds;

    /**
     * Seznam rodičů seřazený od přímého rodiče po kořen.
     */
    private List<String> parents;

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

    public Integer getPartyTypeId() {
        return partyTypeId;
    }

    public void setPartyTypeId(final Integer partyTypeId) {
        this.partyTypeId = partyTypeId;
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

    public List<String> getParents() {
        return parents;
    }

    public void setParents(final List<String> parents) {
        this.parents = parents;
    }

    public void addParent(final String parentName){
        if(parents == null){
            parents = new LinkedList<>();
        }
        parents.add(parentName);
    }

    public void addParents(final List<String> nextParents){
        if(parents == null){
            parents = new LinkedList<>();
        }
        if(nextParents != null){
            parents.addAll(nextParents);
        }
    }

    public List<Integer> getRelationRoleTypIds() {
        return relationRoleTypIds;
    }

    public void setRelationRoleTypIds(final List<Integer> relationRoleTypIds) {
        this.relationRoleTypIds = relationRoleTypIds;
    }
}
