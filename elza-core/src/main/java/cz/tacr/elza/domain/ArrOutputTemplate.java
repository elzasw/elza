package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Aby mít více šablon pro jeden výstup
 *
 * @author Sergey Iryupin
 * @since 11.08.2020
 */
@Entity(name = "arr_output_template")
public class ArrOutputTemplate {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer outputTemplateId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutput.class)
    @JoinColumn(name = "outputId", nullable = false)
    private ArrOutput output;
    
	@Column(nullable = false, insertable = false, updatable = false)
	private Integer outputId;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = RulTemplate.class)
    @JoinColumn(name = "templateId")
    private RulTemplate template;

	@Column(nullable = false, insertable = false, updatable = false)
	private Integer templateId;

	public Integer getOutputTemplateId() {
		return outputTemplateId;
	}

	public void setOutputTemplateId(Integer outputTemplateId) {
		this.outputTemplateId = outputTemplateId;
	}

	public ArrOutput getOutput() {
		return output;
	}

	public void setOutput(ArrOutput output) {
		this.output = output;
		this.outputId = output != null ? output.getOutputId() : null;
	}

	public Integer getOutputId() {
		return outputId;
	}

	public RulTemplate getTemplate() {
		return template;
	}

	public void setTemplate(RulTemplate template) {
		this.template = template;
	}

	public Integer getTemplateId() {
		return templateId;
	}

}
