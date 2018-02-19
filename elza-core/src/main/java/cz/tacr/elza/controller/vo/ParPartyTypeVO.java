package cz.tacr.elza.controller.vo;

import java.util.LinkedList;
import java.util.List;

import cz.tacr.elza.domain.ParPartyType;


/**
 * VO objekt pro {@link ParPartyType}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public class ParPartyTypeVO {

    /**
     * Id.
     */
    private Integer id;

    /**
     * Kod typu osoby.
     */
    private String code;
    /**
     * Název typu osoby.
     */
    private String name;

    /**
     * Popis typu osoby.
     */
    private String description;

    private List<ParRelationTypeVO> relationTypes;
    private List<ParComplementTypeVO> complementTypes;
    private List<ApTypeVO> apTypes;
    private List<UIPartyGroupVO> partyGroups;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<ParRelationTypeVO> getRelationTypes() {
        return relationTypes;
    }

    public void setRelationTypes(final List<ParRelationTypeVO> relationTypes) {
        this.relationTypes = relationTypes;
    }

    public List<ParComplementTypeVO> getComplementTypes() {
        return complementTypes;
    }

    public void setComplementTypes(final List<ParComplementTypeVO> complementTypes) {
        this.complementTypes = complementTypes;
    }

    public List<ApTypeVO> getApTypes() {
        return apTypes;
    }

    public void setApTypes(final List<ApTypeVO> apTypes) {
        this.apTypes = apTypes;
    }

    public void addRelationType(final ParRelationTypeVO relationTypeVO) {
        if (relationTypes == null) {
            relationTypes = new LinkedList<>();
        }
        relationTypes.add(relationTypeVO);
    }

    public void addComplementType(final ParComplementTypeVO complementTypeVO) {
        if (complementTypes == null) {
            complementTypes = new LinkedList<>();
        }
        complementTypes.add(complementTypeVO);
    }

    public void addApType(final ApTypeVO apTypeVO){
        if(apTypes == null){
            apTypes = new LinkedList<>();
        }
        apTypes.add(apTypeVO);
    }

    public List<UIPartyGroupVO> getPartyGroups() {
        return partyGroups;
    }

    public void setPartyGroups(final List<UIPartyGroupVO> partyGroups) {
        this.partyGroups = partyGroups;
    }
}
