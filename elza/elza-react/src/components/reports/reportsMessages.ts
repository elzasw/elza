import { defineMessages } from "react-intl";

export const headerMessages = defineMessages({
    asPocet: {
        id: "admin_reports_header_AS_POCET",
        defaultMessage: "Počet archivních souborů",
    },
    jpPocet: {
        id: "admin_reports_header_JP_POCET",
        defaultMessage: "Počet jednotek popisu",
    },
    ppPocet: {
        id: "admin_reports_header_PP_POCET",
        defaultMessage: "Počet prvků popisu",
    },
    aePocet: {
        id: "admin_reports_header_AE_POCET",
        defaultMessage: "Počet archivních entit",
    },
    pbPocet: {
        id: "admin_reports_header_PB_POCET",
        defaultMessage: "Počet přístupových bodů",
    },
    vpbPocet: {
        id: "admin_reports_header_VPB_POCET",
        defaultMessage: "Počet vazeb přístupových bodů",
    },
    internalCode: {
        id: "admin_reports_header_INTERNAL_CODE",
        defaultMessage: "Číslo instituce",
    },
    indexValue: {
        id: "admin_reports_header_INDEX_VALUE",
        defaultMessage: "Instituce",
    },
    fondsCnt: {
        id: "admin_reports_header_FONDS_CNT",
        defaultMessage: "Počet archivních souborů",
    },
    levelsCnt: {
        id: "admin_reports_header_LEVELS_CNT",
        defaultMessage: "Počet jednotek popisu",
    },
    itemsCnt: {
        id: "admin_reports_header_ITEMS_CNT",
        defaultMessage: "Počet prvků popisu",
    },
    refentsCnt: {
        id: "admin_reports_header_REFENTS_CNT",
        defaultMessage: "Počet vazeb přístupových bodů",
    },
    dateYear: {
        id: "admin_reports_header_DATE_YEAR",
        defaultMessage: "Rok"
    },
    dateMonth: {
        id: "admin_reports_header_DATE_MONTH",
        defaultMessage: "Měsíc"
    },
    username: {
        id: "admin_reports_header_USERNAME",
        defaultMessage: "Uživatel"
    },
    levelNew: {
        id: "admin_reports_header_LEVEL_NEW",
        defaultMessage: "Nové jednotky popisu"
    },
    levelDelete: {
        id: "admin_reports_header_LEVEL_DELETE",
        defaultMessage: "Smazané jednotky popisu"
    },
    itemNew: {
        id: "admin_reports_header_ITEM_NEW",
        defaultMessage: "Nové prvky popisu"
    },
    itemUpdate: {
        id: "admin_reports_header_ITEM_UPDATE",
        defaultMessage: "Změněné prvky popisu"
    },
    itemDelete: {
        id: "admin_reports_header_ITEM_DELETE",
        defaultMessage: "Smazané prvky popisu"
    },
    apNew: {
        id: "admin_reports_header_AP_NEW",
        defaultMessage: "Nové arch. entity"
    },
    apUpdate: {
        id: "admin_reports_header_AP_UPDATE",
        defaultMessage: "Změněné arch. entity"
    },
    apDelete: {
        id: "admin_reports_header_AP_DELETE",
        defaultMessage: "Smazané arch. entity"
    },
    apReplace: {
        id: "admin_reports_header_AP_REPLACE",
        defaultMessage: "Nahrazené arch. entity"
    },
    apusgNew: {
        id: "admin_reports_header_APUSG_NEW",
        defaultMessage: "Nové vazby přístupových bodů"
    },
    apusgDelete: {
        id: "admin_reports_header_APUSG_DELETE",
        defaultMessage: "Odstraněné vazby přístupových bodů"
    },
    instName: {
        id: "admin_reports_header_INST_NAME",
        defaultMessage: "Instituce"
    },
    instCode: {
        id: "admin_reports_header_INST_CODE",
        defaultMessage: "Číslo instituce"
    },
    fondsNumber: {
        id: "admin_reports_header_FONDS_NUMBER",
        defaultMessage: "Číslo archivního souboru"
    },
    fondsName: {
        id: "admin_reports_header_FONDS_NAME",
        defaultMessage: "Název archivního souboru"
    },
    faNumber: {
        id: "admin_reports_header_FA_NUMBER",
        defaultMessage: "Číslo archivní pomůcky"
    },
    outputName: {
        id: "admin_reports_header_OUTPUT_NAME",
        defaultMessage: "Název výstupu"
    },
    faType: {
        id: "admin_reports_header_FA_TYPE",
        defaultMessage: "Druh archivní pomůcky"
    },
    faDate: {
        id: "admin_reports_header_FA_DATE",
        defaultMessage: "Datum archivní pomůcky"
    },
    faUnitCnt: {
        id: "admin_reports_header_FA_UNIT_COUNT",
        defaultMessage: "Součet jednotek popisu"
    },
    outputType: {
        id: "admin_reports_header_OUTPUT_TYPE",
        defaultMessage: "Typ výstupu"
    },
    templName: {
        id: "admin_reports_header_TEMPL_NAME",
        defaultMessage: "Šablona"
    },
    outputDate: {
        id: "admin_reports_header_OUTPUT_DATE",
        defaultMessage: "Čas generování výstupu"
        },
    extSysName: {
        id: "admin_reports_header_EXTERNAL_SYSTEM_NAME",
        defaultMessage: "Název externího systému"
        },
})

export const formMessages = defineMessages({
    arrangement: {
        id: "admin_reports_form_category_ARRANGEMENT",
        defaultMessage: "Zpracování archiválií",
    },
    entity: {
        id: "admin_reports_form_category_ENTITY",
        defaultMessage: "Archivní entity",
    },
    output: {
        id: "admin_reports_form_category_OUTPUT",
        defaultMessage: "Výstupy",
    },
    sysTotalCount: {
        id: "admin_reports_form_report_SYS_TOTAL_COUNT",
        defaultMessage: "Souhrnné informace – aktuální stav",
    },
    sysMonthUserCount: {
        id: "admin_reports_form_report_SYS_MONTH_USER_COUNT",
        defaultMessage: "Přehled po měsících dle uživatelů",
    },
    sysInstitutionCount: {
        id: "admin_reports_form_report_SYS_INSTITUTION_COUNT",
        defaultMessage: "Přehled k datu dle institucí",
    },
    sysExtSystemCount: {
        id: "admin_reports_form_report_SYS_EXT_SYSTEM_COUNT",
        defaultMessage: "Přehled k datu dle zápisu do externích systémů",
    },
    sysOutputCount: {
        id: "admin_reports_form_report_SYS_OUTPUT_COUNT",
        defaultMessage: "Přehled k datu dle zápisu do externích systémů",
    },
    dateFrom: {
        id: "admin_reports_form_property_DATE_FROM",
        defaultMessage: "Datum od",
    },
    dateTo: {
        id: "admin_reports_form_property_DATE_TO",
        defaultMessage: "Datum do",
    },
    date: {
        id: "admin_reports_form_property_DATE",
        defaultMessage: "Datum",
    },
    instituce: {
        id: "admin_reports_form_property_INSTITUCE",
        defaultMessage: "Instituce",
    },
})
