/**
 *
 * Komponenta umožňující minimalizování a opětovné zvětšení bloku na tlačítko.
 * !!! Je nutné si oblast celou nastylovat. Komponenta nemá vlastní css a je ho při použítí nutné napsat vlastní. (className)
 * @param opened boolean default true: zda se má při inicializaci zobrazit rozbalený (true) nebo zabaleny (false).
 * @param onShowHide handler: co se má stát po kliknutí
 * @param alwaysRender bolean default false zda se ma vzdycky vyrenderovat objekt
 * @param openedIcon String default 'chevron-up' ikona pro rozbaleni
 * @param closedIcon String default 'chevron-down' ikona pro sbaleni 
 *
 * v kontextu je přístupný isParentOpened boolen - this.context.isParentOpened.
 * příklad použití
 * <ToggleContent className="fa-file-toggle-container" alwaysRender opened={false} closedIcon="chevron-right" openedIcon="chevron-left">...</ToggleContent>
 *
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
            isParentOpened: typeof this.props.opened == 'undefined' ? true : this.props.opened,
            openedIcon: this.props.openedIcon || "chevron-up",
            closedIcon: this.props.closedIcon || "chevron-down",
            alwaysRender: typeof this.props.alwaysRender == 'undefined' ? false : this.props.alwaysRender
        };        
    }

    getChildContext() {
        return { isParentOpened: this.state.isParentOpened };
    }

    handleToggle() {
        this.setState({ isParentOpened: !this.state.isParentOpened });
        this.props.onShowHide && this.props.onShowHide(!this.state.isParentOpened);
    }

    render() {
        var toggleGlyph = this.state.isParentOpened ? this.state.openedIcon : this.state.closedIcon;

        var cls = classNames({
            "toggle-content-container": true,
            opened: this.state.isParentOpened,
            closed: !this.state.isParentOpened,
            [this.props.className]: true
        });

        var title = this.state.isParentOpened ? i18n('toggle.action.minimize') : i18n('toggle.action.restore');

        var render = this.state.isParentOpened || this.state.alwaysRender;
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
    isParentOpened: React.PropTypes.bool
}

module.exports = ToggleContent;
