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

import cz.tacr.elza.api.interfaces.IRegScope;


/**
 * Souřadnice.
 *
 * @author Petr Compel
 * @since 18. 4. 2016
 */
@Entity(name = "reg_coordinates")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RegCoordinates implements IRegScope {

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

    public Integer getCoordinatesId() {
        return coordinatesId;
    }

    public void setCoordinatesId(final Integer coordinatesId) {
        this.coordinatesId = coordinatesId;
    }

    /**
     *  @return souřadnice
     */
    public Geometry getValue() {
        return value;
    }

    /**
     * @param value souřadnice
     */
    public void setValue(final Geometry value) {
        this.value = value;
    }

    /**
     * @return popis
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description popis
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Vazba na heslo rejstříku.
     * @return  objekt hesla
     */
    public RegRecord getRegRecord() {
        return regRecord;
    }

    /**
     * Vazba na heslo rejstříku.
     * @param regRecord objekt hesla
     */
    public void setRegRecord(final RegRecord regRecord) {
        this.regRecord = regRecord;
    }

    @Override
    public RegScope getRegScope() {
        return regRecord.getScope();
    }
}
