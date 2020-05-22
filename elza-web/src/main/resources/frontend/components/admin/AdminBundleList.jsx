import React from "react";
import {
    AbstractReactComponent,
    CollapsablePanel
} from 'components/shared';
import {WebApi} from 'actions/index.jsx';
import AdminBundleHeader from "./AdminBundleHeader";
import AdminBundleBody from "./AdminBundleBody";

import './AdminBundleList.less';

class AdminBundleList extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.stop = false;
        this.state = {
            fetched: false,
            asyncRequest: []
        };
    }

    componentDidMount() {
        WebApi.getAsyncRequestInfo().then(
            data => {
                this.setState({asyncRequest: data, fetched: true}, () => {
                    this.refresh();
                });
            }
        )
    }

    componentWillUnmount() {
        this.stop = true;
    }

    refresh = () => {
        if (this.stop) {
            return;
        }

        WebApi.getAsyncRequestInfo().then(
            newData => {
                this.setState({
                    ...this.state,
                    fetched: true,
                    asyncRequest: newData
                }, () => {
                    setTimeout(this.refresh, 10000);
                });
            }
        )
    }

    render() {
        let {fetched, asyncRequest} = this.state;

        return <div>
            <CollapsablePanel tabIndex={0} header={<AdminBundleHeader name={"Validace JP"} data={asyncRequest[0]} />}>
                <AdminBundleBody type={"NODE"}/>
            </CollapsablePanel>

            <CollapsablePanel tabIndex={1} header={<AdminBundleHeader name={"Hromadné akce"} data={asyncRequest[1]} />}>
                <AdminBundleBody  type={"BULK"}/>
            </CollapsablePanel>

            <CollapsablePanel tabIndex={2} header={<AdminBundleHeader name={"Výstupy"} data={asyncRequest[2]} />}>
                <AdminBundleBody  type={"OUTPUT"}/>
            </CollapsablePanel>
        </div>
    }
}

export default AdminBundleList;
