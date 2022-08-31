package cz.tacr.elza.controller.vo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import cz.tacr.elza.controller.vo.ap.ApStateVO;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.RevStateApproval;


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

    /**
     * Příznak neplatnostni / vymazání entity
     */
    private boolean invalid;

    /**
     * ID nahrazující entity
     */
    private Integer replacedById;

    /**
     * Externí identifikátory rejstříkového hesla.
     */
    private Collection<ApBindingVO> bindings;

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

    /**
     * Identifikátor pravidel.
     */
    private Integer ruleSetId;

    /**
     * Stav revize
     */
    private RevStateApproval revStateApproval;

    /**
     * Nový typ rejstříku.
     */
    private Integer newTypeId;

    /**
     * Identifikátor nové preferované části z revize
     */
    private Integer newPreferredPart;

    /**
     * Identifikátor nové preferované části, která existuje pouze v revizi
     */
    private Integer revPreferredPart;

    /**
     * Seznam částí přístupového bodu z revize
     */
    private List<ApPartVO> revParts = new ArrayList<>();

    /**
     *  Seznam ID entit, které tato entita nahradila
     */
    private List<Integer> replacedIds;

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

    public Collection<ApBindingVO> getBindings() {
        return bindings;
    }

    public void setBindings(Collection<ApBindingVO> bindings) {
        this.bindings = bindings;
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

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(final Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public RevStateApproval getRevStateApproval() {
        return revStateApproval;
    }

    public void setRevStateApproval(RevStateApproval revStateApproval) {
        this.revStateApproval = revStateApproval;
    }

    public Integer getNewTypeId() {
        return newTypeId;
    }

    public void setNewTypeId(Integer newTypeId) {
        this.newTypeId = newTypeId;
    }

    public Integer getNewPreferredPart() {
        return newPreferredPart;
    }

    public void setNewPreferredPart(Integer newPreferredPart) {
        this.newPreferredPart = newPreferredPart;
    }

    public Integer getRevPreferredPart() {
        return revPreferredPart;
    }

    public void setRevPreferredPart(Integer revPreferredPart) {
        this.revPreferredPart = revPreferredPart;
    }

    public List<ApPartVO> getRevParts() {
        return revParts;
    }

    public void setRevParts(List<ApPartVO> revParts) {
        this.revParts = revParts;
    }

    public Integer getReplacedById() {
        return replacedById;
    }

    public void setReplacedById(Integer replacedById) {
        this.replacedById = replacedById;
    }

    public List<Integer> getReplacedIds() {
        return replacedIds;
    }

    public void setReplacedIds(List<Integer> replacedIds) {
        this.replacedIds = replacedIds;
    }
}
