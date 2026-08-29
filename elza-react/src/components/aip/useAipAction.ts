import { useCallback, useEffect, useRef, useState } from 'react';
import { DaAipActionState, DaAipActionVO } from 'elza-api';

import { Api } from '../../api';
import { useAppSelector } from 'utils/hooks';
import { useWebsocket } from 'components/shared/web-socket/WebsocketProvider';
import { EventType } from 'typings/websocket/EventType';

interface AipActionUpdateEvent {
    eventType: EventType;
    action: DaAipActionVO;
}

/**
 * Sleduje jednu akci nad AIPy: server posílá celý její snímek, tady se jen nahradí to, co držíme.
 *
 * Doručení není zaručené - skrytá záložka se odpojí a zprávy se nikde neukládají - proto se stav
 * akce načte znovu při každém (znovu)připojení websocketu.
 */
export function useAipAction(initial?: DaAipActionVO) {
    const [action, setAction] = useState<DaAipActionVO | undefined>(initial);
    const websocket = useWebsocket();
    const websocketConnected = useAppSelector((state) => state.webSocket.connected);

    // Aktuální ID akce pro posluchače, který se registruje jen jednou.
    const actionIdRef = useRef<number | undefined>(initial?.id);
    actionIdRef.current = action?.id;

    useEffect(() => {
        setAction(initial);
    }, [initial]);

    const refetch = useCallback(async () => {
        const id = actionIdRef.current;
        if (id == null) {
            return;
        }
        try {
            const response = await Api.aips.aipGetAipAction(id);
            setAction(response.data);
        } catch {
            // Akce mohla mezitím zmizet; drží se poslední známý stav.
        }
    }, []);

    useEffect(() => {
        const listener = websocket.addListener((message: AipActionUpdateEvent) => {
            if (message.eventType !== EventType.AIP_ACTION_UPDATE || !message.action) {
                return;
            }
            if (message.action.id !== actionIdRef.current) {
                return;
            }
            setAction(message.action);
        });
        return () => websocket.removeListener(listener);
    }, [websocket]);

    useEffect(() => {
        if (websocketConnected) {
            refetch();
        }
    }, [websocketConnected, refetch]);

    const finished = action != null
        && (action.state === DaAipActionState.Finished || action.state === DaAipActionState.Error);

    return { action, finished, refetch };
}
