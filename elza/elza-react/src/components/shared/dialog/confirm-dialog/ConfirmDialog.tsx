import { i18n } from "components/shared";
import React, { FC } from "react";
import { Modal } from "react-bootstrap";
import { Action, ActionCreator } from "redux";
import { ThunkAction } from "redux-thunk";
import { modalDialogHide, modalDialogShow } from "../../../../actions/global/modalDialog";
import { AppState } from "../../../../typings/store";
import { Button } from "../../../ui";
import "./ConfirmDialog.scss";


const ConfirmDialog:FC<{
    message: React.ReactNode;
    confirmLabel?: string;
    cancelLabel?: string;
    onSubmit: (result: boolean) => void;
}> = ({
    message = i18n("confirmDialog.default.message"),
    confirmLabel = i18n("global.action.store"),
    cancelLabel = i18n("global.action.cancel"),
    onSubmit,
}) => {
    //return <div> {message} </div>
    const submitCreator = (result: boolean) => 
        () => onSubmit(result);
    
    return (
        <div className="confirm-dialog-container">
            <div className="confirm-dialog-content">
                {message}
            </div>
            <Modal.Footer>
                <Button variant="outline-secondary" onClick={submitCreator(true)}>
                    {confirmLabel}
                </Button>
                <Button variant="link" onClick={submitCreator(false)}>
                    {cancelLabel}
                </Button>
            </Modal.Footer>
        </div>
    );
}

export const showConfirmDialog: ActionCreator<
    ThunkAction<Promise<boolean>, AppState, void, Action>
> = (
    message?: React.ReactNode,
    title: string = i18n("confirmDialog.default.title"),
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
                <ConfirmDialog 
                    onSubmit={handleSubmit}
                    message={message}
                    />
            ))
        })
    }
}
