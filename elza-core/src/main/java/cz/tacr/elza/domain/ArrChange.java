package cz.tacr.elza.domain;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Seznam provedených změn v archivních pomůckách.
 *
 * @since 22.7.15
 */
@Entity(name = "arr_change")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrChange {

    public final static String FIELD_PRIMARY_NODE_ID = "primaryNodeId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer changeId;

    @Column(nullable = false)
    private OffsetDateTime changeDate;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId", nullable = true)
    private UsrUser user;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = FIELD_PRIMARY_NODE_ID, nullable = true)
    private ArrNode primaryNode;

    @Column(name = FIELD_PRIMARY_NODE_ID, nullable = true, updatable = false, insertable = false)
    private Integer primaryNodeId;

    @Enumerated(EnumType.STRING)
    @Column(length = 25, nullable = true)
    private Type type;

    /**
    *
    * @return číslo změny.
    */
    public Integer getChangeId() {
        return changeId;
    }

    /**
     * Nastaví číslo změny.
     * @param changeId  číslo změny.
     */
    public void setChangeId(final Integer changeId) {
        this.changeId = changeId;
    }

    /**
    *
    * @return datum změny.
    */
    public OffsetDateTime getChangeDate() {
        return changeDate;
    }

    /**
     * Nastaví datum změny.
     * @param changeDate datum změny.
     */
    public void setChangeDate(final OffsetDateTime changeDate) {
        this.changeDate = changeDate;
    }

    /**
     * @return uživatel, který provedl změnu
     */
    public UsrUser getUser() {
        return user;
    }

    /**
     * @param user uživatel, který provedl změnu
     */
    public void setUser(final UsrUser user) {
        this.user = user;
    }

    /**
     * @return typ změny
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type typ změny
     */
    public void setType(final Type type) {
        this.type = type;
    }

    public ArrNode getPrimaryNode() {
        return primaryNode;
    }

    public void setPrimaryNode(final ArrNode primaryNode) {
        this.primaryNode = primaryNode;
        this.primaryNodeId = (primaryNode == null) ? null : primaryNode.getNodeId();
    }

    public Integer getPrimaryNodeId() {
        return primaryNodeId;
    }

    public void setPrimaryNodeId(Integer primaryNodeId) {
        this.primaryNodeId = primaryNodeId;
    }

    @Override
    public String toString() {
        return "ArrChange pk=" + changeId;
    }

    /**
     * Typ změny.
     */
    public enum Type {

        /**
         * Vytvoření AS.
         */
        CREATE_AS,

        /**
         * Připojení JP k výstupu.
         */
        ADD_NODES_OUTPUT,

        /**
         * Odpojení JP od výstupu.
         */
        REMOVE_NODES_OUTPUT,

        /**
         * Založení JP.
         */
        ADD_LEVEL,

        /**
         * Přesun JP.
         */
        MOVE_LEVEL,

        /**
         * Zrušení JP.
         */
        DELETE_LEVEL,

        /**
         * Přidání rozšíření k JP.
         */
        ADD_NODE_EXTENSION,

        /**
         * Zrušení rozšíření k JP.
         */
        DELETE_NODE_EXTENSION,

        /**
         * Nastaví rozšíření k JP.
         */
        SET_NODE_EXTENSION,

        /**
         * Změna atributu včetně změny pořadí.
         */
        UPDATE_DESC_ITEM,

        /**
         * Založení atributu.
         */
        ADD_DESC_ITEM,

        /**
         * Zrušení atributu.
         */
        DELETE_DESC_ITEM,

        /**
         * Změna hodnoty strukturovaného atributu.
         */
        UPDATE_STRUCTURE_ITEM,

        /**
         * Založení hodnoty strukturovaného atributu.
         */
        ADD_STRUCTURE_ITEM,

        /**
         * Zrušení hodnoty strukturovaného atributu.
         */
        DELETE_STRUCTURE_ITEM,

        /**
         * Založení strukturovaného typu.
         */
        ADD_STRUCTURE_DATA,

        /**
         * Hromadné založení strukturovaného typu.
         */
        ADD_STRUCTURE_DATA_BATCH,

        /**
         * Hromadná úprava strukturovaného typu.
         */
        UPDATE_STRUCT_DATA_BATCH,

        /**
         * Smazání strukturovaného typu.
         */
        DELETE_STRUCTURE_DATA,

        /**
         * Přiřazení rozšíření k AS.
         */
        ADD_FUND_STRUCTURE_EXT,

        /**
         * Nastavení přiřazení k AS.
         */
        SET_FUND_STRUCTURE_EXT,

        /**
         * Odebrání rozšíření u AS.
         */
        DELETE_FUND_STRUCTURE_EXT,

        /**
         * Hromadná změna atributů - změny z tabulkového zobrazení, pokud se týká jen jedné JP, tak jde o typ Změna atributu.
         */
        BATCH_CHANGE_DESC_ITEM,

        /**
         * Hromadné vymazání atributů - změny z tabulkového zobrazení, pokud se týká jen jedné JP, tak jde o typ Zrušení atributu.
         */
        BATCH_DELETE_DESC_ITEM,

        /**
         * Hromadné funkce.
         */
        BULK_ACTION,

        /**
         * Import AS.
         */
        IMPORT,

        /**
         * Požadavek na digitalizaci.
         */
        CREATE_DIGI_REQUEST,

        /**
         * Požadavek na delimitaci/skartaci.
         */
        CREATE_DAO_REQUEST,

        /**
         * Vytvoření položky ve frontě.
         */
        CREATE_REQUEST_QUEUE,

        /**
         * Vytvoření vazby na DAO
         */
        CREATE_DAO_LINK,

        /**
         * Zrušení vazby na DAO
         */
        DELETE_DAO_LINK,

        /**
         * Úprava dat výstupu.
         */
        UPDATE_OUTPUT,

        /**
         * Register replace
         */
        REPLACE_REGISTER,

        /**
         * Party replace
         */
        REPLACE_PARTY,

        /**
         * Generate output
         */
        GENERATE_OUTPUT,

        /**
         * Synchronizace JP
         */
        SYNCHRONIZE_JP,

        /**
         * Změna záznamu podle scénářů
         */
        CHANGE_SCENARIO_ITEMS,
        
        /**
         * Vytváření nového záznamu ArrFile.
         */
        ADD_ATTACHMENT,

        /**
         * Smazání záznamu ArrFile.
         */
        DELETE_ATTACHMENT,
    }
}
