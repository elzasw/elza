import React, { FC } from "react";
import { useDispatch } from "react-redux";
import { modalDialogHide, modalDialogShow } from "../actions/global/modalDialog";
import i18n from "./i18n";
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
    const dispatch = useDispatch();

    const openValidationDialog = (message: string[]) => {
        return dispatch(modalDialogShow(
            this,
            i18n('validationResult.title'),
            <ValidationResultModal onClose={() => {
                dispatch(modalDialogHide())
            }} message={message}/>));
    }

    if (message) {
        return <SmallButton title={i18n("validationResult.show")} onClick={() => openValidationDialog(message)} >
            <Icon className="validation-icon" glyph="fa-exclamation-triangle" />
        </SmallButton>
    }
    return null;
};

export default ValidationResultIcon;
