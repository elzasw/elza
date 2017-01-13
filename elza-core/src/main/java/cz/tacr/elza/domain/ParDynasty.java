package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Souhrn fyzických osob spojených příbuzenskou vazbou.
 */
@Entity(name = "par_dynasty")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParDynasty extends ParParty {

    @Column(nullable = false)
    @JsonIgnore
    private String genealogy;

    public String getGenealogy() {
        return genealogy;
    }

    public void setGenealogy(final String genealogy) {
        this.genealogy = genealogy;
    }
}
