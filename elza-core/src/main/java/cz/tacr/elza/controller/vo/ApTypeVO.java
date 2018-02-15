package cz.tacr.elza.controller.vo;

import java.util.LinkedList;
import java.util.List;


/**
 * VO pro Číselník typů rejstříkových hesel.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public class ApTypeVO {

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
     * Příznak, zda může daný typ rejstříku obsahovat hesla nebo se jedná jen o "nadtyp".
     */
    private Boolean addRecord;
    /**
     * Odkaz na sebe sama (hierarchie typů rejstříků).
     */
    private Integer parentApTypeId;
    /**
     * Určení, zda hesla daného typu mohou být "abstraktní" osobou/původcem a jakého typu.
     */
    private Integer partyTypeId;
    /**
     * Seznam potomků.
     */
    private List<ApTypeVO> children;

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

    public Integer getParentApTypeId() {
        return parentApTypeId;
    }

    public void setParentApTypeId(final Integer parentApTypeId) {
        this.parentApTypeId = parentApTypeId;
    }

    public List<ApTypeVO> getChildren() {
        return children;
    }

    public void setChildren(final List<ApTypeVO> children) {
        this.children = children;
    }

    public void addChild(final ApTypeVO child) {
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
