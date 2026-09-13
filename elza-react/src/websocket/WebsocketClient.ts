import { Client } from '@stomp/stompjs';

import { checkUserLogged } from 'actions/global/login.jsx';
import { webSocketConnect, webSocketDisconnect } from 'actions/global/webSocket.jsx';
import { createException } from 'components/ExceptionUtils.jsx';
import { store } from 'stores/index.jsx';
import { EventType } from 'typings/websocket/EventType';

/**
 * Store je zatím netypované JS: dispatch nezná thunky a stav nemá typ. Adaptér drží tu nejistotu
 * na jednom místě - až bude store otypovaný, stačí ho smazat.
 */
const appStore = store as unknown as {
    dispatch: (action: unknown) => unknown;
    getState: () => { userDetail?: { id?: number } };
};

/** Zpráva ze serveru. Co v ní je, říká eventType; posluchač si ji zúží sám. */
export type WebsocketMessage = any;

/** Posluchač dostane každou zprávu a sám si vybere, které se týkají jeho. */
export type WebsocketListener = (message: WebsocketMessage) => void;

/** Obsluhy podle typu události - to, co se nesměruje posluchačům. */
export type WebsocketEventMap = Record<string, (message: WebsocketMessage) => void>;

/** Co z rámce STOMP tenhle klient potřebuje. */
export interface WebsocketFrame {
    body?: string;
    command?: string;
    headers?: Record<string, string>;
}

/** Požadavek čekající na potvrzení serverem, viz {@link WebsocketClient.send}. */
interface PendingRequest {
    url: string;
    headers: Record<string, unknown>;
    data: string;
    onSuccess?: (body: WebsocketMessage) => void;
    onError?: (body: WebsocketMessage) => void;
}

/**
 * Spojení se serverem přes STOMP: drží ho otevřené, rozesílá došlé zprávy a posílá požadavky.
 *
 * Co se s jednotlivými zprávami děje, klient neřeší: obsluhy podle typu dostane v eventMap a
 * posluchače si přidávají komponenty samy. Napojení událostí na aplikaci je ve websocketActions.jsx,
 * sem z ní vede jen store, přihlášení uživatele a hlášení výjimek.
 */
export class WebsocketClient {

    /**
     * Posluchači zpráv.
     *
     * Set, protože se posluchač smí odhlásit přímo ve chvíli, kdy zprávu zpracovává - watchAipAction
     * to dělá, jakmile akce doběhne. Set to má definované: kdo je odebraný dřív, než na něj přijde
     * řada, zprávu už nedostane, a na doručení ostatním to nemá vliv.
     */
    listeners = new Set<WebsocketListener>();

    private readonly url: string;
    private readonly eventMap: WebsocketEventMap;

    private stompClient: Client | null = null;
    private nextReceiptId = 0;
    private pendingRequests: Record<string, PendingRequest> = {};
    private isPageVisible = true;
    private forcedDisconnect = false;

    constructor(url: string, eventMap: WebsocketEventMap) {
        this.url = url;
        this.eventMap = eventMap;

        document.addEventListener("visibilitychange", () => {
            if (document.visibilityState === "visible") {
                console.log("#ws page is visible")
                this.isPageVisible = true;

                this.reconnect();
            } else {
                console.log("#ws page is not visible")
                this.isPageVisible = false;
            }
        })
    }

    connect = (heartbeatOut = 20000, heartbeatIn = 45000) => {
        if (!Client) {
            throw Error("STOMP client missing.")
        }

        this.forcedDisconnect = false;
        this.stompClient = new Client({
            brokerURL: this.url,
            onConnect: this.onConnect,
            onUnhandledReceipt: this.onReceipt,
            onStompError: this.onStompError,
            onWebSocketError: this.onWebsocketError,
            onWebSocketClose: this.onWebsocketClose,
            heartbeatOutgoing: heartbeatOut,
            heartbeatIncoming: heartbeatIn,
            debug: (message) => { return; },
        });

        console.info('#ws Websocket connecting to ' + this.url);
        this.stompClient.activate();
        console.log("#ws activated")
    };

    disconnect = (error = false, force = false) => {
        if (this.stompClient) {
            // When ready state is not CLOSING(2) or CLOSED(3) and stompClient exists
            console.log('#ws Websocket disconnected');
            this.stompClient.deactivate();
            this.stompClient = null;
            // Notify components about disconnected websocket
            appStore.dispatch(webSocketDisconnect(error));
        }
        if (force) { this.forcedDisconnect = true }
    };

    reconnect = () => {
        appStore.dispatch(checkUserLogged((logged: boolean) => {
            if (logged) {
                // reconnect logged in user when stompClient is not active
                if (!this.stompClient?.active) {
                    console.log("#ws reconnect after disconnect", logged);
                    this.connect();
                }
            } else {
                // disconnect user when not logged in
                this.disconnect();
            }
        }))
    };

    send = (url: string, data: string, onSuccess?: PendingRequest['onSuccess'], onError?: PendingRequest['onError']) => {
        const headers: Record<string, unknown> = {};

        if (onSuccess || onError) {
            headers.receipt = this.nextReceiptId;

            let nextRequest: PendingRequest = {
                url: url,
                headers: headers,
                data: data,
                onSuccess: onSuccess,
                onError: onError,
            };

            this.pendingRequests[this.nextReceiptId] = nextRequest;
            this.nextReceiptId++;
        }
        console.log("#ws Websocket send", url, headers, data)

        this.stompClient.publish({
            destination: url,
            headers: headers as Record<string, string>,
            body: data,
        });
        return headers.receipt;
    };

    addListener = (listener: WebsocketListener) => {
        this.listeners.add(listener);
        return listener;
    }

    removeListener = (listener: WebsocketListener) => {
        if (!this.listeners.delete(listener)) {
            console.warn("#ws Odhlašovaný posluchač už v seznamu není", listener);
        }
    }

    onConnect = (frame: WebsocketFrame) => {
        console.info('#ws Websocket connected');
        appStore.dispatch(webSocketConnect());
        this.stompClient.subscribe('/topic/api/changes', this.onMessage);
        // Per-user channel: messages addressed to the logged user only (AI request
        // updates today, other user-targeted events later), routed by eventType
        // like the broadcast above. The server rejects a subscription to anyone
        // else's topic (see UserTopicSubscriptionInterceptor).
        const userId = appStore.getState().userDetail?.id;
        // Bootstrap admin has no persisted id and subscribes to the shared "admin"
        // segment — the server-side interceptor allows it only for id-less users.
        const userTopic = userId != null ? String(userId) : 'admin';
        this.stompClient.subscribe('/topic/user/' + userTopic, this.onMessage);
    };

    // Handles websocket disconnects
    onWebsocketClose = (error?: unknown) => {
        // Prevent reconnect after intentional disconnect
        if (this.forcedDisconnect) {
            // stompClient is already null
            return;
        }

        console.log("#ws websocket close", error);
        if (!this.isPageVisible) {
            console.log("#ws disconnect not visible", error);
            this.disconnect();
        } else {
            this.reconnect();
        }
    }

    // Handles websocket connection errors (e.g. unintentional disconnects)
    onWebsocketError = (error?: unknown) => {
        console.warn("#ws websocket error", error);

        this.reconnect();
    }

    // Handles error message received through STOMP
    onStompError = (error: WebsocketFrame) => {
        console.error("#ws stomp error", error);

        this.handleError(error);
    };

    handleError = (error: WebsocketFrame) => {
        const { body, command, headers } = error;

        const data = body ? JSON.parse(body) : {};
        // display error when page is visible or exception is well structured
        // e.g. "Session closed." will not be displayed
        if (this.isPageVisible || data.type) {
            // createException nemusí výjimku rozpoznat a pak nevrací nic; dispatch(undefined) by spadl
            const exception = createException(data.type ? data : { body, command, headers });
            if (exception) {
                appStore.dispatch(exception);
            }
        }
        this.reconnect();
    }

    onMessage = (frame: WebsocketFrame) => {
        var body = JSON.parse(frame.body);
        const eventType = body.eventType;
        console.info('#ws WEBSOCKET MESSAGE:', body);

        this.listeners.forEach((listener) => {
            listener(body);
        })

        if (this.eventMap[eventType]) {
            this.eventMap[eventType](body);
        } else if (!Object.values(EventType).includes(eventType)) {
            // Typy, které si odebírají posluchači, se tu nesměrují - varovat smí jen typ,
            // který klient nezná vůbec, jinak hlášení zevšední a nikdo si ho nevšimne.
            console.warn("#ws Unknown event type '" + eventType + "'", body);
        }
    };

    onReceipt = (frame: WebsocketFrame) => {
        let { body, headers } = frame;
        const receiptId = headers['receipt-id'];
        console.info('#ws WEBSOCKET RECEIPT:', frame, '| Remaining requests:', this.pendingRequests);

        let request = receiptId && this.pendingRequests[receiptId];

        if (request) {
            const bodyObj = JSON.parse(body);
            if (bodyObj && !bodyObj.errorMessage) {
                request.onSuccess(bodyObj);
            } else {
                request.onError(bodyObj);
            }
            delete this.pendingRequests[receiptId];
        } else {
            console.warn('#ws Unknown request - id:', receiptId);
        }
    };
}
