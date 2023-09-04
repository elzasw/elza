/**
 * Akce pro zobrazení a skrytí modálního dialogu.
 */
import React from 'react';
import { connect } from 'react-redux';
import * as types from 'actions/constants/ActionTypes';
import { Modal } from 'react-bootstrap';
import i18n from '../../components/i18n';

const AsyncWaitingDialog = connect()(
    class extends React.Component {
        constructor(props) {
            super(props);

            const { dispatch, callAsync, resultCallback, errorCallback } = props;

            callAsync
                .then((...data) => {
                    dispatch(modalDialogHide());
                    resultCallback && resultCallback(...data);
                })
                .catch((...error) => {
                    dispatch(modalDialogHide());
                    console.error(error);
                    errorCallback && errorCallback(...error);
                });
        }

        render() {
            const { title, message } = this.props;

            let tit;
            if (title) {
                tit = typeof title === 'string' ? title : React.createElement(title);
            }
            const msg =
                typeof message === 'string'
                    ? message
                    : typeof message === 'object'
                        ? message
                        : React.createElement(message);
            return (
                <div>
                    <Modal.Body>
                        {tit && <h2>{tit}</h2>}
                        {msg}
                    </Modal.Body>
                </div>
            );
        }
    },
);

/**
 * Zobrazení dialogu pro asynchronní operace, např. čekání na data nebo čekání na tisk apod.
 * @param title titulek zprávy pro uživatele, není povinné
 * @param message text zprávy, pokud není uvedeno, použije se global.data.loading
 * @param callAsync asynchronní operace, která má být zavolána, reference na funkci
 * @param resultCallback callbak po úspěšném zavolání callAsync
 * @param errorCallback callback po chybě po zavolání callback
 * @return {function(*, *)}
 */
export function showAsyncWaiting(title, message, callAsync, resultCallback, errorCallback = null) {
    const messageText = message || i18n('global.data.loading');
    return (dispatch, getState) => {
        dispatch(
            modalDialogShow(
                this,
                null,
                <AsyncWaitingDialog
                    title={title}
                    message={messageText}
                    callAsync={callAsync}
                    resultCallback={resultCallback}
                    errorCallback={errorCallback}
                />,
                '',
                null,
            ),
        );
    };
}

export function modalDialogShow(component, title, content, dialogClassName = '', onClose = null) {
    return {
        type: types.GLOBAL_MODAL_DIALOG_SHOW,
        component,
        title,
        content,
        dialogClassName,
        onClose,
    };
}
export function modalDialogHide(key) {
    return {
        key,
        type: types.GLOBAL_MODAL_DIALOG_HIDE,
    };
}
