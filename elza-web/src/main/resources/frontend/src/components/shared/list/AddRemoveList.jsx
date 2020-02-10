import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap'
import {indexById} from 'stores/app/utils.jsx'

import './AddRemoveList.less';
import AbstractReactComponent from "../../AbstractReactComponent";
import NoFocusButton from "../button/NoFocusButton";
import Icon from "../icon/Icon";
import i18n from "../../i18n";

class AddRemoveList extends AbstractReactComponent {

    static propTypes = {
        items: PropTypes.array.isRequired,
        label: PropTypes.string,  // pokud je uvedeno, zobrazí se jako nadpis celé sekce
        addInLabel: PropTypes.bool,   // pokud je true, je akce přidání zobrazena u labelu - tedy nahoře
        onAdd: PropTypes.func.isRequired,
        onRemove: PropTypes.func.isRequired,
        renderItem: PropTypes.func.isRequired,
        addTitle: PropTypes.string,
        addLabel: PropTypes.string,
        removeTitle: PropTypes.string,
        readOnly: PropTypes.bool.isRequired,
    };

    static defaultProps = {
        addTitle: "global.action.add",
        removeTitle: "global.action.remove",
        readOnly: false,
        renderItem: (props) => <div key={"rendered-item-" + props.index}>{props.item.name}</div>
    };

    handleRemove = (item, index) => {
        const {onRemove} = this.props;
        onRemove(item, index);
    };

    render() {
        const {addInLabel, label, items, readOnly, className, onAdd, renderItem, addTitle, removeTitle, addLabel} = this.props;

        const groups = items == null ? [] : items.map((item, index) => {
            return (
                <div className="item-container" key={"item-" + index}>
                    {renderItem({item, index})}
                    {!readOnly && <div className="item-actions-container">
                        <NoFocusButton className="remove" onClick={this.handleRemove.bind(this, item, index)} title={i18n(removeTitle)}>
                            <Icon glyph="fa-remove"/>
                        </NoFocusButton>
                    </div>}
                </div>
            )
        });

        let addAction;
        if (!readOnly) {
            addAction = <div className="actions-container">
                <NoFocusButton onClick={onAdd} title={i18n(addTitle)}>
                    <Icon glyph="fa-plus"/> {addLabel && i18n(addLabel)}
                </NoFocusButton>
            </div>
        }

        return (
            <div className={className ? "list-add-remove-container " + className : "list-add-remove-container"}>
                {(label || addInLabel) && <div className="top-label">
                    <div className="list-label">{label}</div>
                    <div className="list-action">{addInLabel && addAction}</div>
                </div>}
                <div className="item-list-container">
                    {groups}
                </div>
                {!addInLabel && addAction}
            </div>
        )
    }
}

export default AddRemoveList;

