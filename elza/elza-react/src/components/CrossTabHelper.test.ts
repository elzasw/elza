import { describe, it, expect, vi, afterEach } from 'vitest';

import CrossTabHelper, { CrossTabEventType } from './CrossTabHelper';

const PARENT_COMMUNICATED = '__PARENT_COMMUNICATED__';

type TestLayout = Parameters<typeof CrossTabHelper.onUnmount>[0];

function createLayout() {
    const processCrossTabEvent = vi.fn(() => true);
    const that = {
        processCrossTabEvent,
        onHandshakeCallback: vi.fn(),
    } as unknown as TestLayout;
    return { that, processCrossTabEvent };
}

function sendParentMessage() {
    window.dispatchEvent(
        new MessageEvent('message', {
            data:
                PARENT_COMMUNICATED +
                JSON.stringify({ type: CrossTabEventType.SHOW_IN_MAP, data: { id: 1 } }),
        }),
    );
}

/**
 * Odhlášení komponenty sahalo na `that.child`, který je po vypršení handshake
 * undefined; volání spadlo na TypeError a jen se vypsalo jako varování
 * "CrossTabEvent problem in disconnect".
 */
describe('CrossTabHelper.onUnmount', () => {
    const layouts: TestLayout[] = [];

    const openLayout = () => {
        const layout = createLayout();
        layouts.push(layout.that);
        CrossTabHelper.init(layout.that);
        return layout;
    };

    afterEach(() => {
        // Aby posluchači jednoho testu nepřežili do dalšího.
        layouts.splice(0).forEach(that => CrossTabHelper.onUnmount(that));
    });

    it('bez navázaného spojení nespadne ani nevaruje', () => {
        const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
        const { that } = createLayout();

        expect(() => CrossTabHelper.onUnmount(that)).not.toThrow();
        expect(warn).not.toHaveBeenCalled();
    });

    it('po vypršení handshake nespadne ani nevaruje', async () => {
        const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
        vi.useFakeTimers();
        try {
            const { that } = openLayout();
            expect(that.child).toBeDefined();

            // 4000 ms je handshakeExpiryLimit z CrossTabHelper.init
            await vi.advanceTimersByTimeAsync(4000);
            expect(that.child).toBeUndefined();

            expect(() => CrossTabHelper.onUnmount(that)).not.toThrow();
        } finally {
            vi.useRealTimers();
        }
        expect(warn).not.toHaveBeenCalled();
    });

    it('zruší čekající handshake', () => {
        vi.useFakeTimers();
        try {
            const { that } = openLayout();
            expect(vi.getTimerCount()).toBeGreaterThan(0);

            CrossTabHelper.onUnmount(that);

            expect(vi.getTimerCount()).toBe(0);
        } finally {
            vi.useRealTimers();
        }
    });

    it('po odhlášení už zprávy rodiče nedoručí', () => {
        const { that, processCrossTabEvent } = openLayout();

        sendParentMessage();
        expect(processCrossTabEvent).toHaveBeenCalledTimes(1);

        CrossTabHelper.onUnmount(that);
        processCrossTabEvent.mockClear();

        sendParentMessage();
        expect(processCrossTabEvent).not.toHaveBeenCalled();
    });

    it('po vypršení handshake už zprávy rodiče nedoručí', async () => {
        vi.useFakeTimers();
        try {
            const { that, processCrossTabEvent } = openLayout();

            await vi.advanceTimersByTimeAsync(4000);
            expect(that.child).toBeUndefined();

            sendParentMessage();

            expect(processCrossTabEvent).not.toHaveBeenCalled();
        } finally {
            vi.useRealTimers();
        }
    });

    it('opakovaná inicializace nenechává za sebou živé posluchače', () => {
        const { that, processCrossTabEvent } = openLayout();
        CrossTabHelper.init(that);

        sendParentMessage();

        expect(processCrossTabEvent).toHaveBeenCalledTimes(1);
    });

    it('posluchače odebere, neumlčí je jen vynulováním callbacků', () => {
        const { that } = openLayout();
        const onCommunication = vi.spyOn(that.child, 'onCommunication');

        CrossTabHelper.onUnmount(that);
        sendParentMessage();

        expect(onCommunication).not.toHaveBeenCalled();
    });

    it('vrátí window.onbeforeunload, který across-tabs přepsal', () => {
        const original = vi.fn();
        window.onbeforeunload = original;
        try {
            const { that } = openLayout();
            expect(window.onbeforeunload).not.toBe(original);

            CrossTabHelper.onUnmount(that);

            expect(window.onbeforeunload).toBe(original);
        } finally {
            window.onbeforeunload = null;
        }
    });

    it('odpojí i rodičovskou část', () => {
        const { that } = createLayout();
        layouts.push(that);
        CrossTabHelper.initParent(that);
        expect(that.parent).toBeDefined();

        CrossTabHelper.onUnmount(that);

        expect(that.parent).toBeUndefined();
    });
});
