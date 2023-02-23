package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Queue for structured objects
 *
 * Requests to generate value, sortValue and complement in structured object
 *
 * @since 27.10.2017
 */
@Entity(name = "arr_sobj_vrequest")
@Table
public class ArrSobjVrequest {

    public static final String TABLE_NAME = "arr_sobj_vrequest";

    public static final String FIELD_STRUCTURED_OBJECT_ID = "structuredObjectId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer sobjVrequestId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrStructuredObject.class)
    @JoinColumn(name = FIELD_STRUCTURED_OBJECT_ID, nullable = false)
    private ArrStructuredObject structuredObject;

    @Column(name = FIELD_STRUCTURED_OBJECT_ID, updatable = false, insertable = false)
    private Integer structuredObjectId;

    public Integer getSobjVrequestId() {
        return sobjVrequestId;
    }

    public void setSobjVrequestId(Integer sobjVrequestId) {
        this.sobjVrequestId = sobjVrequestId;
    }

    public ArrStructuredObject getStructuredObject() {
        return structuredObject;
    }

    public void setStructuredObject(ArrStructuredObject structuredObject) {
        this.structuredObject = structuredObject;
        this.structuredObjectId = (structuredObject == null)?null:structuredObject.getStructuredObjectId();
    }

    public Integer getStructuredObjectId() {
        return structuredObjectId;
    }

    public void setStructuredObjectId(Integer structuredObjectId) {
        this.structuredObjectId = structuredObjectId;
    }


}
