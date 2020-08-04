package cz.tacr.elza.controller.vo;

import java.util.LinkedList;
import java.util.List;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApType;

import javax.annotation.Nullable;


/**
 * VO pro Číselník typů rejstříkových hesel.
 *
 * @since 21.12.2015
 */
public class ApTypeVO
        extends BaseCodeVo {
    /**
     * Příznak, zda může daný typ rejstříku obsahovat hesla nebo se jedná jen o "nadtyp".
     */
    private Boolean addRecord;
    /**
     * Odkaz na sebe sama (hierarchie typů rejstříků).
     */
    private Integer parentApTypeId;
    /**
     * Seznam potomků.
     */
    private List<ApTypeVO> children;

    private List<Integer> relationRoleTypIds;

    /**
     * Seznam rodičů seřazený od přímého rodiče po kořen.
     */
    @Deprecated
    // TODO: change to parent reference or id
    private List<String> parents;

    public Boolean getAddRecord() {
        return addRecord;
    }

    public void setAddRecord(final Boolean addRecord) {
        this.addRecord = addRecord;
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

    @Deprecated
    public List<String> getParents() {
        return parents;
    }

    @Deprecated
    public void setParents(final List<String> parents) {
        this.parents = parents;
    }

    @Deprecated
    public void addParent(final String parentName){
        if(parents == null){
            parents = new LinkedList<>();
        }
        parents.add(parentName);
    }

    @Deprecated
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

    /**
     * Creates value object from AP type. Hierarchy is not set.
     */
    public static ApTypeVO newInstance(ApType src, StaticDataProvider staticData) {
        ApTypeVO vo = new ApTypeVO();
        vo.setAddRecord(!src.isReadOnly());
        vo.setCode(src.getCode());
        vo.setId(src.getApTypeId());
        vo.setName(src.getName());
        vo.setParentApTypeId(src.getParentApTypeId());
        return vo;
    }
}
