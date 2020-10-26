import React from 'react';
import ReactDOM from 'react-dom';

import {connect} from 'react-redux';
import {LinkContainer} from 'react-router-bootstrap';
import {FundForm, i18n, Icon, Ribbon} from 'components/index.jsx';
import {AbstractReactComponent, RibbonGroup, Utils} from 'components/shared';
import {Button} from '../../components/ui';
import {modalDialogShow} from 'actions/global/modalDialog.jsx';
import {createFund} from 'actions/arr/fund.jsx';
import {storeLoadData} from 'actions/store/store.jsx';
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx';
import * as perms from 'actions/user/Permission.jsx';
import PageLayout from '../shared/layout/PageLayout';
import './HomePage.scss';
import {FOCUS_KEYS} from '../../constants.tsx';
import SearchFundsForm from '../../components/arr/SearchFundsForm';
import {WebApi} from "../../actions/WebApi";

// Testování
// import AutocompleteTest from "./test/AutocompleteTest";

/**
 * Home stránka
 */
class HomePage extends AbstractReactComponent {
    UNSAFE_componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps);
    }

    componentDidMount() {
        this.trySetFocus(this.props);
    }

    trySetFocus = props => {
        const {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, null, 1)) {
                // focus po ztrátě
                if (this.refs.list) {
                    // ještě nemusí existovat
                    this.setState({}, () => {
                        const listEl = ReactDOM.findDOMNode(this.refs.list);
                        Utils.setInputFocus(listEl, false);
                        focusWasSet();
                    });
                }
            } else if (isFocusFor(focus, FOCUS_KEYS.HOME, 1) || isFocusFor(focus, FOCUS_KEYS.HOME, 1, 'list')) {
                this.setState({}, () => {
                    const listEl = ReactDOM.findDOMNode(this.refs.list);
                    Utils.setInputFocus(listEl, false);
                    focusWasSet();
                });
            }
        }
    };

    /**
     * Vyvolání dialogu s vyhledáním na všemi AS.
     */
    handleFundsSearchForm = () => {
        this.props.dispatch(modalDialogShow(this, i18n('arr.fund.title.search'), <SearchFundsForm />));
    };

    handleAddFund = () => {
        const {userDetail} = this.props;
        let initData = {};
        if (!userDetail.hasOne(perms.ADMIN, perms.FUND_ADMIN)) {
            initData.fundAdmins = [{id: 'default', user: userDetail}];
        }
        WebApi.getAllScopes().then(scopes => {
            this.props.dispatch(
                modalDialogShow(
                    this,
                    i18n('arr.fund.title.add'),
                    <FundForm
                        create
                        initialValues={initData}
                        scopeList={scopes}
                        onSubmitForm={data => {
                            return this.props.dispatch(createFund(data));
                        }}
                    />,
                ),
            );
        });
    };

    buildRibbon = () => {
        const {userDetail} = this.props;
        const altActions = [];
        if (userDetail.hasOne(perms.FUND_ADMIN, perms.FUND_CREATE)) {
            altActions.push(
                <Button key="add-fa" onClick={this.handleAddFund}>
                    <Icon glyph="fa-plus-circle" />
                    <div>
                        <span className="btnText">{i18n('ribbon.action.arr.fund.add')}</span>
                    </div>
                </Button>,
            );
        }

        altActions.push(
            <Button key="search-fa" onClick={this.handleFundsSearchForm}>
                <Icon glyph="fa-search" />
                <div>
                    <span className="btnText">{i18n('ribbon.action.arr.fund.search')}</span>
                </div>
            </Button>,
        );

        let altSection;
        if (altActions.length > 0) {
            altSection = (
                <RibbonGroup className="small" key="ribbon-group-home">
                    {altActions}
                </RibbonGroup>
            );
        }

        return <Ribbon ref="ribbon" home altSection={altSection} {...this.props} />;
    };

    renderHistoryItem = (name, desc, type, data, keyIndex) => {
        let glyph;
        switch (type) {
            case 'REGISTRY_REGION':
                glyph = 'fa-th-list';
                break;
            case 'ARR_REGION':
                glyph = 'fa-file-text';
                break;
            case 'ARR_REGION_FUND':
                glyph = 'fa-file-text';
                break;
            default:
                break;
        }

        const hasDesc = desc && desc.length > 0;

        let descComp;
        if (hasDesc) {
            descComp = <small>{desc}</small>;
        } else {
            descComp = <small>&nbsp;</small>;
        }

        return (
            <Button
                className="history-list-item history-button"
                onClick={() => this.props.dispatch(storeLoadData(type, data))}
                key={'button-' + keyIndex}
            >
                <Icon glyph={glyph} />
                <div className="history-name">{name}</div>
                {false && descComp}
            </Button>
        );
    };

    arrToString = arr => {
        return arr.map((d, index) => {
            if (index > 0 && index < arr.length) {
                return ',  ' + d;
            } else {
                return d;
            }
        });
    };

    getFundDesc = fund => {
        const descs = fund.nodes.nodes.map(nodeobj => nodeobj.name);
        return this.arrToString(descs);
    };

    renderHistory = () => {
        const {stateRegion} = this.props;
        //eslint-disable-next-line array-callback-return
        const registryItems = stateRegion.registryRegionFront.map((x, index) => {
            if (x.data) {
                const name = x.data.record;
                const desc = x.data.characteristics;
                return this.renderHistoryItem(name, desc, 'REGISTRY_REGION', x, index);
            }
        });
        const arrItems = stateRegion.arrRegionFront.map((x, index) => {
            const name = x.name + (x.lockDate ? ' ' + Utils.dateToString(new Date(x.lockDate)) : '');
            const desc = this.getFundDesc(x);
            return this.renderHistoryItem(name, desc, 'ARR_REGION_FUND', x, index);
        });

        if (arrItems.length === 0) {
            arrItems.push(
                this.renderMessage(
                    i18n('home.recent.fund.emptyList.title'),
                    i18n('home.recent.fund.emptyList.message'),
                ),
            );
        }
        if (registryItems.length === 0) {
            registryItems.push(
                this.renderMessage(
                    i18n('home.recent.registry.emptyList.title'),
                    i18n('home.recent.registry.emptyList.message'),
                ),
            );
        }

        arrItems.push(this.renderLink('/fund', i18n('home.recent.fund.goTo')));
        registryItems.push(this.renderLink('/registry', i18n('home.recent.registry.goTo')));

        return (
            <div ref="list" className="history-list-container">
                <div className="button-container">
                    <h4>{i18n('home.recent.fund.title')}</h4>
                    <div className="section">{arrItems}</div>
                    <h4>{i18n('home.recent.registry.title')}</h4>
                    <div className="section">{registryItems}</div>
                </div>
            </div>
        );
    };

    /**
     * Vykreslení informace o prázné historii
     */
    renderMessage = (title, message) => (
        <div key="blank" className="unselected-msg history-list-item no-history">
            <div className="title">{title}</div>
            <div className="message">{message}</div>
        </div>
    );

    /**
     * Vykreslení odkazu do příslušných modulů
     */
    renderLink = (to, text, glyph = 'fa-arrow-right') => (
        <LinkContainer key={to} to={to} className="history-list-item history-button link">
            <Button>
                <Icon glyph={glyph} />
                <div className="history-name">{text}</div>
            </Button>
        </LinkContainer>
    );

    render() {
        // Test komponent - jen pro vývojové účely
        // return <AutocompleteTest/>

        const {splitter} = this.props;

        let centerPanel = <div className="splitter-home">{this.renderHistory()}</div>;

        return <PageLayout splitter={splitter} ribbon={this.buildRibbon()} centerPanel={centerPanel} />;
    }
}

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, stateRegion, focus, userDetail} = state;
    return {
        splitter,
        arrRegion,
        refTables,
        stateRegion,
        focus,
        userDetail,
    };
}

export default connect(mapStateToProps)(HomePage);
