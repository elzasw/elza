package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity(name = "arr_data_structure_ref")
@Table
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ArrDataStructureRef extends ArrData {

    public static final String STRUCTURE_DATA = "structureData";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrStructureData.class)
    @JoinColumn(name = "structureDataId", nullable = false)
    private ArrStructureData structureData;

    @Column(name = "structureDataId", updatable = false, insertable = false)
    private Integer structureDataId;

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
        this.structureData = src.structureData;
        this.structureDataId = src.structureDataId;
    }

    public ArrStructureData getStructureData() {
        return structureData;
    }

    public void setStructureData(final ArrStructureData structureData) {
        this.structureData = structureData;
        this.structureDataId = structureData == null ? null : structureData.getStructureDataId();
    }

    public Integer getStructureDataId() {
        return structureDataId;
    }

    @Override
    public String getFulltextValue() {
        return structureData.getValue();
    }

    @Override
    public ArrData makeCopy() {
        return new ArrDataStructureRef(this);
    }
}

