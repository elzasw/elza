import { DataType, NodeItem } from 'elza-api';
import { useSyncExternalStore } from 'react';
import type { NodeTemplateItem } from '../templates/templates';

export type ClipboardItem = NodeTemplateItem;

export interface ItemClipboardPayload {
    version: typeof CLIPBOARD_VERSION;
    fundId: number;
    fundVersionId: number;
    sourceNodeId: number;
    copiedAt: number;
    items: ClipboardItem[];
}

export type ItemClipboardSource = Omit<ItemClipboardPayload, 'version' | 'copiedAt' | 'items'>;

export const CLIPBOARD_STORAGE_KEY = 'ELZA-ITEM-CLIPBOARD';
const CLIPBOARD_VERSION = 1;

const dataTypes: string[] = Object.values(DataType);

const listeners = new Set<() => void>();
let snapshotRaw: string | null = null;
let snapshot: ItemClipboardPayload | undefined;

function isOptionalNumber(value: unknown) {
    return value == undefined || typeof value === 'number';
}

function isClipboardItem(value: unknown): value is ClipboardItem {
    if (typeof value !== 'object' || value == null) {
        return false;
    }
    const item = value as Record<string, unknown>;
    const isUndefinedItem = item.undefined === true;
    const data = item.data as Record<string, unknown> | undefined;
    const hasValidData = typeof data === 'object' && data != null && dataTypes.includes(data.dataType as string);

    return (
        typeof item.itemTypeId === 'number' &&
        isOptionalNumber(item.itemSpecId) &&
        isOptionalNumber(item.position) &&
        (item.undefined == undefined || typeof item.undefined === 'boolean') &&
        (isUndefinedItem || hasValidData)
    );
}

/** Parses a stored payload; anything malformed, of another version or without items counts as empty. */
export function parseClipboardPayload(raw: string | null): ItemClipboardPayload | undefined {
    if (!raw) {
        return undefined;
    }
    let parsed: unknown;
    try {
        parsed = JSON.parse(raw);
    } catch {
        return undefined;
    }
    if (typeof parsed !== 'object' || parsed == null) {
        return undefined;
    }
    const payload = parsed as Record<string, unknown>;
    const hasValidHeader =
        payload.version === CLIPBOARD_VERSION &&
        typeof payload.fundId === 'number' &&
        typeof payload.fundVersionId === 'number' &&
        typeof payload.sourceNodeId === 'number' &&
        typeof payload.copiedAt === 'number';
    const hasValidItems =
        Array.isArray(payload.items) && payload.items.length > 0 && payload.items.every(isClipboardItem);

    return hasValidHeader && hasValidItems ? (payload as unknown as ItemClipboardPayload) : undefined;
}

function readRaw() {
    try {
        return localStorage.getItem(CLIPBOARD_STORAGE_KEY);
    } catch {
        return null;
    }
}

function refreshSnapshot() {
    const raw = readRaw();
    if (raw !== snapshotRaw) {
        snapshotRaw = raw;
        snapshot = parseClipboardPayload(raw);
    }
}

function notify() {
    listeners.forEach((listener) => listener());
}

function handleStorage(event: StorageEvent) {
    const affectsClipboard = event.key === CLIPBOARD_STORAGE_KEY || event.key === null;
    if (affectsClipboard) {
        refreshSnapshot();
        notify();
    }
}

function subscribe(listener: () => void) {
    if (listeners.size === 0) {
        window.addEventListener('storage', handleStorage);
    }
    listeners.add(listener);
    return () => {
        listeners.delete(listener);
        if (listeners.size === 0) {
            window.removeEventListener('storage', handleStorage);
        }
    };
}

export function readItemClipboard() {
    refreshSnapshot();
    return snapshot;
}

/** Strips server-side identity from the node's own DescItems, optionally of one DescItemType only. */
export function createClipboardItems(descItems: NodeItem[], nodeId: number, itemTypeId?: number): ClipboardItem[] {
    return descItems
        .filter((descItem) => descItem.nodeId === nodeId)
        .filter((descItem) => itemTypeId == undefined || descItem.itemTypeId === itemTypeId)
        .map(({ itemTypeId: typeId, itemSpecId, position, undefined: isUndefined, data }) => ({
            itemTypeId: typeId,
            itemSpecId,
            position,
            undefined: isUndefined,
            data,
        }));
}

/** Replaces the stored content. Returns false when there is nothing to store or storage is unavailable. */
export function writeItemClipboard(source: ItemClipboardSource, items: ClipboardItem[]) {
    if (items.length === 0) {
        return false;
    }
    const payload: ItemClipboardPayload = {
        version: CLIPBOARD_VERSION,
        ...source,
        copiedAt: Date.now(),
        items,
    };
    try {
        localStorage.setItem(CLIPBOARD_STORAGE_KEY, JSON.stringify(payload));
    } catch {
        return false;
    }
    refreshSnapshot();
    notify();
    return true;
}

/**
 * Adds items to the stored content: stored items of the same DescItemTypes are replaced, others are kept.
 * Content of another fund is not combined; it is replaced as by a plain write.
 */
export function appendItemClipboard(source: ItemClipboardSource, items: ClipboardItem[]) {
    const current = readItemClipboard();
    const isSameFund = current != undefined && current.fundId === source.fundId;
    if (!isSameFund) {
        return writeItemClipboard(source, items);
    }
    const replacedTypeIds = new Set(items.map(({ itemTypeId }) => itemTypeId));
    const keptItems = current.items.filter(({ itemTypeId }) => !replacedTypeIds.has(itemTypeId));
    return writeItemClipboard(source, [...keptItems, ...items]);
}

/** Removes one stored item by its index; removing the last one empties the clipboard. */
export function removeItemClipboardItem(index: number) {
    const current = readItemClipboard();
    if (!current) {
        return;
    }
    const items = current.items.filter((_item, itemIndex) => itemIndex !== index);
    if (items.length === 0) {
        clearItemClipboard();
        return;
    }
    const { fundId, fundVersionId, sourceNodeId } = current;
    writeItemClipboard({ fundId, fundVersionId, sourceNodeId }, items);
}

export function clearItemClipboard() {
    try {
        localStorage.removeItem(CLIPBOARD_STORAGE_KEY);
    } catch {
        return;
    }
    refreshSnapshot();
    notify();
}

/** The current clipboard content, kept in sync across tabs. */
export function useItemClipboard() {
    return useSyncExternalStore(subscribe, readItemClipboard);
}
