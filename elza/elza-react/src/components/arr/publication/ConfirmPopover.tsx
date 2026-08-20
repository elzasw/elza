import { Button, Popover, PopoverSurface, PopoverTrigger, PositioningShorthand, makeStyles, tokens } from "@fluentui/react-components";
import { MouseEvent, useState } from "react";
import { defineMessages, useIntl } from "react-intl";

const useConfirmPopoverStyles = makeStyles({
    popover: {
        display: "flex",
        flexDirection: "column",
        gap: tokens.spacingVerticalS,
    },
    actions: {
        display: "flex",
        gap: tokens.spacingHorizontalS,
    },
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

interface Props {
    text: string;
    confirmLabel?: string;
    positioning?: PositioningShorthand;
    onConfirm: () => void;
    children: React.ReactElement;
}

export type { Props as ConfirmPopoverProps };

export function ConfirmPopover({ text, confirmLabel, positioning = "after", onConfirm, children }: Props) {
    const classes = useConfirmPopoverStyles();
    const { formatMessage } = useIntl();
    const [open, setOpen] = useState(false);

    return (
        <Popover withArrow positioning={positioning} open={open} onOpenChange={(_, data) => setOpen(data.open)}>
            <PopoverTrigger disableButtonEnhancement>
                {children}
            </PopoverTrigger>
            <PopoverSurface className={classes.popover}>
                <div>{text}</div>
                <div className={classes.actions}>
                    <Button appearance="primary" size="small" className={classes.confirmBtn} onClick={(e: MouseEvent<HTMLButtonElement>) => { e.stopPropagation(); setOpen(false); onConfirm(); }}>
                        {confirmLabel ?? formatMessage(messages.confirmYes)}
                    </Button>
                    <Button size="small" onClick={(e: MouseEvent<HTMLButtonElement>) => { e.stopPropagation(); setOpen(false); }}>
                        {formatMessage(messages.confirmNo)}
                    </Button>
                </div>
            </PopoverSurface>
        </Popover>
    );
}
