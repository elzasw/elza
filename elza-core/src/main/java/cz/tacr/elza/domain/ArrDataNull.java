package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * @author Martin Šlapa
 * @since 18.11.2015
 */
@Entity(name = "arr_data_null")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataNull extends ArrData implements cz.tacr.elza.api.ArrDataNull {

}
