import React from 'react';
import ReactDOM from 'react-dom';
import AbstractReactComponent from "../../AbstractReactComponent";
import * as Utils from "../../Utils";
import Icon from "../icon/Icon";
import NoFocusButton from "../button/NoFocusButton";
import {Panel, PanelGroup} from 'react-bootstrap';
import {Shortcuts} from 'react-shortcuts';
import {PropTypes} from 'prop-types';
import defaultKeymap from './CollapsablePanelKeymap.jsx';

class CollapsablePanel extends AbstractReactComponent {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };
    componentWillMount(){
        Utils.addShortcutManager(this,defaultKeymap);
    }
    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }
    static PropTypes = {
        isOpen: React.PropTypes.bool.isRequired,
        pinned: React.PropTypes.bool.isRequired,
        onSelect: React.PropTypes.func.isRequired,
        onPin: React.PropTypes.func.isRequired,
        eventKey: React.PropTypes.any,
        header: React.PropTypes.element
    };
    panelToggle = (e)=>{
        const {onSelect, eventKey} = this.props;
        onSelect && onSelect(eventKey);
    }
    panelPin = (e)=>{
        const {onPin, eventKey} = this.props;
        onPin && onPin(eventKey);
    }
    actionMap = {
        "PANEL_TOGGLE":this.panelToggle,
        "PANEL_PIN":this.panelPin
    }
    handleShortcuts = (action,e)=>{
        e.stopPropagation();
        e.preventDefault();
        this.actionMap[action](e);
    }

    render() {
        const {children, header, isOpen, onSelect, onPin, eventKey, pinned, tabIndex, ...otherProps} = this.props;
        return <PanelGroup activeKey={isOpen} onSelect={() => this.panelToggle()} accordion {...otherProps} className={isOpen ? 'open' : null}>
            <Panel eventKey={true}
                header={<Shortcuts name="CollapsablePanel" handler={(action,e)=>this.handleShortcuts(action,e)} tabIndex={0}>
                    {header}
                    {onPin && <NoFocusButton className={"btn-action pull-right" + (pinned ? " pinned" : " hover-button")} onClick={() => this.panelPin()}>
                        <Icon glyph="fa-thumb-tack" />
                    </NoFocusButton>}
                </Shortcuts>}>
                {children}
            </Panel>
        </PanelGroup>
    }
}

export default CollapsablePanel
