/**
 * Ribbon aplikace - obsahuje základní globální akce v aplikaci.
 */
import PropTypes from 'prop-types';

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux';
import {IndexLinkContainer, LinkContainer} from 'react-router-bootstrap';
import {AbstractReactComponent, i18n, Icon, RibbonGroup, RibbonMenu, RibbonSplit} from 'components/shared';
import {Dropdown, Button as BootstrapButton} from 'react-bootstrap';
import {Button} from '../ui';
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx';
import {logout} from 'actions/global/login.jsx';
import * as perms from 'actions/user/Permission.jsx';

import {modalDialogShow} from 'actions/global/modalDialog.jsx';
import {userPasswordChange} from 'actions/admin/user.jsx';
import {routerNavigate} from 'actions/router.jsx';
import PasswordForm from '../admin/PasswordForm';
import {
    URL_ENTITY,
    URL_FUND,
    URL_NODE,
    urlFundActions, urlFundDaos,
    urlFundGrid,
    urlFundMovements,
    urlFundOutputs, urlFundRequests, urlFundTree, urlFund, URL_FUND_GRID_PATH, GRID
} from "../../constants";

// Nacteni globalni promenne ze <script> v <head>
const displayUserInfo = window.displayUserInfo !== undefined ? window.displayUserInfo : true;

class Ribbon extends AbstractReactComponent {
    static propTypes = {
        showUser: PropTypes.bool,
        subMenu: PropTypes.bool,
        primarySection: PropTypes.object,
        admin: PropTypes.bool,
        arr: PropTypes.bool,
        altSection: PropTypes.object,
        itemSection: PropTypes.object,
        fundId: PropTypes.number,
        versionId: PropTypes.number,
        isCurrentVersion: PropTypes.bool,
    };

    static defaultProps = {
        subMenu: false,
        showUser: true,
    };

    ribbonDefaultFocusRef = null;

    constructor(props) {
        super(props);
        this.ribbonDefaultFocusRef = React.createRef();
    }

    state = {};

    componentDidMount() {
        const theme = localStorage.getItem("theme") || "light";
        document.getElementsByTagName('body')[0].className = theme;
        this.setState({theme})

        this.trySetFocus();
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps);
    }

    trySetFocus = (props = this.props) => {
        const {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, null, null, 'ribbon')) {
                this.setState({}, () => {
                    if (this.ribbonDefaultFocusRef.current) {
                        this.ribbonDefaultFocusRef.current.focus();
                        focusWasSet();
                    }
                });
            }
        }
    };

    handleBack = () => {
        this.props.dispatch(routerNavigate(URL_FUND));
    };

    handleLogout = () => {
        this.props.dispatch(logout()).then(() => {
            const logoutUrl = window.logoutUrl;
            if (logoutUrl) {
                location.assign(logoutUrl);
            }
        });
    };

    handlePasswordChangeForm = () => {
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('admin.user.passwordChange.title'),
                <PasswordForm onSubmitForm={this.handlePasswordChange} />,
            ),
        );
    };

    handlePasswordChange = data => {
        return this.props.dispatch(userPasswordChange(data.oldPassword, data.password));
    };

    handleChangeTheme = () => {
        const body = document.getElementsByTagName('body')[0];
        if(body.className.indexOf("light") >= 0){
            this.setState({theme: "dark"})
            body.className = body.className.replace("light", "dark");
            localStorage.setItem("theme","dark")
        } else {
            this.setState({theme: "light"})
            body.className = body.className.replace("dark", "light");
            localStorage.setItem("theme","light")
        }
    }

    render() {
        const {
            subMenu,
            userDetail,
            altSection,
            itemSection,
            primarySection,
            fundId,
            versionId,
            status: {saveCounter},
            showUser,
        } = this.props;

        let section = null;
        // Aktomatické sekce podle vybrané oblasti
        if (this.props.admin) {
            const isSuperuser = userDetail.hasOne(perms.ADMIN);
            // Users can be administered if controlls some group or user
            const administersUser =
                userDetail.hasOne(perms.GROUP_CONTROL_ENTITITY, perms.USR_PERM) ||
                userDetail.hasOne(perms.USER_CONTROL_ENTITITY, perms.USR_PERM);
            const administersGroup = userDetail.hasOne(perms.GROUP_CONTROL_ENTITITY, perms.USR_PERM);

            section = (
                <RibbonGroup key="ribbon-group-admin" className="large">
                    {administersUser && (
                        <LinkContainer key="ribbon-btn-admin-user" to="/admin/user">
                            <Button variant={'default'}>
                                <Icon glyph="fa-user" />
                                <span className="btnText">{i18n('ribbon.action.admin.user')}</span>
                            </Button>
                        </LinkContainer>
                    )}
                    {administersGroup && (
                        <LinkContainer key="ribbon-btn-admin-groups" to="/admin/group">
                            <Button variant={'default'}>
                                <Icon glyph="fa-group" />
                                <span className="btnText">{i18n('ribbon.action.admin.group')}</span>
                            </Button>
                        </LinkContainer>
                    )}
                    {(administersGroup || administersUser) && (
                        <LinkContainer key="ribbon-btn-admin-funds" to="/admin/fund">
                            <Button variant={'default'}>
                                <Icon glyph="fa-database" />
                                <span className="btnText">{i18n('ribbon.action.admin.fund')}</span>
                            </Button>
                        </LinkContainer>
                    )}
                    {isSuperuser && [
                        <LinkContainer key="ribbon-btn-admin-packages" to="/admin/packages">
                            <Button variant={'default'}>
                                <Icon glyph="fa-archive" />
                                <span className="btnText">{i18n('ribbon.action.admin.packages')}</span>
                            </Button>
                        </LinkContainer>,
                        <LinkContainer key="ribbon-btn-admin-bulkActions" to="/admin/backgroundProcesses">
                            <Button>
                                <Icon glyph="fa-list-alt" />
                                <span className="btnText">{i18n('ribbon.action.admin.backgroundProcesses')}</span>
                            </Button>
                        </LinkContainer>,
                        <LinkContainer key="ribbon-btn-admin-requestsQueue" to="/admin/requestsQueue">
                            <Button variant={'default'}>
                                <Icon glyph="fa-shopping-basket" />
                                <span className="btnText">{i18n('ribbon.action.admin.requestsQueue')}</span>
                            </Button>
                        </LinkContainer>,
                        <LinkContainer key="ribbon-btn-admin-external-systems" to="/admin/extSystem">
                            <Button variant={'default'}>
                                <Icon glyph="fa-external-link" />
                                <span className="btnText">{i18n('ribbon.action.admin.externalSystems')}</span>
                            </Button>
                        </LinkContainer>,
                        <LinkContainer key="ribbon-btn-admin-show-logs" to="/admin/logs">
                            <Button variant={'default'}>
                                <Icon glyph="fa-file-text-o" />
                                <span className="btnText">{i18n('ribbon.action.admin.showLogs')}</span>
                            </Button>
                        </LinkContainer>,
                    ]}
                </RibbonGroup>
            );
        }
        if (this.props.arr) {
            const arrParts = [];
            if (userDetail.hasRdPage(fundId)) {
                
                // právo na čtení
                arrParts.push(
                    <IndexLinkContainer key="ribbon-btn-arr-index" to={urlFundTree(fundId, versionId)}>
                        <BootstrapButton ref={this.ribbonDefaultFocusRef} variant={'default'} className={window.location.pathname.startsWith(URL_NODE) ? "active" : ""}>
                            <Icon glyph="fa-sitemap" />
                            <span className="btnText">{i18n('ribbon.action.arr.arr')}</span>
                        </BootstrapButton>
                    </IndexLinkContainer>,
                );

                arrParts.push(
                    <LinkContainer key="ribbon-btn-arr-dataGrid" to={urlFundGrid(fundId, versionId, this.props.serializedFilter)}>
                        <Button variant={'default'} className={window.location.pathname.includes(GRID) ? "active" : ""}>
                            <Icon glyph="fa-table" />
                            <span className="btnText">{i18n('ribbon.action.arr.dataGrid')}</span>
                        </Button>
                    </LinkContainer>,
                );
            }
            if (userDetail.hasArrPage(fundId)) {
                // právo na pořádání
                arrParts.push(
                    <LinkContainer key="ribbon-btn-arr-movements" to={urlFundMovements(fundId, versionId)}>
                        <Button variant={'default'}>
                            <Icon glyph="fa-exchange" />
                            <span className="btnText">{i18n('ribbon.action.arr.movements')}</span>
                        </Button>
                    </LinkContainer>,
                );
            }

            if (userDetail.hasRdPage(fundId, versionId)) {
                // právo na výstupy
                arrParts.push(
                    <LinkContainer key="ribbon-btn-arr-output" to={urlFundOutputs(fundId, versionId)}>
                        <Button variant={'default'}>
                            <Icon glyph="fa-print" />
                            <span className="btnText">{i18n('ribbon.action.arr.output')}</span>
                        </Button>
                    </LinkContainer>,
                );
            }

            if (userDetail.hasRdPage(fundId)) {
                // právo na hromadné akce
                arrParts.push(
                    <LinkContainer key="ribbon-btn-arr-actions" to={urlFundActions(fundId, versionId)}>
                        <Button variant={'default'}>
                            <Icon glyph="fa-calculator" />
                            <span className="btnText">{i18n('ribbon.action.arr.fund.bulkActions')}</span>
                        </Button>
                    </LinkContainer>,
                );
            }

            if (userDetail.hasArrPage(fundId)) {
                // právo na pořádání
                arrParts.push(
                    <LinkContainer key="ribbon-btn-arr-requests" to={urlFundRequests(fundId, versionId)}>
                        <Button variant={'default'}>
                            <Icon glyph="fa-shopping-basket" />
                            <span className="btnText">{i18n('ribbon.action.arr.fund.requests')}</span>
                        </Button>
                    </LinkContainer>,
                );
                arrParts.push(
                    <LinkContainer key="ribbon-btn-arr-daos" to={urlFundDaos(fundId, versionId)}>
                        <Button variant={'default'}>
                            <Icon glyph="fa-camera" />
                            <span className="btnText">{i18n('ribbon.action.arr.fund.daos')}</span>
                        </Button>
                    </LinkContainer>,
                );
            }

            section = (
                <RibbonGroup key="ribbon-group-arr" className="large">
                    {arrParts}
                </RibbonGroup>
            );
        }

        const parts = [];
        if (subMenu) {
            // submenu se šipkou zpět
            parts.push(
                <RibbonGroup key="ribbon-group-main" className="large big-icon">
                    <LinkContainer key="ribbon-btn-arr-back" to={urlFund(fundId)}>
                        <Button
                            variant={'default'}
                            className="large"
                            title={i18n('ribbon.action.back')}
                        >
                            <Icon glyph="fa-arrow-circle-o-left" />
                        </Button>
                    </LinkContainer>
                </RibbonGroup>,
            );
        } else if (primarySection) {
            section = primarySection;
        } else {
            // standardní menu s hlavním rozcestníkem
            parts.push(
                <RibbonGroup key="ribbon-group-main" className="large">
                    <IndexLinkContainer key="ribbon-btn-home" to="/">
                        <BootstrapButton ref={this.ribbonDefaultFocusRef} variant={'default'}>
                            <Icon glyph="fa-home" />
                            <span className="btnText">{i18n('ribbon.action.home')}</span>
                        </BootstrapButton>
                    </IndexLinkContainer>
                    <LinkContainer key="ribbon-btn-fund" to="/fund">
                        <Button variant={'default'}>
                            <Icon glyph="fa-database" />
                            <span className="btnText">{i18n('ribbon.action.fund')}</span>
                        </Button>
                    </LinkContainer>
                    <LinkContainer key="ribbon-btn-registry" to={URL_ENTITY}>
                        <Button variant={'default'}>
                            <Icon glyph="fa-th-list" />
                            <span className="btnText">{i18n('ribbon.action.registry')}</span>
                        </Button>
                    </LinkContainer>
                    {userDetail.hasOne(
                        perms.ADMIN,
                        perms.USR_PERM,
                        perms.USER_CONTROL_ENTITITY,
                        perms.GROUP_CONTROL_ENTITITY,
                    ) && (
                        <LinkContainer key="ribbon-btn-admin" to="/admin">
                            <Button variant={'default'}>
                                <Icon glyph="fa-cog" />
                                <span className="btnText">{i18n('ribbon.action.admin')}</span>
                            </Button>
                        </LinkContainer>
                    )}
                </RibbonGroup>,
            );
        }
        // <LinkContainer key="ribbon-btn-arr" to="/arr"><Button><Icon glyph="fa-file-text" /><div><span className="btnText">{i18n('ribbon.action.arr')}</span></div></Button></LinkContainer>

        section && parts.push(section);
        altSection && parts.push(altSection);
        itemSection && parts.push(itemSection);

        const partsWithSplit = [];
        parts.forEach((part, index) => {
            partsWithSplit.push(part);
        });
        const _showUser = displayUserInfo && showUser;

        return (
            <div className="ribbon-menu-container">
                <RibbonMenu>
                    {partsWithSplit}
                </RibbonMenu>
                {_showUser && (
                    <div className="user-menu-container">
                        <Dropdown className="user-menu" id={'user-menu'} alignRight>
                            <Dropdown.Toggle key="user-menu" id="user-menu">
                                {userDetail.username} <Icon glyph="fa-user" />
                            </Dropdown.Toggle>

                            <Dropdown.Menu popperConfig={{strategy: 'fixed'}}>
                                {userDetail.authTypes.indexOf('PASSWORD') >= 0 && [
                                    <Dropdown.Item
                                        key="pass-change"
                                        eventKey="1"
                                        onClick={this.handlePasswordChangeForm}
                                    >
                                        {i18n('ribbon.action.admin.user.passwordChange')}
                                    </Dropdown.Item>,
                                    <Dropdown.Divider key="divired" />,
                                ]}
                                {
                                    <>
                                        <Dropdown.Item eventKey="3" onClick={this.handleChangeTheme}>
                                            {this.state.theme === "dark" ? <Icon glyph="fa-check-square"/> : <Icon glyph="fa-square-o"/>} {i18n("ribbon.action.darkTheme")}
                                        </Dropdown.Item>
                                        <Dropdown.Divider key="divider" />
                                    </>
                                }
                                <Dropdown.Item eventKey="2" onClick={this.handleLogout}>
                                    {i18n('ribbon.action.logout')}
                                </Dropdown.Item>
                            </Dropdown.Menu>
                        </Dropdown>
                        {saveCounter > 0 && (
                            <div className="save-msg-container">
                                <span className="save-msg">
                                    <Icon glyph="fa-spinner fa-spin" />
                                    {i18n('ribbon.saving')}
                                </span>
                            </div>
                        )}
                        </div>
                )}
                </div>
        );
    }
}

function mapStateToProps(state) {
    const {focus, login, userDetail, status, arrRegion} = state;
    return {
        serializedFilter: arrRegion.funds?.[arrRegion.activeIndex]?.fundDataGrid?.serializedFilter,
        focus,
        login,
        userDetail,
        status,
    };
}

export default connect(mapStateToProps, null, null, {forwardRef: true})(Ribbon);
