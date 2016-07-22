/**
 * Router, který reaguje na akce přepnutí routeru.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent} from 'components/index.jsx';
import {routerNavigateFinish} from 'actions/router.jsx'

var localRouterHistory = []

function getIndexBefore(pathPrefix) {
    for (let a=localRouterHistory.length - 1; a>=0; a--) {
        const path = localRouterHistory[a];
        if (path.startsWith(pathPrefix)) {
            continue;
        }
        return a;
    }
    return -1;
}

var AppRouter = class AppRouter extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'changeRouteIfNeeded',
            "handleRouterListener"
        );
    }
    
    componentDidMount() {
        this.changeRouteIfNeeded(this.props.router);

        this.context.router.listen(this.handleRouterListener)
    }

    handleRouterListener(info) {
        const {pathname} = info;
        localRouterHistory.push(pathname);
    }

    componentWillReceiveProps(nextProps) {
        this.changeRouteIfNeeded(nextProps.router);
    }

    changeRouteIfNeeded(router) {
        const navigateTo = router.navigateTo;

        if (navigateTo) {
            if (navigateTo.startsWith("/~arr")) {
                const i = getIndexBefore("/arr");
                let navigateToBack;
                if (i >= 0) {
                    navigateToBack = localRouterHistory[i];
                } else {
                    navigateToBack = "/";
                }
                this.context.router.push(navigateToBack);
                this.dispatch(routerNavigateFinish());
            } else {
                this.context.router.push(navigateTo);
                this.dispatch(routerNavigateFinish());
            }
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
