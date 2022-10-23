import React from "react";
import { Modal } from "react-bootstrap";
import { Button } from "../../../ui";
import "./MultiButtonDialog.scss";

export interface ButtonDefinition<T> {
    label: string;
    variant: "outline-secondary" | "link";
    value: T;
}

export interface MultiButtonDialogProps<T> {
    message: React.ReactNode;
    buttons: ButtonDefinition<T>[];
    onSubmit: (result: T) => void;
}

export const MultiButtonDialog = <T,>({
    message,
    buttons,
    onSubmit,
}:MultiButtonDialogProps<T>) => {
    const submitCreator = (result: T) => 
        () => onSubmit(result);

    return (
        <div className="multi-button-dialog-container">
            <div className="multi-button-dialog-content">
                {message}
            </div>
            <Modal.Footer>
                {buttons.map(({label, variant, value}) => 
                    <Button variant={variant} onClick={submitCreator(value)}>
                        {label}
                    </Button>
                )}
            </Modal.Footer>
        </div>
    );
}
