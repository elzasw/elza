import { FormattedMessage } from "react-intl";
import { globalMessages } from "components/shared/lang/messages";
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
    closeLabel?: React.ReactNode;
    onSubmit: () => void;
}> = ({
    message,
    closeLabel = <FormattedMessage {...globalMessages.close} />,
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
    title: React.ReactNode;
}) => {
    return (dispatch) => {
        return new Promise<boolean>((resolve) => {
            const handleSubmit = () => {
                resolve(true);
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
