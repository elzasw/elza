package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrStructuredObject;

/**
 * VO pro hodnotu strukt. typu.
 *
 * @since 08.11.2017
 */
public class ArrStructureDataVO {

    protected Integer id;
    protected String value;
    protected String complement;
    protected ArrStructuredObject.State state;
    protected Boolean assignable;
    protected String errorDescription;
    protected String typeCode;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(String complement) {
        this.complement = complement;
    }

    public ArrStructuredObject.State getState() {
        return state;
    }

    public void setState(ArrStructuredObject.State state) {
        this.state = state;
    }

    public Boolean getAssignable() {
        return assignable;
    }

    public void setAssignable(Boolean assignable) {
        this.assignable = assignable;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public static ArrStructureDataVO newInstance(final ArrStructuredObject structureData) {
        ArrStructureDataVO structureDataVO = new ArrStructureDataVO();
        structureDataVO.setId(structureData.getStructuredObjectId());
        structureDataVO.setTypeCode(structureData.getStructuredType().getCode());
        structureDataVO.setValue(structureData.getValue());
        structureDataVO.setComplement(structureData.getComplement());
        structureDataVO.setErrorDescription(structureData.getErrorDescription());
        structureDataVO.setAssignable(structureData.getAssignable());
        structureDataVO.setState(structureData.getState());
        return structureDataVO;
    }

}
