import PropTypes from 'prop-types';
import React from 'react';
import {WebApi} from 'actions/index.jsx';
import {AbstractReactComponent, Autocomplete, i18n, Icon} from 'components/shared';
import {decorateAutocompleteValue} from './DescItemUtils.jsx';
import {Button} from '../../ui';
import DescItemLabel from './DescItemLabel.jsx';

import './DescItemFileRef.scss';
import ItemTooltipWrapper from './ItemTooltipWrapper.jsx';

class DescItemFileRef extends AbstractReactComponent {
    static propTypes = {
        fundId: PropTypes.number.isRequired,
        onFundFiles: PropTypes.func.isRequired,
        onCreateFile: PropTypes.func.isRequired,
    };

    state = {
        fileList: [],
    };

    focusEl = null;

    focus = () => {
        this.focusEl.focus();
    };

    handleSearchChange = text => {
        text = text === '' ? null : text;

        WebApi.findFundFiles(this.props.fundId, text).then(json => {
            this.setState({
                fileList: json.rows,
            });
        });
    };

    handleFundFiles = () => {
        if (this.props.onFundFiles) {
            this.focusEl.closeMenu();
            this.props.onFundFiles();
        } else {
            console.warn('undefined handleFundFiles');
        }
    };

    handleCreateFile = () => {
        if (this.props.onCreateFile) {
            this.focusEl.closeMenu();
            this.props.onCreateFile();
        } else {
            console.warn('undefined handleCreateFile');
        }
    };

    renderFooter = () => {
        return (
            <div className="create-file">
                <Button variant="outline-secondary" onClick={this.handleCreateFile}>
                    <Icon glyph="fa-plus" className="mr-1" />
                    {i18n('arr.fund.files.action.add')}
                </Button>
                <Button variant="outline-secondary" className="ml-1" onClick={this.handleFundFiles}>{i18n('arr.panel.title.files')}</Button>
            </div>
        );
    };

    render() {
        const {descItem, locked, onChange, onBlur, readMode} = this.props;
        const value = descItem.file ? descItem.file : null;

        if (readMode) {
            if (value) {
                return <DescItemLabel value={value.name} notIdentified={descItem.undefined} />;
            } else {
                return <DescItemLabel value="" notIdentified={descItem.undefined} />;
            }
        }

        const footer = this.renderFooter();

        return (
            <div className="desc-item-value desc-item-value-parts">
                <ItemTooltipWrapper
                    tooltipTitle="dataType.fileRef.format"
                    {...decorateAutocompleteValue(
                        this,
                        descItem.hasFocus,
                        descItem.error.value,
                        locked || descItem.undefined,
                        ['autocomplete-file'],
                    )}
                >
                    <Autocomplete
                        ref={ref => this.focusEl = ref}
                        customFilter
                        value={descItem.undefined ? {name: i18n('subNodeForm.descItemType.notIdentified')} : value}
                        items={this.state.fileList}
                        getItemId={item => (item ? item.id : null)}
                        getItemName={item => (item ? item.name : '')}
                        onSearchChange={this.handleSearchChange}
                        onChange={onChange}
                        onBlur={onBlur}
                        footer={footer}
                    />
                </ItemTooltipWrapper>
            </div>
        );
    }
}

export default DescItemFileRef;
