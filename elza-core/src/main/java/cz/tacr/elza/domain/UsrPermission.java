package cz.tacr.elza.domain;

import java.io.Serializable;

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
 * Oprávnění pro uživatele / skupinu.
 *
 * @author Martin Šlapa
 * @since 26.04.2016
 */
@Entity(name = "usr_permission")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class UsrPermission implements Serializable {

    @Id
    @GeneratedValue
    private Integer permissionId;

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private Permission permission;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId")
    private UsrUser user;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrGroup.class)
    @JoinColumn(name = "groupId")
    private UsrGroup group;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId")
    private ArrFund fund;

    /** Slouží jen pro čtení. */
    @Column(name = "fundId", updatable = false, insertable = false, nullable = false)
    private Integer fundId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegScope.class)
    @JoinColumn(name = "scopeId")
    private RegScope scope;

    /** Slouží jen pro čtení. */
    @Column(name = "scopeId", updatable = false, insertable = false, nullable = false)
    private Integer scopeId;

    /**
     * @return identifikátor entity
     */
    public Integer getPermissionId() {
        return permissionId;
    }

    /**
     * @param permissionId identifikátor entity
     */
    public void setPermissionId(final Integer permissionId) {
        this.permissionId = permissionId;
    }

    /**
     * @return typ oprávnění
     */
    public Permission getPermission() {
        return permission;
    }

    /**
     * @param permission typ oprávnění
     */
    public void setPermission(final Permission permission) {
        this.permission = permission;
    }

    /**
     * @return uživatel, který má oprávnění přidělený
     */
    public UsrUser getUser() {
        return user;
    }

    /**
     * @param user uživatel, který má oprávnění přidělený
     */
    public void setUser(final UsrUser user) {
        this.user = user;
    }

    /**
     * @return skupina, která má oprávnění přidělený
     */
    public UsrGroup getGroup() {
        return group;
    }

    /**
     * @param group skupina, která má oprávnění přidělený
     */
    public void setGroup(final UsrGroup group) {
        this.group = group;
    }

    /**
     * @return archivní soubor, ke kterému se oprávnění vztahuje
     */
    public ArrFund getFund() {
        return fund;
    }

    /**
     * @param fund archivní soubor, ke kterému se oprávnění vztahuje
     */
    public void setFund(final ArrFund fund) {
        this.fund = fund;
        this.fundId = fund == null ? null : fund.getFundId();

    }

    /**
     * @return scope, ke kterému se oprávnění vztahuje
     */
    public RegScope getScope() {
        return scope;
    }

    /**
     * @param scope scope, ke kterému se oprávnění vztahuje
     */
    public void setScope(final RegScope scope) {
        this.scope = scope;
        this.scopeId = scope == null ? null : scope.getScopeId();
    }

    public Integer getFundId() {
        return fundId;
    }

    public Integer getScopeId() {
        return scopeId;
    }

    public void setFundId(final Integer fundId) {
        this.fundId = fundId;
    }

    public void setScopeId(final Integer scopeId) {
        this.scopeId = scopeId;
    }

    /**
     * Typ oprávnění. Řeší, zda-li oprávnění má ještě nějaké návaznosti.
     */
    public enum PermissionType {
        /**
         * Oprávnění se nevztahuje na konkrétní entitu.
         */
        ALL,

        /**
         * Oprávnění se vztahuje na konkrétní fund
         */
        FUND,

        /**
         * Oprávnění se vztahuje na konkrétní scope
         */
        SCOPE
    }

    /**
     * Oprávnění.
     *
     * - WR - write
     * - RD - read
     * - RW - read/write
     *
     * - FUND - Archivní soubor
     * - REG - Rejstříky
     *
     */
    public enum Permission {

        /**
         * administrátor - všechna oprávnění
         * - má uživatel system
         */
        ADMIN,

        /**
         * čtení vybraného AS
         * - má náhled jen na konrétní přiřazený AS ve všech verzích včeně OUPUT bez aktivních operací
         */
        FUND_RD(PermissionType.FUND),

        /**
         * čtení všech AS
         * - má náhled na všechny AS ve všech verzích včeně OUPUT bez aktivních operací
         */
        FUND_RD_ALL,

        /**
         * pořádání vybrané AS (pořádání je myšleno pořádání již vytvořeného AS, tvorba/úprava JP, tvorba úvodu, práce
         * s přílohami, správa obalů)
         * - úvod a přílohy zatím neřešíme
         * - platí jen pro konkrétní přiřazený AS
         * - aktivní správa obalů AS
         * - vidí OUTPUT, ale nemůže s nimi aktivně pracovat
         * - nemůže spouštět hromadné akce
         * - může přiřazovat rejstřík, ale jen v rozsahu práv na rejstříky (scope rejstříků), když nebude mít ani čtení
         *   rejstříků, tak nemůže nic přiřadit, opačně buď může přiřadit, nebo i zakládat nový ....
         */
        FUND_ARR(PermissionType.FUND),

        /**
         * pořádání všech AS
         * - obdobně jako výše, ale pro všechny AS
         */
        FUND_ARR_ALL,

        /**
         * čtení vybraného scope rejstříku (Pro přístup k rejstříkům a k osobám je řešen společným oprávněním.)
         * - přístup do části rejstříků včetně osob
         * - může jen pasivně číst rejstříková hesla z vybraného scope
         */
        REG_SCOPE_RD(PermissionType.SCOPE),

        /**
         * čtení všech scope rejstříků
         * - obdobně jako výše jen pro všechna rejstříková hesla
         */
        REG_SCOPE_RD_ALL,

        /**
         * zápis/úprava vybraného scope rejstříku
         * - obdobně jako výše, ale může hesla upravovat, přidávat, rušit, ale jen pro přiřazený scope
         */
        REG_SCOPE_WR(PermissionType.SCOPE),

        /**
         * zápis/úprava všech scope rejstříků
         * - obdboně jako výše pro všechna rejstříková hesla
         */
        REG_SCOPE_WR_ALL,

        /**
         * tvorba výstupů vybraného AS (AP, ad-hoc tisky)
         * - možnost vytvářet, měnit OUTPUT u přiřazeného AS
         * - může i verzovat OUTPUT což vyvolá verzi AS, ale beze změny pravidel
         * - nemůže exportovat, jen vytvářet
         */
        FUND_OUTPUT_WR(PermissionType.FUND),

        /**
         * tvorba výstupů všech AS
         * - obdobně jako výše ale pro všechny AS
         */
        FUND_OUTPUT_WR_ALL,

        /**
         * verzování a editace vybrané AS
         * - verzování a změna pravidel vpřiřazeného AS + přiřazení scope rejstříku + změna pravidel
         * - nemůže mazat AS
         */
        FUND_VER_WR(PermissionType.FUND),

        /**
         * administrace všech AS (verzování, zakládání AS, zrušení AS, import)
         * - všechna práva na všechny AS včetně rejstříků, OUTPUT apodobně (vyjma uživatelů a případného dalšího systémového
         *   nastavení)
         */
        FUND_ADMIN,

        /**
         * export vybrané AS
         * - možnost exportu AS či OUTPUT přiřazeného AS
         */
        FUND_EXPORT(PermissionType.FUND),

        /**
         * export všech AS
         * - obdobně jako výše ale pro všechny AS
         */
        FUND_EXPORT_ALL,

        /**
         * správa oprávnění a uživatelů
         * - zatím neřešíme
         */
        USR_PERM,

        /**
         * spouštění hromadných akcí vybrané AS
         * - možnost spuštění hromadných akcí přiřazeného AS
         */
        FUND_BA(PermissionType.FUND),

        /**
         * spouštění hromadných akcí všech AS
         * - obdobně jako výše ale pro všechny AS
         */
        FUND_BA_ALL,

        /**
         * drobné úpravy uzavřených vybraných AS
         * - zatím neřešíme
         */
        FUND_CL_VER_WR(PermissionType.FUND),

        /**
         * drobné úpravy uzavřených všech AS
         * - zatím neřešíme
         */
        FUND_CL_VER_WR_ALL,

        /**
         * Ukládání mapování typů vztahů a entit mezi INTERPI a ELZA.
         */
        INTERPI_MAPPING_WR;

        /**
         * Typ oprávnění
         */
        private PermissionType type;

        Permission() {
            type = PermissionType.ALL;
        }

        Permission(final PermissionType type) {
            this.type = type;
        }

        public PermissionType getType() {
            return type;
        }
    }
}
