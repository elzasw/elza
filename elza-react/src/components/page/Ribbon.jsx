/**
 * Ribbon aplikace - obsahuje základní globální akce v aplikaci.
 */
import PropTypes from 'prop-types';

import React from 'react';
import ReactDOM from 'react-dom';
import { connect } from 'react-redux';
import { IndexLinkContainer, LinkContainer } from 'react-router-bootstrap';
import { AbstractReactComponent, Icon, RibbonGroup, RibbonMenu, RibbonSplit } from 'components/shared';
import { Dropdown, Button as BootstrapButton } from 'react-bootstrap';
import { FormattedMessage, defineMessages, injectIntl } from 'react-intl';
import { Button } from '../ui';
import { canSetFocus, focusWasSet, isFocusFor } from 'actions/global/focus.jsx';
import { logout } from 'actions/global/login.jsx';

import { modalDialogShow } from 'actions/global/modalDialog.jsx';
import { userPasswordChange } from 'actions/admin/user.jsx';
import { routerNavigate } from 'actions/router.jsx';
import PasswordForm from '../admin/PasswordForm';
import {
    URL_FUND,
    URL_NODE,
    JAVA_ATTR_CLASS,
    urlFundActions, urlFundDaos,
    urlFundGrid,
    urlFundAb,
    urlFundMovements,
    urlFundOutputs, urlFundRequests, urlFundTree, urlFund, URL_FUND_GRID_PATH, GRID, urlFundPublication, PUBLICATION,
    AIP
} from "../../constants";
import { extSystemListFetchIfNeeded } from 'actions/admin/extSystem.jsx';
import { EXT_SYSTEM_CLASS } from 'components/admin/extSystem/ExtSystemForm';
import { UserSettingsModal } from 'components/user/UserSettingsModal';
import { AiAssistantRibbonButton } from 'components/ai-assistant/AiAssistantRibbonButton';
import { ExperimentalFeature } from 'components/shared/ExperimentalFeature';
import { MainNavigation } from './MainNavigation';

// Nacteni globalni promenne ze <script> v <head>
const displayUserInfo = window.displayUserInfo !== undefined ? window.displayUserInfo : true;

// Ids jsou převzaty z legacy katalogu beze změny - přejmenování id při migraci
// zahodí jeho překlady při dalším locale:merge.
const messages = defineMessages({
    publication: {
        id: 'ribbon.action.publication',
        defaultMessage: 'Publikace',
    },
    back: {
        id: 'ribbon.action.back',
        defaultMessage: 'Zpět',
    },
    passwordChange: {
        id: 'ribbon.action.admin.user.passwordChange',
        defaultMessage: 'Změnit heslo',
    },
    passwordChangeTitle: {
        id: 'admin.user.passwordChange.title',
        defaultMessage: 'Změna hesla',
    },
    userSettings: {
        id: 'userSettings.button.title',
        defaultMessage: 'Nastavení',
    },
    logout: {
        id: 'ribbon.action.logout',
        defaultMessage: 'Odhlásit',
    },
    arrArr: { id: 'ribbon.action.arr.arr', defaultMessage: 'Pořádání' },
    arrDataGrid: { id: 'ribbon.action.arr.dataGrid', defaultMessage: 'Tabulkové zobrazení' },
    arrAb: { id: 'ribbon.action.arr.ab', defaultMessage: 'Archivní balíčky' },
    arrMovements: { id: 'ribbon.action.arr.movements', defaultMessage: 'Přesuny' },
    arrOutput: { id: 'ribbon.action.arr.output', defaultMessage: 'Výstupy' },
    arrBulkActions: { id: 'ribbon.action.arr.fund.bulkActions', defaultMessage: 'Funkce' },
    arrRequests: { id: 'ribbon.action.arr.fund.requests', defaultMessage: 'Požadavky' },
    arrDaos: { id: 'ribbon.action.arr.fund.daos', defaultMessage: 'Digitální entity' },
    saving: {
        id: 'ribbon.saving',
        defaultMessage: 'Ukládání',
    },
});

class Ribbon extends AbstractReactComponent {
    static propTypes = {
        showUser: PropTypes.bool,
        subMenu: PropTypes.bool,
        primarySection: PropTypes.object,
        arr: PropTypes.bool,
        altSection: PropTypes.node,
        itemSection: PropTypes.node,
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
        this.trySetFocus();
        this.props.dispatch(extSystemListFetchIfNeeded());
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps);
    }

    trySetFocus = (props = this.props) => {
        const { focus } = props;

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
        this.props.dispatch(logout(true)).then(() => {
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
                this.props.intl.formatMessage(messages.passwordChangeTitle),
                <PasswordForm onSubmitForm={this.handlePasswordChange} />,
            ),
        );
    };

    handlePasswordChange = data => {
        return this.props.dispatch(userPasswordChange(data.oldPassword, data.password));
    };

    handleUserSettings = () => {
        const { dispatch } = this.props;
        // Fluent dialog nese vlastní titulek i obal, proto se vkládá jako obsah bez wrapperu.
        dispatch(modalDialogShow(
            this,
            undefined,
            ({ key, visible, onClose }) => <UserSettingsModal key={key} open={visible} onClose={onClose} />,
        ))
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
            status: { saveCounter },
            showUser,
            extSystemList
        } = this.props;

        let section = null;
        // Aktomatické sekce podle vybrané oblasti
        if (this.props.arr) {
            const arrParts = [];
            if (userDetail.hasRdPage(fundId)) {

                // právo na čtení
                arrParts.push(
                    <IndexLinkContainer key="ribbon-btn-arr-index" to={urlFundTree(fundId, versionId)}>
                        <BootstrapButton ref={this.ribbonDefaultFocusRef} variant={'default'} className={window.location.pathname.startsWith(URL_NODE) ? "active" : ""}>
                            <Icon glyph="fa-sitemap" />
                            <span className="btnText"><FormattedMessage {...messages.arrArr} /></span>
                        </BootstrapButton>
                    </IndexLinkContainer>,
                );

                arrParts.push(
                    <LinkContainer key="ribbon-btn-arr-dataGrid" to={urlFundGrid(fundId, versionId, this.props.serializedFilter)}>
                        <Button variant={'default'} className={window.location.pathname.includes(GRID) ? "active" : ""}>
                            <Icon glyph="fa-table" />
                            <span className="btnText"><FormattedMessage {...messages.arrDataGrid} /></span>
                        </Button>
                    </LinkContainer>,
                );
                arrParts.push(
                    <LinkContainer key="ribbon-btn-arr-ab" to={urlFundAb(fundId, versionId)}>
                        <Button variant='default' className={window.location.pathname.includes(AIP) ? "active" : ""}>
                            <Icon glyph="fa-archive" />
                            <span className="btnText"><FormattedMessage {...messages.arrAb} /></span>
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
                            <span className="btnText"><FormattedMessage {...messages.arrMovements} /></span>
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
                            <span className="btnText"><FormattedMessage {...messages.arrOutput} /></span>
                        </Button>
                    </LinkContainer>,
                );
            }

            if (userDetail.hasRdPage(fundId)) {
                arrParts.push(
                    <LinkContainer key="ribbon-btn-arr-publication" to={urlFundPublication(fundId, versionId)}>
                        <Button variant={'default'}>
                            <Icon glyph="fa-newspaper-o" />
                            <span className="btnText"><FormattedMessage {...messages.publication} /></span>
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
                            <span className="btnText"><FormattedMessage {...messages.arrBulkActions} /></span>
                        </Button>
                    </LinkContainer>,
                );
            }

            if (userDetail.hasRdPage(fundId)) {
                const hasDigitizationFrontdesk = extSystemList?.rows?.some(
                    (sys) => sys[JAVA_ATTR_CLASS] === EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk,
                );

                if (hasDigitizationFrontdesk) {
                    arrParts.push(
                        <LinkContainer key="ribbon-btn-arr-requests" to={urlFundRequests(fundId, versionId)}>
                            <Button variant={'default'}>
                                <Icon glyph="fa-shopping-basket" />
                                <span className="btnText"><FormattedMessage {...messages.arrRequests} /></span>
                            </Button>
                        </LinkContainer>,
                    );
                }

                const hasDigitalRepository = extSystemList?.rows?.some(
                    (sys) => sys[JAVA_ATTR_CLASS] === EXT_SYSTEM_CLASS.ArrDigitalRepository,
                );

                if (hasDigitalRepository) {
                    arrParts.push(
                        <LinkContainer key="ribbon-btn-arr-daos" to={urlFundDaos(fundId, versionId)}>
                            <Button variant={'default'}>
                                <Icon glyph="fa-camera" />
                                <span className="btnText"><FormattedMessage {...messages.arrDaos} /></span>
                            </Button>
                        </LinkContainer>,
                    );
                }
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
                    <LinkContainer key="ribbon-btn-arr-back" to={fundId != null ? urlFund(fundId) : urlFund("")}>
                        <Button
                            variant={'default'}
                            className="large"
                            title={this.props.intl.formatMessage(messages.back)}
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
            parts.push(<MainNavigation key="ribbon-group-main" ref={this.ribbonDefaultFocusRef} />);
        }

        section && parts.push(section);
        altSection && parts.push(altSection);
        itemSection && parts.push(itemSection);

        const partsWithSplit = [];
        parts.forEach((part, index) => {
            partsWithSplit.push(part);
        });
        // console.log("#### parts", parts)
        const _showUser = displayUserInfo && showUser;

        return (
            <div className="ribbon-menu-container">
                <RibbonMenu>
                    {React.Children.toArray(partsWithSplit)}
                </RibbonMenu>
                {_showUser && (
                    <div className="user-menu-container">
                        <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', height: '50%' }}>
                        <ExperimentalFeature>
                          <AiAssistantRibbonButton />
                        </ExperimentalFeature>
                        <Dropdown className="user-menu" id={'user-menu'} align="end">
                            <Dropdown.Toggle key="user-menu" id="user-menu">
                                {userDetail.username} <Icon glyph="fa-user" />
                            </Dropdown.Toggle>

                            <Dropdown.Menu popperConfig={{ strategy: 'fixed' }}>
                                {userDetail.authTypes.indexOf('PASSWORD') >= 0 && [
                                    <Dropdown.Item
                                        key="pass-change"
                                        eventKey="1"
                                        onClick={this.handlePasswordChangeForm}
                                    >
                                        <FormattedMessage {...messages.passwordChange} />
                                    </Dropdown.Item>,
                                    <Dropdown.Divider key="divired" />,
                                ]}
                                {
                                    <>
                                        <Dropdown.Item eventKey="4" onClick={this.handleUserSettings}>
                                            <FormattedMessage {...messages.userSettings} />
                                        </Dropdown.Item>
                                        <Dropdown.Divider key="divider" />
                                    </>
                                }
                                <Dropdown.Item eventKey="2" onClick={this.handleLogout}>
                                    <FormattedMessage {...messages.logout} />
                                </Dropdown.Item>
                            </Dropdown.Menu>
                        </Dropdown>
                        </div>
                        {saveCounter > 0 && (
                            <div className="save-msg-container">
                                <span className="save-msg">
                                    <Icon glyph="fa-spinner fa-spin" />
                                    <FormattedMessage {...messages.saving} />
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
    const { focus, login, userDetail, status, arrRegion, app } = state;
    return {
        serializedFilter: arrRegion.funds?.[arrRegion.activeIndex]?.fundDataGrid?.serializedFilter,
        focus,
        login,
        userDetail,
        status,
        extSystemList: app.extSystemList,
    };
}

// forwardRef zůstává zachován i přes injectIntl, aby se nezměnila stávající
// sémantika connect({ forwardRef: true }).
export default connect(mapStateToProps, null, null, { forwardRef: true })(
    injectIntl(Ribbon, { forwardRef: true }),
);
