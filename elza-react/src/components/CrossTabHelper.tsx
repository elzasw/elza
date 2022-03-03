import { default as AcrossTabs } from 'across-tabs';

export enum CrossTabEventType {SHOW_IN_MAP = 'SHOW_IN_MAP'}

export interface CrossTabEvent {
    type: CrossTabEventType;
    data: object;
}
interface CrossTabUserClassInstance {
    child?: AcrossTabs.Child;
    parent?: AcrossTabs.Parent;
    processCrossTabEvent: (event: CrossTabEvent) => boolean;
    onHandshakeCallback: (...data) => void;
}

export const TAB_EVENT_SHOW_IN_MAP = 'TAB_EVENT_SHOW_IN_MAP';
export const WINDOW_MAP_NAME = 'WINDOW_MAP_NAME';
const NO_PARENT_EX = '_NO_PARENT';

export const getThisLayout = (): CrossTabUserClassInstance => ((window as any)?.thisLayout);

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
                that.child = undefined;
                reject(NO_PARENT_EX);
            };
            const onInit = () => {
                console.log('CrossTab', 'Connected as child', that.child);
                resolve();
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
                that.child = new AcrossTabs.Child(config);
            } catch (e) {
                reject(e);
            }
        });
    }

    static onUnmount(that: CrossTabUserClassInstance) {
        try {
            if (that.child !== null) {
                that.child.onParentDisconnect();
            }
        } catch (e) {
            console.warn('CrossTabEvent', 'problem in disconnect', e);
            // ignore
        }
    }

    static generateRandomString = function() {
        return Math.random().toString(20).substr(2);
    };

    static getWindowFromEvent = (eventType: string): string => {
        switch (eventType) {
            case TAB_EVENT_SHOW_IN_MAP:
                return WINDOW_MAP_NAME;
            default:
                return '';
        }
    }

    static getUrlFromEvent = (): string => {
        return (window as any).serverContextPath + '/map';
    }

    static sendEvent(that: CrossTabUserClassInstance, event: CrossTabEvent) {
        CrossTabHelper.tryInitChild(that, 300).then(() => {
            console.log('CrossTabEvent', 'sending to parent window', event);
            that.child.sendMessageToParent(event);
        }).catch(() => CrossTabHelper.sendEventByParent(that, event));
    }

    static sendEventByParent(that: CrossTabUserClassInstance, event: CrossTabEvent, everyTimeNewTab: boolean = false) {
        const onNewChildInit = (tab) => {
            console.log('CrossTabEvent', 'sending to new child', event);
            that.parent.broadCastTo(tab.id, event);
            that.parent.onHandshakeCallback = (...e) => {
                if (that.onHandshakeCallback) {
                    that.onHandshakeCallback(...e);
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
            const url = CrossTabHelper.getUrlFromEvent();
            that.parent.openNewTab({url, windowName});
        }
    }

    static initParent(that: CrossTabUserClassInstance, onHandshake?: Function, everyTimeNewTab: boolean = false) {
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

        const onHandshakeCallback = (...e) => {
            if (onHandshake) {
                onHandshake(...e);
            }
            if (that.onHandshakeCallback) {
                that.onHandshakeCallback(...e);
            }
        };

        if (!that.parent) {
            that.parent = new AcrossTabs.Parent({
                removeClosedTabs: true,
                onPollingCallback: undefined,
                onHandshakeCallback,
                onChildCommunication,
            });
        } else {
            that.parent.onHandshakeCallback = onHandshakeCallback;
        }
    }
}

export default CrossTabHelper;
