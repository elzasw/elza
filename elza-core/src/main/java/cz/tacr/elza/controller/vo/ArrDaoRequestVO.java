package cz.tacr.elza.controller.vo;

import java.util.List;

import cz.tacr.elza.domain.ArrDaoRequest;

/**
 * Value objekt {@link ArrDaoRequest}
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
public class ArrDaoRequestVO extends ArrRequestVO {

    private ArrDaoRequest.Type type;

    private String description;

    private Integer daosCount;

    private Integer digitalRepositoryId;

    private List<ArrDaoVO> daos;

    public ArrDaoRequest.Type getType() {
        return type;
    }

    public void setType(final ArrDaoRequest.Type type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Integer getDaosCount() {
        return daosCount;
    }

    public void setDaosCount(final Integer daosCount) {
        this.daosCount = daosCount;
    }

    public List<ArrDaoVO> getDaos() {
        return daos;
    }

    public void setDaos(final List<ArrDaoVO> daos) {
        this.daos = daos;
    }

    public Integer getDigitalRepositoryId() {
        return digitalRepositoryId;
    }

    public void setDigitalRepositoryId(final Integer digitalRepositoryId) {
        this.digitalRepositoryId = digitalRepositoryId;
    }
}
