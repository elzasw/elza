import React from 'react';
import ReactDOM from 'react-dom';
import {Panel, PanelGroup} from 'react-bootstrap';
import {Shortcuts} from 'react-shortcuts'
import AbstractReactComponent from "../../AbstractReactComponent";
import NoFocusButton from "../button/NoFocusButton";
import Icon from "../icon/Icon";

class CollapsablePanel extends AbstractReactComponent {
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
        onSelect(eventKey);
    }
    panelPin = (e)=>{
        const {onPin, eventKey} = this.props;
        onPin(eventKey);
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
                header={<Shortcuts name="CollapsablePanel" handler={(action,e)=>this.handleShortcuts(action,e)} tabIndex={"0"}>
                    {header}
                    <NoFocusButton className={"btn-action pull-right" + (pinned ? " pinned" : " hover-button")} onClick={() => panelPin()}>
                        <Icon glyph="fa-thumb-tack" />
                    </NoFocusButton>
                </Shortcuts>}>
                {children}
            </Panel>
        </PanelGroup>
    }
}

export default CollapsablePanel
