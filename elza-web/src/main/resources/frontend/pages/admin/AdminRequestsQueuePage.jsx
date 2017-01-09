/**
 * Komponenta zobrazení požadavků ve frontě.
 */
import React from 'react';
import {connect} from 'react-redux';
import {Table, Button} from 'react-bootstrap';
import {AbstractReactComponent, i18n, Loading} from 'components/index.jsx';
import {getIndexStateFetchIfNeeded, reindex} from 'actions/admin/fulltext.jsx';
import {Ribbon, AdminPackagesList, AdminPackagesUpload} from 'components/index.jsx';
import {PageLayout} from 'pages/index.jsx';
import * as digitizationActions from 'actions/arr/digitizationActions';
import {getRequestType, DIGITIZATION, createDigitizationName} from 'components/arr/ArrUtils.jsx'
import {dateTimeToString} from "components/Utils.jsx";
import {WebApi} from 'actions/index.jsx';

const AdminRequestsQueuePage = class extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    buildRibbon() {
        return (
            <Ribbon admin {...this.props} />
        )
    }


    componentDidMount() {
        this.dispatch(digitizationActions.fetchInQueueListIfNeeded());
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(digitizationActions.fetchInQueueListIfNeeded());
    }

    handleDelete = (item) => {
        if (confirm(i18n("requestQueue.delete.confirm"))) {
            WebApi.removeArrRequestQueueItem(item.request.id);
        }
    };

    createDescription = (type, request) => {
        const {userDetail} = this.props;
        switch (type) {
            case DIGITIZATION: {
                return " - " + createDigitizationName(request, userDetail);
            }
            default:
                return "TODO [createDescription]: " + type;
        }
    };

    render() {
        const {requestInQueueList, splitter} = this.props;

        let centerPanel;
        if (requestInQueueList.fetched) {
            centerPanel = (
                <div>
                    <Table striped bordered condensed hover>
                        <thead>
                            <tr>
                                <th>{i18n("requestQueue.title.create")}</th>
                                <th>{i18n("requestQueue.title.attemptToSend")}</th>
                                <th>{i18n("requestQueue.title.description")}</th>
                                <th>{i18n("requestQueue.title.error")}</th>
                                <th>{i18n("requestQueue.title.username")}</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                        {requestInQueueList.rows.map(item => {
                            let type = getRequestType(item.request);
                            return <tr key={item.id}>
                                <td>{dateTimeToString(new Date(item.create))}</td>
                                <td>{item.attemptToSend && dateTimeToString(new Date(item.attemptToSend))}</td>
                                <td>{i18n("arr.request.title.type." + type)} {this.createDescription(type, item.request)}</td>
                                <td>{item.error}</td>
                                <td>{item.request.username}</td>
                                <td><Button onClick={() => this.handleDelete(item)}>{i18n("global.action.delete")}</Button></td>
                            </tr>
                        })}
                        </tbody>
                    </Table>
                </div>
            );
        } else {
            centerPanel = <Loading/>;
        }

        return (
            <PageLayout
                splitter={splitter}
                className='admin-requestsQueue-page'
                ribbon={this.buildRibbon()}
                centerPanel={centerPanel}
            />
        )
    }
};

/**
 * Namapování state do properties.
 *
 * @param state state aplikace
 * @returns {{packages: *}}
 */
function mapStateToProps(state) {
    const {app:{requestInQueueList}, splitter, userDetail} = state;
    return {
        splitter,
        requestInQueueList,
        userDetail
    }
}

export default connect(mapStateToProps)(AdminRequestsQueuePage);
