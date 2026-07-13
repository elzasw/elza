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
