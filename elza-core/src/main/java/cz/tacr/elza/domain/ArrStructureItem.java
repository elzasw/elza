package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Hodnota strukturovaného datového typu.
 *
 * @since 27.10.2017
 */
@Entity(name = "arr_structure_item")
@Table
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
public class ArrStructureItem extends ArrItem {

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrStructureData.class)
    @JoinColumn(name = "structureDataId", nullable = false)
    @JsonIgnore
    private ArrStructureData structureData;

    @Column(name = "structureDataId", updatable = false, insertable = false)
    private Integer structureDataId;

    public ArrStructureItem() {

    }

    protected ArrStructureItem(ArrStructureItem src) {
        super(src);

        this.structureData = src.structureData;
        this.structureDataId = src.structureDataId;
    }

    @Override
    public Integer getNodeId() {
        return null;
    }

    @Override
    public Integer getFundId() {
        return null;
    }

    @Override
    public ArrNode getNode() {
        return null;
    }

    @Override
    public ArrOutputDefinition getOutputDefinition() {
        return null;
    }

    @Override
    public ArrStructureData getStructureData() {
        return structureData;
    }

    @Override
    public Integer getStructureDataId() {
        return structureDataId;
    }

    public void setStructureData(final ArrStructureData structureData) {
        this.structureData = structureData;
        this.structureDataId = structureData == null ? null : structureData.getStructureDataId();
    }

    @Override
    public ArrStructureItem makeCopy() {
        return new ArrStructureItem(this);
    }
}
