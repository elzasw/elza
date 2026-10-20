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
} from "@fluentui/react-components";
import { CopyRegular } from "@fluentui/react-icons";
import { PublicationDetail } from "elza-api";
import { defineMessages, useIntl } from "react-intl";
import { globalMessages } from "components/shared/lang/messages";
import { addToastrDanger, addToastrInfo } from "components/shared/toastr/ToastrActions";
import { copyTextToClipboard } from "utils/clipboard";
import { useThunkDispatch } from "utils/hooks";

const useStyles = makeStyles({
    surface: {
        width: "800px",
        maxWidth: "95vw",
    },
    meta: {
        color: tokens.colorNeutralForeground3,
        marginBottom: tokens.spacingVerticalS,
    },
    text: {
        margin: "0",
        padding: tokens.spacingHorizontalS,
        maxHeight: "60vh",
        overflow: "auto",
        whiteSpace: "pre-wrap",
        overflowWrap: "anywhere",
        fontFamily: tokens.fontFamilyMonospace,
        fontSize: tokens.fontSizeBase200,
        backgroundColor: tokens.colorNeutralBackground3,
        border: `1px solid ${tokens.colorNeutralStroke2}`,
        borderRadius: tokens.borderRadiusMedium,
        userSelect: "text",
    },
});

const messages = defineMessages({
    title: { id: "publication.error.title", defaultMessage: "Chybová zpráva" },
});

/**
 * Errors from preparing the file are stored as serialized JSON exceptions;
 * indent them so the individual fields are readable. Anything else is shown as-is.
 */
const formatErrorMessage =(text: string): string => {
    const trimmed = text.trim();
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
        try {
            return JSON.stringify(JSON.parse(trimmed), null, 2);
        } catch {
            // not JSON after all
        }
    }
    return text;
};

interface Props {
    publication: PublicationDetail | null;
    onClose: () => void;
}

/**
 * Shows the full error message of a publication and lets the user copy it.
 * The copied text is the message exactly as stored, not the indented form.
 */
export function PublicationErrorDialog({ publication, onClose }: Props) {
    const classes = useStyles();
    const { formatMessage } = useIntl();
    const dispatch = useThunkDispatch();

    const errorMessage = publication?.errorMessage ?? "";
    const errorAt = publication?.errorAt ? new Date(publication.errorAt).toLocaleString() : null;

    const handleCopy = async () => {
        const copied = await copyTextToClipboard(errorMessage);
        dispatch(
            copied
                ? addToastrInfo(formatMessage(globalMessages.copyToClipboardFinished))
                : addToastrDanger(formatMessage(globalMessages.copyToClipboardUnavailable)),
        );
    };

    return (
        <Dialog open={publication != null} onOpenChange={(_, data) => { if (!data.open) { onClose(); } }}>
            <DialogSurface className={classes.surface}>
                <DialogBody>
                    <DialogTitle>{formatMessage(messages.title)}</DialogTitle>
                    <DialogContent>
                        <div className={classes.meta}>
                            {[publication?.typeName, errorAt].filter(Boolean).join(" · ")}
                        </div>
                        <pre className={classes.text} data-testid="publication-error-text">
                            {formatErrorMessage(errorMessage)}
                        </pre>
                    </DialogContent>
                    <DialogActions>
                        <Button appearance="primary" icon={<CopyRegular />} onClick={handleCopy}>
                            {formatMessage(globalMessages.copyToClipboard)}
                        </Button>
                        <Button onClick={onClose}>
                            {formatMessage(globalMessages.close)}
                        </Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}
