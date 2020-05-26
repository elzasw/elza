import PropTypes from 'prop-types';
import React from 'react';
import {WebApi} from 'actions/index.jsx';
import {AbstractReactComponent, Autocomplete} from 'components/shared';
import {renderUserItem} from './adminRenderUtils.jsx';

class UserField extends AbstractReactComponent {
    static defaultProps = {
        tags: false,
        excludedGroupId: null,
    };

    static propTypes = {
        value: PropTypes.object,
        onChange: PropTypes.func.isRequired,
        inline: PropTypes.bool,
        touched: PropTypes.bool,
        error: PropTypes.string,
        tags: PropTypes.bool,
        excludedGroupId: PropTypes.number,
    };

    refAutocomplete = null;

    constructor(props) {
        super(props);

        this.state = {
            dataList: [],
        };
    }

    focus = () => {
        this.refAutocomplete.focus();
    };

    handleSearchChange = text => {
        text = text === '' ? null : text;

        WebApi.findUser(text, true, false, 200, this.props.excludedGroupId).then(json => {
            this.setState({
                dataList: json.users,
            });
        });
    };

    render() {
        // onChange nutno excludnout z other props - jinak by vlezno na autocomplete a přestal by fugnovat event on Change na komponentě
        const {tags, value, ...otherProps} = this.props;
        const {dataList} = this.state;

        return (
            <Autocomplete
                tags={tags}
                ref={ref => (this.refAutocomplete = ref)}
                className="form-group"
                customFilter
                value={value}
                items={dataList}
                onSearchChange={this.handleSearchChange}
                {...otherProps}
                renderItem={renderUserItem}
            />
        );
    }
}

export default UserField;
