package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vividsolutions.jts.geom.Geometry;
import cz.tacr.elza.api.RegScope;
import org.hibernate.annotations.Type;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.*;


/**
 * Záznamy v rejstříku.
 *
 * @author Petr Compel
 * @since 18. 4. 2016
 */
@Entity(name = "reg_coordinates")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RegCoordinates implements cz.tacr.elza.api.RegCoordinates<RegRecord> {

    @Id
    @GeneratedValue
    private Integer coordinatesId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRecord.class)
    @JoinColumn(name = "recordId", nullable = false)
    private RegRecord regRecord;


    @Column(nullable = false)
    @Type(type="org.hibernate.spatial.GeometryType")
    private Geometry value;


    @Column(nullable = true)
    private String description;

    public Integer getCoordinatesId() {
        return coordinatesId;
    }

    public void setCoordinatesId(Integer coordinatesId) {
        this.coordinatesId = coordinatesId;
    }

    @Override
    public Geometry getValue() {
        return value;
    }
    @Override
    public void setValue(final Geometry value) {
        this.value = value;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public RegRecord getRegRecord() {
        return regRecord;
    }

    @Override
    public void setRegRecord(final RegRecord regRecord) {
        this.regRecord = regRecord;
    }

    @Override
    public RegScope getRegScope() {
        return regRecord.getScope();
    }
}
