import React, { FC } from "react";
import { useDispatch } from "react-redux";
import { modalDialogHide, modalDialogShow } from "../actions/global/modalDialog";
import { defineMessages, useIntl } from 'react-intl';

// Id jsou převzatá z legacy katalogu beze změny. Texty jdou do title=, tedy řetězec.
const messages = defineMessages({
    title: { id: 'validationResult.title', defaultMessage: 'Výsledek validace archivní entity' },
    show: { id: 'validationResult.show', defaultMessage: 'Zobrazit výsledek validace' },
});
import { Icon } from "./index";
import "./ValidationResultIcon.scss";
import ValidationResultModal from "./ValidationResultModal";
import { SmallButton } from "./shared/button/small-button";

type Props = {
    message?: string[];
};

const ValidationResultIcon: FC<Props> = ({
    message,
}) => {
    const intl = useIntl();
    const dispatch = useDispatch();

    const openValidationDialog = (message: string[]) => {
        return dispatch(modalDialogShow(
            this,
            intl.formatMessage(messages.title),
            <ValidationResultModal onClose={() => {
                dispatch(modalDialogHide())
            }} message={message}/>));
    }

    if (message) {
        return <SmallButton title={intl.formatMessage(messages.show)} onClick={() => openValidationDialog(message)} >
            <Icon className="validation-icon" glyph="fa-exclamation-triangle" />
        </SmallButton>
    }
    return null;
};

export default ValidationResultIcon;
