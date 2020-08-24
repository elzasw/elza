import {WebApi} from 'actions';
import {structureTypeInvalidate} from 'actions/arr/structureType';
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog';
import classNames from 'classnames';
import AddStructureDataForm from 'components/arr/structure/AddStructureDataForm';
import {Autocomplete, i18n, Icon} from 'components/shared';
import PropTypes from 'prop-types';
import React from 'react';
import {connect, DispatchProp} from 'react-redux';
import {
    structureNodeFormFetchIfNeeded,
    structureNodeFormSelectId,
    structureNodeFormSetData,
} from '../../../actions/arr/structureNodeForm';
import Loading from '../../shared/loading/Loading';
import {Button} from '../../ui';
import StructureSubNodeForm from '../structure/StructureSubNodeForm';
import DescItemLabel from './DescItemLabel.jsx';
import './DescItemStructureRef.scss';
import {IDescItemBaseProps} from './DescItemTypes';
import {decorateAutocompleteValue} from './DescItemUtils.jsx';
import ItemTooltipWrapper from './ItemTooltipWrapper.jsx';

class DescItemStructureRef extends React.Component {
    state = {data: [], active: false};

    static propTypes = {
        versionId: PropTypes.number.isRequired,
        structureTypeCode: PropTypes.string.isRequired,
    };

    constructor(props) {
        super(props);
        if (props.anonymous && !props.structureNodeForm) {
            if (props.descItem.value) {
                props.dispatch(structureNodeFormSelectId(props.versionId, props.descItem.value));
                props.dispatch(structureNodeFormFetchIfNeeded(props.versionId, props.descItem.value));
            } else {
                const {structureTypeCode, versionId} = props;
                WebApi.createStructureData(versionId, structureTypeCode, this.findValue()).then(structureData => {
                    this.props.onChange({id: structureData.id});
                    props.dispatch(structureNodeFormSelectId(props.versionId, structureData.id));
                });
            }
        }
    }

    componentDidUpdate(prevProps, prevState, snapshot) {
        const {descItem, structureId, anonymous, structureNodeForm} = this.props;
        if (((descItem && descItem.value) || structureId) && anonymous && structureNodeForm) {
            const {
                versionId,
                structureNodeForm: {id, state, subNodeForm},
            } = this.props;
            if (
                state === 'TEMP' &&
                subNodeForm.formData &&
                subNodeForm.formData.descItemGroups &&
                subNodeForm.formData.descItemGroups.filter(
                    i =>
                        i.descItemTypes &&
                        i.descItemTypes.filter(n => n.descItems && n.descItems.filter(q => q.id).length > 0).length > 0,
                ).length > 0
            ) {
                this.props.dispatch(structureNodeFormSetData(id, {state: 'TEMP->OK'}));
                WebApi.confirmStructureData(versionId, id).then(data => {
                    this.props.onChange(data);
                    this.props.onBlur();
                    this.props.dispatch(structureNodeFormSetData(id, data));
                });
            }

            this.props.dispatch(structureNodeFormFetchIfNeeded(versionId, descItem.value));
        }
    }

    componentWillUnmount() {
        const {structureNodeForm} = this.props;
        if (!structureNodeForm) {
            return;
        }
        const {anonymous} = this.props;
        if (anonymous) {
            const {
                versionId,
                structureNodeForm: {id, state, subNodeForm},
            } = this.props;
            if (
                state === 'TEMP' &&
                subNodeForm.formData &&
                subNodeForm.formData.descItemGroups &&
                subNodeForm.formData.descItemGroups.filter(
                    i =>
                        i.descItemTypes &&
                        i.descItemTypes.filter(n => n.descItems && n.descItems.filter(q => q.id).length === 0)
                            .length === 0,
                ).length === 0
            ) {
                WebApi.deleteStructureData(versionId, id);
            }
        }
    }

    handleFocus = () => {
        this.setState({active: true});
        this.props.onFocus && this.props.onFocus();
    };

    handleBlur = () => {
        this.setState({active: false});
        this.props.onBlur && this.props.onBlur();
    };

    handleSearchChange = text => {
        const {versionId, structureTypeCode} = this.props;
        WebApi.findStructureData(versionId, structureTypeCode, text, true).then(({rows}) => {
            this.setState({
                data: rows,
            });
        });
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
                    i18n('arr.structure.modal.add.title', structureTypeName),
                    <AddStructureDataForm
                        fundId={fundId}
                        fundVersionId={versionId}
                        structureData={structureData}
                        descItemFactory={descItemFactory}
                        onSubmit={() => {
                            WebApi.confirmStructureData(versionId, structureData.id).then(structure => {
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
                    '',
                    prop => {
                        WebApi.deleteStructureData(versionId, structureData.id);
                        //this.blur(); // blur to save
                        //this.focus();
                        //setTimeout(this.focus, 3000) // regain focus
                    },
                ),
            );
        });
    };

    focus = () => {
        //console.log("### focus desc item structure ref", this, this.input);
        if (this.props.anonymous) {
        } else {
            this.input.focus();
        }
    };

    blur = () => {
        if (this.props.anonymous) {
        } else {
            this.input.blur();
        }
    };

    /**
     * Render Item
     * @param item
     * @param focus isHighlighted
     * @param active isSelected
     * @returns Element
     */
    renderItem = props => {
        const {item, highlighted, selected, ...otherProps} = props;
        return (
            <div {...otherProps} className={classNames('item', {focus: highlighted, active: selected})} key={item.id}>
                {item.value} <span className="item-complement">{item.complement}</span>
            </div>
        );
    };

    renderFooter = () => {
        const {structureTypeName} = this.props;
        return (
            <div className="create-structure">
                <Button onClick={this.addNewStructure} variant={'outline-secondary'}>
                    <Icon glyph="fa-plus" />
                    {i18n('arr.structure.add', structureTypeName)}
                </Button>
            </div>
        );
    };

    render() {
        const {
            descItem,
            onChange,
            locked,
            readMode,
            cal,
            anonymous,
            structureNodeForm,
            versionId,
            fundId,
            descItemFactory,
        } = this.props;
        const structureData = descItem.structureData;
        if (readMode || descItem.undefined) {
            const calValue = cal && structureData === null ? i18n('subNodeForm.descItemType.calculable') : '';
            return (
                <DescItemLabel
                    value={structureData ? structureData.value : calValue}
                    cal={cal}
                    notIdentified={descItem.undefined}
                />
            );
        }

        if (anonymous) {
            return (
                <div className="desc-item-value desc-item-value-parts">
                    {structureNodeForm && structureNodeForm.fetched ? (
                        <StructureSubNodeForm
                            id={structureNodeForm.id}
                            versionId={versionId}
                            readMode={cal}
                            fundId={fundId}
                            selectedSubNodeId={structureNodeForm.id}
                            descItemFactory={descItemFactory}
                        />
                    ) : (
                        <Loading />
                    )}
                </div>
            );
        }

        return (
            <div className="desc-item-value desc-item-value-parts">
                <ItemTooltipWrapper tooltipTitle="dataType.structureRef.format">
                    <Autocomplete
                        {...decorateAutocompleteValue(
                            this,
                            descItem.hasFocus,
                            descItem.error.value,
                            locked || descItem.undefined,
                            ['autocomplete-structure'],
                        )}
                        ref={ref => (this.input = ref)}
                        customFilter
                        onFocus={this.handleFocus}
                        onBlur={this.handleBlur}
                        value={structureData}
                        disabled={locked}
                        items={this.state.data}
                        onSearchChange={this.handleSearchChange}
                        onChange={onChange}
                        renderItem={
                            descItem.undefined
                                ? {name: i18n('subNodeForm.descItemType.notIdentified')}
                                : this.renderItem
                        }
                        getItemName={item => (item ? item.value : '')}
                        onEmptySelect={this.addNewStructure}
                        footer={this.renderFooter()}
                    />
                </ItemTooltipWrapper>
            </div>
        );
    }
}

export default connect(
    (state, props) => {
        const {structures} = state;
        const key = props.descItem.value;
        return {
            structureNodeForm:
                key && structures.stores.hasOwnProperty(String(key)) ? structures.stores[String(key)] : null,
        };
    },
    null,
    null,
    {forwardRef: true},
)(DescItemStructureRef);
