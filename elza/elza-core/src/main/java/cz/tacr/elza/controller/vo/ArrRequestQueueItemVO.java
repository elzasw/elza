package cz.tacr.elza.controller.vo;

import java.time.ZoneId;
import java.util.Date;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrRequestQueueItem;

/**
 * VO {{@link cz.tacr.elza.domain.ArrRequestQueueItem}}
 *
 * @author Martin Šlapa
 * @since 12.12.2016
 */
public class ArrRequestQueueItemVO {

    private Integer id;

    private ArrRequestVO request;

    private String username;

    private Date create;

    private Date attemptToSend;

    private String error;

    private Boolean send;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public ArrRequestVO getRequest() {
        return request;
    }

    public void setRequest(final ArrRequestVO request) {
        this.request = request;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public Date getCreate() {
        return create;
    }

    public void setCreate(final Date create) {
        this.create = create;
    }

    public Date getAttemptToSend() {
        return attemptToSend;
    }

    public void setAttemptToSend(final Date attemptToSend) {
        this.attemptToSend = attemptToSend;
    }

    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error;
    }

    public Boolean getSend() {
        return send;
    }

    public void setSend(final Boolean send) {
        this.send = send;
    }

    public static ArrRequestQueueItemVO newInstance(final ArrRequestQueueItem requestQueueItem) {
        ArrRequestQueueItemVO requestQueueItemVO = new ArrRequestQueueItemVO();
        ArrChange createChange = requestQueueItem.getCreateChange();
        requestQueueItemVO.setId(requestQueueItem.getRequestQueueItemId());
        requestQueueItemVO.setCreate(Date.from(createChange.getChangeDate().toInstant()));
        requestQueueItemVO.setAttemptToSend(Date.from(requestQueueItem.getAttemptToSend().atZone(ZoneId.systemDefault()).toInstant()));
        requestQueueItemVO.setError(requestQueueItem.getError());
        requestQueueItemVO.setUsername(createChange.getUser() == null ? null : createChange.getUser().getUsername());
        return requestQueueItemVO;
    }
}
