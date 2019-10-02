package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.Validate;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity(name = "arr_data_structure_ref")
@Table
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ArrDataStructureRef extends ArrData {

    public static final String STRUCTURED_OBJECT = "structuredObject";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrStructuredObject.class)
    @JoinColumn(name = "structuredObjectId", nullable = false)
    private ArrStructuredObject structuredObject;

    @Column(name = "structuredObjectId", updatable = false, insertable = false)
    private Integer structuredObjectId;

    /**
     * Public empty constructor
     */
    public ArrDataStructureRef() {

    }

    /**
     * Copy constructor
     *
     * Copy constuctor is used internally, see method makeCopy
     *
     * @param src
     */
    protected ArrDataStructureRef(ArrDataStructureRef src) {
        super(src);
        copyValue(src);
    }

    private void copyValue(ArrDataStructureRef src) {
        this.structuredObject = src.structuredObject;
        this.structuredObjectId = src.structuredObjectId;
    }

    public ArrStructuredObject getStructuredObject() {
        return structuredObject;
    }

    public void setStructuredObject(final ArrStructuredObject structuredObject) {
        this.structuredObject = structuredObject;
        this.structuredObjectId = structuredObject == null ? null : structuredObject.getStructuredObjectId();
    }

    public Integer getStructuredObjectId() {
        return structuredObjectId;
    }

    @Override
    public String getFulltextValue() {
        return structuredObject.getValue();
    }

    @Override
    public ArrData makeCopy() {
        return new ArrDataStructureRef(this);
    }

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataStructureRef data = (ArrDataStructureRef) srcData;
        return (this.structuredObjectId.equals(data.structuredObjectId));
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataStructureRef src = (ArrDataStructureRef) srcData;
        copyValue(src);
    }

    @Override
    protected void validateInternal() {
        Validate.notNull(structuredObject);
        Validate.notNull(structuredObjectId);
    }
}

