package cz.tacr.elza.ax;

import javax.persistence.*;

import cz.tacr.elza.ax.IdObject;


@MappedSuperclass
public class IdEntity implements IdObject<Integer> {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    public Integer getId() {
        return id;
    }

    public String getClassAndId() {
        return getClass().getSimpleName() + "#" + getId();
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IdEntity) {
            IdEntity oth = (IdEntity) obj;
            if (getId() != null && oth.getId() != null) {
                boolean equals = getId().equals(oth.getId());
                return equals;
            }
            return super.equals(obj);
        }
        return false;
    }

}
