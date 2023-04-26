import { i18n } from "components/shared";
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
    title: string = i18n("confirmDialog.default.title"),
    confirmLabel?: string,
    cancelLabel?: string
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
                        label: confirmLabel || i18n("global.action.store"),
                        value: true,
                    },{
                        variant: "link",
                        label: cancelLabel || i18n("global.action.cancel"),
                        value: false,
                        }]}
                    />
            ))
        })
    }
}
