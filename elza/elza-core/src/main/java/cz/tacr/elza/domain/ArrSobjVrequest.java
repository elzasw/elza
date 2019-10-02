package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer sobjVrequestId;
    
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrStructuredObject.class)
    @JoinColumn(name = "structuredObjectId", nullable = false)
    private ArrStructuredObject structuredObject;

    @Column(name = "structuredObjectId", updatable = false, insertable = false)
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
