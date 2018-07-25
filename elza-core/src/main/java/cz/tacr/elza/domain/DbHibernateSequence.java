package cz.tacr.elza.domain;

import javax.persistence.*;

@Entity
@Table(name = "db_hibernate_sequences")
public class DbHibernateSequence {

    @Id
    @Access(AccessType.PROPERTY)
    private String sequenceName;

    @Column(nullable = false)
    private Long nextVal;

    public String getSequenceName() {
        return sequenceName;
    }

    public void setSequenceName(final String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public Long getNextVal() {
        return nextVal;
    }

    public void setNextVal(final Long nextVal) {
        this.nextVal = nextVal;
    }
}
