import './LinkedNodes.scss';

import PropTypes from 'prop-types';

import React from 'react';
import {AbstractReactComponent} from 'components/shared';
import { FormattedMessage } from 'react-intl';
import { daoMessages } from 'components/arr/daoMessages';
import {connect} from 'react-redux';
import {WebApi} from "../../actions/WebApi";
import NodeLabel from "./NodeLabel";
import {routerNavigate} from "../../actions/router";

class LinkedNodes extends AbstractReactComponent {

    constructor(props) {
        super(props);
        this.state = {
            isFetching: false,
            data: []
        }
    }

    static propTypes = {
        versionId: PropTypes.number.isRequired,
        nodeId: PropTypes.number.isRequired,
    };

    componentDidMount() {
        const {versionId, nodeId} = this.props;
        this.setState({isFetching: true})
        WebApi.findLinkedNodes(versionId, nodeId)
            .then(data => {
                this.setState({data})
            })
            .finally(() => {
                this.setState({isFetching: false})
            })
    }

    handleNavigate = (uuid) => {
        this.props.dispatch(routerNavigate(`/node/${uuid}`));
    }

    render() {
        const {nodeId, versionId} = this.props;
        const {isFetching, data} = this.state;

        if (data.length === 0) {
            return <></>;
        }

        return <div className="linked-nodes">
            <div className="linked-nodes-title">{<FormattedMessage {...daoMessages.linkedNodesTitle} />}</div>
            {data.map((node, index) => <div className="node" onClick={() => this.handleNavigate(node.nodeUuid)}
                                            key={index}>
                <NodeLabel inline node={node}/>
                <span className="fund">{node.fundName}</span>
            </div>)}
        </div>
    }
}

export default connect()(LinkedNodes);
