// --
import PropTypes from 'prop-types';

import React from 'react';
import './HorizontalLoader.scss';
import HorizontalLoader from './HorizontalLoader';

/**
 * Loader pro načítání dat - horizontální, typicky pro dlouhé seznamy položek apod. - informace pro zobrazení zjišťuje ze store
 */
export default class StoreHorizontalLoader extends React.Component {

    static propTypes = {
        text: PropTypes.string,
        store: PropTypes.object,  // store, ze kterého načítá isFetching a fetched
    };

    shouldComponentUpdate(nextProps, nextState) {
        return (this.props.store.isFetching !== nextProps.store.isFetching)
            || (this.props.store.fetching !== nextProps.store.fetching)
            || (this.props.store.fetched !== nextProps.store.fetched)
            || (this.props.store.id !== nextProps.store.id)
            || (this.props.store.filter !== nextProps.store.filter)
            ;
    }

    render() {
        const { store: { fetching, isFetching, fetched }, ...other } = this.props;

        const useFetching = fetching || isFetching;

        if (!fetched || useFetching) {
            return <HorizontalLoader
                rerenderProgress
                fetched={fetched}
                showText={!fetched}
                hover={fetched}
                {...other}
            />;
        } else {
            return <div></div>;
        }
    }
}
