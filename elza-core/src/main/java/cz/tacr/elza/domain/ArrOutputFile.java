package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;


/**
 * Implementace třídy {@link cz.tacr.elza.api.ArrOutputFile}
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 17.6.2016
 */
@Entity(name = "arr_output_file")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrOutputFile extends DmsFile implements cz.tacr.elza.api.ArrOutputFile<ArrOutputResult> {

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutputResult.class)
    @JoinColumn(name = "outputResultId", nullable = false)
    private ArrOutputResult outputResult;

    @Override
    public ArrOutputResult getOutputResult() {
        return outputResult;
    }

    @Override
    public void setOutputResult(ArrOutputResult outputResult) {
        this.outputResult = outputResult;
    }
}
