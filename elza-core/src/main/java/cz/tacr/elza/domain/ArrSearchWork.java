package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import static cz.tacr.elza.domain.enumeration.StringLength.*;

/**
 * Entity for used values
 */
@Entity(name = "arr_search_work")
@Table
public class ArrSearchWork {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db    
    private Integer searchWorkId;

    @Column(length = LENGTH_50, nullable = false)
    private String indexName;

    @Column(length = LENGTH_250, nullable = false)
    private Class entityClass;

    @Column(nullable = false)
    private Integer entityId;

    @Enumerated(EnumType.STRING)
    @Column(length = LENGTH_ENUM, nullable = false)
    private WorkType workType;

    @Column(nullable = false)
    private LocalDateTime insertTime;

    @Column(nullable = true)
    private LocalDateTime startTime;

    public Integer getSearchWorkId() {
        return searchWorkId;
    }

    public void setSearchWorkId(Integer searchWorkId) {
        this.searchWorkId = searchWorkId;
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

    public WorkType getWorkType() {
        return workType;
    }

    public void setWorkType(WorkType workType) {
        this.workType = workType;
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

    public enum WorkType {
        DELETE,
        INDEX
    }

}
