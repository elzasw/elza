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
    usageDetail: {
        id: "aiAssistant.usage.detail",
        defaultMessage: "Vstupní tokeny: {input}, výstupní tokeny: {output}, cena: {cost}",
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
    activityToolGeneric: {
        id: "aiAssistant.activity.tool.generic",
        defaultMessage: "Nástroj {tool}",
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
