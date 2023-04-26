import { i18n } from "components/shared";
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
    title: string = i18n("confirmDialog.default.title"),
    confirmLabel?: string,
    cancelLabel?: string
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
                            label: confirmLabel || i18n("global.title.yes"),
                            value: YesNoDialogResult.YES,
                        },{
                            variant: "outline-secondary",
                            label: cancelLabel || i18n("global.title.no"),
                            value: YesNoDialogResult.NO,
                        },{
                            variant: "link",
                            label: cancelLabel || i18n("global.action.cancel"),
                            value: YesNoDialogResult.CANCEL,
                        }]}
                    />
            ))
        })
    }
}
