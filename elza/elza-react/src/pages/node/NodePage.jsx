/**
 * Stránka pro přesměrování na konkrétní JP.
 */

import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent} from 'components/shared';
import PageLayout from '../shared/layout/PageLayout';
import Ribbon from '../../components/page/Ribbon';
import {fundsSelectFund} from '../../actions/fund/fund';
import {createFundRoot, getFundFromFundAndVersion} from '../../components/arr/ArrUtils';
import {selectFundTab} from '../../actions/arr/fund';
import {WebApi} from '../../actions';
import {routerNavigate} from '../../actions/router';
import {fundSelectSubNode} from '../../actions/arr/node';
import Loading from '../../components/shared/loading/Loading';
import './NodePage.scss';

class NodePage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {
            fetching: false,
        };
    }

    UNSAFE_componentWillReceiveProps(props) {}

    waitForLoadAS = fce => {
        const next = fce();
        if (next) {
            setTimeout(() => {
                this.waitForLoadAS(fce);
            }, 50);
        }
    };

    componentDidMount() {
        const uuid = this.props.match.params.uuid;
        console.info('Select JP: ' + uuid);

        this.setState({fetching: true});
        WebApi.selectNode(uuid)
            .then(data => {
                const fund = data.fund;
                this.props.dispatch(fundsSelectFund(fund.id));
                const fundVersion = fund.versions.find(v => !v.lockDate);
                this.props.dispatch(routerNavigate('/'));
                this.props.dispatch(routerNavigate('/arr'));
                const fundObj = getFundFromFundAndVersion(fund, fundVersion);
                this.props.dispatch(selectFundTab(fundObj));

                this.waitForLoadAS(() => {
                    let arrRegion = this.props.arrRegion;
                    this.props.dispatch((dispatch, getState) => {
                        arrRegion = getState().arrRegion; // aktuální stav ve store
                    });

                    const selectFund = arrRegion.funds[arrRegion.activeIndex];

                    if (selectFund.fundTree.fetched) {
                        // čekáme na načtení stromu, potom můžeme vybrat JP
                        const nodeWithParent = data.nodeWithParent;
                        const node = nodeWithParent.node;
                        let parentNode = nodeWithParent.parentNode;
                        if (parentNode == null) {
                            // root
                            parentNode = createFundRoot(selectFund);
                        }
                        this.props.dispatch(fundSelectSubNode(fundVersion.id, node.id, parentNode, false, null, false));
                        return false;
                    } else {
                        return true;
                    }
                });
            })
            .catch(error => {
                this.setState({message: error.message});
            })
            .finally(() => {
                this.setState({fetching: false});
            });
    }

    buildRibbon = () => {
        return <Ribbon ref="ribbon" {...this.props} />;
    };

    render() {
        const {splitter} = this.props;
        const {fetching, message} = this.state;
        return (
            <PageLayout
                splitter={splitter}
                className="node-page"
                ribbon={this.buildRibbon()}
                centerPanel={
                    <div className="content">
                        {fetching && <Loading />}
                        {message && <h2>{message}</h2>}
                    </div>
                }
            />
        );
    }
}

function mapStateToProps(state) {
    const {focus, splitter, arrRegion, userDetail} = state;

    return {
        focus,
        splitter,
        arrRegion,
        userDetail,
    };
}

export default connect(mapStateToProps)(NodePage);
