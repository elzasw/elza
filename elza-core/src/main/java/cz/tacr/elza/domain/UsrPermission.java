package cz.tacr.elza.domain;

import java.util.HashSet;
import java.util.Set;

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
 * Oprávnění pro uživatele / skupinu.
 *
 */
@Entity(name = "usr_permission")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class UsrPermission {

	public static final String FIELD_USER = "user";
    public static final String FIELD_USER_ID = "userId";
	public static final String FIELD_GROUP = "group";
    public static final String FIELD_GROUP_ID = "groupId";
    public static final String FIELD_NODE = "node";
    public static final String FIELD_NODE_ID = "nodeId";
    public static final String FIELD_USER_CONTROL = "userControl";
    public static final String FIELD_USER_CONTROL_ID = "userControlId";
    public static final String FIELD_GROUP_CONTROL_ID = "groupControlId";
	public static final String FIELD_GROUP_CONTROL = "groupControl";
	public static final String FIELD_PERMISSION = "permission";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
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

    @Column(name = "groupId", updatable = false, insertable = false)
    private Integer groupId;

    @Column(name = "userId", updatable = false, insertable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId")
    private ArrFund fund;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId")
    private ArrNode node;

    /** Slouží jen pro čtení. */
    @Column(name = "nodeId", updatable = false, insertable = false, nullable = false)
    private Integer nodeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userControlId")
    private UsrUser userControl;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrGroup.class)
    @JoinColumn(name = "groupControlId")
    private UsrGroup groupControl;

    /** Slouží jen pro čtení. */
    @Column(name = "fundId", updatable = false, insertable = false, nullable = false)
    private Integer fundId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApScope.class)
    @JoinColumn(name = "scopeId")
    private ApScope scope;

    /** Slouží jen pro čtení. */
    @Column(name = "scopeId", updatable = false, insertable = false, nullable = false)
    private Integer scopeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = WfIssueList.class)
    @JoinColumn(name = "issueListId")
    private WfIssueList issueList;

    /** Slouží jen pro čtení. */
    @Column(name = "issueListId", updatable = false, insertable = false, nullable = false)
    private Integer issueListId;

    /** Slouží jen pro čtení. */
    @Column(name = "userControlId", updatable = false, insertable = false, nullable = false)
    private Integer userControlId;

    /** Slouží jen pro čtení. */
    @Column(name = "groupControlId", updatable = false, insertable = false, nullable = false)
    private Integer groupControlId;

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
		if (user != null) {
			this.userId = user.getUserId();
		} else {
			this.userId = null;
		}
	}

	public Integer getUserId() {
		return userId;
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
		if (group != null) {
			groupId = group.getGroupId();
		} else {
			groupId = null;
		}
	}

	public Integer getGroupId() {
		return groupId;
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
    public ApScope getScope() {
        return scope;
    }

    /**
     * @param scope scope, ke kterému se oprávnění vztahuje
     */
    public void setScope(final ApScope scope) {
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

    public Integer getNodeId() {
        return nodeId;
    }

    public ArrNode getNode() {
        return node;
    }

    public void setNode(final ArrNode node) {
        this.node = node;
        this.nodeId = node == null ? null : node.getNodeId();
    }

    /**
     * @return protokol, ke kterému se oprávnění vztahuje
     */
    public WfIssueList getIssueList() {
        return issueList;
    }

    /**
     * @return protokol, ke kterému se oprávnění vztahuje
     */
    public void setIssueList(WfIssueList issueList) {
        this.issueList = issueList;
        this.issueListId = issueList == null ? null : issueList.getIssueListId();
    }

    public Integer getIssueListId() {
        return issueListId;
    }

    public UsrUser getUserControl() {
        return userControl;
    }

    public void setUserControl(UsrUser userControl) {
        this.userControl = userControl;
        this.userControlId = userControl == null ? null : userControl.getUserId();
    }

    public UsrGroup getGroupControl() {
        return groupControl;
    }

    public void setGroupControl(UsrGroup groupControl) {
        this.groupControl = groupControl;
        this.groupControlId = groupControl == null ? null : groupControl.getGroupId();
    }

    public Integer getUserControlId() {
        return userControlId;
    }

    public Integer getGroupControlId() {
        return groupControlId;
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
         * Oprávnění se vztahuje na konkrétní fund.
         */
        FUND,

        /**
         * Oprávnění se vztahuje na konkrétního uživatele - např. jako spravovanou entitu.
         */
        USER,

        /**
		 * Oprávnění se vztahuje na konkrétní skupinu - např. jako spravovanou
		 * entitu.
		 */
		GROUP,

        /**
		 * Oprávnění se vztahuje na konkrétní scope.
		 */
		SCOPE,

        /**
         * Oprávnění se vztahuje na konkrétní protokol.
         */
        ISSUE_LIST,

        /**
         * Oprávnění se vztahuje na konkrétní JP.
         */
        NODE
    }

    /**
     * Oprávnění.
     *
     * - WR - write
     * - RD - read
     * - RW - read/write
     *
     * - FUND - Archivní soubor
     * - AP - Rejstříky
     *
     */
    public enum Permission {

        /**
         * administrátor - všechna oprávnění
         * - má uživatel system
         */
        ADMIN {
            @Override
            public boolean isEqualOrHigher(Permission permission) {
                return true;
            }
        },

        /**
         * čtení vybraného AS
         * - má náhled jen na konrétní přiřazený AS ve všech verzích včeně OUPUT bez aktivních operací
         */
        FUND_RD(PermissionType.FUND),

        /**
         * čtení všech AS
         * - má náhled na všechny AS ve všech verzích včeně OUPUT bez aktivních operací
         */
        FUND_RD_ALL {
            @Override
            public boolean isEqualOrHigher(Permission permission) {
                if (permission == FUND_RD_ALL || permission == FUND_RD) {
                    return true;
                }
                return false;
            }
        },

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
        FUND_ARR(PermissionType.FUND) {
            @Override
            public boolean isEqualOrHigher(Permission permission) {
                if (permission == FUND_ARR || permission == FUND_ARR_NODE) {
                    return true;
                }
                return false;
            }
        },

        /**
         * pořádání všech AS
         * - obdobně jako výše, ale pro všechny AS
         */
        FUND_ARR_ALL {
            @Override
            public boolean isEqualOrHigher(Permission permission) {
                if (permission == FUND_ARR_ALL || permission == FUND_ARR || permission == FUND_ARR_NODE) {
                    return true;
                }
                return false;
            }
        },

        /**
         * čtení vybraného scope rejstříku (Pro přístup k rejstříkům a k osobám je řešen společným oprávněním.)
         * - přístup do části rejstříků včetně osob
         * - může jen pasivně číst rejstříková hesla z vybraného scope
         */
        AP_SCOPE_RD(PermissionType.SCOPE),

        /**
         * čtení všech scope rejstříků
         * - obdobně jako výše jen pro všechna rejstříková hesla
         */
        AP_SCOPE_RD_ALL {
            @Override
            public boolean isEqualOrHigher(Permission permission) {
                if (permission == AP_SCOPE_RD_ALL || permission == AP_SCOPE_RD) {
                    return true;
                }
                return false;
            }
        },

        /**
         * Zakládání a změny nových
         * <ul>
         * <li>založení nového přístupového bodu</li>
         * <li>nastavení stavu „Nový", „Ke schválení" i „K doplnění"</li>
         * <li>změnu přístupového bodu, pokud je ve stavu „Nový" i „Ke schválení" i „K doplnění"</li>
         * </ul>
         */
        AP_SCOPE_WR(PermissionType.SCOPE),

        /**
         * Zakládání a změny nových
         * - obdboně jako výše pro všechna rejstříková hesla
         */
        AP_SCOPE_WR_ALL {
            @Override
            public boolean isEqualOrHigher(Permission permission) {
                if (permission == AP_SCOPE_WR_ALL || permission == AP_SCOPE_WR) {
                    return true;
                }
                return false;
            }
        },

        /**
         * Schvalování přístupových bodů
         * <ul>
         * <li>změnit stav na „Schválený" ze stavu „Nový" nebo „Ke schválení" nebo "K doplnění"</li>
         * <li>změnit stav na "K doplnění" ze stavu "Ke schválení" nebo "Nový"</li>
         * </ul>
         */
        AP_CONFIRM(PermissionType.SCOPE),

        /**
         * Schvalování přístupových bodů
         * - obdboně jako výše pro všechna rejstříková hesla
         */
        AP_CONFIRM_ALL {
            @Override
            public boolean isEqualOrHigher(Permission permission) {
                if (permission == AP_CONFIRM_ALL || permission == AP_CONFIRM) {
                    return true;
                }
                return false;
            }
        },

        /**
         * Změna schválených přístupových bodů
         * <ul>
         * <li>editace již schválených přístupových bodů</li>
         * </ul>
         */
        AP_EDIT_CONFIRMED(PermissionType.SCOPE),

        /**
         * Změna schválených přístupových bodů
         * - obdboně jako výše pro všechna rejstříková hesla
         */
        AP_EDIT_CONFIRMED_ALL {
            @Override
            public boolean isEqualOrHigher(Permission permission) {
                if (permission == AP_EDIT_CONFIRMED_ALL || permission == AP_EDIT_CONFIRMED) {
                    return true;
                }
                return false;
            }
        },

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
        FUND_OUTPUT_WR_ALL {
            @Override
            public boolean isEqualOrHigher(Permission permission) {
                if (permission == FUND_OUTPUT_WR_ALL || permission == FUND_OUTPUT_WR) {
                    return true;
                }
                return false;
            }
        },

        /**
         * konfigurace AS
         * - verzování a změna pravidel vpřiřazeného AS + přiřazení scope rejstříku + změna pravidel
         * - nemůže mazat AS
         */
        FUND_VER_WR(PermissionType.FUND),

        /**
         * administrace všech AS (verzování, zakládání AS, zrušení AS, import)
         * - všechna práva na všechny AS včetně rejstříků, OUTPUT apodobně (vyjma uživatelů a případného dalšího systémového
         *   nastavení)
         */
        FUND_ADMIN {
            @Override
            public boolean isEqualOrHigher(Permission permission) {
                if (permission == FUND_ADMIN || permission == FUND_CREATE || permission == FUND_VER_WR) {
                    return true;
                }
                return false;
            }
        },

        /**
         * Právo zakládání nového AS.
         */
        FUND_CREATE,

        /**
         * export vybrané AS
         * - možnost exportu AS či OUTPUT přiřazeného AS
         */
        FUND_EXPORT(PermissionType.FUND),

        /**
         * export všech AS
         * - obdobně jako výše ale pro všechny AS
         */
        FUND_EXPORT_ALL {
            @Override
            public boolean isEqualOrHigher(Permission permission) {
                if (permission == FUND_EXPORT_ALL || permission == FUND_EXPORT) {
                    return true;
                }
                return false;
            }
        },

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
        FUND_BA_ALL {
            @Override
            public boolean isEqualOrHigher(Permission permission) {
                if (permission == FUND_BA_ALL || permission == FUND_BA) {
                    return true;
                }
                return false;
            }
        },

        /**
         * drobné úpravy uzavřených vybraných AS
         * - zatím neřešíme
         */
         FUND_CL_VER_WR(PermissionType.FUND),

        /**
         * drobné úpravy uzavřených všech AS
         * - zatím neřešíme
         */
        FUND_CL_VER_WR_ALL {
            @Override
            public boolean isEqualOrHigher(Permission permission) {
                if (permission == FUND_CL_VER_WR || permission == FUND_CL_VER_WR_ALL) {
                    return true;
                }
                return false;
            }
        },

        /**
         * Spravovaná entita - uživatel.
         */
        USER_CONTROL_ENTITITY(PermissionType.USER),

        /**
         * Spravovaná entita skupina.
         */
        GROUP_CONTROL_ENTITITY(PermissionType.GROUP),

        /**
         * Správa protokolů pro konkrétní AS
         */
        FUND_ISSUE_ADMIN(PermissionType.FUND),

        /**
         * Správa protokolů pro všechny AS
         */
        FUND_ISSUE_ADMIN_ALL {
            @Override
            public boolean isEqualOrHigher(Permission permission) {
                if (permission == FUND_ISSUE_ADMIN_ALL || permission == FUND_ISSUE_ADMIN) {
                    return true;
                }
                return false;
            }
        },

        /**
         * Zobrazení připomínek pro konkrétní issue list
         */
        FUND_ISSUE_LIST_RD(PermissionType.ISSUE_LIST),

        /**
         * Tvorba připomínek pro konkrétní issue list
         */
        FUND_ISSUE_LIST_WR(PermissionType.ISSUE_LIST),

        /**
         * Pořádání na podstrom AS.
         */
        FUND_ARR_NODE(PermissionType.NODE);

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

        /**
         * Oprávnění typu fund ALL.
         */
        static Set<Permission> fundAllPerms = new HashSet<>();
        static {
            fundAllPerms.add(UsrPermission.Permission.FUND_ARR_ALL);
            fundAllPerms.add(UsrPermission.Permission.FUND_OUTPUT_WR_ALL);
            fundAllPerms.add(UsrPermission.Permission.FUND_RD_ALL);
            fundAllPerms.add(UsrPermission.Permission.FUND_BA_ALL);
            fundAllPerms.add(UsrPermission.Permission.FUND_EXPORT_ALL);
            fundAllPerms.add(UsrPermission.Permission.FUND_CL_VER_WR_ALL);
            fundAllPerms.add(UsrPermission.Permission.FUND_ISSUE_ADMIN_ALL);
        }

        /**
         * @return oprávnění typu fund ALL
         */
        public static Set<Permission> getFundAllPerms() {
            return fundAllPerms;
        }

        /**
         * Check if this permission is equal or higher then given permission
         *
         * @param permission
         *            Permission to be checked
         * @return Return true if this permission is same or higher then given
         *         permission
         */
        public boolean isEqualOrHigher(Permission permission)
        {
            if(this==permission) {
                return true;
            } // if permission is global
            return false;
    }
    }
}
