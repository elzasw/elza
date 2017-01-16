package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.search.annotations.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.search.IndexArrDataWhenHasDescItemInterceptor;


/**
 * Atributu archivního popisu bez hodnoty - napr. enum
 *
 * @author Martin Šlapa
 * @since 18.11.2015
 */
@Indexed(interceptor = IndexArrDataWhenHasDescItemInterceptor.class)
@Entity(name = "arr_data_null")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataNull extends ArrData {

    @Override
    public String getFulltextValue() {
        RulItemSpec descItemSpec = getItem().getItemSpec();

        return descItemSpec == null ? null : descItemSpec.getName();
    }
}
