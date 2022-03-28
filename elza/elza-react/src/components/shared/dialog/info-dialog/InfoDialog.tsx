import { i18n } from "components/shared";
import React, { FC } from "react";
import { Modal } from "react-bootstrap";
import { Action, ActionCreator } from "redux";
import { ThunkAction } from "redux-thunk";
import { modalDialogHide, modalDialogShow } from "../../../../actions/global/modalDialog";
import { AppState } from "../../../../typings/store";
import { Button } from "../../../ui";
import "./InfoDialog.scss";


const InfoDialog:FC<{
    message: React.ReactNode;
    closeLabel?: string;
    onSubmit: () => void;
}> = ({
    message,
    closeLabel = i18n("global.action.close"),
    onSubmit,
}) => {
    const handleSubmit = () => onSubmit();
    
    return (
        <div className="info-dialog-container">
            <div className="info-dialog-content">
                {message}
            </div>
            <Modal.Footer>
                <Button variant="link" onClick={handleSubmit}>
                    {closeLabel}
                </Button>
            </Modal.Footer>
        </div>
    );
}

export const showInfoDialog: ActionCreator<
    ThunkAction<Promise<boolean>, AppState, void, Action>
> = ({
    message, 
    title,
}:{
    message: React.ReactNode;
    title: string;
}) => {
    return (dispatch) => {
        return new Promise<boolean>((resolve) => {
            const handleSubmit = () => {
                resolve();
                dispatch(modalDialogHide());
            }

            dispatch(modalDialogShow(
                null,
                title,
                <InfoDialog 
                    onSubmit={handleSubmit}
                    message={message}
                    />
            ))
        })
    }
}
