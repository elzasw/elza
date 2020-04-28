import { Col, Row } from 'react-bootstrap';
import React, { useEffect, useState } from 'react';
import DetailHistoryItem from './DetailHistoryItem';
import { ThunkDispatch } from 'redux-thunk';
import { Action } from 'redux';
import { SimpleListActions as SimpleListAction } from '../../../shared/list';
import { connect } from 'react-redux';
import { AeStateHistoryVO } from '../../../api/generated/model';
import './DetailHistory.scss';
//import * as EntitiesClientApiCall from '../../api/call/EntitiesClientApiCall';
import Icon from '../../shared/icon/Icon';
import Loading from '../../shared/loading/Loading';

type Props = {
  entityId: number;
  commentCount?: number;
};

type AllProps = ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps> & Props;

const DetailHistory: React.FC<AllProps> = props => {
  const [collapsed, setCollapsed] = useState(true);

  const { fetchHistoryList, entityId, historyList } = props;
  useEffect(() => {
    if (!collapsed) {
      fetchHistoryList(entityId);
    }
  }, [fetchHistoryList, entityId, collapsed]);

  const [isFetching, setIsFetching] = useState(false);
  const [isListFetched, setIsListFetched] = useState(false);

  useEffect(() => {
    setIsFetching(!historyList.fetched || historyList.isFetching);
    setIsListFetched(historyList.fetched);
  }, [historyList.fetched, historyList.isFetching]);


  const renderCollapsedContent = () => {
    return <div style={{ textAlign: 'center' }} className="pt-2">
      <Icon glyph="fa-coments" size={'2x'}/>
      <h3>{props.commentCount}</h3>
    </div>;
  };

  const renderList = () => {
    const itemCount = isListFetched ? historyList.count : props.commentCount;
    return <div
      header={<Row className="p-2">
        <Col className="pr-2">
          <Icon glyph="fa-coments" size={'2x'}/>
        </Col>
        <Col>
          <h3>Historie stavů ({itemCount})</h3>
        </Col>
      </Row>}
      dataSource={historyList.rows}
      renderItem={item => (<DetailHistoryItem historyItem={item as AeStateHistoryVO}/>)}
      loading={!isListFetched}
    />;
  };

  const renderExtendedContent = () => {
    return <>
      {isListFetched ? renderList() : <Loading/>}
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
      collapsible
      collapsed={collapsed}
      onCollapse={() => setCollapsed(!collapsed)}
      width="300"
      reverseArrow={true}
      trigger={renderTrigger()}
      className="brl-1 history-sider"
    >
      <div className="layout-scroll">
        {renderContent()}
      </div>
    </div>
  );
};

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>) => ({
  fetchHistoryList: (id: number) => {
    /*dispatch(
      SimpleListAction.fetchIfNeeded(Constants.DETAIL_HISTORY_LIST, id, (id) =>
          EntitiesClientApiCall.standardApi
            .getStateHistory(id)
            .then(x => x.data)
      )
    );*/
  }
});

const mapStateToProps = ({ app }: any) => ({
  historyList: {} as any//app[Constants.DETAIL_HISTORY_LIST]
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DetailHistory);
