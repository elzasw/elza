import React from "react";
import {Table} from 'react-bootstrap';
import './AdminBulkHeader.scss';
import i18n from "../i18n";
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
                    <span>{i18n('admin.bulk.title.load')}</span>
                    <span>{Math.round(data.load * 10000) / 100}%</span>
                </div>
                <div className={"bulk-stat"}>
                    <span>{i18n('admin.bulk.title.requestPerHour')}</span>
                    <span>{data.requestPerHour}</span>
                </div>
                <div className={"bulk-stat"}>
                    <span>{i18n('admin.bulk.title.waitingRequests')}</span>
                    <span>{data.waitingRequests}</span>
                </div>
                <div className={"bulk-stat"}>
                    <span>{i18n('admin.bulk.title.runningThreadCount')}</span>
                    <span>{data.runningThreadCount}</span>
                </div>
                <div className={"bulk-stat"}>
                    <span>{i18n('admin.bulk.title.totalThreadCount')}</span>
                    <span>{data.totalThreadCount}</span>
                </div>
            </div>
            }
            <div className={"bulk-threads"}>
                <Table striped bordered condensed hover>
                    <thead>
                        <tr>
                            <th colSpan={3}>{i18n('admin.bulk.title.runningThreads')}</th>
                        </tr>
                    </thead>
                    <tbody>
                    {(data == null || data.currentThreads.length === 0) ?
                        <tr>
                            <td colSpan={3}>{i18n('admin.bulk.title.noRunningThread')}</td>
                        </tr> :
                        data.currentThreads.map(thread => {
                            let beginTime = localUTCToDateTime(thread.beginTime);
                            beginTime = beginTime == null ? thread.beginTime : dateToDateTimeString(beginTime);
                            return <tr>
                                <td>{i18n('admin.bulk.title.beginTime', beginTime)}</td>
                                <td>{i18n('admin.bulk.title.requestId', thread.requestId)}</td>
                                <td>{i18n('admin.bulk.title.currentId', thread.currentId)}</td>
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
