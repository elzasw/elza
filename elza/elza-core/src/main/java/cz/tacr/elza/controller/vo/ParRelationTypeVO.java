package cz.tacr.elza.controller.vo;

import java.util.LinkedList;
import java.util.List;

import cz.tacr.elza.api.UseUnitdateEnum;


/**
 * VO pro typů vztahu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public class ParRelationTypeVO {

    /**
     * Id.
     */
    private Integer id;
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
    private ParRelationClassTypeVO relationClassType;

    private List<ParRelationRoleTypeVO> relationRoleTypes;

    /**
     * Způsob použití datace.
     */
    private UseUnitdateEnum useUnitdate;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
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

    public ParRelationClassTypeVO getRelationClassType() {
        return relationClassType;
    }

    public void setRelationClassType(final ParRelationClassTypeVO relationClassType) {
        this.relationClassType = relationClassType;
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

    public UseUnitdateEnum getUseUnitdate() {
        return useUnitdate;
    }

    public void setUseUnitdate(final UseUnitdateEnum useUnitdate) {
        this.useUnitdate = useUnitdate;
    }
}
