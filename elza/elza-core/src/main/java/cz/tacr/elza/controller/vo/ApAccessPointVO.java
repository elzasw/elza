package cz.tacr.elza.controller.vo;

import cz.tacr.elza.controller.vo.ap.ApFormVO;
import cz.tacr.elza.controller.vo.ap.ApStateVO;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;


/**
 * VO rejstříkového záznamu.
 */
public class ApAccessPointVO extends AbstractApAccessPoint {

    /**
     * Id hesla.
     */
    private Integer id;

    private String uuid;

    /**
     * Typ rejstříku.
     */
    private Integer typeId;

    /**
     * Id třídy rejstříku.
     */
    private Integer scopeId;

    /**
     * Stav schválení.
     */
    private ApState.StateApproval stateApproval;

    /**
     * Komentář ke stavu schválení.
     */
    private String comment;

    /**
     * Jméno přistupového bodu.
     */
    private String name;

    /**
     * Podrobný popis přístupového bodu.
     */
    private String description;

    private boolean invalid;

    /**
     * Externí identifikátory rejstříkového hesla.
     */
    private Collection<ApExternalIdVO> externalIds;

    /**
     * Stav přístupového bodu.
     */
    @Nullable
    private ApStateVO state;

    /**
     * Chyby v přístupovém bodu.
     */
    @Nullable
    private String errorDescription;

    /**
     * Identifikátor preferované části
     */
    private Integer preferredPart;

    /**
     * Seznam částí přístupového bodu
     */
    private List<ApPartVO> parts = new ArrayList<>();

    /**
     * Poslední změna přístupového bodu
     */
    private ApChangeVO lastChange;

    /**
     * Vlastník přístupového bodu
     */
    private UserVO ownerUser;

    /**
     * Počet komentářů
     */
    private Integer comments;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public ApState.StateApproval getStateApproval() {
        return stateApproval;
    }

    public void setStateApproval(final ApState.StateApproval stateApproval) {
        this.stateApproval = stateApproval;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public Integer getScopeId() {
        return scopeId;
    }

    public void setScopeId(Integer scopeId) {
        this.scopeId = scopeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public Collection<ApExternalIdVO> getExternalIds() {
        return externalIds;
    }

    public void setExternalIds(Collection<ApExternalIdVO> externalIds) {
        this.externalIds = externalIds;
    }

    @Nullable
    public ApStateVO getState() {
        return state;
    }

    public void setState(@Nullable final ApStateVO state) {
        this.state = state;
    }

    @Nullable
    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(@Nullable final String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public Integer getPreferredPart() {
        return preferredPart;
    }

    public void setPreferredPart(Integer preferredPart) {
        this.preferredPart = preferredPart;
    }

    public List<ApPartVO> getParts() {
        return parts;
    }

    public void setParts(List<ApPartVO> parts) {
        this.parts = parts;
    }

    public ApChangeVO getLastChange() {
        return lastChange;
    }

    public void setLastChange(ApChangeVO lastChange) {
        this.lastChange = lastChange;
    }

    public UserVO getOwnerUser() {
        return ownerUser;
    }

    public void setOwnerUser(UserVO ownerUser) {
        this.ownerUser = ownerUser;
    }

    public Integer getComments() {
        return comments;
    }

    public void setComments(Integer comments) {
        this.comments = comments;
    }
}
