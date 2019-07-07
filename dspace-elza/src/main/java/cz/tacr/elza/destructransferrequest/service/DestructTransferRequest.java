package cz.tacr.elza.destructransferrequest.service;

import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

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
     * GUID Request.
     * VARCHAR2(50)
     */
    @Column(name = "uuid", length = 50)
    private String uuid;

    /**
     * Request Type.
     * VARCHAR2(20)
     */
    @Enumerated(EnumType.STRING)
    @Column(name="request_type", nullable = false)
    private RequestType requestType;

    /**
     * Dao Identifiers.
     * VARCHAR2(2000)
     */
    @Column(name = "dao_identifiers", length = 2000)
    private String daoIdentifiers;

    /**
     * Identifier.
     * VARCHAR2(50)
     */
    @Column(name = "identifier", length = 50)
    private String identifier;

    /**
     * System Identifier.
     * VARCHAR2(50)
     */
    @Column(name = "system_identifier", length = 50)
    private String systemIdentifier;

    /**
     * Description.
     * VARCHAR2(1000)
     */
    @Column(name = "Description", length = 1000)
    private String description;

    /**
     * User Name.
     * VARCHAR2(50)
     */
    @Column(name = "user_name", length = 50)
    private String userName;

    /**
     * Target Fund.
     * VARCHAR2(50)
     */
    @Column(name = "target_fund", length = 50)
    private String targetFund;

    /**
     * Status.
     * VARCHAR2(20)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    /**
     * Request Date.
     * TIMESTAMP
     */
    @Column(name = "request_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date requestDate = null;

    /**
     * Processing Date.
     * TIMESTAMP
     */
    @Column(name = "processing_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date processingDate = null;

    /**
     * Description.
     * VARCHAR2(1000)
     */
    @Column(name = "rejected_message", length = 1000)
    private String rejectedMessage;

    //***************************** CONSTRUCTOR *******************************
    protected DestructTransferRequest()
    {

    }

    //*************************** ENUM DEFINICE *******************************
    public enum Status {
        /** Požadavek byl uložen do fronty */
        QUEUED,

        /** Požadavek nemohl být zpracován z důvodu chyby a chybové hlášení vráceno do Elza */
        REJECTED,

        /** Požadavek byl zpracován a informace odeslána do Elza */
        PROCESSED,

        /** Chyba při odeslání odpovědi do Elza, provede se znovuodeslání do Elza */
        ERROR
    }

    public enum RequestType {
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public String getDaoIdentifiers() {
        return daoIdentifiers;
    }

    public void setDaoIdentifiers(String daoIdentifiers) {
        this.daoIdentifiers = daoIdentifiers;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getSystemIdentifier() {
        return systemIdentifier;
    }

    public void setSystemIdentifier(String systemIdentifier) {
        this.systemIdentifier = systemIdentifier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTargetFund() {
        return targetFund;
    }

    public void setTargetFund(String targetFund) {
        this.targetFund = targetFund;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    public Date getProcessingDate() {
        return processingDate;
    }

    public void setProcessingDate(Date processingDate) {
        this.processingDate = processingDate;
    }

    public String getRejectedMessage() {
        return rejectedMessage;
    }

    public void setRejectedMessage(String rejectedMessage) {
        this.rejectedMessage = rejectedMessage;
    }

    //****************************** METODY ********************************
    public void initDestructionRequest(cz.tacr.elza.ws.types.v1.DestructionRequest destructRequest) {
        this.setUuid(UUID.randomUUID().toString());
        this.setRequestType(RequestType.DESTRUCTION);
        this.setIdentifier(destructRequest.getIdentifier());
        this.setSystemIdentifier(destructRequest.getSystemIdentifier());
        this.setDescription(destructRequest.getDescription());
        this.setUserName(destructRequest.getUsername());
        this.setStatus(Status.QUEUED);
        this.setRequestDate(new Date());
    }

    public void initTransferRequest(cz.tacr.elza.ws.types.v1.TransferRequest transferRequest) {
        this.setUuid(UUID.randomUUID().toString());
        this.setRequestType(RequestType.TRANSFER);
        this.setIdentifier(transferRequest.getIdentifier());
        this.setSystemIdentifier(transferRequest.getSystemIdentifier());
        this.setDescription(transferRequest.getDescription());
        this.setUserName(transferRequest.getUsername());
        this.setStatus(Status.QUEUED);
        this.setRequestDate(new Date());
    }
}
