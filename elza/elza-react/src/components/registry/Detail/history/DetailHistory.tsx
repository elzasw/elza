import React, { useEffect, useState } from 'react';
import { connect } from 'react-redux';
import { Action } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import { WebApi } from "../../../../actions/WebApi";
import { ApStateHistoryVO } from "../../../../api/ApStateHistoryVO";
import * as Constants from '../../../../constants';
import { SimpleListActions as SimpleListAction } from '../../../../shared/list';
import { storeFromArea } from "../../../../shared/utils";
import { SimpleListStoreState } from "../../../../types";
import Icon from '../../../shared/icon/Icon';
import Loading from '../../../shared/loading/Loading';
import './DetailHistory.scss';
import DetailHistoryItem from './DetailHistoryItem';

type Props = {
    apId: number;
    commentCount?: number;
};

type AllProps = ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps> & Props;

const DetailHistory: React.FC<AllProps> = props => {
    const [collapsed, setCollapsed] = useState(false);

    const {fetchRegistryDetailHistory, apId, registryDetailHistory} = props;
    useEffect(() => {
        if (!collapsed) {
            fetchRegistryDetailHistory(apId);
        }
    }, [fetchRegistryDetailHistory, apId, collapsed]);

    const renderCollapsedContent = () => {
        return <div style={{textAlign: 'center'}} className="pt-2">
            <Icon glyph="fa-comments" size={'2x'}/>
            <h3>{props.commentCount}</h3>
        </div>;
    };

    const renderList = () => {
        const itemCount = registryDetailHistory.fetched ? registryDetailHistory.count : props.commentCount;
        return <div>
            <h3>Historie stavů ({itemCount})</h3>
            {registryDetailHistory.fetched && registryDetailHistory.rows!.map((item, index) => (
                <DetailHistoryItem key={index} historyItem={item}/>))}
            {!registryDetailHistory.fetched && <Loading/>}
        </div>
    };

    const renderExtendedContent = () => {
        return <>
            {registryDetailHistory.fetched ? renderList() : <Loading/>}
        </>;
    };

    const renderContent = () => {
        if (collapsed) {
            return renderCollapsedContent();
        } else {
            return renderExtendedContent();
        }
    };

    const renderTrigger = () => {
        let content;
        if (collapsed) {
            content = <Icon type={'left'}/>;
        } else {
            content = <><Icon type={'right'}/> Skrýt panel</>;
        }

        return <div className="brt-1 brb-1">
            {content}
        </div>;
    };

    return (
        <div
            // collapsible
            // collapsed={collapsed}
            // onCollapse={() => setCollapsed(!collapsed)}
            // width="300"
            // reverseArrow={true}
            // trigger={renderTrigger()}
            className="brl-1 history-sider"
        >
            <div className="layout-scroll">
                {renderContent()}
            </div>
        </div>
    );
};

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>) => ({
    fetchRegistryDetailHistory: (id: number) => {
        dispatch(SimpleListAction.fetchIfNeeded(
            Constants.REGISTRY_DETAIL_HISTORY,
            id,
            (id) => WebApi.findStateHistories(id).then((data) => {
                return {rows: data, count: data.length};
            })
        ));
    }
});

const mapStateToProps = (state) => ({
    registryDetailHistory: storeFromArea(state, Constants.REGISTRY_DETAIL_HISTORY) as SimpleListStoreState<ApStateHistoryVO>,
});

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(DetailHistory);
