package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static cz.tacr.elza.domain.enumeration.StringLength.LENGTH_250;

/**
 * Hibernate Search support - fronta entit pro preindexovani
 */
@Entity(name = "sys_index_work")
@Table
public class SysIndexWork {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Long indexWorkId;

    @Column(length = LENGTH_250, nullable = false)
    private String indexName;

    @Column(length = LENGTH_250, nullable = false)
    private Class entityClass;

    @Column(nullable = false)
    private Integer entityId;

    @Column(nullable = false)
    private LocalDateTime insertTime;

    @Column(nullable = true)
    private LocalDateTime startTime;

    public Long getIndexWorkId() {
        return indexWorkId;
    }

    public void setIndexWorkId(Long indexWorkId) {
        this.indexWorkId = indexWorkId;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public Class getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(Class entityClass) {
        this.entityClass = entityClass;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public LocalDateTime getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(LocalDateTime insertTime) {
        this.insertTime = insertTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

}
