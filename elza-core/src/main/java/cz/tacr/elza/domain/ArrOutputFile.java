package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Soubor v Output.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 17.6.2016
 */
@Entity(name = "arr_output_file")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrOutputFile extends DmsFile {

    public static final String OUTPUT_RESULT = "outputResult";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutputResult.class)
    @JoinColumn(name = "outputResultId", nullable = false)
    private ArrOutputResult outputResult;

    public ArrOutputResult getOutputResult() {
        return outputResult;
    }

    public void setOutputResult(final ArrOutputResult outputResult) {
        this.outputResult = outputResult;
    }
}
