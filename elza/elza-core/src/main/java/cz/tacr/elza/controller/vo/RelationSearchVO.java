package cz.tacr.elza.controller.vo;

/**
 * Parametry pro nalezení vztahů.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 6. 1. 2017
 */
public class RelationSearchVO {

    private Integer systemId;
    private Integer scopeId;

    public Integer getSystemId() {
        return systemId;
    }
    public void setSystemId(final Integer systemId) {
        this.systemId = systemId;
    }
    public Integer getScopeId() {
        return scopeId;
    }
    public void setScopeId(final Integer scopeId) {
        this.scopeId = scopeId;
    }
}
