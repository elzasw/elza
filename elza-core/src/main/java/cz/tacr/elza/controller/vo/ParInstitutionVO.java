package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ParInstitution;

/**
 * VO pro instituce.
 *
 * @author Martin Šlapa
 * @since 21.3.2016
 */
public class ParInstitutionVO {

    private Integer id;

    private ParInstitutionTypeVO institutionType;

    private Integer accessPointId;

    private String name;

    private String code;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public ParInstitutionTypeVO getInstitutionType() {
        return institutionType;
    }

    public void setInstitutionType(final ParInstitutionTypeVO institutionType) {
        this.institutionType = institutionType;
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

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public void setAccessPointId(Integer accessPointId) {
        this.accessPointId = accessPointId;
    }
    
    public static ParInstitutionVO newInstance(final ParInstitution institution, final ApIndex displayName) {
    	ParInstitutionTypeVO type = new ParInstitutionTypeVO();
    	type.setName(institution.getInstitutionType().getName());
    	type.setCode(institution.getInstitutionType().getCode());
    	type.setId(institution.getInstitutionType().getInstitutionTypeId());

    	ParInstitutionVO result = new ParInstitutionVO();
    	result.setId(institution.getInstitutionId());
    	result.setAccessPointId(institution.getAccessPointId());
    	result.setCode(institution.getInternalCode());
    	result.setInstitutionType(type);
    	result.setName(displayName != null? displayName.getIndexValue() : null);

    	return result;
    }
}
