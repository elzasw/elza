package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Slouží pro evidenci vlastních drl souborů (pro strukturované datové typy i pro groovy scripty na serializaci hodnot).
 *
 * @since 17.10.2017
 */
@Entity(name = "rul_component")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulComponent {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer componentId;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String filename;

    @Column(length = StringLength.LENGTH_SHA256)
    private String hash;

    /**
     * @return identifikátor entity
     */
    public Integer getComponentId() {
        return componentId;
    }

    /**
     * @param componentId identifikátor entity
     */
    public void setComponentId(final Integer componentId) {
        this.componentId = componentId;
    }

    /**
     * @return název souboru
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename název souboru
     */
    public void setFilename(final String filename) {
        this.filename = filename;
    }

    /**
     * @return hash souboru
     */
    public String getHash() {
        return hash;
    }

    /**
     * @param hash hash souboru
     */
    public void setHash(final String hash) {
        this.hash = hash;
    }
}
