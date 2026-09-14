// import React from 'react';
import { modalDialogShow } from 'actions/global/modalDialog.jsx';
// import {connect} from 'react-redux';
// import AbstractReactComponent from '../../AbstractReactComponent';
import NoFocusButton from '../button/NoFocusButton';
import ExceptionDetail from './ExceptionDetail';
import { FormattedMessage, defineMessages } from 'react-intl';

// Id je převzaté z legacy katalogu beze změny.
const messages = defineMessages({
    detail: { id: 'global.exception.detail', defaultMessage: 'Detail' },
});
import { useThunkDispatch } from 'utils/hooks';

export interface ExceptionData<PropertyType> {
    code?: string;
    message?: string;
    stackTrace?: string;
    properties?: PropertyType;
}

interface Props<PropertyType extends object> {
    title?: React.ReactNode;
    data?: ExceptionData<PropertyType>;
    textRenderer?: (properties: PropertyType | Record<string, never>, message: string) => string;
}

export default function Exception<P extends object>({
    title,
    data,
    textRenderer
}: Props<P>) {
    const dispatch = useThunkDispatch();

    const openDetail = () => {
        dispatch(
            modalDialogShow(
                this,
                undefined,
                ({ onClose }) => <ExceptionDetail
                    onClose={onClose}
                    title={title}
                    data={data}
                />,
                'dialog-lg top max-height',
            ),
        );
    };

    const renderDetail = () => {
        return (
            <div>
                <NoFocusButton onClick={openDetail}><FormattedMessage {...messages.detail} /></NoFocusButton>
            </div>
        );
    };

    const renderText = () => {
        if (textRenderer) {
            return textRenderer(data?.properties || {}, data.message ? data.message : '');
        }
    };

    return (
        <div>
            {renderText()}
            {renderDetail()}
        </div>
    );
}
