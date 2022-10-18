import { createFund } from 'actions/arr/fund.jsx';
import { canSetFocus, focusWasSet, isFocusFor } from 'actions/global/focus.jsx';
import { modalDialogShow } from 'actions/global/modalDialog.jsx';
import * as perms from 'actions/user/Permission.jsx';
import { Api } from 'api';
import { FundForm, i18n, Icon, Ribbon } from 'components/index.jsx';
import { AbstractReactComponent, RibbonGroup, TooltipTrigger, Utils } from 'components/shared';
import React from 'react';
import ReactDOM from 'react-dom';
import { connect } from 'react-redux';
import { LinkContainer } from 'react-router-bootstrap';
import { WebApi } from "../../actions/WebApi";
import SearchFundsForm from '../../components/arr/SearchFundsForm';
import { Button } from '../../components/ui';
import { FOCUS_KEYS, urlFundTree, URL_ENTITY } from '../../constants.tsx';
import PageLayout from '../shared/layout/PageLayout';
import './HomePage.scss';


// Testování
// import AutocompleteTest from "./test/AutocompleteTest";

const truncateStringWithTooltip = (string, length, maxWidth = "13em") => {
    if(string.length <= length){
        return string;
    }
    return <TooltipTrigger content={<div style={{maxWidth}}>{string}</div>} placement='vertical'>
        {`${string.slice(0, length-3).trim()}...`}
    </TooltipTrigger>
}

const FundItem = ({fundDetail, version}) => {
    const name = truncateStringWithTooltip(fundDetail.name, 90)

    return <LinkContainer to={urlFundTree(fundDetail.id, version.lockDate === null ? undefined : version.id)} className="history-list-item history-button">
        <Button>
            <div className="background-text-container">
                {/* <Icon glyph='fa-database' /> */}
                <div className="background-text">{fundDetail.name}</div>
            </div>
            <div className="fund-content">
                <div className="history-name">
                    {name}
                </div>
                <div className="desc-container">
                    <>
                        <div className="fund-desc-container">
                            {fundDetail.mark && <div className="fund-desc-item" >
                                {fundDetail.mark}
                            </div>}
                            <div className="fund-desc-item version" >
                                {version.lockDate && <>
                                    <Icon glyph={'fa-lock'}/> Verze {new Date(version.lockDate).toLocaleString()}
                                    </>}
                            </div>
                        </div>
                        </>
                </div>
                <div className="fund-label">
                    {[fundDetail.fundNumber, fundDetail.internalCode].filter((item) => item).join(", ")}
                    &nbsp;
                </div>
            </div>
        </Button>
    </LinkContainer>
}

const EntityItem = ({entity}) => {
    return (
        <TooltipTrigger 
            style={{zIndex: 2, display: "inline-block"}} 
            content={entity.data.description 
                && <div style={{maxWidth: "13em"}} >
                    {entity.data.description}
                </div>} 
            placement="vertical"
        >
            <LinkContainer 
                to={`/entity/${entity.id}`} 
                className="history-list-item history-button" 
            >
                <Button>
                    <div className="background-text-container">
                        {/* <Icon glyph='fa-th-list' /> */}
                        <div className="background-text">{entity.data.name}</div>
                    </div>
                    <div style={{zIndex: 2}} className="history-name">
                        {truncateStringWithTooltip(entity.data.name, 120)}
                    </div>
                </Button>
            </LinkContainer>
        </TooltipTrigger>
    )
}

/**
 * Home stránka
 */
class HomePage extends AbstractReactComponent {
    state = {
        fundDetails: []
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps);
    }

    componentDidMount() {
        const funds = this.props.stateRegion?.arrRegionFront;
        if(funds?.length > 0){
            Promise.all(funds.map((fund) => Api.funds.getFund(fund.id, {overrideErrorHandler: true})
                .catch(()=>{return undefined;})))
                .then((responses)=>{
                    const fundDetails = responses.filter((response) => response != undefined).map((response) => response.data);
                    this.setState({ fundDetails })
                });
        }
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

    renderHistory = () => {
        const {registryRegionFront, arrRegionFront} = this.props.stateRegion;
        const {fundDetails} = this.state;

        const registryItems = registryRegionFront.map((item) => {
            if (item.data) {
                return <EntityItem entity={item}/>;
            }
            return <></>
        });

        const arrItems = [];
        arrRegionFront.forEach(({activeVersion, id}) => {
            const item = fundDetails.find((fund) => fund.id === id);
            if(item){
                arrItems.push(<FundItem fundDetail={item} version={activeVersion}/>);
            }
        })

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
