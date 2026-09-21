/**
 * across-tabs nema vlastni typy ani balicek @types. Deklarujeme jen tu cast API,
 * kterou pouziva CrossTabHelper; zbytek zustava netypovany zamerne.
 */
declare module "across-tabs" {
    namespace AcrossTabs {
        interface Tab {
            id: string;
            name?: string;
            status?: string;
            ref?: Window;
            [key: string]: unknown;
        }

        interface ChildConfig {
            onReady?: () => void;
            onInitialize?: () => void;
            onParentDisconnect?: () => void;
            onParentCommunication?: (message: unknown) => void;
            [key: string]: unknown;
        }

        interface ParentConfig {
            onHandshakeCallback?: (tab: Tab) => void;
            onPollingCallback?: (tabs: Tab[]) => void;
            onChildDisconnect?: (tab: Tab) => void;
            onChildCommunication?: (message: unknown) => void;
            [key: string]: unknown;
        }

        class Child {
            constructor(config?: ChildConfig);
            /** Knihovna umoznuje callbacky nastavit i dodatecne na instanci. */
            onParentDisconnect: () => void;
            /** Konfigurace, ze ktere knihovna cte callbacky pri kazde prijate zprave. */
            config: ChildConfig;
            /** Id timeoutu hlidajiciho handshake s rodicem; po vyprseni uz neexistuje. */
            timeout?: ReturnType<typeof window.setTimeout>;
            /** Obsluha zprav od rodice; knihovna ji vola z posluchace na okne. */
            onCommunication(message: MessageEvent): void;
            sendMessageToParent(message: unknown): void;
            getTabInfo(): Tab;
        }

        class Parent {
            constructor(config?: ParentConfig);
            /** Knihovna umoznuje callbacky nastavit i dodatecne na instanci. */
            onHandshakeCallback: (tab: Tab) => void;
            openNewTab(config: {url: string; windowName?: string; windowFeatures?: string}): Tab;
            getOpenedTabs(): Tab[];
            getAllTabs(): Tab[];
            closeAllTabs(): void;
            broadCastAll(message: unknown): void;
            broadCastTo(tabId: string, message: unknown): void;
        }
    }

    const AcrossTabs: {
        Child: typeof AcrossTabs.Child;
        Parent: typeof AcrossTabs.Parent;
    };

    export default AcrossTabs;
}
