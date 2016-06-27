package cz.tacr.elza.controller.vo;

import java.util.List;


/**
 * VO Výstup z archivního souboru.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.04.2016
 */
public class ArrOutputDefinitionVO {

    private Integer id;

    private String internalCode;

    private String name;

    private Boolean temporary;

    private Boolean deleted;

    private List<ArrOutputVO> outputs;

    private List<TreeNodeClient> nodes;

    private Integer outputTypeId;

    private Integer version;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getInternalCode() {
        return internalCode;
    }

    public void setInternalCode(final String internalCode) {
        this.internalCode = internalCode;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Boolean getTemporary() {
        return temporary;
    }

    public void setTemporary(final Boolean temporary) {
        this.temporary = temporary;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
    }

    public List<ArrOutputVO> getOutputs() {
        return outputs;
    }

    public void setOutputs(final List<ArrOutputVO> outputs) {
        this.outputs = outputs;
    }

    public List<TreeNodeClient> getNodes() {
        return nodes;
    }

    public void setNodes(final List<TreeNodeClient> nodes) {
        this.nodes = nodes;
    }

    public Integer getOutputTypeId() {
        return outputTypeId;
    }

    public void setOutputTypeId(Integer outputTypeId) {
        this.outputTypeId = outputTypeId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }
}
