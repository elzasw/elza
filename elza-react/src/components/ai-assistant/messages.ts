import { defineMessages } from "react-intl";

export const aiAssistantMessages = defineMessages({
    windowTitle: {
        id: "aiAssistant.window.title",
        defaultMessage: "AI asistent",
    },
    inputPlaceholder: {
        id: "aiAssistant.input.placeholder",
        defaultMessage: "Napište zprávu…",
    },
    send: {
        id: "aiAssistant.action.send",
        defaultMessage: "Odeslat",
    },
    cancel: {
        id: "aiAssistant.action.cancel",
        defaultMessage: "Zrušit",
    },
    empty: {
        id: "aiAssistant.state.empty",
        defaultMessage: "Zeptejte se AI asistenta na cokoli.",
    },
    thinking: {
        id: "aiAssistant.state.thinking",
        defaultMessage: "AI přemýšlí…",
    },
    usage: {
        id: "aiAssistant.usage.summary",
        defaultMessage: "Využití",
    },
    usageTokens: {
        id: "aiAssistant.usage.tokens",
        defaultMessage: "Vstupní tokeny: {input}, výstupní tokeny: {output}",
    },
    usagePrice: {
        id: "aiAssistant.usage.price",
        defaultMessage: "Cena: {credits} kr.",
    },
    usageProfile: {
        id: "aiAssistant.usage.profile",
        defaultMessage: "Model: {profile}",
    },
    usageStarted: {
        id: "aiAssistant.usage.started",
        defaultMessage: "Zahájeno: {datetime}",
    },
    usageDuration: {
        id: "aiAssistant.usage.duration",
        defaultMessage: "Doba zpracování: {duration}",
    },
    usageMenuItem: {
        id: "aiAssistant.usage.menuItem",
        defaultMessage: "Spotřeba kreditů",
    },
    usageDialogTitle: {
        id: "aiAssistant.usage.dialog.title",
        defaultMessage: "Spotřeba kreditů",
    },
    usageDialogClose: {
        id: "aiAssistant.usage.dialog.close",
        defaultMessage: "Zavřít",
    },
    usageNoData: {
        id: "aiAssistant.usage.noData",
        defaultMessage: "Informace o spotřebě nejsou pro tohoto poskytovatele k dispozici.",
    },
    usageAccountHeading: {
        id: "aiAssistant.usage.accountHeading",
        defaultMessage: "Váš účet",
    },
    usageSharedAccountHeading: {
        id: "aiAssistant.usage.sharedAccountHeading",
        defaultMessage: "Sdílený účet organizace",
    },
    usageCustomerHeading: {
        id: "aiAssistant.usage.customerHeading",
        defaultMessage: "Rozpočet organizace",
    },
    balanceSpentOfAllowance: {
        id: "aiAssistant.balance.spentOfAllowance",
        defaultMessage: "{spent} z {allowance} kreditů",
    },
    balanceSpent: {
        id: "aiAssistant.balance.spent",
        defaultMessage: "{spent} kreditů",
    },
    balanceUsedPercent: {
        id: "aiAssistant.balance.usedPercent",
        defaultMessage: "Vyčerpáno {percent} %",
    },
    balanceUnlimited: {
        id: "aiAssistant.balance.unlimited",
        defaultMessage: "Bez limitu",
    },
    balancePlan: {
        id: "aiAssistant.balance.plan",
        defaultMessage: "Tarif: {plan}",
    },
    balanceResets: {
        id: "aiAssistant.balance.resets",
        defaultMessage: "Obnovení přídělu: {date}",
    },
    balanceCustomer: {
        id: "aiAssistant.balance.customer",
        defaultMessage: "{spent} z {budget} kreditů",
    },
    balanceCustomerNoCap: {
        id: "aiAssistant.balance.customerNoCap",
        defaultMessage: "{spent} kreditů",
    },
    errorNoSubscription: {
        id: "aiAssistant.error.noSubscription",
        defaultMessage: "AI služba nemá aktivní předplatné. Obraťte se na správce.",
    },
    errorQuotaExceeded: {
        id: "aiAssistant.error.quotaExceeded",
        defaultMessage: "Měsíční rozpočet organizace pro AI služby je vyčerpán.",
    },
    errorAccountQuotaExceeded: {
        id: "aiAssistant.error.accountQuotaExceeded",
        defaultMessage: "Váš kreditový příděl je pro toto období vyčerpán.",
    },
    unsupportedBlock: {
        id: "aiAssistant.block.unsupported",
        defaultMessage: "Nepodporovaný typ obsahu.",
    },
    citations: {
        id: "aiAssistant.block.citations",
        defaultMessage: "Zdroje",
    },
    errorPrefix: {
        id: "aiAssistant.state.errorPrefix",
        defaultMessage: "Chyba",
    },
    contextNone: {
        id: "aiAssistant.context.none",
        defaultMessage: "Bez kontextu",
    },
    contextPromptLabel: {
        id: "aiAssistant.context.promptLabel",
        defaultMessage: "Kontext:",
    },
    settings: {
        id: "aiAssistant.action.settings",
        defaultMessage: "Nastavení",
    },
    profile: {
        id: "aiAssistant.action.profile",
        defaultMessage: "Model",
    },
    profileDefault: {
        id: "aiAssistant.profile.default",
        defaultMessage: "Výchozí",
    },
    fullWidthResponses: {
        id: "aiAssistant.settings.fullWidthResponses",
        defaultMessage: "Odpovědi na celou šířku",
    },
    newChat: {
        id: "aiAssistant.action.newChat",
        defaultMessage: "Nová konverzace",
    },
    history: {
        id: "aiAssistant.action.history",
        defaultMessage: "Historie konverzací",
    },
    expandPanel: {
        id: "aiAssistant.action.expandPanel",
        defaultMessage: "Rozbalit panel",
    },
    collapsePanel: {
        id: "aiAssistant.action.collapsePanel",
        defaultMessage: "Sbalit panel",
    },
    contextModule: {
        id: "aiAssistant.context.module",
        defaultMessage: "Modul",
    },
    moduleArrangement: {
        id: "aiAssistant.context.module.arrangement",
        defaultMessage: "Pořádání",
    },
    moduleRegistry: {
        id: "aiAssistant.context.module.registry",
        defaultMessage: "Entity",
    },
    contextFund: {
        id: "aiAssistant.context.fund",
        defaultMessage: "Archivní soubor",
    },
    contextNode: {
        id: "aiAssistant.context.node",
        defaultMessage: "Jednotka popisu",
    },
    contextAccessPoint: {
        id: "aiAssistant.context.accessPoint",
        defaultMessage: "Přístupový bod",
    },
    steps: {
        id: "aiAssistant.activity.steps",
        defaultMessage: "Postup zpracování",
    },
    activityToolSearchNodes: {
        id: "aiAssistant.activity.tool.searchNodes",
        defaultMessage: "Vyhledávání v archivním popisu",
    },
    activityToolGetItemTypes: {
        id: "aiAssistant.activity.tool.getItemTypes",
        defaultMessage: "Načtení typů prvků popisu",
    },
    activityToolSearchKnowledge: {
        id: "aiAssistant.activity.tool.searchKnowledge",
        defaultMessage: "Vyhledávání v poradně",
    },
    activityToolGetSection: {
        id: "aiAssistant.activity.tool.getSection",
        defaultMessage: "Čtení dokumentace",
    },
    activityToolGeneric: {
        id: "aiAssistant.activity.tool.generic",
        defaultMessage: "Nástroj {tool}",
    },
    activityPreparation: {
        id: "aiAssistant.activity.preparation",
        defaultMessage: "Příprava zpracování",
    },
    activityStepGeneric: {
        id: "aiAssistant.activity.step.generic",
        defaultMessage: "Krok zpracování",
    },
    activityResultCount: {
        id: "aiAssistant.activity.resultCount",
        defaultMessage: "{count, plural, one {# výsledek} few {# výsledky} other {# výsledků}}",
    },
    activityResultPartial: {
        id: "aiAssistant.activity.resultPartial",
        defaultMessage: "výsledek zkrácen",
    },
    activityMoreLinks: {
        id: "aiAssistant.activity.moreLinks",
        defaultMessage: "{count, plural, one {+# další} few {+# další} other {+# dalších}}",
    },
    activityLinkOpen: {
        id: "aiAssistant.activity.linkOpen",
        defaultMessage: "Zobrazit záznam",
    },
});

export const aiContextSegmentLabels = {
    module: aiAssistantMessages.contextModule,
    fund: aiAssistantMessages.contextFund,
    node: aiAssistantMessages.contextNode,
    accessPoint: aiAssistantMessages.contextAccessPoint,
};

export const aiModuleLabels = {
    arrangement: aiAssistantMessages.moduleArrangement,
    registry: aiAssistantMessages.moduleRegistry,
};
