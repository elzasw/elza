import React from 'react';
import ReactDOM from 'react-dom';
import {i18n, NoFocusButton, Icon, AbstractReactComponent} from 'components'
import {Panel, PanelGroup} from 'react-bootstrap';


class CollapsablePanel extends AbstractReactComponent {
    static PropTypes = {
        isOpen: React.PropTypes.bool.isRequired,
        pinned: React.PropTypes.bool.isRequired,
        onSelect: React.PropTypes.func.isRequired,
        onPin: React.PropTypes.func.isRequired,
        eventKey: React.PropTypes.any,
        header: React.PropTypes.element
    };

    render() {
        const {children, header, isOpen, onSelect, onPin, eventKey, pinned, ...otherProps} = this.props;
        return <PanelGroup activeKey={isOpen} onSelect={() => onSelect(eventKey)} accordion {...otherProps} className={isOpen ? 'open' : null}>
            <Panel eventKey={true}
                   header={<div>
                       {header}
                       <NoFocusButton className={"pull-right" + (pinned ? "" : " hover-button")} onClick={() => onPin(eventKey)}>
                           <Icon glyph="fa-thumb-tack" />
                       </NoFocusButton>
                   </div>}>
                {children}
            </Panel>
        </PanelGroup>
    }
}

export default CollapsablePanel