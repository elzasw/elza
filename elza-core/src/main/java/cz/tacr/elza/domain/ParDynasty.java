package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;


/**
 * Souhrn fyzických osob spojených příbuzenskou vazbou.
 */
@Entity(name = "par_dynasty")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParDynasty extends ParParty implements cz.tacr.elza.api.ParDynasty {

    @Lob
    @Column
    @Type(type="org.hibernate.type.TextType")
    @JsonIgnore
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
