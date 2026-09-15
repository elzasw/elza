import { FormattedMessage, defineMessages } from "react-intl";
import { globalMessages } from "components/shared/lang/messages";

// Id je převzaté z legacy katalogu beze změny.
const messages = defineMessages({
    defaultTitle: { id: "confirmDialog.default.title", defaultMessage: "Potvrzení" },
});
import React from "react";
import { Action, ActionCreator } from "redux";
import { ThunkAction } from "redux-thunk";
import { modalDialogHide, modalDialogShow } from "../../../../actions/global/modalDialog";
import { AppState } from "../../../../typings/store";
import { MultiButtonDialog } from "./MultiButtonDialog";

export const showConfirmDialog: ActionCreator<
    ThunkAction<Promise<boolean>, AppState, void, Action>
> = (
    message?: React.ReactNode,
    title: React.ReactNode = <FormattedMessage {...messages.defaultTitle} />,
    confirmLabel?: React.ReactNode,
    cancelLabel?: React.ReactNode
) => {
    return (dispatch) => {
        return new Promise<boolean>((resolve) => {
            const handleSubmit = (result: boolean) => {
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
                        label: confirmLabel || <FormattedMessage {...globalMessages.ok} />,
                        value: true,
                    },{
                        variant: "link",
                        label: cancelLabel || <FormattedMessage {...globalMessages.cancel} />,
                        value: false,
                        }]}
                    />
            ))
        })
    }
}
