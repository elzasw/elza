import { default as AcrossTabs } from 'across-tabs';
import { getMapUrlWithContext } from '../pages/map/MapPage';
import { getComponentUrlWithContext } from 'pages/component/ComponentPage';

export enum CrossTabEventType {
    SHOW_IN_MAP = 'SHOW_IN_MAP',
    DISPLAY_COMPONENT = 'DISPLAY_COMPONENT'
}

export interface CrossTabEvent {
    type: CrossTabEventType;
    data: unknown;
}
/** Knihovna predava do callbacku zalozku, se kterou se prave podarilo spojit. */
type HandshakeCallback = (tab: AcrossTabs.Tab) => void;

export interface CrossTabUserClassInstance {
    child?: AcrossTabs.Child;
    parent?: AcrossTabs.Parent;
    processCrossTabEvent: (event: CrossTabEvent) => boolean;
    onHandshakeCallback: HandshakeCallback;
}

export const TAB_EVENT_SHOW_IN_MAP = 'TAB_EVENT_SHOW_IN_MAP';
export const WINDOW_MAP_NAME = 'WINDOW_MAP_NAME';

export const TAB_EVENT_DISPLAY_COMPONENT = 'TAB_EVENT_DISPLAY_COMPONENT';
export const WINDOW_COMPONENT = 'WINDOW_COMPONENT';

const NO_PARENT_EX = '_NO_PARENT';

/**
 * Uklid po across-tabs.
 *
 * Knihovna si posluchace registruje jako nove arrow funkce
 * (`window.addEventListener('message', evt => this.onCommunication(evt))`),
 * takze je vlastnim removeEventListener nikdy neodebere a po kazdem spojeni
 * zustanou viset na okne - Child se pritom zaklada pri kazdem odeslani udalosti.
 * Zachytime si je proto pri konstrukci a odebirame sami. Stejnym zpusobem
 * knihovna prepisuje window.onbeforeunload; puvodni hodnotu vracime zpet.
 */
const disposers = new WeakMap<object, () => void>();

function createWithOwnedListeners<T extends object>(create: () => T): T {
    const captured: Array<[string, EventListenerOrEventListenerObject]> = [];
    const addEventListener = window.addEventListener;
    const beforeUnloadBefore = window.onbeforeunload;

    window.addEventListener = function (type, listener, options) {
        if (listener) {
            captured.push([type, listener]);
        }
        return addEventListener.call(window, type, listener, options);
    } as typeof window.addEventListener;

    let instance: T;
    try {
        // Konstruktor knihovny bezi synchronne, okno je tedy podstrcene jen po
        // dobu jeho behu.
        instance = create();
    } finally {
        window.addEventListener = addEventListener;
    }

    const beforeUnloadAfter = window.onbeforeunload;

    disposers.set(instance, () => {
        captured.forEach(([type, listener]) => window.removeEventListener(type, listener));
        // Vracime jen svuj vlastni handler, ne ten, ktery po nas nekdo nasadil.
        if (window.onbeforeunload === beforeUnloadAfter) {
            window.onbeforeunload = beforeUnloadBefore;
        }
    });

    return instance;
}

function disposeListeners(instance?: object) {
    const disposer = instance && disposers.get(instance);
    if (disposer) {
        disposers.delete(instance);
        disposer();
    }
}

declare global {
    interface Window {
        /** Komponenta Layout se registruje na okno, aby na ni dosahly i dialogy mimo svuj strom. */
        thisLayout?: CrossTabUserClassInstance;
    }
}

export const getThisLayout = (): CrossTabUserClassInstance | undefined => window?.thisLayout;

class CrossTabHelper {

    static init(that: CrossTabUserClassInstance) {
        CrossTabHelper.tryInitChild(that).catch(e => {
            if (e === NO_PARENT_EX) {
                // nic
            } else {
                console.error(e);
            }
        });
    }

    static tryInitChild(that: CrossTabUserClassInstance, timeout = 4000) {
        return new Promise((resolve, reject) => {
            const onParentCommunication = (event: CrossTabEvent) => {
                console.log('CrossTabEvent', 'Received', event);
                if (!that.processCrossTabEvent) {
                    console.warn('CrossTabHelper, processCrossTabEvent not implemented');
                    return;
                }
                that.processCrossTabEvent(event);
            };

            const onDisconnect = () => {
                // Nestaci zapomenout referenci - bez ni uz nema kdo umlcet
                // posluchace, ktery si knihovna nechala na okne.
                CrossTabHelper.disconnectChild(that);
                reject(NO_PARENT_EX);
            };
            const onInit = () => {
                console.log('CrossTab', 'Connected as child', that.child);
                resolve({});
            };

            const config = {
                //onReady: onReady,
                //onInitialize: onInitialize,
                isSiteInsideFrame: false, // dont set if not required
                handshakeExpiryLimit: timeout, // msec
                onParentCommunication,
                onParentDisconnect: onDisconnect,
                onInitialize: onInit,
                onHandShakeExpiry: onDisconnect,
            };

            try {
                // Predchozi spojeni je treba zrusit, jinak po sobe kazdy pokus
                // necha zivy timeout a posluchac, ktery uz nema komu predat
                // prijatou zpravu.
                CrossTabHelper.disconnectChild(that);
                that.child = createWithOwnedListeners(() => new AcrossTabs.Child(config));
            } catch (e) {
                reject(e);
            }
        });
    }

    /**
     * Zahodi spojeni navazane v tryInitChild.
     *
     * `that.child` je undefined, dokud se navazani nepovede, a znovu potom, co
     * spojeni zanikne (odpojeny rodic nebo vyprseny handshake) - drivejsi test
     * na `null` tedy neplatil a volani padalo na TypeError, ktery se jen
     * zalogoval jako varovani.
     */
    static disconnectChild(that: CrossTabUserClassInstance) {
        const child = that.child;
        that.child = undefined;

        if (!child) {
            return;
        }

        // Cekajici handshake by jinak po odpojeni zavolal onHandShakeExpiry.
        window.clearTimeout(child.timeout);
        disposeListeners(child);

        // Pojistka pro pripad, ze by si knihovna posluchace zaregistrovala
        // jinudy, nez pres window.addEventListener: bez callbacku uz prijata
        // zprava nema kam dojit.
        if (child.config) {
            child.config.onParentCommunication = undefined;
            child.config.onParentDisconnect = undefined;
            child.config.onInitialize = undefined;
            child.config.onHandShakeExpiry = undefined;
        }
    }

    /**
     * Zahodi rodicovskou cast. Otevrene zalozky zustavaji - jejich seznam si
     * across-tabs drzi mimo instanci, takze je dalsi Parent zase uvidi.
     */
    static disconnectParent(that: CrossTabUserClassInstance) {
        const parent = that.parent;
        that.parent = undefined;

        if (!parent) {
            return;
        }

        disposeListeners(parent);
    }

    static onUnmount(that: CrossTabUserClassInstance) {
        CrossTabHelper.disconnectChild(that);
        CrossTabHelper.disconnectParent(that);
    }

    static generateRandomString = function() {
        return Math.random().toString(20).slice(2);
    };

    static getWindowFromEvent = (eventType: string): string => {
        switch (eventType) {
            case TAB_EVENT_SHOW_IN_MAP:
                return WINDOW_MAP_NAME;
            case TAB_EVENT_DISPLAY_COMPONENT:
                return WINDOW_COMPONENT;
            default:
                return '';
        }
    }

    static getUrlFromEvent = (eventType: string): string => {
        switch (eventType) {
            case CrossTabEventType.SHOW_IN_MAP:
                return getMapUrlWithContext();
            case CrossTabEventType.DISPLAY_COMPONENT:
                return getComponentUrlWithContext();
            default:
                return '';
        }
    }

    static sendEvent(that: CrossTabUserClassInstance, event: CrossTabEvent) {
        CrossTabHelper.tryInitChild(that, 300).then(() => {
            // Spojeni mohlo mezitim zaniknout (odhlaseni komponenty, novy pokus).
            if (!that.child) {
                throw NO_PARENT_EX;
            }
            console.log('CrossTabEvent', 'sending to parent window', event);
            that.child.sendMessageToParent(event);
        }).catch(() => CrossTabHelper.sendEventByParent(that, event));
    }

    static sendEventByParent(that: CrossTabUserClassInstance, event: CrossTabEvent, everyTimeNewTab: boolean = false) {
        const onNewChildInit: HandshakeCallback = tab => {
            console.log('CrossTabEvent', 'sending to new child', event);
            that.parent.broadCastTo(tab.id, event);
            that.parent.onHandshakeCallback = newTab => {
                if (that.onHandshakeCallback) {
                    that.onHandshakeCallback(newTab);
                }
            };
        };
        CrossTabHelper.initParent(that, onNewChildInit, everyTimeNewTab);
        let windowName = CrossTabHelper.getWindowFromEvent(event.type);
        const tabs: AcrossTabs.Tab[] = that.parent.getOpenedTabs();
        if (everyTimeNewTab) {
            windowName += '_' + CrossTabHelper.generateRandomString();
            sessionStorage.removeItem('__vwo_new_tab_info__');
        }
        if (!everyTimeNewTab && tabs.filter(i => i.windowName === windowName).length > 0) {
            console.log('CrossTabEvent', 'sending to active child', event);
            that.parent.broadCastAll(event);
        } else {
            console.log('CrossTabEvent', 'creating new child');
            const url = CrossTabHelper.getUrlFromEvent(event.type);
            that.parent.openNewTab({url, windowName});
        }
    }

    static initParent(that: CrossTabUserClassInstance, onHandshake?: HandshakeCallback, everyTimeNewTab: boolean = false) {
        const onChildCommunication = (event: CrossTabEvent) => {
            console.log('CrossTabEvent', 'As Parent - received event', event);

            if (!that.processCrossTabEvent) {
                console.warn('CrossTabHelper, processCrossTabEvent not implemented');
                return;
            }
            if (that.processCrossTabEvent(event)) {
                return;
            }
            CrossTabHelper.sendEventByParent(that, event, everyTimeNewTab);
        };

        // Knihovna vola callback vzdy s jednou zalozkou, se kterou se spojila.
        const onHandshakeCallback: HandshakeCallback = tab => {
            if (onHandshake) {
                onHandshake(tab);
            }
            if (that.onHandshakeCallback) {
                that.onHandshakeCallback(tab);
            }
        };

        if (!that.parent) {
            that.parent = createWithOwnedListeners(() => new AcrossTabs.Parent({
                removeClosedTabs: true,
                onPollingCallback: undefined,
                onHandshakeCallback,
                onChildCommunication,
            }));
        } else {
            that.parent.onHandshakeCallback = onHandshakeCallback;
        }
    }
}

export default CrossTabHelper;
