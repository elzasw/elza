package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vividsolutions.jts.geom.Geometry;

import cz.tacr.elza.api.RegScope;
import cz.tacr.elza.api.interfaces.IRegScope;


/**
 * Implementace třídy {@link cz.tacr.elza.api.RegCoordinates}
 *
 * @author Petr Compel
 * @since 18. 4. 2016
 */
@Entity(name = "reg_coordinates")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RegCoordinates implements cz.tacr.elza.api.RegCoordinates<RegRecord>, IRegScope {

    @Id
    @GeneratedValue
    private Integer coordinatesId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRecord.class)
    @JoinColumn(name = "recordId", nullable = false)
    private RegRecord regRecord;


    @Column(nullable = false, columnDefinition = "geometry")
    private Geometry value;


    @Column
    private String description;

    @Override
    public Integer getCoordinatesId() {
        return coordinatesId;
    }

    @Override
    public void setCoordinatesId(final Integer coordinatesId) {
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
