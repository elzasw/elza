package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Entity;


/**
 *  Skupina osob vystupujících pod jedním názvem, ovšem doba trvání akce je předem časově omezena (akce,
 *  konference, sjezd apod.).
 */
@Entity(name = "par_event")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParEvent extends ParParty implements cz.tacr.elza.api.ParEvent {

}
