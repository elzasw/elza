package cz.tacr.elza.ui.view;

import java.io.Serializable;

/**
 * VO pro předání identifikátoru archivní pomůcky a verze.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 5. 8. 2015
 */
public class VOFindingAidVersionParam implements Serializable {

    private Integer findingAidId;

    private Integer versionId;

    public VOFindingAidVersionParam(final Integer findingAidId, final Integer versionId) {
        this.findingAidId = findingAidId;
        this.versionId = versionId;
    }

    @Override
    public String toString() {
        String result = "";
        if (findingAidId != null) {
            result += findingAidId.toString();
            result += "/";
        }
        if (versionId != null) {
            result += versionId.toString();
        }

        return result;
    }

    public Integer getFindingAidId() {
        return findingAidId;
    }

    public void setFindingAidId(final Integer findingAidId) {
        this.findingAidId = findingAidId;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(final Integer versionId) {
        this.versionId = versionId;
    }
}
