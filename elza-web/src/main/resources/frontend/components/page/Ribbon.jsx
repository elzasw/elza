/**
 * Ribbon aplikace - obsahuje základní globální akce v aplikaci.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Icon, i18n} from 'components/index.jsx';
import {
    RibbonMenu,
    RibbonGroup,
    RibbonSplit,
    ToggleContent,
    FindindAidFileTree,
    PasswordForm,
    AbstractReactComponent,
    ModalDialog,
    NodeTabs,
    FundTreeTabs
} from 'components/index.jsx';
import {ButtonGroup, Button, DropdownButton, MenuItem} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {AppStore} from 'stores/index.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {logout} from 'actions/global/login.jsx';
import * as perms from 'actions/user/Permission.jsx';

import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {userPasswordChange} from 'actions/admin/user.jsx'

const Ribbon = class Ribbon extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'trySetFocus',
            'handleLogout',
            'handlePasswordChangeForm',
            'handlePasswordChange'
        )

        this.state = {};
    }

    componentDidMount() {
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps)
    }

    trySetFocus(props) {
        const {focus} = props

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
        const {userDetail, altSection, itemSection, fundId, status: {saving}} = this.props

        let section = null;

        // Aktomatické sekce podle vybrané oblasti
        if (this.props.admin) {
            if (userDetail.hasOne(perms.FUND_ADMIN)) {
                section = (
                    <RibbonGroup key="ribbon-group-admin" className="large">
                        <LinkContainer key="ribbon-btn-admin-user" to="/admin/user"><Button><Icon glyph="fa-user"/>
                            <div><span className="btnText">{i18n('ribbon.action.admin.user')}</span></div>
                        </Button></LinkContainer>
                        <LinkContainer key="ribbon-btn-admin-groups" to="/admin/group"><Button><Icon glyph="fa-group"/>
                            <div><span className="btnText">{i18n('ribbon.action.admin.group')}</span></div>
                        </Button></LinkContainer>
                        <LinkContainer key="ribbon-btn-admin-packages" to="/admin/packages"><Button><Icon glyph="fa-archive"/>
                            <div><span className="btnText">{i18n('ribbon.action.admin.packages')}</span></div>
                        </Button></LinkContainer>
                        <LinkContainer key="ribbon-btn-admin-fulltext" to="/admin/fulltext"><Button><Icon glyph="fa-search"/>
                            <div><span className="btnText">{i18n('ribbon.action.admin.fulltext')}</span></div>
                        </Button></LinkContainer>
                    </RibbonGroup>
                );
            }
        }
        if (this.props.arr) {
            const arrParts = []
            if (userDetail.hasArrPage(fundId)) {    // právo na pořádání
                arrParts.push(<IndexLinkContainer key="ribbon-btn-arr-index" to="/arr"><Button ref='ribbonDefaultFocus'><Icon glyph="fa-sitemap" /><div><span className="btnText">{i18n('ribbon.action.arr.arr')}</span></div></Button></IndexLinkContainer>)
            }
                
            if (userDetail.hasArrOutputPage(fundId)) {    // právo na outputy
                arrParts.push(<LinkContainer key="ribbon-btn-arr-output" to="/arr/output"><Button><Icon glyph="fa-print" /><div><span className="btnText">{i18n('ribbon.action.arr.output')}</span></div></Button></LinkContainer>)
            }

            if (userDetail.hasFundActionPage(fundId)) {    // právo na hromadné akce
                arrParts.push(<LinkContainer key="ribbon-btn-arr-actions" to="/arr/actions"><Button><Icon glyph="fa-cog" /><div><span className="btnText">{i18n('ribbon.action.arr.fund.bulkActions')}</span></div></Button></LinkContainer>)
            }
            
            section = (
                <RibbonGroup key="ribbon-group-arr" className="large">
                    {arrParts}
                </RibbonGroup>
            );
        }

        const parts = []
        parts.push(
            <RibbonGroup key="ribbon-group-main" className="large">
                <IndexLinkContainer key="ribbon-btn-home" to="/"><Button ref='ribbonDefaultFocus'><Icon glyph="fa-home" /><div><span className="btnText">{i18n('ribbon.action.home')}</span></div></Button></IndexLinkContainer>
                <LinkContainer key="ribbon-btn-fund" to="/fund"><Button><Icon glyph="fa-paste" /><div><span className="btnText">{i18n('ribbon.action.fund')}</span></div></Button></LinkContainer>
                <LinkContainer key="ribbon-btn-arr" to="/arr"><Button><Icon glyph="fa-file-text" /><div><span className="btnText">{i18n('ribbon.action.arr')}</span></div></Button></LinkContainer>
                <LinkContainer key="ribbon-btn-registry" to="/registry"><Button><Icon glyph="fa-th-list" /><div><span className="btnText">{i18n('ribbon.action.registry')}</span></div></Button></LinkContainer>
                <LinkContainer key="ribbon-btn-party" to="/party"><Button><Icon glyph="fa-users" /><div><span className="btnText">{i18n('ribbon.action.party')}</span></div></Button></LinkContainer>
                <LinkContainer key="ribbon-btn-admin" to="/admin"><Button><Icon glyph="fa-cog" /><div><span className="btnText">{i18n('ribbon.action.admin')}</span></div></Button></LinkContainer>
            </RibbonGroup>
        )
        section && parts.push(section)
        altSection && parts.push(altSection)
        itemSection && parts.push(itemSection)

        const partsWithSplit = []
        {parts.forEach((part, index) => {
            partsWithSplit.push(part)
            if (index + 1 < parts.length) {
                partsWithSplit.push(<RibbonSplit key={"ribbon-spliter-"+index+1} />)
            }
        })}

        return (
            <RibbonMenu opened onShowHide={this.handleRibbonShowHide}>
                {partsWithSplit}
                <RibbonGroup className="large right">
                    {saving && <span>{i18n('ribbon.saving')}</span>}
                    <DropdownButton bsStyle='default' title={userDetail.username} key='user-menu' id='user-menu'>
                        <MenuItem eventKey="1" onClick={this.handlePasswordChangeForm}>{i18n('ribbon.action.admin.user.passwordChange')}</MenuItem>
                    </DropdownButton>
                    <Button key="ribbon-btn-logout" onClick={this.handleLogout} ref='ribbonDefaultFocus'><Icon glyph="fa-sign-out" /><div><span className="btnText">{i18n('ribbon.action.logout')}</span></div></Button>
                </RibbonGroup>
            </RibbonMenu>
        )
    }

    handleLogout() {
        this.dispatch(logout());
    }

    handlePasswordChangeForm() {
        this.dispatch(modalDialogShow(this, i18n('admin.user.passwordChange.title'), <PasswordForm onSubmitForm={this.handlePasswordChange} />))
    }

    handlePasswordChange(data) {
        this.dispatch(userPasswordChange(data.oldPassword, data.newPassword));
    }
}

function mapStateToProps(state) {
    const {focus, login, userDetail, status} = state;
    return {
        focus,
        login,
        userDetail,
        status,
    }
}

module.exports = connect(mapStateToProps)(Ribbon);