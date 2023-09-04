/**
 * Render Modálního dialogu ze store
 */
import { modalDialogHide } from 'actions/global/modalDialog.jsx';
import React from 'react';
import { useSelector } from 'react-redux';
import './ModalDialog.scss';
import { ModalDialogWrapper } from './ModalDialogWrapper';
import { useThunkDispatch } from 'utils/hooks';
import { AppState, DialogCloseType } from 'typings/store';

const ModalDialog = () => {
    const { items, lastKey } = useSelector((state: AppState) => state.modalDialog);
    const dispatch = useThunkDispatch();

    if (items.length < 1) {
        return <></>;
    }

    const handleClose = (closeType: DialogCloseType, key?: number) => {
        dispatch(modalDialogHide(key));

        items.find((dialog) => dialog.key === key)?.onClose?.(closeType); // Call 'onClose' on specified dialog, if defined
    };

    return <div>{items.map((dialog) => {
        const visible = dialog.key === lastKey;
        if (typeof dialog.content === "function") {
            return dialog.content({
                key: dialog.key,
                visible,
                onClose: () => handleClose(DialogCloseType.DIALOG_CONTENT, dialog.key),
            })
        } else {
            return (
                <ModalDialogWrapper
                    key={dialog.key}
                    className={dialog.dialogClassName}
                    title={dialog.title}
                    onHide={() => handleClose(DialogCloseType.DIALOG, dialog.key)}
                    visible={visible}
                >
                    {React.Children.map(dialog.content, (element) =>
                        React.cloneElement(element, {
                            onClose: () => handleClose(DialogCloseType.DIALOG_CONTENT, dialog.key),
                        }),
                    )}
                </ModalDialogWrapper>
            );
        }
    })}</div>;
}

export default ModalDialog;
