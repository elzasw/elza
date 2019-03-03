package cz.tacr.elza.domain;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Soubor v Output.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@Entity(name = "arr_output_result")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"outputDefinitionId"}))
public class ArrOutputResult {

    public static final String TABLE_NAME = "arr_output_result";
	
	/**
	 * Name of field with link to output definition
	 */
    public static final String OUTPUT_DEFINITION = "outputDefinition";

    public static final String FIELD_CHANGE_ID = "changeId";

    @Id
    @GeneratedValue
    private Integer outputResultId;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrOutputDefinition.class)
    @JoinColumn(name = "outputDefinitionId", nullable = false)
    private ArrOutputDefinition outputDefinition;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulTemplate.class)
    @JoinColumn(name = "templateId", nullable = false)
    private RulTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_CHANGE_ID, nullable = false)
    private ArrChange change;

    @OneToMany(mappedBy = "outputResult", fetch = FetchType.LAZY)
    private List<ArrOutputFile> outputFiles;

    public Integer getOutputResultId() {
        return outputResultId;
    }

    public void setOutputResultId(final Integer outputResultId) {
        this.outputResultId = outputResultId;
    }

    public ArrOutputDefinition getOutputDefinition() {
        return outputDefinition;
    }

    public void setOutputDefinition(final ArrOutputDefinition outputDefinition) {
        this.outputDefinition = outputDefinition;
    }

    public RulTemplate getTemplate() {
        return template;
    }

    public void setTemplate(final RulTemplate template) {
        this.template = template;
    }

    public ArrChange getChange() {
        return change;
    }

    public void setChange(final ArrChange change) {
        this.change = change;
    }

    public List<ArrOutputFile> getOutputFiles() {
        return outputFiles;
    }

    public void setOutputFiles(final List<ArrOutputFile> outputFiles) {
        this.outputFiles = outputFiles;
    }
}
