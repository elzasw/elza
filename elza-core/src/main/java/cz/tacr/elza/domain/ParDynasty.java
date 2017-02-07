package cz.tacr.elza.domain;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Souhrn fyzických osob spojených příbuzenskou vazbou.
 */
@Entity(name = "par_dynasty")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParDynasty extends ParParty {

    @Lob
    @Column
    @Type(type="org.hibernate.type.TextType")
    @JsonIgnore
    private String genealogy;

    public String getGenealogy() {
        return genealogy;
    }

    public void setGenealogy(final String genealogy) {
        this.genealogy = genealogy;
    }
}
