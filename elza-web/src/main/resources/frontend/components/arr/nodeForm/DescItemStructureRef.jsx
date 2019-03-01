import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, Autocomplete} from 'components/shared';
import {decorateAutocompleteValue} from './DescItemUtils.jsx';
import {WebApi} from 'actions/WebApi.jsx';
import {indexById} from 'stores/app/utils.jsx';
import DescItemLabel from './DescItemLabel.jsx';
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";
import PropTypes from 'prop-types';
import './DescItemStructureRef.less';
import classNames from 'classnames';
import {modalDialogHide, modalDialogShow} from "actions/global/modalDialog";
import AddStructureDataForm from "components/arr/structure/AddStructureDataForm";
import {
    //structureTypeFetchIfNeeded,
    //AREA,
    //structureTypeFilter,
    structureTypeInvalidate
} from "actions/arr/structureType";
import {Button} from 'react-bootstrap';


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
        const {versionId, structureTypeCode} = this.props;
        WebApi.findStructureData(versionId, structureTypeCode, text, true)
            .then(({rows}) => {
                this.setState({
                    data: rows
                })
            })
    };

    findValue = () => {
        const input = this.input;
        return input && input.input && input.input.props.value;
    };

    addNewStructure = () => {
        const {structureTypeCode, versionId, fundId, structureTypeName, onChange, descItemFactory} = this.props;
        WebApi.createStructureData(versionId, structureTypeCode, this.findValue()).then(structureData => {
            this.props.dispatch(
                modalDialogShow(
                    this,
                    i18n("arr.structure.modal.add.title", structureTypeName),
                    <AddStructureDataForm
                       fundId={fundId}
                       fundVersionId={versionId}
                       structureData={structureData}
                       descItemFactory={descItemFactory}
                       onSubmit={() => {
                           WebApi.confirmStructureData(versionId, structureData.id).then((structure)=>{
                               onChange && onChange(structure);
                               this.blur(); // blur to save
                               //this.input.focus();
                               //setTimeout(()=>{this.input.focus()}, 3000) // regain focus
                           });
                       }}
                      onSubmitSuccess={() => {
                          this.props.dispatch(modalDialogHide());
                          this.props.dispatch(structureTypeInvalidate());
                          //this.focus();
                          //setTimeout(this.focus, 3000) // regain focus
                       }}
                      />,
                    "",
                    (prop) => {
                        WebApi.deleteStructureData(versionId, structureData.id);
                        //this.blur(); // blur to save
                        //this.focus();
                        //setTimeout(this.focus, 3000) // regain focus
                    }
                )
            );
        });
    }

    focus = () => {
        //console.log("### focus desc item structure ref", this, this.input);
        this.input.focus();
    };

    blur = () => {
        this.input.blur();
    }

    /**
     * Render Item
     * @param item
     * @param focus isHighlighted
     * @param active isSelected
     * @returns Element
     */
    renderItem = (props)  => {
        const {item, highlighted, selected, ...otherProps} = props;
        return <div {...otherProps} className={classNames('item', {focus: highlighted, active: selected})} key={item.id} >
            {item.value}
        </div>
    };

    renderFooter = () => {
        const {structureTypeName} = this.props;
        return <div className="create-structure">
            <Button onClick={this.addNewStructure}><Icon glyph='fa-plus'/>{i18n('arr.structure.add', structureTypeName)}</Button>
       </div>
    }

    render() {
        const {descItem, onChange, onBlur, locked, singleDescItemTypeEdit, readMode, cal} = this.props;
        const structureData = descItem.structureData;
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
                       ref={(ref)=>{this.input = ref; console.log("### add input ref", ref);}}
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
                        onEmptySelect={this.addNewStructure}
                        footer={this.renderFooter()}
                    />
                </ItemTooltipWrapper>
            </div>
        )
    }
}

export default connect(null, null, null, {withRef: true})(DescItemStructureRef);
