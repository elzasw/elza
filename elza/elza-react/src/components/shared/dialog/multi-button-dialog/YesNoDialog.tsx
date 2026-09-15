import { FormattedMessage, defineMessages } from "react-intl";
import { globalMessages } from "components/shared/lang/messages";

// Id je převzaté z legacy katalogu beze změny.
const messages = defineMessages({
    defaultTitle: { id: "confirmDialog.default.title", defaultMessage: "Potvrzení" },
    yes: { id: "global.title.yes", defaultMessage: "Ano" },
    no: { id: "global.title.no", defaultMessage: "Ne" },
});
import React from "react";
import { Action, ActionCreator } from "redux";
import { ThunkAction } from "redux-thunk";
import { modalDialogHide, modalDialogShow } from "../../../../actions/global/modalDialog";
import { AppState } from "../../../../typings/store";
import { MultiButtonDialog } from "./MultiButtonDialog";

export enum YesNoDialogResult {
    YES = "YES",
    NO = "NO",
    CANCEL = "CANCEL"
}

export const showYesNoDialog: ActionCreator<
    ThunkAction<Promise<YesNoDialogResult>, AppState, void, Action>
> = (
    message?: React.ReactNode,
    title: React.ReactNode = <FormattedMessage {...messages.defaultTitle} />,
    confirmLabel?: React.ReactNode,
    cancelLabel?: React.ReactNode
) => {
    return (dispatch) => {
        return new Promise<YesNoDialogResult>((resolve) => {
            const handleSubmit = (result: YesNoDialogResult) => {
                resolve(result);
                dispatch(modalDialogHide());
            }

            dispatch(modalDialogShow(
                null,
                title,
                <MultiButtonDialog
                    onSubmit={handleSubmit}
                    message={message}
                    buttons={[{
                            variant: "outline-secondary",
                            label: confirmLabel || <FormattedMessage {...messages.yes} />,
                            value: YesNoDialogResult.YES,
                        },{
                            variant: "outline-secondary",
                            label: cancelLabel || <FormattedMessage {...messages.no} />,
                            value: YesNoDialogResult.NO,
                        },{
                            variant: "link",
                            label: cancelLabel || <FormattedMessage {...globalMessages.cancel} />,
                            value: YesNoDialogResult.CANCEL,
                        }]}
                    />
            ))
        })
    }
}
