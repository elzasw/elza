package cz.tacr.elza.controller.vo;

/**
 * VO pro instituce.
 *
 * @author Martin Šlapa
 * @since 21.3.2016
 */
public class ParInstitutionVO {

    private Integer id;

    private ParInstitutionTypeVO institutionType;

    private Integer partyId;

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

    public Integer getPartyId() {
        return partyId;
    }

    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
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
}
