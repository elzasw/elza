import { describe, it, expect, vi } from 'vitest';
import { Client } from '@stomp/stompjs';
import { FakeStompClient, getLatestStompClient } from './stomp';

/**
 * Verifies the FakeStompClient contract. Acts as living documentation for
 * test authors who need to drive WebSocket behavior from a test — copy the
 * pattern below into tests for components/reducers that react to STOMP frames.
 */
describe('FakeStompClient', () => {
    it('replaces the real @stomp/stompjs Client', () => {
        expect(Client).toBe(FakeStompClient);
    });

    it('invokes onConnect asynchronously after activate()', async () => {
        const onConnect = vi.fn();
        const client = new Client({ brokerURL: 'ws://test', onConnect });

        client.activate();

        expect(onConnect).not.toHaveBeenCalled();
        await Promise.resolve();
        expect(onConnect).toHaveBeenCalledOnce();
    });

    it('records published frames for assertion', () => {
        const client = new Client({ brokerURL: 'ws://test' }) as unknown as FakeStompClient;

        client.publish({ destination: '/app/ping', headers: { receipt: 'r1' }, body: 'hello' });

        expect(client.publishedMessages).toEqual([
            { destination: '/app/ping', headers: { receipt: 'r1' }, body: 'hello' },
        ]);
    });

    it('delivers server frames to subscribers', () => {
        const client = new Client({ brokerURL: 'ws://test' }) as unknown as FakeStompClient;
        const received: unknown[] = [];
        client.subscribe('/topic/api/changes', (frame) => {
            received.push(JSON.parse(frame.body ?? ''));
        });

        client.deliverFrame('/topic/api/changes', { eventType: 'FUND_UPDATE', ids: [42] });

        expect(received).toEqual([{ eventType: 'FUND_UPDATE', ids: [42] }]);
    });

    it('getLatestStompClient returns the most recently created instance', () => {
        new Client({ brokerURL: 'ws://first' });
        new Client({ brokerURL: 'ws://second' });

        expect(getLatestStompClient().config.brokerURL).toBe('ws://second');
    });
});
