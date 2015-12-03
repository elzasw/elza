/**
 * Komponenta umožňující minimalizování a opětovné zvětšení na tlačítko.
 */

import React from 'react';

var classNames = require('classnames');

import {i18n} from 'components';
import {ButtonToolbar, ButtonGroup, Button, Glyphicon} from 'react-bootstrap';

var ToggleContent = class ToggleContent extends React.Component {
    constructor(props) {
        super(props);

        this.handleToggle = this.handleToggle.bind(this);
        this.state = {
            opened: typeof this.props.opened == 'undefined' ? true : this.props.opened,
            openedIcon: this.props.openedIcon || "chevron-up",
            closedIcon: this.props.closedIcon || "chevron-down",
            alwaysRender: typeof this.props.alwaysRender == 'undefined' ? false : this.props.alwaysRender
        };        
    }

    getChildContext() {
        return { opened: this.state.opened };
    }

    handleToggle() {
        this.setState({ opened: !this.state.opened });
        this.props.onShowHide && this.props.onShowHide(!this.state.opened);
    }

    render() {
        var toggleGlyph = this.state.opened ? this.state.openedIcon : this.state.closedIcon;

        var cls = classNames({
            "toggle-content-container": true,
            opened: this.state.opened,
            closed: !this.state.opened,
            [this.props.className]: true
        });

        var title = this.state.opened ? i18n('toggle.action.minimize') : i18n('toggle.action.restore');

        var render = this.state.opened || this.state.alwaysRender;
        var child = null;
        if (render) {
            child = this.props.children;
        }

        return (
            <div className={cls}>
                <div className="content">
                    {child}
                </div>
                <div className="toggle-container">
                    <Button className="toggle" title={title} onClick={this.handleToggle}><Glyphicon glyph={toggleGlyph} /></Button>
                </div>
            </div>
        )
    }
}

ToggleContent.childContextTypes = {
    opened: React.PropTypes.bool
}

module.exports = ToggleContent;
