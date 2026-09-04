import { AxiosResponse } from 'axios';
import { DaAipActionItemState, DaAipActionState, DaAipActionVO } from 'elza-api';
import { IntlShape } from 'react-intl';

import { modalDialogShow } from '../../actions/global/modalDialog';
import { addToastrDanger, addToastrSuccess, addToastrWarning } from '../shared/toastr/ToastrActions';
import { EventType } from 'typings/websocket/EventType';
import AipActionProgressDialog from './AipActionProgressDialog';
import { actionMessages } from './messages';

type Dispatch = (action: unknown) => unknown;

/** The websocket the progress of the action arrives over; see useWebsocket. */
export interface ActionWebsocket {
    addListener: (listener: (message: any) => void) => unknown;
    removeListener: (listener: unknown) => void;
}

interface AipActionUpdateEvent {
    eventType: EventType;
    action?: DaAipActionVO;
}

export const isActionFinished = (action: DaAipActionVO) =>
    action.state === DaAipActionState.Finished || action.state === DaAipActionState.Error;

/**
 * Follows the action over the websocket until it finishes, independently of any dialog: the dialog
 * can be closed while the action still runs, and the user has to hear about the outcome even then.
 *
 * @returns a function that stops following
 */
export function watchAipAction(
    websocket: ActionWebsocket,
    action: DaAipActionVO,
    onFinished: (action: DaAipActionVO) => void,
): () => void {
    if (isActionFinished(action)) {
        onFinished(action);
        return () => undefined;
    }
    const handle = websocket.addListener((message: AipActionUpdateEvent) => {
        if (message.eventType !== EventType.AIP_ACTION_UPDATE || message.action?.id !== action.id) {
            return;
        }
        if (isActionFinished(message.action)) {
            websocket.removeListener(handle);
            onFinished(message.action);
        }
    });
    return () => websocket.removeListener(handle);
}

/**
 * Tells the user how the finished action went. An action that skipped every AIP did nothing - a
 * success would claim otherwise - so it is reported as a warning with the number skipped.
 */
export function notifyActionOutcome(dispatch: Dispatch, intl: IntlShape, action: DaAipActionVO) {
    const items = action.items ?? [];
    const count = (state: DaAipActionItemState) => items.filter(item => item.state === state).length;
    const errors = count(DaAipActionItemState.Error);
    const finished = count(DaAipActionItemState.Finished);
    const skipped = count(DaAipActionItemState.Skipped);

    if (errors > 0) {
        dispatch(addToastrDanger(intl.formatMessage(actionMessages.toastErrors, { count: errors })));
    } else if (finished > 0) {
        dispatch(addToastrSuccess(intl.formatMessage(actionMessages.toastFinished, { count: finished, skipped })));
    } else {
        dispatch(addToastrWarning(intl.formatMessage(actionMessages.toastSkipped, { count: skipped })));
    }
}

/**
 * Spustí akci nad AIPy a otevře dialog s jejím průběhem.
 *
 * Akce se provádí na pozadí, takže odpověď serveru nese jen zadání - výsledek chodí až přes
 * websocket. Dokud je dialog otevřený, ukazuje výsledek sám; toastr dostane uživatel, který ho
 * zavřel dřív, než akce skončila.
 */
export async function runAipAction(
    dispatch: Dispatch,
    intl: IntlShape,
    websocket: ActionWebsocket,
    title: string,
    request: () => Promise<AxiosResponse<DaAipActionVO>>,
    onFinished: () => void,
) {
    const response = await request();
    let dialogOpen = true;
    watchAipAction(websocket, response.data, (action) => {
        onFinished();
        if (!dialogOpen) {
            notifyActionOutcome(dispatch, intl, action);
        }
    });
    dispatch(modalDialogShow(
        null,
        title,
        <AipActionProgressDialog initialAction={response.data} />,
        '',
        // modalDialog.js declares the callback with a null default, which TypeScript reads as its type
        (() => { dialogOpen = false; }) as never,
    ));
}
