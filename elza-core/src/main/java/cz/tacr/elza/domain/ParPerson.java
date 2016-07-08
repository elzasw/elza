package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Entity;


/**
 *  Fyzická osoba. Stručná charakteristika osoby je uložena v záznamu v rejstříku.
 */
@Entity(name = "par_person")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParPerson extends ParParty implements cz.tacr.elza.api.ParPerson {

}
