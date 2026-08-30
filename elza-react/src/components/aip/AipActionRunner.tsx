import { AxiosResponse } from 'axios';
import { DaAipActionVO } from 'elza-api';
import { IntlShape } from 'react-intl';

import { modalDialogShow } from '../../actions/global/modalDialog';
import { addToastrDanger, addToastrSuccess } from '../shared/toastr/ToastrActions';
import AipActionProgressDialog from './AipActionProgressDialog';
import { actionMessages } from './messages';

type Dispatch = (action: unknown) => unknown;

/**
 * Spustí akci nad AIPy a otevře dialog s jejím průběhem.
 *
 * Akce se provádí na pozadí, takže odpověď serveru nese jen zadání - výsledek chodí až přes
 * websocket. Bez tohoto dialogu by po stisku tlačítka nebylo vidět vůbec nic.
 */
export async function runAipAction(
    dispatch: Dispatch,
    intl: IntlShape,
    title: string,
    request: () => Promise<AxiosResponse<DaAipActionVO>>,
    onFinished: () => void,
) {
    const response = await request();
    const notify = (errors: number, total: number) => {
        dispatch(errors > 0
            ? addToastrDanger(intl.formatMessage(actionMessages.toastErrors, { count: errors }))
            : addToastrSuccess(intl.formatMessage(actionMessages.toastFinished, { count: total })));
    };
    dispatch(modalDialogShow(
        null,
        title,
        <AipActionProgressDialog
            initialAction={response.data}
            onFinished={onFinished}
            onNotify={notify}
        />,
    ));
}
