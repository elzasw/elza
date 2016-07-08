package cz.tacr.elza.controller.vo;

import java.util.LinkedList;
import java.util.List;


/**
 * VO pro Seznam typů vztahů.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public class ParRelationTypeVO {

    /**
     * Id.
     */
    private Integer relationTypeId;
    /**
     * Název.
     */
    private String name;
    /**
     * Kod.
     */
    private String code;
    /**
     * Typ třídy.
     */
    private String classType;

    private List<ParRelationRoleTypeVO> relationRoleTypes;

    public Integer getRelationTypeId() {
        return relationTypeId;
    }

    public void setRelationTypeId(final Integer relationTypeId) {
        this.relationTypeId = relationTypeId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getClassType() {
        return classType;
    }

    public void setClassType(final String classType) {
        this.classType = classType;
    }

    public List<ParRelationRoleTypeVO> getRelationRoleTypes() {
        return relationRoleTypes;
    }

    public void setRelationRoleTypes(final List<ParRelationRoleTypeVO> relationRoleTypes) {
        this.relationRoleTypes = relationRoleTypes;
    }


    public void addRelationRoleType(final ParRelationRoleTypeVO relationRoleTypeVO) {
        if (relationRoleTypes == null) {
            relationRoleTypes = new LinkedList<>();
        }
        relationRoleTypes.add(relationRoleTypeVO);
    }

}
