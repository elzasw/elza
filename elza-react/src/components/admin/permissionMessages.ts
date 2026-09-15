import { defineMessages } from "react-intl";
import type { MessageMap } from "components/shared/lang/dynamicMessage";

/**
 * Popisky jednotlivých oprávnění.
 *
 * `PermissionCheckboxsForm` dřív dostával prefix klíče jako prop
 * (`labelPrefix="admin.perms.tabs.funds.perm."`) a skládal z něj id za běhu -
 * statický extraktor takový klíč nevidí, takže se do katalogu nikdy nedostal.
 * Prefix proto přestal být data a stal se identitou mapy: každý panel předá
 * tu svoji.
 *
 * Id jsou převzatá z legacy katalogu beze změny.
 */

export const advancedPermissionMessages = defineMessages({
    ADMIN: { id: "admin.perms.tabs.advanced.perm.ADMIN", defaultMessage: "Superuživatel" },
    FUND_ADMIN: {
        id: "admin.perms.tabs.advanced.perm.FUND_ADMIN",
        defaultMessage: "Administrace všech AS",
    },
    FUND_ISSUE_ADMIN_ALL: {
        id: "admin.perms.tabs.advanced.perm.FUND_ISSUE_ADMIN_ALL",
        defaultMessage: "Správa lektorování všech AS",
    },
    FUND_CREATE: {
        id: "admin.perms.tabs.advanced.perm.FUND_CREATE",
        defaultMessage: "Zakládání nových AS",
    },
    USR_PERM: {
        id: "admin.perms.tabs.advanced.perm.USR_PERM",
        defaultMessage: "Správa oprávnění a uživatelů",
    },
    AP_EXTERNAL_WR: {
        id: "admin.perms.tabs.advanced.perm.AP_EXTERNAL_WR",
        defaultMessage: "Zápis do externích systémů",
    },
    REPORT_ALL: {
        id: "admin.perms.tabs.advanced.perm.REPORT_ALL",
        defaultMessage: "Zobrazení přehledů",
    },
});

export const fundPermissionMessages = defineMessages({
    FUND_RD: { id: "admin.perms.tabs.funds.perm.FUND_RD", defaultMessage: "Čtení" },
    FUND_ARR: { id: "admin.perms.tabs.funds.perm.FUND_ARR", defaultMessage: "Pořádání" },
    FUND_OUTPUT_WR: {
        id: "admin.perms.tabs.funds.perm.FUND_OUTPUT_WR",
        defaultMessage: "Tvorba výstupů",
    },
    FUND_EXPORT: { id: "admin.perms.tabs.funds.perm.FUND_EXPORT", defaultMessage: "Export" },
    FUND_PUBLISH: { id: "admin.perms.tabs.funds.perm.FUND_PUBLISH", defaultMessage: "Publikování" },
    FUND_BA: {
        id: "admin.perms.tabs.funds.perm.FUND_BA",
        defaultMessage: "Spouštění hromadných akcí",
    },
    FUND_CL_VER_WR: {
        id: "admin.perms.tabs.funds.perm.FUND_CL_VER_WR",
        defaultMessage: "Drobné úpravy uzavřeného AS",
    },
    FUND_VER_WR: {
        id: "admin.perms.tabs.funds.perm.FUND_VER_WR",
        defaultMessage: "Verzování a úprava vlastností",
    },
    FUND_ISSUE_ADMIN: {
        id: "admin.perms.tabs.funds.perm.FUND_ISSUE_ADMIN",
        defaultMessage: "Správa lektorování",
    },
    "FUND_ARR_NODE.add": {
        id: "admin.perms.tabs.funds.perm.FUND_ARR_NODE.add",
        defaultMessage: "Přidat konkrétní JP pro pořádání",
    },
});

export const scopePermissionMessages = defineMessages({
    AP_SCOPE_RD: { id: "admin.perms.tabs.scopes.perm.AP_SCOPE_RD", defaultMessage: "Čtení" },
    AP_SCOPE_WR: {
        id: "admin.perms.tabs.scopes.perm.AP_SCOPE_WR",
        defaultMessage: "Zakládání a změny nových",
    },
    AP_CONFIRM: {
        id: "admin.perms.tabs.scopes.perm.AP_CONFIRM",
        defaultMessage: "Schvalování archivních entit",
    },
    AP_EDIT_CONFIRMED: {
        id: "admin.perms.tabs.scopes.perm.AP_EDIT_CONFIRMED",
        defaultMessage: "Změna schválených archivních entit",
    },
});

/** Popisky "všechny AS" / "všechny oblasti entit" - zdroj zděděného oprávnění. */
export const permissionScopeMessages = defineMessages({
    fundAll: {
        id: "admin.perms.tabs.funds.items.fundAll",
        defaultMessage: "Všechny archivní soubory",
    },
    scopeAll: {
        id: "admin.perms.tabs.scopes.items.scopeAll",
        defaultMessage: "Všechny oblasti entit",
    },
});

export const permissionMessageMaps: Record<string, MessageMap> = {
    advanced: advancedPermissionMessages,
    funds: fundPermissionMessages,
    scopes: scopePermissionMessages,
};
