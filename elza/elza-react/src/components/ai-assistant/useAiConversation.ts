import { useCallback, useEffect, useRef, useState } from 'react';
import { Api } from 'api';
import { AiConversationDetail, AiRequest } from 'elza-api';
import { useWebsocket } from 'components/shared/web-socket/WebsocketProvider';
import { EventType } from 'typings/websocket/EventType';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import { AiContext } from './useCurrentAiContext';

export function isRequestInProgress(request: AiRequest) {
    // Unknown states are treated as in progress per the rendering contract.
    const terminalStates = new Set(['done', 'error', 'cancelled']);
    return !terminalStates.has(request.state);
}

/**
 * Pushed to the owner's user queue whenever a request changes; a complete
 * snapshot, so it simply replaces the request in the conversation state.
 */
interface AiRequestUpdateEvent {
    eventType: EventType.AI_REQUEST_UPDATE;
    conversationId: number;
    request: AiRequest;
}

function extractErrorMessage(error: unknown): string {
    const response = (error as { response?: { status?: number; statusText?: string } })?.response;
    if (response?.status) {
        return `${response.status}${response.statusText ? ` ${response.statusText}` : ''}`;
    }
    const message = (error as { message?: string })?.message;
    return typeof message === 'string' && message ? message : 'Neznámá chyba';
}

interface UseAiConversationOptions {
    externalSystemCode: string;
    getContext?: () => AiContext | null;
}

export function useAiConversation({ externalSystemCode, getContext }: UseAiConversationOptions) {
    const websocket = useWebsocket();
    const websocketConnected = useAppSelector((state) => state.webSocket.connected);
    const [detail, setDetail] = useState<AiConversationDetail | null>(null);
    const [pending, setPending] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [activeConversationId, setActiveConversationId] = useState<number | null>(null);

    const conversationIdRef = useRef<number | null>(null);

    useEffect(() => {
        conversationIdRef.current = detail?.conversation.id ?? null;
        setActiveConversationId(detail?.conversation.id ?? null);
    }, [detail]);

    // Pending follows the loaded/pushed requests; send() sets it optimistically
    // before the server responds.
    useEffect(() => {
        if (detail) {
            setPending(detail.requests.some(isRequestInProgress));
        }
    }, [detail]);

    const openConversation = useCallback(async (id: number) => {
        setError(null);
        conversationIdRef.current = id;
        try {
            const { data: fresh } = await Api.aiprovider.aiProviderGetConversation(id);
            setDetail(fresh);
        } catch (fetchError) {
            setError(extractErrorMessage(fetchError));
        }
    }, []);

    const newConversation = useCallback(() => {
        conversationIdRef.current = null;
        setDetail(null);
        setPending(false);
        setError(null);
    }, []);

    const refetch = useCallback(async () => {
        const conversationId = conversationIdRef.current;
        if (conversationId === null) return;
        try {
            const { data: fresh } = await Api.aiprovider.aiProviderGetConversation(conversationId);
            setDetail(fresh);
        } catch (fetchError) {
            setError(extractErrorMessage(fetchError));
        }
    }, []);

    // Live updates: the server pushes the complete request snapshot to the
    // user queue; replace it by id (or append a request another tab started).
    useEffect(() => {
        const listener = websocket.addListener((message: AiRequestUpdateEvent) => {
            const conversationId = conversationIdRef.current;
            if (message.eventType !== EventType.AI_REQUEST_UPDATE
                || conversationId === null
                || message.conversationId !== conversationId
                || !message.request) {
                return;
            }
            setDetail((previous) => {
                if (previous === null || previous.conversation.id !== message.conversationId) {
                    return previous;
                }
                const exists = previous.requests.some((request) => request.id === message.request.id);
                const requests = exists
                    ? previous.requests.map((request) =>
                          request.id === message.request.id ? message.request : request)
                    : [...previous.requests, message.request];
                return { ...previous, requests };
            });
        });
        return () => websocket.removeListener(listener);
    }, [websocket]);

    // Push delivery is best effort (hidden tabs disconnect) - resync the open
    // conversation whenever the websocket (re)connects.
    useEffect(() => {
        if (websocketConnected) {
            refetch();
        }
    }, [websocketConnected, refetch]);

    const send = useCallback(
        async (userInstructions: string, taskType?: string, profile?: string) => {
            setError(null);
            setPending(true);
            const currentContext = getContext?.() ?? null;

            try {
                if (conversationIdRef.current === null) {
                    if (!taskType) {
                        setPending(false);
                        return;
                    }
                    const { data: fresh } = await Api.aiprovider.aiProviderCreateConversation({
                        externalSystemCode,
                        taskType,
                        profile,
                        userInstructions,
                        context: currentContext?.objects,
                    });
                    conversationIdRef.current = fresh.conversation.id;
                    setDetail(fresh);
                } else {
                    const { data: fresh } = await Api.aiprovider.aiProviderCreateRequest(
                        conversationIdRef.current, { userInstructions, profile });
                    setDetail(fresh);
                }
            } catch (sendError) {
                setError(extractErrorMessage(sendError));
                setPending(false);
            }
        },
        [externalSystemCode, getContext]
    );

    const requests = detail?.requests ?? [];

    return { requests, pending, error, send, activeConversationId, openConversation, newConversation };
}
