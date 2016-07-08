package cz.tacr.elza.xmlexport.v1;

import java.util.Set;

import org.springframework.util.Assert;

/**
 * Nastavení exportu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 22. 4. 2016
 */
public class XmlExportConfig {

    private Integer versionId;

    private String transformationName;

    private Set<Integer> nodeIds;

    public XmlExportConfig(final Integer versionId) {
        Assert.notNull(versionId);

        this.versionId = versionId;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public String getTransformationName() {
        return transformationName;
    }

    public void setTransformationName(String transformationName) {
        this.transformationName = transformationName;
    }

    public Set<Integer> getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(Set<Integer> nodeIds) {
        this.nodeIds = nodeIds;
    }
}
