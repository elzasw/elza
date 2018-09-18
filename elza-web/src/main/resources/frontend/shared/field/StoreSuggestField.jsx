// @flow
import React from 'react';
import {connect} from 'react-redux'
import {storeFromArea} from "./../utils";
import {FormControl} from 'react-bootstrap';
import * as StoreSuggest from "./storeSuggestActions.jsx";
import Autocomplete from "../../components/shared/autocomplete/Autocomplete";

/**
 * Store suggest
 *
 * Field který automatický vytvoří store pod danou AREA a zavolá api a zobrazí autocompleteField
 */
class StoreSuggestField extends React.Component {

    static PropTypes = {
        area: React.PropTypes.string.isRequired,
        apiCall: React.PropTypes.func.isRequired
    };

    componentDidMount() {
        const {area, apiCall} = this.props;
        this.props.dispatch(StoreSuggest.fetchListIfNeeded(area, apiCall));
    }

    render() {
        const {store, ...other} = this.props;

        if (!store || !store.fetched || !store.rows) {
            return <FormControl
                type="text"
                disabled={true}
            />
        }

        return <Autocomplete
            items={store.rows}
            {...other}
        />;
    }
}

function mapStateToProps(state, props) {
    return {
        store: storeFromArea(state, props.area, false),
    };
}

export default connect(mapStateToProps)(StoreSuggestField);
