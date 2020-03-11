/**
 * Router, který reaguje na akce přepnutí routeru.
 */

import React from 'react';
import {withRouter} from 'react-router';
import {connect} from 'react-redux';
import {AbstractReactComponent} from 'components/index.jsx';
import {routerNavigateFinish} from 'actions/router.jsx';

const localRouterHistory = [];

function getIndexBefore(pathPrefix) {
    for (let a = localRouterHistory.length - 1; a >= 0; a--) {
        const path = localRouterHistory[a];
        if (path.startsWith(pathPrefix)) {
            continue;
        }
        return a;
    }
    return -1;
}

class AppRouter extends AbstractReactComponent {

    componentDidMount() {
        this.changeRouteIfNeeded(this.props.routerStore);

        this.props.history.listen(this.handleRouterListener);
    }

    handleRouterListener = (info) => {
        const {pathname} = info;
        localRouterHistory.push(pathname);
    };

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.changeRouteIfNeeded(nextProps.routerStore);
    }

    changeRouteIfNeeded = (routerStore) => {
        const navigateTo = routerStore.navigateTo;

        if (navigateTo) {
            if (navigateTo.startsWith('/~arr')) {
                const i = getIndexBefore('/arr');
                let navigateToBack;
                if (i >= 0) {
                    navigateToBack = localRouterHistory[i];
                } else {
                    navigateToBack = '/';
                }
                this.props.history.push(navigateToBack);
                this.props.dispatch(routerNavigateFinish());
            } else {
                this.props.history.push(navigateTo);
                this.props.dispatch(routerNavigateFinish());
            }
        }
    };

    render() {
        return <div style={{display: 'none'}}></div>;
    }
}


function mapStateToProps(state) {
    const {router} = state;
    return {
        routerStore: router,
    };
}

export default withRouter(connect(mapStateToProps)(AppRouter));
