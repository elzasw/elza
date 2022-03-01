/**
 * Stránka pro přesměrování na konkrétní Archivní entitu.
 */

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n} from '../../components/shared';
import PageLayout from "../shared/layout/PageLayout";
import Ribbon from "../../components/page/Ribbon";
import Loading from "../../components/shared/loading/Loading";
import './EntityPage.less';
import {WebApi} from "../../actions";
import {withRouter} from "react-router";
import {goToAe} from "../../actions/registry/registry";

class EntityPage extends AbstractReactComponent {

    constructor(props) {
        super(props);
        this.state = {
            fetching: false,
            message: undefined,
        }
    }

    componentDidMount() {
        const {dispatch, match, history} = this.props;
        const uuid = match.params.uuid;

        console.info('Select AE: ', uuid);

        this.setState({fetching: true});
        WebApi.getAccessPoint(uuid)
            .then(data => {
                console.info('Select AE: ', data);
                dispatch(goToAe(history, data.id));
            })
            .catch(() => {
                // zobrazeni chybove hlasky pokud entita nebyla nalezena
                this.setState({
                    message: i18n("registry.entity.notFound")
                });
            })
            .finally(() => {
                this.setState({
                    fetching: false,
                });
            });
    }

    buildRibbon = () => {
        return (
            <Ribbon ref='ribbon' {...this.props} />
        )
    };

    render() {
        const {splitter} = this.props;
        const {fetching, message} = this.state;
        return (
            <PageLayout
                splitter={splitter}
                className='entity-page'
                ribbon={this.buildRibbon()}
                centerPanel={<div className="content">
                    {fetching && <Loading/>}
                    {message && <h2>{message}</h2>}
                </div>}
            />
        )
    }
}

function mapStateToProps(state) {
    const {focus, splitter} = state;
    return {
        focus,
        splitter,
    }
}

export default withRouter(connect(mapStateToProps)(EntityPage));
