/**
 * Router, který reaguje na akce přepnutí routeru.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent} from 'components/index.jsx';
import {routerNavigateFinish} from 'actions/router.jsx'

var AppRouter = class AppRouter extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('changeRouteIfNeeded');
    }
    
    componentDidMount() {
        this.changeRouteIfNeeded(this.props.router);
    }

    componentWillReceiveProps(nextProps) {
        this.changeRouteIfNeeded(nextProps.router);
    }

    changeRouteIfNeeded(router) {
        if (router.navigateTo) {
            this.context.router.push(router.navigateTo);
            this.dispatch(routerNavigateFinish());
        }
    }

    render() {
        return (
            <div style={{display: 'none'}}>
            </div>
        )
    }
}

AppRouter.contextTypes = {
    router: React.PropTypes.object.isRequired
}

function mapStateToProps(state) {
    const {router} = state
    return {
        router,
    }
}

module.exports = connect(mapStateToProps)(AppRouter);
