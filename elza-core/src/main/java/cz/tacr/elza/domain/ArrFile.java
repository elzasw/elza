package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.*;


/**
 * Soubor Fund
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 17.6.2016
 */
@Entity(name = "arr_file")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrFile extends DmsFile implements cz.tacr.elza.api.ArrFile<ArrFund, DmsFile> {

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @Override
    public ArrFund getFund() {
        return fund;
    }

    @Override
    public void setFund(final ArrFund fund) {
        this.fund = fund;
    }


}
