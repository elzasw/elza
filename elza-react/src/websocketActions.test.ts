import { describe, it, expect } from 'vitest';

import { websocket } from './websocketActions';

/**
 * Rozesílání zpráv posluchačům.
 *
 * Posluchač se běžně odhlašuje přímo ve chvíli, kdy zprávu dostane - watchAipAction to dělá, jakmile
 * akce doběhne. Doručení ostatním na tom nesmí záviset.
 */
describe('websocket.onMessage', () => {

    const frame = () => ({
        body: JSON.stringify({ eventType: 'AIP_ACTION_UPDATE', action: { id: 1 } }),
    });

    it('doručí zprávu i posluchači za tím, který se právě odhlásil', () => {
        const ws = new websocket('ws://test/stomp', {});
        const received: string[] = [];

        const first = () => {
            ws.removeListener(first);
            received.push('first');
        };
        ws.addListener(first);
        ws.addListener(() => received.push('second'));

        ws.onMessage(frame());

        expect(received).toEqual(['first', 'second']);
    });

    it('odhlášenému posluchači už další zprávu nedoručí', () => {
        const ws = new websocket('ws://test/stomp', {});
        const received: string[] = [];

        const listener = () => received.push('listener');
        ws.addListener(listener);
        ws.onMessage(frame());
        ws.removeListener(listener);
        ws.onMessage(frame());

        expect(received).toEqual(['listener']);
    });

    /** Odhlášení posluchače, který v seznamu není, nesmí odebrat nikoho jiného. */
    it('odhlášení neznámého posluchače nechá ostatní na pokoji', () => {
        const ws = new websocket('ws://test/stomp', {});
        const received: string[] = [];

        ws.addListener(() => received.push('live'));
        ws.removeListener(() => undefined);

        ws.onMessage(frame());

        expect(received).toEqual(['live']);
    });
});
