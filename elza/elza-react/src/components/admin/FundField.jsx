import PropTypes from 'prop-types';
import React from 'react';
import {WebApi} from 'actions/index.jsx';
import {AbstractReactComponent, Autocomplete} from 'components/shared';

class FundField extends AbstractReactComponent {
    static propTypes = {
        value: PropTypes.object,
        onChange: PropTypes.func.isRequired,
        inline: PropTypes.bool,
        touched: PropTypes.bool,
        error: PropTypes.string,
    };

    state = {
        dataList: [],
    };

    focus = () => {
        this.refs.autocomplete.focus();
    };

    handleSearchChange = text => {
        const {excludedId} = this.props;
        text = text === '' ? null : text;
        WebApi.findFunds(text).then(json => {
            const newFunds = json.funds.filter(i => i.id !== excludedId);
            this.setState({
                dataList: newFunds,
            });
            return null;
        });
    };

    render() {
        // onChange nutno excludnout z other props - jinak by vlezno na autocomplete a přestal by fugnovat event on Change na komponentě
        const {value, onChange, ...otherProps} = this.props;
        const {dataList} = this.state;

        return (
            <Autocomplete
                ref="autocomplete"
                className="form-group"
                customFilter
                value={value}
                items={dataList}
                onSearchChange={this.handleSearchChange}
                onChange={onChange}
                {...otherProps}
                tags={false}
            />
        );
    }
}

export default FundField;
