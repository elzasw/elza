import {
    Button,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTrigger,
    makeStyles,
    tokens,
} from "@fluentui/react-components";
import { useState } from "react";
import { defineMessages, useIntl } from "react-intl";

const useStyles = makeStyles({
    confirmBtn: {
        backgroundColor: tokens.colorStatusDangerBackground3,
        ":hover": {
            backgroundColor: tokens.colorStatusDangerBackground3Hover,
        },
        ":active": {
            backgroundColor: tokens.colorStatusDangerBackground3Pressed,
        },
    },
});

const messages = defineMessages({
    confirmYes: { id: "confirmPopover.yes", defaultMessage: "Potvrdit" },
    confirmNo:  { id: "confirmPopover.no",  defaultMessage: "Zrušit" },
});

interface ConfirmOptions {
    text: string;
    confirmLabel?: string;
    destructive?: boolean;
    onConfirm: () => void;
    onCancel?: () => void;
}

interface Props {
    children: (confirm: (options: ConfirmOptions) => void) => React.ReactNode;
}

export function ConfirmDialog({ children }: Props) {
    const classes = useStyles();
    const { formatMessage } = useIntl();
    const [open, setOpen] = useState(false);
    const [options, setOptions] = useState<ConfirmOptions | null>(null);

    const confirm = (newOptions: ConfirmOptions) => {
        setOptions(newOptions);
        setOpen(true);
    };

    const handleConfirm = () => {
        setOpen(false);
        options?.onConfirm();
    };

    const handleCancel = () => {
        setOpen(false);
        options?.onCancel?.();
    };

    return (
        <>
            {children(confirm)}
            <Dialog open={open} onOpenChange={(_, data) => { if (!data.open) { handleCancel(); } }}>
                <DialogSurface>
                    <DialogBody>
                        <DialogContent>{options?.text}</DialogContent>
                        <DialogActions>
                            <Button appearance="primary" className={options?.destructive ? classes.confirmBtn : undefined} onClick={handleConfirm}>
                                {options?.confirmLabel ?? formatMessage(messages.confirmYes)}
                            </Button>
                            <DialogTrigger disableButtonEnhancement>
                                <Button onClick={handleCancel}>
                                    {formatMessage(messages.confirmNo)}
                                </Button>
                            </DialogTrigger>
                        </DialogActions>
                    </DialogBody>
                </DialogSurface>
            </Dialog>
        </>
    );
}
