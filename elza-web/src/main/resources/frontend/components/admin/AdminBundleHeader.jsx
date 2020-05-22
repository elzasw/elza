import React from "react";
import {Table} from 'react-bootstrap';
import './AdminBundleHeader.less';

class AdminBundleHeader extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return <div className={"bundle-containter"}>
            <div className={"bundle-type"}>
                {this.props.name}
            </div>
            {this.props.data &&
            <div className={"bundle-description"}>
                <div className={"bundle-stat"}>
                    <span>Zatížení</span>
                    <span>{Math.round(this.props.data.load * 10000) / 100}%</span>
                </div>
                <div className={"bundle-stat"}>
                    <span>Požadavků za hodinu</span>
                    <span>{this.props.data.requestPerHour}</span>
                </div>
                <div className={"bundle-stat"}>
                    <span>Čekajících požadavků</span>
                    <span>{this.props.data.waitingRequests}</span>
                </div>
                <div className={"bundle-stat"}>
                    <span>Běžících vláken</span>
                    <span>{this.props.data.runningThreadCount}</span>
                </div>
                <div className={"bundle-stat"}>
                    <span>Vláken celkem</span>
                    <span>{this.props.data.totalThreadCount}</span>
                </div>
            </div>
            }
            <div className={"bundle-threads"}>
                <Table striped bordered condensed hover>
                    <thead>
                        <tr>
                            <th>Běžící vlákna</th>
                        </tr>
                    </thead>
                    <tbody>
                    {(this.props.data == null) ?
                        <tr><td>Neběží žádné vlákno</td></tr> :
                        (this.props.data.runningThreadCount > 0) ?
                            this.props.data.currentThreads.map(thread =>
                                <tr><td>id: {thread.currentId}</td></tr>):
                            <tr><td>Neběží žádné vlákno</td></tr>
                    }
                    </tbody>
                </Table>
            </div>
        </div>
    }
}

export default AdminBundleHeader;
