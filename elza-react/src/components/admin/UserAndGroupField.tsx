// ---
import PropTypes from 'prop-types';

import * as React from 'react';
import {WebApi} from 'actions/index.jsx';
import {Autocomplete, Utils} from 'components/shared';
import {renderUserOrGroupItem} from './adminRenderUtils.jsx';
import {RowsResponse, UsrGroupVO} from '../../types';
import {ApSearchType} from '../../typings/globals';
import {DEFAULT_LIST_SIZE} from '../../constants';
import { FormattedMessage, type MessageDescriptor } from 'react-intl';
import { searchTypeMessages } from 'components/registry/searchTypeMessages';
import {Dropdown} from 'react-bootstrap';

import './UserAndGroupField.scss';
import {UsrUserVO} from '../../api/UsrUserVO';

type Props = {
    tags: boolean;
    findUserApi: (
        fullText: string,
        active: boolean,
        disabled: boolean,
        max: number,
        groupId: number | null,
        searchTypeName?: ApSearchType,
        searchTypeUsername?: ApSearchType,
    ) => Promise<RowsResponse<UsrUserVO>>;
    /** Vrací {groups, groupsCount}, ne RowsResponse - viz WebApi.findGroup. */
    findGroupApi: (fulltext: string) => Promise<{groups: UsrGroupVO[]; groupsCount: number}>;
};

enum SEARCH_TYPE {
    USERNAME = 'USERNAME',
    USERNAME_AND_GROUPS = 'USERNAME_AND_GROUPS',
    USERNAME_AND_PARTY = 'USERNAME_AND_PARTY',
    PARTY_RIGHT_LIKE = 'PARTY_RIGHT_LIKE',
    PARTY_FULLTEXT = 'PARTY_FULLTEXT',
    GROUP = 'GROUP',
}

type State = {
    dataList: AutocompleteItem[];
    searchType: SEARCH_TYPE;
};

// Mapa se dřív skládala z naformátovaných řetězců při načtení modulu, tedy
// dávno před tím, než je znám jazyk. Nově drží deskriptory a formátuje se
// až při renderu.
const SEARCH_TYPE_MESSAGE: Record<SEARCH_TYPE, MessageDescriptor> = {
    [SEARCH_TYPE.USERNAME]: searchTypeMessages.username,
    [SEARCH_TYPE.USERNAME_AND_PARTY]: searchTypeMessages.usernameAndParty,
    [SEARCH_TYPE.PARTY_RIGHT_LIKE]: searchTypeMessages.partyRightLike,
    [SEARCH_TYPE.PARTY_FULLTEXT]: searchTypeMessages.partyFulltext,
    [SEARCH_TYPE.GROUP]: searchTypeMessages.group,
    [SEARCH_TYPE.USERNAME_AND_GROUPS]: searchTypeMessages.usernameAndGroups,
};

type AutocompleteItem = {id: string; user?: UsrUserVO; group?: UsrGroupVO};

/**
 * Field pro vybrání skupiny nebo uživatele.
 */
class UserAndGroupField extends React.Component<Props, State> {
    autocompleteRef: React.ElementRef<typeof Autocomplete>;
    // Typ z Props: parametry lambd se odvodí a signatury zůstávají na jednom místě.
    static defaultProps: Props = {
        tags: false,
        // WebApi se vyhledává až při volání, ne při definici třídy - vzniká na poslední řádce
        // svého modulu a v kruhu importů ještě nemusí existovat.
        findUserApi: (fullText, active, disabled, max, groupId, searchTypeName, searchTypeUsername) =>
            WebApi.findUser(fullText, active, disabled, max, groupId, searchTypeName, searchTypeUsername),
        findGroupApi: (fulltext) => WebApi.findGroup(fulltext),
    };

    static propTypes = {
        findUserApi: PropTypes.func, // api meoda pro dohledání uživatele, standardně WebApi.findUser, předpis (fulltext, active, disabled, max = DEFAULT_LIST_SIZE, groupId = null)
        findGroupApi: PropTypes.func, // api meoda pro dohledání skupinz, standardně WebApi.findGroup, předpis: (fulltext, max = DEFAULT_LIST_SIZE)
        tags: PropTypes.bool,
    };

    state = {
        dataList: [],
        searchType: SEARCH_TYPE.USERNAME_AND_GROUPS,
    };

    focus = () => {
        this.autocompleteRef!.focus();
    };

    handleSearchChange = text => {
        const {findUserApi, findGroupApi} = this.props;

        text = text === '' ? null : text;
        const {searchType} = this.state;
        let searchTypeParty: ApSearchType | undefined = undefined,
            searchTypeUsername: ApSearchType | undefined = undefined;
        let searchGroup = false;
        switch (searchType) {
            case SEARCH_TYPE.PARTY_FULLTEXT:
                searchTypeParty = ApSearchType.FULLTEXT;
                searchTypeUsername = ApSearchType.DISABLED;
                break;
            case SEARCH_TYPE.PARTY_RIGHT_LIKE:
                searchTypeParty = ApSearchType.RIGHT_SIDE_LIKE;
                searchTypeUsername = ApSearchType.DISABLED;
                break;
            case SEARCH_TYPE.USERNAME_AND_PARTY:
                searchTypeParty = ApSearchType.FULLTEXT;
                searchTypeUsername = ApSearchType.FULLTEXT;
                break;
            case SEARCH_TYPE.USERNAME:
                searchTypeParty = ApSearchType.DISABLED;
                searchTypeUsername = ApSearchType.FULLTEXT;
                break;
            case SEARCH_TYPE.USERNAME_AND_GROUPS:
                searchGroup = true;
                searchTypeParty = ApSearchType.DISABLED;
                searchTypeUsername = ApSearchType.FULLTEXT;
                break;
            case SEARCH_TYPE.GROUP:
                searchGroup = true;
                break;
            default:
                console.warn('Unknown search type:', searchType);
                break;
        }

        const findUser =
            searchTypeParty !== undefined || searchTypeUsername !== undefined
                ? findUserApi(text, true, false, DEFAULT_LIST_SIZE, null, searchTypeParty, searchTypeUsername)
                : Promise.resolve({data: []});
        const findGroup = searchGroup ? findGroupApi(text) : Promise.resolve({groups: []});

        Utils.barrier(findUser, findGroup)
            .then((data: [{data: {data: UsrUserVO[]}}, {data: {groups: UsrGroupVO[]}}]) => ({
                users: data[0].data.data,
                groups: data[1].data.groups,
            }))
            .then(data => {
                console.log(data);
                const rows: AutocompleteItem[] = [];
                for (const u of data.users) {
                    rows.push({
                        id: `u-${u.id}`,
                        user: u,
                    });
                }
                for (const g of data.groups) {
                    rows.push({
                        id: `g-${g.id}`,
                        group: g,
                    });
                }

                this.setState({
                    dataList: rows,
                });
            });
    };

    render() {
        // onChange nutno excludnout z other props - jinak by vlezno na autocomplete a přestal by fugnovat event on Change na komponentě
        const {findGroupApi, findUserApi, ...otherProps} = this.props;
        const {dataList, searchType} = this.state;

        return (
            <div className={'position-relative user-and-group-field'}>
                <Dropdown
                    className={'user-and-group-search-type'}
                    onSelect={eventKey => this.setState({searchType: eventKey as SEARCH_TYPE})}
                >
                    <Dropdown.Toggle variant="outline-secondary" id="dropdown-basic">
                        <FormattedMessage {...SEARCH_TYPE_MESSAGE[searchType]} />
                    </Dropdown.Toggle>
                    <Dropdown.Menu>
                        {Object.values(SEARCH_TYPE).map(i => (
                            <Dropdown.Item key={i} eventKey={i}>
                                <FormattedMessage {...SEARCH_TYPE_MESSAGE[i]} />
                            </Dropdown.Item>
                        ))}
                    </Dropdown.Menu>
                </Dropdown>
                <Autocomplete
                    ref={ref => (this.autocompleteRef = ref!)}
                    className="form-group"
                    customFilter
                    items={dataList}
                    onSearchChange={this.handleSearchChange}
                    {...otherProps}
                    renderItem={renderUserOrGroupItem}
                />
            </div>
        );
    }
}

export default UserAndGroupField;
