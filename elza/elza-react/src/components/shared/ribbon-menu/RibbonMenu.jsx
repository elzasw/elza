import React from 'react';

import {ButtonToolbar} from 'react-bootstrap';

import './RibbonMenu.scss';

const RibbonMenu = (
    {
        children,
    },
) => {
    return (
        <ButtonToolbar className="ribbon-menu">
                {children}
        </ButtonToolbar>
    );
};

export default RibbonMenu;
