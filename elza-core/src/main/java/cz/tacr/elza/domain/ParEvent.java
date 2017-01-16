package cz.tacr.elza.domain;

import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 *  Skupina osob vystupujících pod jedním názvem, ovšem doba trvání akce je předem časově omezena (akce,
 *  konference, sjezd apod.).
 */
@Entity(name = "par_event")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParEvent extends ParParty {

}
