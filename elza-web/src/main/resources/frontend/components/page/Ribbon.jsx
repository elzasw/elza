/**
 * Ribbon aplikace - obsahuje základní globální akce v aplikaci.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Icon, i18n} from 'components';
import {RibbonMenu, RibbonGroup, RibbonSplit, ToggleContent, FindindAidFileTree} from 'components';
import {AbstractReactComponent, ModalDialog, NodeTabs, FundTreeTabs} from 'components';
import {ButtonGroup, Button, DropdownButton, MenuItem} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {AppStore} from 'stores'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus'

var Ribbon = class Ribbon extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('trySetFocus')

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

        return (
            <RibbonMenu opened onShowHide={this.handleRibbonShowHide}>
                <RibbonGroup className="large">
                    <IndexLinkContainer to="/"><Button ref='ribbonDefaultFocus'><Icon glyph="fa-home" /><div><span className="btnText">{i18n('ribbon.action.home')}</span></div></Button></IndexLinkContainer>
                    <LinkContainer to="/fund"><Button><Icon glyph="fa-paste" /><div><span className="btnText">{i18n('ribbon.action.fund')}</span></div></Button></LinkContainer>
                    <LinkContainer to="/arr"><Button><Icon glyph="fa-file-text" /><div><span className="btnText">{i18n('ribbon.action.arr')}</span></div></Button></LinkContainer>
                    <LinkContainer to="/registry"><Button><Icon glyph="fa-th-list" /><div><span className="btnText">{i18n('ribbon.action.registry')}</span></div></Button></LinkContainer>
                    <LinkContainer to="/party"><Button><Icon glyph="fa-users" /><div><span className="btnText">{i18n('ribbon.action.party')}</span></div></Button></LinkContainer>
                    <LinkContainer to="/admin"><Button><Icon glyph="fa-cog" /><div><span className="btnText">{i18n('ribbon.action.admin')}</span></div></Button></LinkContainer>
                </RibbonGroup>

                <RibbonSplit />

                {section}
                {this.props.altSection}
                {this.props.altSection && <RibbonSplit />}
                {this.props.itemSection}
            </RibbonMenu>
        )
    }
}

function mapStateToProps(state) {
    const {focus} = state
    return {
        focus,
    }
}

module.exports = connect(mapStateToProps)(Ribbon);