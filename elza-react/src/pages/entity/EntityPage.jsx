/**
 * Stránka pro přesměrování na konkrétní Archivní entitu.
 */

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent} from '../../components/shared';
import PageLayout from "../shared/layout/PageLayout";
import Ribbon from "../../components/page/Ribbon";
import {routerNavigate} from "../../actions/router";
import {registryDetailFetchIfNeeded} from '../../actions/registry/registry'
import Loading from "../../components/shared/loading/Loading";
import './EntityPage.less';
import {i18n} from '../../components/shared';

class EntityPage extends AbstractReactComponent {

    constructor(props) {
        super(props);
        this.state = {
            fetching: false,
            message: undefined,
        }
    }

    componentDidMount() {
        const { dispatch, match} = this.props;
        const uuid = match.params.uuid;

        // vybrani pozadovane archivni entity
        dispatch(registryDetailFetchIfNeeded(uuid)).then(()=>{
            // presmerovani na stranku s archivnimi entitami
            dispatch(routerNavigate('/registry'));
        }).catch(()=>{
            // zobrazeni chybove hlasky pokud entita nebyla nalezena
            this.setState({
                message: i18n("registry.entity.notFound")
            });
        }).finally(()=>{
            this.setState({
                fetching: false,
            });
        })
        this.setState({fetching: true});
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

export default connect(mapStateToProps)(EntityPage);
