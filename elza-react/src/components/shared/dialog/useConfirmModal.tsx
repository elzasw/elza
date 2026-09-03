import { ReactNode, useContext } from 'react';
import {
    Button,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
    makeStyles,
    tokens,
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

const useStyles = makeStyles({
    destructiveConfirm: {
        backgroundColor: tokens.colorStatusDangerBackground3,
        ':hover': {
            backgroundColor: tokens.colorStatusDangerBackground3Hover,
        },
        ':active': {
            backgroundColor: tokens.colorStatusDangerBackground3Pressed,
        },
    },
});

interface ConfirmOptions {
    message: ReactNode;
    title?: string;
    confirmLabel?: string;
    cancelLabel?: string;
    /** Marks the confirming action as one that destroys data. */
    destructive?: boolean;
}

interface Props extends ConfirmOptions {
    onResult: (confirmed: boolean) => void;
}

function ConfirmDialog({ message, title, confirmLabel, cancelLabel, destructive, onResult }: Props) {
    const intl = useIntl();
    const styles = useStyles();

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
                        <Button
                            appearance="primary"
                            className={destructive ? styles.destructiveConfirm : undefined}
                            onClick={() => onResult(true)}
                        >
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
