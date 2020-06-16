export enum Permission {
    /**
     * administrátor - všechna oprávnění
     * - má uživatel system
     */
    ADMIN = 'ADMIN',

    /**
     * čtení vybraného AS
     * - má náhled jen na konrétní přiřazený AS ve všech verzích včeně OUPUT bez aktivních operací
     */
    FUND_RD = 'FUND_RD',

    /**
     * čtení všech AS
     * - má náhled na všechny AS ve všech verzích včeně OUPUT bez aktivních operací
     */
    FUND_RD_ALL = 'FUND_RD_ALL',

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
    FUND_ARR = 'FUND_ARR',

    /**
     * pořádání všech AS
     * - obdobně jako výše, ale pro všechny AS
     */
    FUND_ARR_ALL = 'FUND_ARR_ALL',

    /**
     * čtení vybraného scope rejstříku (Pro přístup k rejstříkům a k osobám je řešen společným oprávněním.)
     * - přístup do části rejstříků včetně osob
     * - může jen pasivně číst rejstříková hesla z vybraného scope
     */
    AP_SCOPE_RD = 'AP_SCOPE_RD',

    /**
     * čtení všech scope rejstříků
     * - obdobně jako výše jen pro všechna rejstříková hesla
     */
    AP_SCOPE_RD_ALL = 'AP_SCOPE_RD_ALL',

    /**
     * Zakládání a změny nových
     * <ul>
     * <li>založení nového přístupového bodu</li>
     * <li>nastavení stavu „Nový", „Ke schválení" i „K doplnění"</li>
     * <li>změnu přístupového bodu, pokud je ve stavu „Nový" i „Ke schválení" i „K doplnění"</li>
     * </ul>
     */
    AP_SCOPE_WR = 'AP_SCOPE_WR',

    /**
     * Zakládání a změny nových
     * - obdboně jako výše pro všechna rejstříková hesla
     */
    AP_SCOPE_WR_ALL = 'AP_SCOPE_WR_ALL',

    /**
     * Schvalování přístupových bodů
     * <ul>
     * <li>změnit stav na „Schválený" ze stavu „Nový" nebo „Ke schválení" nebo "K doplnění"</li>
     * <li>změnit stav na "K doplnění" ze stavu "Ke schválení" nebo "Nový"</li>
     * </ul>
     */
    AP_CONFIRM = 'AP_CONFIRM',

    /**
     * Schvalování přístupových bodů
     * - obdboně jako výše pro všechna rejstříková hesla
     */
    AP_CONFIRM_ALL = 'AP_CONFIRM_ALL',

    /**
     * Změna schválených přístupových bodů
     * <ul>
     * <li>editace již schválených přístupových bodů</li>
     * </ul>
     */
    AP_EDIT_CONFIRMED = 'AP_EDIT_CONFIRMED',

    /**
     * Změna schválených přístupových bodů
     * - obdboně jako výše pro všechna rejstříková hesla
     */
    AP_EDIT_CONFIRMED_ALL = 'AP_EDIT_CONFIRMED_ALL',

    /**
     * tvorba výstupů vybraného AS (AP, ad-hoc tisky)
     * - možnost vytvářet, měnit OUTPUT u přiřazeného AS
     * - může i verzovat OUTPUT což vyvolá verzi AS, ale beze změny pravidel
     * - nemůže exportovat, jen vytvářet
     */
    FUND_OUTPUT_WR = 'FUND_OUTPUT_WR',

    /**
     * tvorba výstupů všech AS
     * - obdobně jako výše ale pro všechny AS
     */
    FUND_OUTPUT_WR_ALL = 'FUND_OUTPUT_WR_ALL',

    /**
     * konfigurace AS
     * - verzování a změna pravidel vpřiřazeného AS + přiřazení scope rejstříku + změna pravidel
     * - nemůže mazat AS
     */
    FUND_VER_WR = 'FUND_VER_WR',

    /**
     * administrace všech AS (verzování, zakládání AS, zrušení AS, import)
     * - všechna práva na všechny AS včetně rejstříků, OUTPUT apodobně (vyjma uživatelů a případného dalšího systémového
     *   nastavení)
     */
    FUND_ADMIN = 'FUND_ADMIN',

    /**
     * Právo zakládání nového AS.
     */
    FUND_CREATE = 'FUND_CREATE',

    /**
     * export vybrané AS
     * - možnost exportu AS či OUTPUT přiřazeného AS
     */
    FUND_EXPORT = 'FUND_EXPORT',

    /**
     * export všech AS
     * - obdobně jako výše ale pro všechny AS
     */
    FUND_EXPORT_ALL = 'FUND_EXPORT_ALL',

    /**
     * správa oprávnění a uživatelů
     * - zatím neřešíme
     */
    USR_PERM = 'USR_PERM',

    /**
     * spouštění hromadných akcí vybrané AS
     * - možnost spuštění hromadných akcí přiřazeného AS
     */
    FUND_BA = 'FUND_BA',

    /**
     * spouštění hromadných akcí všech AS
     * - obdobně jako výše ale pro všechny AS
     */
    FUND_BA_ALL = 'FUND_BA_ALL',

    /**
     * drobné úpravy uzavřených vybraných AS
     * - zatím neřešíme
     */
    FUND_CL_VER_WR = 'FUND_CL_VER_WR',

    /**
     * drobné úpravy uzavřených všech AS
     * - zatím neřešíme
     */
    FUND_CL_VER_WR_ALL = 'FUND_CL_VER_WR_ALL',

    /**
     * Spravovaná entita - uživatel.
     */
    USER_CONTROL_ENTITITY = 'USER_CONTROL_ENTITITY',

    /**
     * Spravovaná entita skupina.
     */
    GROUP_CONTROL_ENTITITY = 'GROUP_CONTROL_ENTITITY',

    /**
     * Správa protokolů pro konkrétní AS
     */
    FUND_ISSUE_ADMIN = 'FUND_ISSUE_ADMIN',

    /**
     * Správa protokolů pro všechny AS
     */
    FUND_ISSUE_ADMIN_ALL = 'FUND_ISSUE_ADMIN_ALL',

    /**
     * Zobrazení připomínek pro konkrétní issue list
     */
    FUND_ISSUE_LIST_RD = 'FUND_ISSUE_LIST_RD',

    /**
     * Tvorba připomínek pro konkrétní issue list
     */
    FUND_ISSUE_LIST_WR = 'FUND_ISSUE_LIST_WR',

    /**
     * Pořádání na podstrom AS.
     */
    FUND_ARR_NODE = 'FUND_ARR_NODE',
}
