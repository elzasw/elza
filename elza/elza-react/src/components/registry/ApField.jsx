import PropTypes from 'prop-types';
import React from 'react';
import {WebApi} from 'actions/index.jsx';
import {AbstractReactComponent, Autocomplete, i18n, Icon, TooltipTrigger} from 'components/shared';
import {connect} from 'react-redux';
import classNames from 'classnames';
import {debounce} from 'shared/utils';
import {Dropdown, Button} from 'react-bootstrap';
import RegistryListItem from './RegistryListItem';
import {refApTypesFetchIfNeeded} from '../../actions/refTables/apTypes';
import './ApField.scss';
import {ApSearchType} from '../../typings/globals';
import {JAVA_CLASS_AP_ACCESS_POINT_VO, DEFAULT_LIST_SIZE, JAVA_ATTR_CLASS} from '../../constants';

const AUTOCOMPLETE_REGISTRY_LIST_SIZE = DEFAULT_LIST_SIZE;

const SERVER_SEARCH_TYPE = {
    DISABLED: 'DISABLED',
    USERNAME: 'USERNAME',
    RIGHT_LIKE: 'RIGHT_LIKE',
    FULLTEXT: 'FULLTEXT',
};

const SEARCH_TYPE = {
    CREATE_RIGHT_LIKE: 'CREATE_RIGHT_LIKE',
    CREATE_FULLTEXT: 'CREATE_FULLTEXT',
    USERNAME: 'USERNAME',
    USERNAME_AND_PARTY: 'USERNAME_AND_PARTY',
    PARTY_RIGHT_LIKE: 'PARTY_RIGHT_LIKE',
    PARTY_FULLTEXT: 'PARTY_FULLTEXT',
};

const SEARCH_TYPE_LABEL = {
    [SEARCH_TYPE.CREATE_FULLTEXT]: i18n('apField.searchType.create.FULLTEXT'),
    [SEARCH_TYPE.CREATE_RIGHT_LIKE]: i18n('apField.searchType.create.RIGHT_LIKE'),
    [SEARCH_TYPE.USERNAME]: i18n('apField.searchType.USERNAME'),
    [SEARCH_TYPE.USERNAME_AND_PARTY]: i18n('apField.searchType.USERNAME_AND_PARTY'),
    [SEARCH_TYPE.PARTY_RIGHT_LIKE]: i18n('apField.searchType.PARTY_RIGHT_LIKE'),
    [SEARCH_TYPE.PARTY_FULLTEXT]: i18n('apField.searchType.PARTY_FULLTEXT'),
};

const SEARCH_TYPES = [
    SEARCH_TYPE.USERNAME,
    SEARCH_TYPE.USERNAME_AND_PARTY,
    SEARCH_TYPE.PARTY_RIGHT_LIKE,
    SEARCH_TYPE.PARTY_FULLTEXT,
];
const SEARCH_TYPES_CREATE = [SEARCH_TYPE.CREATE_RIGHT_LIKE, SEARCH_TYPE.CREATE_FULLTEXT];

class ApField extends AbstractReactComponent {
    refAutocomplete = null;

    static defaultProps = {
        isCreate: false,
        footer: false,
        itemSpecId: null,
        registryParent: null,
        apTypeId: null,
        roleTypeId: null,
        partyId: null,
        versionId: null,
        useIdAsValue: false,
    };

    static propTypes = {
        isCreate: PropTypes.bool,
        footer: PropTypes.bool,
        value: PropTypes.object,
        onChange: PropTypes.func.isRequired,
        onDetail: PropTypes.func,
        registryParent: PropTypes.number,
        apTypeId: PropTypes.number,
        itemSpecId: PropTypes.number,
        roleTypeId: PropTypes.number,
        partyId: PropTypes.number,
        versionId: PropTypes.number,
        useIdAsValue: PropTypes.bool,
    };

    constructor(props) {
        super(props);
        let searchType = props.isCreate ? SEARCH_TYPES_CREATE[0] : SEARCH_TYPES[0];
        this.state = {
            registryList: [],
            count: null,
            searchText: null,
            searchType,
        };
    }

    componentDidMount() {
        this.props.dispatch(refApTypesFetchIfNeeded());
    }

    focus = () => {
        this.refAutocomplete.focus();
    };

    handleSearchChange = debounce(text => {
        text = text === '' ? null : text;
        this.setState({searchText: text});
        const {registryParent, apTypeId, versionId, itemSpecId, itemTypeId} = this.props;
        const {searchType} = this.state;
        let searchTypeParty = null;
        let searchTypeUsername = null;
        switch (searchType) {
            case SEARCH_TYPE.CREATE_FULLTEXT:
            case SEARCH_TYPE.PARTY_FULLTEXT:
                searchTypeParty = SERVER_SEARCH_TYPE.FULLTEXT;
                searchTypeUsername = SERVER_SEARCH_TYPE.DISABLED;
                break;
            case SEARCH_TYPE.CREATE_RIGHT_LIKE:
            case SEARCH_TYPE.PARTY_RIGHT_LIKE:
                searchTypeParty = SERVER_SEARCH_TYPE.RIGHT_LIKE;
                searchTypeUsername = SERVER_SEARCH_TYPE.DISABLED;
                break;
            case SEARCH_TYPE.USERNAME_AND_PARTY:
                searchTypeParty = SERVER_SEARCH_TYPE.FULLTEXT;
                searchTypeUsername = SERVER_SEARCH_TYPE.FULLTEXT;
                break;
            case SEARCH_TYPE.USERNAME:
                searchTypeParty = SERVER_SEARCH_TYPE.DISABLED;
                searchTypeUsername = SERVER_SEARCH_TYPE.FULLTEXT;
                break;
            default:
                console.warn('Unknown search type:', searchType);
                break;
        }

        WebApi.findAccessPoint(
            text,
            registryParent,
            apTypeId,
            versionId,
            itemTypeId,
            itemSpecId,
            0,
            AUTOCOMPLETE_REGISTRY_LIST_SIZE,
            null,
            true,
            null,
            searchTypeParty,
            searchTypeUsername,
        ).then(json => {
            this.setState({
                registryList: json.rows,
                count: json.count,
            });
        });
    }, 500);

    handleChange = e => {
        this.setState({searchText: null});
        const value = this.normalizeValue(e);
        this.props.onChange(value);
    };

    handleBlur = e => {
        this.setState({searchText: null});
        const value = this.normalizeValue(e);
        this.props.onBlur(value);
    };

    renderFooter = () => {
        const {count, registryList} = this.state;

        const hasCount = count !== null && (count > AUTOCOMPLETE_REGISTRY_LIST_SIZE || count === 0);

        return hasCount ? (
            <div>
                {count > AUTOCOMPLETE_REGISTRY_LIST_SIZE && (
                    <div className="items-count">{i18n('registryField.visibleCount', registryList.length, count)}</div>
                )}
                {count === 0 && <div className="items-count">{i18n('registryField.noItemsFound')}</div>}
            </div>
        ) : null;
    };

    renderRecord = props => {
        const {item, highlighted, selected, ...otherProps} = props;
        const {apTypeIdMap, eidTypes} = this.props;

        return (
            <TooltipTrigger
                key={item.id}
                content={item.characteristics}
                holdOnHover
                placement="horizontal"
                className="tooltip-container"
                {...otherProps}
            >
                <RegistryListItem
                    {...item}
                    key={'reg-' + item.id}
                    eidTypes={eidTypes}
                    apTypeIdMap={apTypeIdMap}
                    className={classNames('item', {focus: highlighted, active: selected})}
                />
            </TooltipTrigger>
        );
    };

    normalizeValue = obj => {
        if (this.props.useIdAsValue) {
            return obj;
        }
        // změna typu aby se objekt dal použít jako návazný
        const newobj = {
            ...obj,
            [JAVA_ATTR_CLASS]: JAVA_CLASS_AP_ACCESS_POINT_VO,
        };
        return newobj;
    };

    render() {
        const {value, footer, detail, className, isCreate, ...otherProps} = this.props;
        const {searchType} = this.state;

        let footerRender = null;
        if (footer) {
            footerRender = this.renderFooter();
        }

        const searchTypes = isCreate ? SEARCH_TYPES_CREATE : SEARCH_TYPES;

        return (
            <div className={'position-relative ap-field'}>
                <Dropdown
                    className={'ap-field-search-type'}
                    onSelect={eventKey => this.setState({searchType: eventKey})}
                >
                    <Dropdown.Toggle variant="outline-secondary" id="dropdown-basic">
                        {SEARCH_TYPE_LABEL[searchType]}
                    </Dropdown.Toggle>
                    <Dropdown.Menu>
                        {searchTypes.map(i => (
                            <Dropdown.Item key={i} eventKey={i}>
                                {SEARCH_TYPE_LABEL[i]}
                            </Dropdown.Item>
                        ))}
                    </Dropdown.Menu>
                </Dropdown>
                <Autocomplete
                    {...otherProps}
                    ref={ref => (this.refAutocomplete = ref)}
                    customFilter
                    className={classNames('ap-field-autocomplete', className)}
                    footer={footerRender}
                    items={this.state.registryList}
                    onSearchChange={this.handleSearchChange}
                    renderItem={this.renderRecord}
                    onChange={this.handleChange}
                    onBlur={this.handleBlur}
                    value={value}
                />
            </div>
        );
    }
}

export default connect(
    state => {
        const {
            userDetail,
            refTables: {apTypes, eidTypes},
        } = state;
        return {
            apTypeIdMap: apTypes.itemsMap,
            userDetail,
            eidTypes: eidTypes.data,
        };
    },
    null,
    null,
    {forwardRef: true},
)(ApField);
