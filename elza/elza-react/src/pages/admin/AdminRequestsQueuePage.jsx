/**
 * Komponenta zobrazení požadavků ve frontě.
 */
import React from 'react';
import {connect} from 'react-redux';
import {Table} from 'react-bootstrap';
import {Button} from '../../components/ui';
import {AbstractReactComponent, i18n, StoreHorizontalLoader} from 'components/shared';
import {Ribbon} from 'components/index.jsx';
import PageLayout from '../shared/layout/PageLayout';
import * as arrRequestActions from 'actions/arr/arrRequestActions';
import {
    createDaoLinkName,
    createDigitizationName,
    DAO,
    DAO_LINK,
    DIGITIZATION,
    getRequestType,
} from 'components/arr/ArrUtils.jsx';
import {dateTimeToString} from 'components/Utils.jsx';
import {WebApi} from 'actions/index.jsx';

class AdminRequestsQueuePage extends AbstractReactComponent {
    buildRibbon() {
        return <Ribbon admin {...this.props} />;
    }

    componentDidMount() {
        this.props.dispatch(arrRequestActions.fetchInQueueListIfNeeded());
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(arrRequestActions.fetchInQueueListIfNeeded());
    }

    handleDelete = item => {
        if (window.confirm(i18n('requestQueue.delete.confirm'))) {
            WebApi.removeArrRequestQueueItem(item.request.id);
        }
    };

    createDescription = (type, request) => {
        const {userDetail} = this.props;
        switch (type) {
            case DIGITIZATION: {
                return ' - ' + createDigitizationName(request, userDetail);
            }
            case DAO_LINK: {
                return ' - ' + createDaoLinkName(request, userDetail);
            }
            case DAO: {
                return ' - ' + i18n('arr.request.title.type.dao.' + request.type);
            }
            default:
                return 'Unknown type: ' + type;
        }
    };

    render() {
        const {requestInQueueList, splitter} = this.props;

        let centerPanel = (
            <div>
                <StoreHorizontalLoader store={requestInQueueList} />
                {requestInQueueList.fetched && (
                    <Table striped bordered condensed hover>
                        <thead>
                            <tr>
                                <th>{i18n('requestQueue.title.create')}</th>
                                <th>{i18n('requestQueue.title.attemptToSend')}</th>
                                <th>{i18n('requestQueue.title.description')}</th>
                                <th>{i18n('requestQueue.title.error')}</th>
                                <th>{i18n('requestQueue.title.username')}</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            {requestInQueueList.rows.map(item => {
                                let type = getRequestType(item.request);
                                return (
                                    <tr key={item.id}>
                                        <td>{dateTimeToString(new Date(item.create))}</td>
                                        <td>{item.attemptToSend && dateTimeToString(new Date(item.attemptToSend))}</td>
                                        <td>
                                            {i18n('arr.request.title.type.' + type)}{' '}
                                            {this.createDescription(type, item.request)}
                                        </td>
                                        <td>{item.error}</td>
                                        <td>{item.request.username}</td>
                                        <td>
                                            <Button onClick={() => this.handleDelete(item)}>
                                                {i18n('global.action.delete')}
                                            </Button>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </Table>
                )}
            </div>
        );

        return (
            <PageLayout
                splitter={splitter}
                className="admin-requestsQueue-page"
                ribbon={this.buildRibbon()}
                centerPanel={centerPanel}
            />
        );
    }
}

/**
 * Namapování state do properties.
 *
 * @param state state aplikace
 * @returns {{packages: *}}
 */
function mapStateToProps(state) {
    const {
        app: {requestInQueueList},
        splitter,
        userDetail,
    } = state;
    return {
        splitter,
        requestInQueueList,
        userDetail,
    };
}

export default connect(mapStateToProps)(AdminRequestsQueuePage);
