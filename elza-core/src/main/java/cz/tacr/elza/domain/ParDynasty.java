package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Entity;


/**
 * Souhrn fyzických osob spojených příbuzenskou vazbou.
 */
@Entity(name = "par_dynasty")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParDynasty extends ParParty implements cz.tacr.elza.api.ParDynasty {

    @Column(nullable = false)
    private String genealogy;


    @Override
    public String getGenealogy() {
        return genealogy;
    }

    @Override
    public void setGenealogy(String genealogy) {
        this.genealogy = genealogy;
    }
}
