/**
 * Ribbon aplikace - obsahuje základní globální akce v aplikaci.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Icon, i18n} from 'components/index.jsx';
import {RibbonMenu, RibbonGroup, RibbonSplit, ToggleContent, FindindAidFileTree} from 'components/index.jsx';
import {AbstractReactComponent, ModalDialog, NodeTabs, FundTreeTabs} from 'components/index.jsx';
import {ButtonGroup, Button, DropdownButton, MenuItem} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {AppStore} from 'stores/index.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {logout} from 'actions/global/login.jsx';

var Ribbon = class Ribbon extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('trySetFocus', 'handleLogout')

        this.state = {};
    }

    componentDidMount() {
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps)
    }

    trySetFocus(props) {
        var {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, null, null, 'ribbon')) {
                this.setState({}, () => {
                   ReactDOM.findDOMNode(this.refs.ribbonDefaultFocus).focus()
                   focusWasSet()
                })
            }
        }
    }

    render() {
        const {userDetail, altSection, itemSection} = this.props

        var section = null;

        // Aktomatické sekce podle vybrané oblasti
        if (this.props.admin) {
            section = (
                <RibbonGroup className="large">
                    <LinkContainer to="/admin/packages"><Button><Icon glyph="fa-archive" /><div><span className="btnText">{i18n('ribbon.action.admin.packages')}</span></div></Button></LinkContainer>
                    <LinkContainer to="/admin/fulltext"><Button><Icon glyph="fa-search" /><div><span className="btnText">{i18n('ribbon.action.admin.fulltext')}</span></div></Button></LinkContainer>
                </RibbonGroup>
            );
        }
        if (this.props.arr) {
            section = (
                <RibbonGroup className="large">
                    <IndexLinkContainer to="/arr"><Button ref='ribbonDefaultFocus'><Icon glyph="fa-sitemap" /><div><span className="btnText">{i18n('ribbon.action.arr.arr')}</span></div></Button></IndexLinkContainer>
                    <LinkContainer to="/arr/output"><Button><Icon glyph="fa-print" /><div><span className="btnText">{i18n('ribbon.action.arr.output')}</span></div></Button></LinkContainer>
                    <LinkContainer to="/arr/actions"><Button><Icon glyph="fa-cog" /><div><span className="btnText">{i18n('ribbon.action.arr.fund.bulkActions')}</span></div></Button></LinkContainer>
                </RibbonGroup>
            );
        }

        const parts = []
        parts.push(
            <RibbonGroup className="large">
                <IndexLinkContainer to="/"><Button ref='ribbonDefaultFocus'><Icon glyph="fa-home" /><div><span className="btnText">{i18n('ribbon.action.home')}</span></div></Button></IndexLinkContainer>
                <LinkContainer to="/fund"><Button><Icon glyph="fa-paste" /><div><span className="btnText">{i18n('ribbon.action.fund')}</span></div></Button></LinkContainer>
                <LinkContainer to="/arr"><Button><Icon glyph="fa-file-text" /><div><span className="btnText">{i18n('ribbon.action.arr')}</span></div></Button></LinkContainer>
                <LinkContainer to="/registry"><Button><Icon glyph="fa-th-list" /><div><span className="btnText">{i18n('ribbon.action.registry')}</span></div></Button></LinkContainer>
                <LinkContainer to="/party"><Button><Icon glyph="fa-users" /><div><span className="btnText">{i18n('ribbon.action.party')}</span></div></Button></LinkContainer>
                <LinkContainer to="/admin"><Button><Icon glyph="fa-cog" /><div><span className="btnText">{i18n('ribbon.action.admin')}</span></div></Button></LinkContainer>
            </RibbonGroup>
        )
        section && parts.push(section)
        altSection && parts.push(altSection)
        itemSection && parts.push(itemSection)

        const partsWithSplit = []
        {parts.forEach((part, index) => {
            partsWithSplit.push(part)
            if (index + 1 < parts.length) {
                partsWithSplit.push(<RibbonSplit />)
            }
        })}

        return (
            <RibbonMenu opened onShowHide={this.handleRibbonShowHide}>
                {partsWithSplit}
                <RibbonGroup className="large right">
                    {userDetail.username}
                    <Button onClick={this.handleLogout} ref='ribbonDefaultFocus'><Icon glyph="fa-sign-out" /><div><span className="btnText">{i18n('ribbon.action.logout')}</span></div></Button>
                </RibbonGroup>
            </RibbonMenu>
        )
    }

    handleLogout() {
        this.dispatch(logout());
    }
}

function mapStateToProps(state) {
    const {focus, login, userDetail} = state
    return {
        focus,
        login,
        userDetail,
    }
}

module.exports = connect(mapStateToProps)(Ribbon);