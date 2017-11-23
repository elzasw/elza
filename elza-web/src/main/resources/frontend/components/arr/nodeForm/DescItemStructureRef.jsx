import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, Autocomplete} from 'components/shared';
import {decorateAutocompleteValue} from './DescItemUtils.jsx'
import {WebApi} from 'actions/index.jsx';
import {indexById} from 'stores/app/utils.jsx';
import DescItemLabel from './DescItemLabel.jsx'
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";
import PropTypes from 'prop-types';
import './DescItemStructureRef.less'
import classNames from 'classnames';

class DescItemStructureRef extends AbstractReactComponent {
    state = {data: [], active: false};

    static PropTypes = {
        fundVersionId: PropTypes.number.isRequired,
        structureTypeCode: PropTypes.string.isRequired,
    };

    handleFocus = () =>  {
        this.setState({active:true});
        this.props.onFocus && this.props.onFocus();
    };

    handleBlur = () => {
        this.setState({active:false});
        this.props.onBlur && this.props.onBlur();
    };

    handleSearchChange = (text) => {
        const {fundVersionId, structureTypeCode} = this.props;
        WebApi.findStructureData(fundVersionId, structureTypeCode, text, true)
            .then(({rows}) => {
                this.setState({
                    data: rows
                })
            })
    };

    focus = () => {
        this.refs.focusEl.focus();
    };

    /**
     * Render Item
     * @param item
     * @param focus isHighlighted
     * @param active isSelected
     * @returns Element
     */
    renderItem = (item, focus, active)  => {
        return <div className={classNames('item', {focus, active})} key={item.id}>
            {item.value}
        </div>
    };

    render() {
        const {descItem, onChange, onBlur, locked, singleDescItemTypeEdit, readMode, cal} = this.props;
        const structureData = descItem.structureData ? descItem.structureData : null;
        if (readMode || descItem.undefined) {
            const calValue = cal && structureData === null ? i18n("subNodeForm.descItemType.calculable") : "";
            return (
                <DescItemLabel value={structureData ? structureData.value : calValue} cal={cal} notIdentified={descItem.undefined} />
            )
        }

        return (
            <div className='desc-item-value desc-item-value-parts'>
                <ItemTooltipWrapper tooltipTitle="dataType.structureRef.format">
                    <Autocomplete
                        {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, locked || descItem.undefined, ['autocomplete-structure'])}
                        ref='focusEl'
                        customFilter
                        onFocus={this.handleFocus}
                        onBlur={this.handleBlur}
                        value={structureData}
                        disabled={locked}
                        items={this.state.data}
                        onSearchChange={this.handleSearchChange}
                        onChange={onChange}
                        renderItem={descItem.undefined ? {name: i18n('subNodeForm.descItemType.notIdentified')} : this.renderItem}
                        getItemName={item => item ? item.value : ""}
                    />
                </ItemTooltipWrapper>
            </div>
        )
    }
}

export default connect(null, null, null, {withRef: true})(DescItemStructureRef);
