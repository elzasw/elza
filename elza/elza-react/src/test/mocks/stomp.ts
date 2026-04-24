/**
 * Fake `@stomp/stompjs` `Client` used by Vitest.
 *
 * ELZA creates a singleton STOMP client at module load (see
 * websocketActions.jsx). Tests must never hit a real broker, so the setup file
 * mocks `@stomp/stompjs` and routes `new Client(...)` to {@link FakeStompClient}.
 *
 * Tests drive the fake via {@link getLatestStompClient} and
 * {@link FakeStompClient.deliverFrame} to simulate server-pushed frames.
 */

export type FrameLike = {
    command?: string;
    headers?: Record<string, string>;
    body?: string;
};

export type ClientConfig = {
    brokerURL?: string;
    onConnect?: (frame: FrameLike) => void;
    onUnhandledReceipt?: (frame: FrameLike) => void;
    onStompError?: (frame: FrameLike) => void;
    onWebSocketError?: (error: unknown) => void;
    onWebSocketClose?: (error: unknown) => void;
    heartbeatIncoming?: number;
    heartbeatOutgoing?: number;
    debug?: (msg: string) => void;
};

type Subscription = {
    id: string;
    unsubscribe: () => void;
};

type PublishedMessage = {
    destination: string;
    headers: Record<string, string>;
    body: string;
};

export class FakeStompClient {
    static instances: FakeStompClient[] = [];

    readonly config: ClientConfig;
    active = false;
    connected = false;
    readonly publishedMessages: PublishedMessage[] = [];
    private readonly subscriptions = new Map<string, (frame: FrameLike) => void>();

    constructor(config: ClientConfig) {
        this.config = config;
        FakeStompClient.instances.push(this);
    }

    activate(): void {
        this.active = true;
        // Mimic real behavior: onConnect fires asynchronously after activate().
        queueMicrotask(() => {
            this.connected = true;
            this.config.onConnect?.({ command: 'CONNECTED', headers: {}, body: '' });
        });
    }

    deactivate(): void {
        this.active = false;
        this.connected = false;
    }

    publish(message: { destination: string; headers?: Record<string, string>; body?: string }): void {
        this.publishedMessages.push({
            destination: message.destination,
            headers: message.headers ?? {},
            body: message.body ?? '',
        });
    }

    subscribe(destination: string, callback: (frame: FrameLike) => void): Subscription {
        this.subscriptions.set(destination, callback);
        return {
            id: destination,
            unsubscribe: () => {
                this.subscriptions.delete(destination);
            },
        };
    }

    /** Simulate a server frame arriving on a subscribed destination. */
    deliverFrame(destination: string, body: unknown): void {
        const handler = this.subscriptions.get(destination);
        if (!handler) {
            throw new Error(`No subscription registered for "${destination}"`);
        }
        handler({ body: JSON.stringify(body) });
    }

    /** Simulate a STOMP error frame. */
    triggerStompError(body: unknown): void {
        this.config.onStompError?.({ body: JSON.stringify(body) });
    }

    /** Simulate a WebSocket close event (intentional or not). */
    triggerWebSocketClose(error?: unknown): void {
        this.config.onWebSocketClose?.(error);
    }
}

export function getLatestStompClient(): FakeStompClient {
    const last = FakeStompClient.instances[FakeStompClient.instances.length - 1];
    if (!last) {
        throw new Error('No FakeStompClient has been constructed yet');
    }
    return last;
}

export function resetStompRegistry(): void {
    FakeStompClient.instances.length = 0;
}
