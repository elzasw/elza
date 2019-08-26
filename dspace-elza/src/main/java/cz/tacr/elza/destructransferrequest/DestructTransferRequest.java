package cz.tacr.elza.destructransferrequest;

import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.Date;

/**
 * Evidence požadavků na skartaci a delimitaci - DAO.
 * Datový objekt namapovaný na tabulku destruc_transfer_request.
 *
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 02.05.2019.
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
@Table(name = "destruc_transfer_request")
public class DestructTransferRequest {

    //****************************** FIELDS ***********************************
    /**
     * ID Request.
     * VARCHAR2(50)
     */
    @Id
    @Column(name="request_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="destructionrequest_seq")
    @SequenceGenerator(name="destructionrequest_seq", sequenceName="destructionrequest_seq", allocationSize = 1)
    private int requestId;

    /**
     * Request Type.
     * VARCHAR2(20)
     */
    @Enumerated(EnumType.STRING)
    @Column(name="type", nullable = false)
    private Type type;

    /**
     * Identifier.
     * VARCHAR2(50)
     */
    @Column(name = "identifier", length = 50)
    private String identifier;

    /**
     * Status.
     * VARCHAR2(20)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    /**
     * Description.
     * VARCHAR2(1000)
     */
    @Column(name = "rejected_message", length = 1000)
    private String rejectedMessage;

    /**
     * Request Date.
     * TIMESTAMP
     */
    @Column(name = "request_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date requestDate = null;

    //***************************** CONSTRUCTOR *******************************
    public DestructTransferRequest()
    {

    }

    //*************************** ENUM DEFINICE *******************************
    public enum Status {
        /** Požadavek nemohl být zpracován z důvodu chyby a chybové hlášení vráceno do Elza */
        REJECTED,

        /** Požadavek byl zpracován a informace odeslána do Elza */
        PROCESSED,
    }

    public enum Type {
        /** Destruction (Skartace) */
        DESTRUCTION,

        /** Transfer (delimitace) */
        TRANSFER
    }

    //*************************** GETTER / SETTER *****************************
    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getRejectedMessage() {
        return rejectedMessage;
    }

    public void setRejectedMessage(String rejectedMessage) {
        this.rejectedMessage = rejectedMessage;
    }

    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    //*************************** METODY *****************************
    public static Type typeFromString (String type) {
       Type result = null;
        switch (type) {
            case "DESTRUCTION":
                result = Type.DESTRUCTION;
                break;
            case "TRANSFER":
                result = Type.TRANSFER;
                break;
        }
        return result;
    }

    public static Status statusFromString (String status) {
        Status result = null;
        switch (status) {
            case "REJECTED":
                result = Status.REJECTED;
                break;
            case "PROCESSED":
                result = Status.PROCESSED;
                break;
        }
        return result;
    }
}
