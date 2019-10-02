import React from 'react';

import {ButtonToolbar} from 'react-bootstrap';

import './RibbonMenu.less';

/**
 * Ribbon menu v pro záhlaví aplikace
 */
class RibbonMenu extends React.Component {

    render() {
        return <ButtonToolbar className="ribbon-menu">
                {this.props.children}            
        </ButtonToolbar>
    }
}

export default RibbonMenu;
