import PropTypes from 'prop-types';
import React from 'react';
import {WebApi} from 'actions/index.jsx';
import {AbstractReactComponent, Autocomplete} from 'components/shared';
import {connect} from 'react-redux';
import {renderGroupItem} from './adminRenderUtils.jsx';

const GroupField = class GroupField extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            "handleChange",
            "handleSearchChange",
            "focus");

        this.state = {
            dataList: []
        };
    }

    focus() {
        this.refs.autocomplete.focus()
    }

    componentDidMount() {
    }

    handleSearchChange(text) {
        text = text == "" ? null : text;

        WebApi.findGroup(text).then(json => {
            this.setState({
                dataList: json.groups
            })
        })
    }

    render() {
        // onChange nutno excludnout z other props - jinak by vlezno na autocomplete a přestal by fugnovat event on Change na komponentě
        const {value, ...otherProps} = this.props;
        const {dataList} = this.state;

        return (
            <Autocomplete
                ref="autocomplete"
                className="form-group"
                customFilter
                value={value}
                items={dataList}
                onSearchChange={this.handleSearchChange}
                {...otherProps}
                renderItem={renderGroupItem}
            />
        )
    }
}

GroupField.propTypes = {
    value: PropTypes.object,
    onChange: PropTypes.func.isRequired,
    inline: PropTypes.bool,
    touched: PropTypes.bool,
    error: PropTypes.string,
}

GroupField.defaultProps = {
}

function mapStateToProps(state) {
    return {
    }
}
export default connect(mapStateToProps)(GroupField);
