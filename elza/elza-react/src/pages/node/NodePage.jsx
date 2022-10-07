/**
 * Stránka pro přesměrování na konkrétní JP.
 */

import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent} from 'components/shared';
import PageLayout from '../shared/layout/PageLayout';
import Ribbon from '../../components/page/Ribbon';
import {WebApi} from '../../actions';
import Loading from '../../components/shared/loading/Loading';
import './NodePage.scss';
import {processNodeNavigation} from "../../utils/ArrShared";

class NodePage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {
            fetching: false,
        };
    }

    UNSAFE_componentWillReceiveProps(props) {}

    componentDidMount() {
        const uuid = this.props.match.params.uuid;
        console.info('Select JP: ' + uuid);

        this.setState({fetching: true});
        WebApi.selectNode(uuid)
            .then(data => processNodeNavigation(this.props.dispatch, data, this.props.arrRegion))
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
