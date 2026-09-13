import React from "react";
import {WebApi} from 'actions/index.jsx';
import './AdminBulkBody.scss';
import { FormattedMessage, defineMessages } from "react-intl";

// Id je převzaté z legacy katalogu beze změny.
const messages = defineMessages({
    queueEmpty: { id: 'admin.bulk.detail.queue.empty', defaultMessage: 'Ve frontě nejsou žádné požadavky' },
});

class AdminBulkBody extends React.Component {
    state = {
        fetched: false,
        asyncRequestDetail: []
    };

    componentDidMount() {
        this.refresh();
    }

    componentWillUnmount() {
        clearTimeout(this.timer);
    }

    refresh = () => {
        WebApi.getAsyncRequestDetail(this.props.type).then(newData => {
            this.setState({fetched: true, asyncRequestDetail: newData});
            this.timer = setTimeout(this.refresh, 10000);
        });
    }

    render() {
        return <div>
            {
                (this.state.asyncRequestDetail.length > 0) ?
                    this.state.asyncRequestDetail.map((row) => (
                         <div className={"bulk-row"}>
                             {row.fund &&
                                 <div className={"bulk-detail"}>
                                     <b title={row.fund.name + " (id: " + row.fund.id + ")"}>{row.fund.name}: </b>
                                     {row.requestCount}x
                                 </div>
                             }
                        </div>
                    )) :
                    <div className={"bulk-detail"}>
                        <b><FormattedMessage {...messages.queueEmpty} /></b>
                    </div>
            }
        </div>
    }
}


export default AdminBulkBody;
