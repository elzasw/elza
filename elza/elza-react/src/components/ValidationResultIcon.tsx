import React, {FC} from "react";
import {Action, Dispatch} from "redux";
import {connect} from "react-redux";
import "./ValidationResultIcon.scss"
import {modalDialogHide, modalDialogShow} from "../actions/global/modalDialog";
import {Icon} from "./index";
import ValidationResultModal from "./ValidationResultModal";
import i18n from "./i18n";

type OwnProps = {
    message?: string[];
};

type Props = ReturnType<typeof mapDispatchToProps> & OwnProps;

const ValidationResultIcon: FC<Props> = props => {
    if (props.message) {
        const data = props.message;

        return <Icon className="validation-icon" glyph="fa-exclamation-triangle" onClick={() => props.onClick(data)}/>
    }
    return null;
};

const mapDispatchToProps = (dispatch: Dispatch<Action>) => ({
    onClick: (message: string[]) => {
        return dispatch(modalDialogShow(
            this,
            i18n('ap.validation.title'),
            <ValidationResultModal onClose={() => {
                dispatch(modalDialogHide())
            }} message={message}/>));
    }
});

export default connect(
    null,
    mapDispatchToProps
)(ValidationResultIcon);
