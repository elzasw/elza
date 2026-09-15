import React from "react";
import {Table} from 'react-bootstrap';
import './AdminBulkHeader.scss';
import { FormattedMessage, defineMessages } from "react-intl";

// Id jsou převzatá z legacy katalogu beze změny. Placeholder {0} zůstává:
// ICU bere jako jméno argumentu i číslo.
const messages = defineMessages({
    load: { id: 'admin.bulk.title.load', defaultMessage: 'Zatížení' },
    requestPerHour: { id: 'admin.bulk.title.requestPerHour', defaultMessage: 'Požadavků za hodinu' },
    waitingRequests: { id: 'admin.bulk.title.waitingRequests', defaultMessage: 'Čekajících požadavků' },
    runningThreadCount: { id: 'admin.bulk.title.runningThreadCount', defaultMessage: 'Běžících vláken' },
    totalThreadCount: { id: 'admin.bulk.title.totalThreadCount', defaultMessage: 'Vláken celkem' },
    runningThreads: { id: 'admin.bulk.title.runningThreads', defaultMessage: 'Běžící vlákna' },
    noRunningThread: { id: 'admin.bulk.title.noRunningThread', defaultMessage: 'Neběží žádné vlákno' },
    beginTime: { id: 'admin.bulk.title.beginTime', defaultMessage: 'Běží od {0}' },
    requestId: { id: 'admin.bulk.title.requestId', defaultMessage: 'ID fronty #{0}' },
    currentId: { id: 'admin.bulk.title.currentId', defaultMessage: 'ID vazby #{0}' },
});
import {dateToDateTimeString, localUTCToDateTime} from "../../shared/utils/commons";

class AdminBulkHeader extends React.Component {

    render() {
        const {data, name} = this.props;

        return <div className={"bulk-containter"}>
            <div className={"bulk-type"}>
                {name}
            </div>
            {data &&
            <div className={"bulk-description"}>
                <div className={"bulk-stat"}>
                    <span><FormattedMessage {...messages.load} /></span>
                    <span>{Math.round(data.load * 10000) / 100}%</span>
                </div>
                <div className={"bulk-stat"}>
                    <span><FormattedMessage {...messages.requestPerHour} /></span>
                    <span>{data.requestPerHour}</span>
                </div>
                <div className={"bulk-stat"}>
                    <span><FormattedMessage {...messages.waitingRequests} /></span>
                    <span>{data.waitingRequests}</span>
                </div>
                <div className={"bulk-stat"}>
                    <span><FormattedMessage {...messages.runningThreadCount} /></span>
                    <span>{data.runningThreadCount}</span>
                </div>
                <div className={"bulk-stat"}>
                    <span><FormattedMessage {...messages.totalThreadCount} /></span>
                    <span>{data.totalThreadCount}</span>
                </div>
            </div>
            }
            <div className={"bulk-threads"}>
                <Table striped bordered condensed hover>
                    <thead>
                        <tr>
                            <th colSpan={3}><FormattedMessage {...messages.runningThreads} /></th>
                        </tr>
                    </thead>
                    <tbody>
                    {(data == null || data.currentThreads.length === 0) ?
                        <tr>
                            <td colSpan={3}><FormattedMessage {...messages.noRunningThread} /></td>
                        </tr> :
                        data.currentThreads.map(thread => {
                            let beginTime = localUTCToDateTime(thread.beginTime);
                            beginTime = beginTime == null ? thread.beginTime : dateToDateTimeString(beginTime);
                            return <tr>
                                <td><FormattedMessage {...messages.beginTime} values={{ 0: beginTime }} /></td>
                                <td><FormattedMessage {...messages.requestId} values={{ 0: thread.requestId }} /></td>
                                <td><FormattedMessage {...messages.currentId} values={{ 0: thread.currentId }} /></td>
                            </tr>;
                        })
                    }
                    </tbody>
                </Table>
            </div>
        </div>
    }
}

export default AdminBulkHeader;
