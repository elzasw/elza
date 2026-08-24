import { ReactNode, useContext } from 'react';
import {
    Button,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
} from '@fluentui/react-components';
import { defineMessages, useIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { FluentDialogContext } from './FluentModalDialog';

const messages = defineMessages({
    title: {
        id: 'confirmDialog.title',
        defaultMessage: 'Potvrzení',
    },
});

interface ConfirmOptions {
    message: ReactNode;
    title?: string;
    confirmLabel?: string;
    cancelLabel?: string;
}

interface Props extends ConfirmOptions {
    onResult: (confirmed: boolean) => void;
}

function ConfirmDialog({ message, title, confirmLabel, cancelLabel, onResult }: Props) {
    const intl = useIntl();

    return (
        <Dialog
            open
            // alert: dialog nezavře kliknutí mimo, jen tlačítko nebo Esc
            modalType="alert"
            onOpenChange={(_event, data) => {
                if (!data.open) {
                    onResult(false);
                }
            }}
        >
            <DialogSurface>
                <DialogBody>
                    <DialogTitle>{title ?? intl.formatMessage(messages.title)}</DialogTitle>
                    <DialogContent>{message}</DialogContent>
                    <DialogActions>
                        <Button appearance="primary" onClick={() => onResult(true)}>
                            {confirmLabel ?? intl.formatMessage(globalMessages.ok)}
                        </Button>
                        <Button onClick={() => onResult(false)}>
                            {cancelLabel ?? intl.formatMessage(globalMessages.cancel)}
                        </Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}

/**
 * Potvrzovací dotaz jako Fluent dialog. Na rozdíl od showConfirmDialog nejde
 * přes redux stack bootstrapových modálů, takže se zobrazí i nad plovoucími
 * okny — Fluent si dialog portáluje nad zbytek stránky.
 */
export function useConfirmModal() {
    const { showModal } = useContext(FluentDialogContext);

    return function confirm(options: ConfirmOptions): Promise<boolean> {
        return showModal<boolean, undefined>({
            createDialog: ({ handleResult }) => (
                <ConfirmDialog {...options} onResult={(confirmed) => handleResult(confirmed, undefined)} />
            ),
        }).then(({ result }) => result === true);
    };
}
