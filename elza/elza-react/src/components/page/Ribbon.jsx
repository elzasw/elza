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

// Nacteni globalni promenne ze <script> v <head>
const displayUserInfo = window.displayUserInfo !== undefined ? window.displayUserInfo : true;

class Ribbon extends AbstractReactComponent {
    static propTypes = {
        subMenu: PropTypes.bool,
        primarySection: PropTypes.object,
        admin: PropTypes.bool,
        arr: PropTypes.bool,
        altSection: PropTypes.object,
        itemSection: PropTypes.object,
        fundId: PropTypes.number,
    };

    static defaultProps = {
        subMenu: false,
    };

    ribbonDefaultFocusRef = null;

    constructor(props) {
        super(props);
        this.ribbonDefaultFocusRef = React.createRef();
    }


    state = {};

    componentDidMount() {
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
        this.props.dispatch(routerNavigate('/~arr'));
    };

    handleLogout = () => {
        this.props.dispatch(logout());
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

    render() {
        const {
            subMenu,
            userDetail,
            altSection,
            itemSection,
            primarySection,
            fundId,
            status: {saveCounter},
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
                                <Icon glyph="fa-list-alt"/>
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
                    <IndexLinkContainer key="ribbon-btn-arr-index" to="/arr">
                        <BootstrapButton ref={this.ribbonDefaultFocusRef} variant={'default'}>
                            <Icon glyph="fa-sitemap" />
                            <span className="btnText">{i18n('ribbon.action.arr.arr')}</span>
                        </BootstrapButton>
                    </IndexLinkContainer>,
                );
                arrParts.push(
                    <LinkContainer key="ribbon-btn-arr-dataGrid" to="/arr/dataGrid">
                        <Button variant={'default'}>
                            <Icon glyph="fa-table" />
                            <span className="btnText">{i18n('ribbon.action.arr.dataGrid')}</span>
                        </Button>
                    </LinkContainer>,
                );
            }
            if (userDetail.hasArrPage(fundId)) {
                // právo na pořádání
                arrParts.push(
                    <LinkContainer key="ribbon-btn-arr-movements" to="/arr/movements">
                        <Button variant={'default'}>
                            <Icon glyph="fa-exchange" />
                            <span className="btnText">{i18n('ribbon.action.arr.movements')}</span>
                        </Button>
                    </LinkContainer>,
                );
            }

            if (userDetail.hasArrOutputPage(fundId)) {
                // právo na výstupy
                arrParts.push(
                    <LinkContainer key="ribbon-btn-arr-output" to="/arr/output">
                        <Button variant={'default'}>
                            <Icon glyph="fa-print" />
                            <span className="btnText">{i18n('ribbon.action.arr.output')}</span>
                        </Button>
                    </LinkContainer>,
                );
            }

            if (userDetail.hasFundActionPage(fundId)) {
                // právo na hromadné akce
                arrParts.push(
                    <LinkContainer key="ribbon-btn-arr-actions" to="/arr/actions">
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
                    <LinkContainer key="ribbon-btn-arr-requests" to="/arr/requests">
                        <Button variant={'default'}>
                            <Icon glyph="fa-shopping-basket" />
                            <span className="btnText">{i18n('ribbon.action.arr.fund.requests')}</span>
                        </Button>
                    </LinkContainer>,
                );
                arrParts.push(
                    <LinkContainer key="ribbon-btn-arr-daos" to="/arr/daos">
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
                    <Button
                        variant={'default'}
                        className="large"
                        onClick={this.handleBack}
                        title={i18n('ribbon.action.back')}
                    >
                        <Icon glyph="fa-arrow-circle-o-left" />
                    </Button>
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
                    <LinkContainer key="ribbon-btn-registry" to="/registry">
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
            if (index + 1 < parts.length) {
                partsWithSplit.push(<RibbonSplit key={'ribbon-spliter-' + (index + 1)} />);
            }
        });

        return (
            <RibbonMenu>
                {partsWithSplit}
                {displayUserInfo && <RibbonGroup className="small" right>
                    <Dropdown className="user-menu">
                        <Dropdown.Toggle key="user-menu" id="user-menu">
                            {userDetail.username} <Icon glyph="fa-user" />
                        </Dropdown.Toggle>

                        <Dropdown.Menu>
                            {userDetail.authTypes.indexOf('PASSWORD') >= 0 && [
                                <Dropdown.Item key="pass-change" eventKey="1" onClick={this.handlePasswordChangeForm}>
                                    {i18n('ribbon.action.admin.user.passwordChange')}
                                </Dropdown.Item>,
                                <Dropdown.Divider key="divired" />,
                            ]}
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
                </RibbonGroup>}
            </RibbonMenu>
        );
    }
}

function mapStateToProps(state) {
    const {focus, login, userDetail, status} = state;
    return {
        focus,
        login,
        userDetail,
        status,
    };
}

export default connect(mapStateToProps, null, null, {forwardRef: true})(Ribbon);
