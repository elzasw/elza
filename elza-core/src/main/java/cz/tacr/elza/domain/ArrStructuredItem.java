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
@Entity(name = "arr_structured_item")
@Table
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
public class ArrStructuredItem extends ArrItem {
	
	public static final String STRUCT_OBJ_FK = "structuredObjectId";

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrStructuredObject.class)
    @JoinColumn(name = STRUCT_OBJ_FK, nullable = false)
    @JsonIgnore
    private ArrStructuredObject structuredObject;

    @Column(name = STRUCT_OBJ_FK, updatable = false, insertable = false)
    private Integer structuredObjectId;

    public ArrStructuredItem() {

    }

    protected ArrStructuredItem(ArrStructuredItem src) {
        super(src);

        this.structuredObject = src.structuredObject;
        this.structuredObjectId = src.structuredObjectId;
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
    public ArrOutput getOutput() {
        return null;
    }

    public ArrStructuredObject getStructuredObject() {
        return structuredObject;
    }

    public Integer getStructuredObjectId() {
        return structuredObjectId;
    }

    public void setStructuredObject(final ArrStructuredObject structuredObject) {
        this.structuredObject = structuredObject;
        this.structuredObjectId = structuredObject == null ? null : structuredObject.getStructuredObjectId();
    }

    @Override
    public ArrStructuredItem makeCopy() {
        return new ArrStructuredItem(this);
    }
}
