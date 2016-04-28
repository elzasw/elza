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

import {Icon, i18n, NoFocusButton} from 'components/index.jsx';
import {ButtonToolbar, ButtonGroup} from 'react-bootstrap';

var ToggleContent = class ToggleContent extends React.Component {
    constructor(props) {
        super(props);

        this.handleToggle = this.handleToggle.bind(this);
        this.state = {
            opened: typeof this.props.opened == 'undefined' ? true : this.props.opened,
            openedIcon: this.props.openedIcon || "fa-chevron-up",
            closedIcon: this.props.closedIcon || "fa-chevron-down",
            alwaysRender: typeof this.props.alwaysRender == 'undefined' ? false : this.props.alwaysRender
        };        
    }

    getChildContext() {
        return { isParentOpened: this.state.opened };
    }

    componentWillReceiveProps(nextProps) {
        if (this.props.opened !== nextProps.opened) {
            this.setState({opened: nextProps.opened});
        }
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
        var children = null;
        if (render) {
            children = React.Children.map(this.props.children, child => {
                return React.cloneElement(child, {opened: this.state.opened});
            })
        }

        return (
            <div className={cls}>
                <div className="content">
                    {children}
                </div>
                <div className="toggle-container">
                    <NoFocusButton className="toggle" title={title} onClick={this.handleToggle}><Icon glyph={toggleGlyph} /></NoFocusButton>
                </div>
            </div>
        )
    }
}

ToggleContent.childContextTypes = {
    isParentOpened: React.PropTypes.bool
}

module.exports = ToggleContent;
