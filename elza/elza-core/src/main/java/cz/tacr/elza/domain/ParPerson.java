package cz.tacr.elza.domain;

import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Fyzická osoba. Stručná charakteristika osoby je uložena v záznamu v rejstříku.
 */
@Entity(name = "par_person")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParPerson extends ParParty {

}
