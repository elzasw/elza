import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
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
    NodeTabs
} from 'components/index.jsx';
import {Button, MenuItem, Dropdown} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {AppStore} from 'stores/index.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {logout} from 'actions/global/login.jsx';
import * as perms from 'actions/user/Permission.jsx';

import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {userPasswordChange} from 'actions/admin/user.jsx'
import {routerNavigate} from "actions/router.jsx"

/**
 * Ribbon aplikace - obsahuje základní globální akce v aplikaci.
 */
class Ribbon extends AbstractReactComponent {

    static PropTypes = {
        subMenu: React.PropTypes.bool,
        admin: React.PropTypes.bool,
        arr: React.PropTypes.bool,
        altSection: React.PropTypes.object,
        itemSection: React.PropTypes.object,
        fundId: React.PropTypes.number
    };

    static defaultProps = {
        subMenu: false,
    };

    state = {};

    componentDidMount() {
        this.trySetFocus()
    }

    componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps)
    }

    trySetFocus = (props = this.props) => {
        const {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, null, null, 'ribbon')) {
                this.setState({}, () => {
                   ReactDOM.findDOMNode(this.refs.ribbonDefaultFocus).focus();
                   focusWasSet()
                })
            }
        }
    };

    handleBack = () => {
        this.dispatch(routerNavigate("/~arr"));
    };

    handleLogout = () => {
        this.dispatch(logout());
    };

    handlePasswordChangeForm = () => {
        this.dispatch(modalDialogShow(this, i18n('admin.user.passwordChange.title'), <PasswordForm onSubmitForm={this.handlePasswordChange} />))
    };

    handlePasswordChange = (data) => {
        this.dispatch(userPasswordChange(data.oldPassword, data.password));
    };

    render() {
        const {subMenu, userDetail, altSection, itemSection, fundId, status: {saveCounter}} = this.props;

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
                        <LinkContainer key="ribbon-btn-admin-requestsQueue" to="/admin/requestsQueue"><Button><Icon glyph="fa-shopping-basket"/>
                            <div><span className="btnText">{i18n('ribbon.action.admin.requestsQueue')}</span></div>
                        </Button></LinkContainer>
                    </RibbonGroup>
                );
            }
        }
        if (this.props.arr) {
            const arrParts = [];
            if (userDetail.hasRdPage(fundId)) {    // právo na čtení
                arrParts.push(<IndexLinkContainer key="ribbon-btn-arr-index" to="/arr"><Button ref='ribbonDefaultFocus'><Icon glyph="fa-sitemap" /><div><span className="btnText">{i18n('ribbon.action.arr.arr')}</span></div></Button></IndexLinkContainer>);
                arrParts.push(<LinkContainer key="ribbon-btn-arr-dataGrid" to="/arr/dataGrid"><Button><Icon glyph="fa-table" /><div><span className="btnText">{i18n('ribbon.action.arr.dataGrid')}</span></div></Button></LinkContainer>);
            }
            if (userDetail.hasArrPage(fundId)) {    // právo na pořádání
                arrParts.push(<LinkContainer key="ribbon-btn-arr-movements" to="/arr/movements"><Button><Icon glyph="fa-exchange" /><div><span className="btnText">{i18n('ribbon.action.arr.movements')}</span></div></Button></LinkContainer>);
            }

            if (userDetail.hasArrOutputPage(fundId) && userDetail.hasArrPage(fundId)) {    // právo na výstupy
                arrParts.push(<LinkContainer key="ribbon-btn-arr-output" to="/arr/output"><Button><Icon glyph="fa-print" /><div><span className="btnText">{i18n('ribbon.action.arr.output')}</span></div></Button></LinkContainer>);
            }

            if (userDetail.hasFundActionPage(fundId)) {    // právo na hromadné akce
                arrParts.push(<LinkContainer key="ribbon-btn-arr-actions" to="/arr/actions"><Button><Icon glyph="fa-calculator" /><div><span className="btnText">{i18n('ribbon.action.arr.fund.bulkActions')}</span></div></Button></LinkContainer>);
            }

            if (userDetail.hasArrPage(fundId)) {    // právo na pořádání
                arrParts.push(<LinkContainer key="ribbon-btn-arr-requests" to="/arr/requests"><Button><Icon glyph="fa-shopping-basket" /><div><span className="btnText">{i18n('ribbon.action.arr.fund.requests')}</span></div></Button></LinkContainer>);
                arrParts.push(<LinkContainer key="ribbon-btn-arr-daos" to="/arr/daos"><Button><Icon glyph="fa-camera" /><div><span className="btnText">{i18n('ribbon.action.arr.fund.daos')}</span></div></Button></LinkContainer>);
            }

            section = (
                <RibbonGroup key="ribbon-group-arr" className="large">
                    {arrParts}
                </RibbonGroup>
            );
        }

        const parts = [];
        if (subMenu) {  // submenu se šipkou zpět
            parts.push(
                <RibbonGroup key="ribbon-group-main" className="large">
                    {false && <IndexLinkContainer key="ribbon-btn-home" to="/"><Button><Icon glyph="fa-arrow-circle-o-left" /><div><span className="btnText">{i18n('ribbon.action.back')}</span></div></Button></IndexLinkContainer>}
                    {false && <IndexLinkContainer key="ribbon-btn-home" to="/"><Button className="large"><Icon glyph="fa-arrow-circle-o-left" /></Button></IndexLinkContainer>}
                    <Button className="large" onClick={this.handleBack} title={i18n('ribbon.action.back')}><Icon glyph="fa-arrow-circle-o-left" /></Button>
                </RibbonGroup>
            )
        } else {    // standardní menu s hlavním rozcestníkem
            parts.push(
                <RibbonGroup key="ribbon-group-main" className="large">
                    <IndexLinkContainer key="ribbon-btn-home" to="/"><Button ref='ribbonDefaultFocus'><Icon glyph="fa-home" /><div><span className="btnText">{i18n('ribbon.action.home')}</span></div></Button></IndexLinkContainer>
                    <LinkContainer key="ribbon-btn-fund" to="/fund"><Button><Icon glyph="fa-paste" /><div><span className="btnText">{i18n('ribbon.action.fund')}</span></div></Button></LinkContainer>
                    <LinkContainer key="ribbon-btn-registry" to="/registry"><Button><Icon glyph="fa-th-list" /><div><span className="btnText">{i18n('ribbon.action.registry')}</span></div></Button></LinkContainer>
                    <LinkContainer key="ribbon-btn-party" to="/party"><Button><Icon glyph="fa-users" /><div><span className="btnText">{i18n('ribbon.action.party')}</span></div></Button></LinkContainer>
                    {userDetail.hasOne(perms.ADMIN) &&
                    <LinkContainer key="ribbon-btn-admin" to="/admin"><Button><Icon glyph="fa-cog" /><div><span className="btnText">{i18n('ribbon.action.admin')}</span></div></Button></LinkContainer>}
                </RibbonGroup>
            )
        }
                // <LinkContainer key="ribbon-btn-arr" to="/arr"><Button><Icon glyph="fa-file-text" /><div><span className="btnText">{i18n('ribbon.action.arr')}</span></div></Button></LinkContainer>

        section && parts.push(section);
        altSection && parts.push(altSection);
        itemSection && parts.push(itemSection);

        const partsWithSplit = [];
        {parts.forEach((part, index) => {
            partsWithSplit.push(part);
            if (index + 1 < parts.length) {
                partsWithSplit.push(<RibbonSplit key={"ribbon-spliter-"+index+1} />)
            }
        })}

        return <RibbonMenu>
            {partsWithSplit}
            <RibbonGroup className="small right">
                <Dropdown bsStyle='default' key='user-menu' id='user-menu'>
                    <Dropdown.Toggle  noCaret>
                        {userDetail.username}<Icon glyph="fa-user" />
                    </Dropdown.Toggle>
                    <Dropdown.Menu>
                        <MenuItem eventKey="1" onClick={this.handlePasswordChangeForm}>{i18n('ribbon.action.admin.user.passwordChange')}</MenuItem>
                        <MenuItem divider/>
                        <MenuItem eventKey="2" onClick={this.handleLogout}>{i18n('ribbon.action.logout')}</MenuItem>
                    </Dropdown.Menu>
                </Dropdown>
                {saveCounter > 0 && <div className="save-msg"><Icon glyph="fa-spinner fa-spin" />{i18n('ribbon.saving')}</div>}
            </RibbonGroup>
        </RibbonMenu>
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

export default connect(mapStateToProps)(Ribbon);
