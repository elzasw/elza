import React from "react";
import ReactDOM from "react-dom";
import {connect, DispatchProp} from "react-redux";
import {
    Icon,
    i18n,
    AbstractReactComponent,
    NoFocusButton,
    Autocomplete
} from "components/shared";
import { decorateAutocompleteValue } from "./DescItemUtils.jsx";
import { WebApi } from "actions/WebApi.jsx";
import { indexById } from "stores/app/utils.jsx";
import DescItemLabel from "./DescItemLabel.jsx";
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";
import PropTypes from "prop-types";
import "./DescItemStructureRef.less";
import classNames from "classnames";
import { modalDialogHide, modalDialogShow } from "actions/global/modalDialog";
import AddStructureDataForm from "components/arr/structure/AddStructureDataForm";
import {
    //structureTypeFetchIfNeeded,
    //AREA,
    //structureTypeFilter,
    structureTypeInvalidate
} from "actions/arr/structureType";
import { Button } from "react-bootstrap";
import StructureSubNodeForm from "../structure/StructureSubNodeForm";
import Loading from "../../shared/loading/Loading";
import {structureNodeFormFetchIfNeeded, structureNodeFormSelectId} from "../../../actions/arr/structureNodeForm";
import type {IDescItemBaseProps} from "./DescItemTypes";


export type ComponentProps = IDescItemBaseProps & {
    structureTypeCode: string,
    structureTypeName: string,
    anonymous: boolean,
    descItemFactory: Function
};

export type ConnectedProps = {
    structureNodeForm: any;
}

export type Props = ComponentProps & ConnectedProps & DispatchProp;

type ComponentState = {
    data: any[],
    active: boolean
};

class DescItemStructureRef extends AbstractReactComponent<
    Props,
    ComponentState
    > {
    static propTypes = {
        fundVersionId: PropTypes.number.isRequired,
        structureTypeCode: PropTypes.string.isRequired,
        anonymous: PropTypes.bool
    };
    props: Props;
    state: ComponentState = { data: [], active: false };

    handleFocus = () => {
        this.setState({ active: true });
        this.props.onFocus && this.props.onFocus();
    };

    handleBlur = () => {
        this.setState({ active: false });
        this.props.onBlur && this.props.onBlur();
    };

    handleSearchChange = text => {
        const { versionId, structureTypeCode } = this.props;
        WebApi.findStructureData(versionId, structureTypeCode, text, true).then(
            ({ rows }) => {
                this.setState({
                    data: rows
                });
            }
        );
    };

    constructor(props: Props) {
        super(props);
        if (props.anonymous && !props.structureNodeForm) {
            if (props.descItem.value) {
                props.dispatch(structureNodeFormSelectId(props.descItem.value));
                props.dispatch(structureNodeFormFetchIfNeeded(props.versionId, props.descItem.value));
            } else {
                const {structureTypeCode, versionId} = props;
                WebApi.createStructureData(
                    versionId,
                    structureTypeCode,
                    this.findValue()
                ).then(structureData => {
                    props.dispatch(structureNodeFormSelectId(structureData.id));
                    props.onChange(structureData);
                    props.onBlur();
                });
            }
        }
    }


    componentWillReceiveProps(nextProps) {
        if (nextProps.descItem.value && nextProps.anonymous && nextProps.structureNodeForm) {
            nextProps.dispatch(structureNodeFormFetchIfNeeded(nextProps.versionId, nextProps.descItem.value));
        }
    }


    findValue = () => {
        const input = this.input;
        return input && input.input && input.input.props.value;
    };

    addNewStructure = () => {
        const {
            structureTypeCode,
            versionId,
            fundId,
            structureTypeName,
            onChange,
            descItemFactory
        } = this.props;
        WebApi.createStructureData(
            versionId,
            structureTypeCode,
            this.findValue()
        ).then(structureData => {
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
                            WebApi.confirmStructureData(versionId, structureData.id).then(
                                structure => {
                                    onChange && onChange(structure);
                                    this.blur(); // blur to save
                                    //this.input.focus();
                                    //setTimeout(()=>{this.input.focus()}, 3000) // regain focus
                                }
                            );
                        }}
                        onSubmitSuccess={() => {
                            this.props.dispatch(modalDialogHide());
                            this.props.dispatch(structureTypeInvalidate());
                            //this.focus();
                            //setTimeout(this.focus, 3000) // regain focus
                        }}
                    />,
                    "",
                    prop => {
                        WebApi.deleteStructureData(versionId, structureData.id);
                        //this.blur(); // blur to save
                        //this.focus();
                        //setTimeout(this.focus, 3000) // regain focus
                    }
                )
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
        const { item, highlighted, selected, ...otherProps } = props;
        return (
            <div
                {...otherProps}
                className={classNames("item", { focus: highlighted, active: selected })}
                key={item.id}
            >
                {item.value} <span className="item-complement">{item.complement}</span>
            </div>
        );
    };

    renderFooter = () => {
        const { structureTypeName } = this.props;
        return (
            <div className="create-structure">
                <Button onClick={this.addNewStructure}>
                    <Icon glyph="fa-plus" />
                    {i18n("arr.structure.add", structureTypeName)}
                </Button>
            </div>
        );
    };

    render() {
        const {
            descItem,
            onChange,
            onBlur,
            locked,
            singleDescItemTypeEdit,
            data,
            readMode,
            cal,
            anonymous,
            structureNodeForm,
            versionId,
            fundId,
            descItemFactory
        } = this.props;

        const structureData = descItem.structureData;
        if (readMode || descItem.undefined) {
            const calValue =
                cal && structureData === null
                    ? i18n("subNodeForm.descItemType.calculable")
                    : "";
            return (
                <DescItemLabel
                    value={structureData ? structureData.value : calValue}
                    cal={cal}
                    notIdentified={descItem.undefined}
                />
            );
        }

        if (anonymous) {
            return <div className="desc-item-value desc-item-value-parts">
                {structureNodeForm && structureNodeForm.fetched ? (
                    <StructureSubNodeForm
                        id={structureNodeForm.id}
                        versionId={versionId}
                        readMode={false}
                        fundId={fundId}
                        selectedSubNodeId={structureNodeForm.id}
                        descItemFactory={descItemFactory}
                    />
                ) : (
                    <Loading />
                )}
            </div>
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
                            ["autocomplete-structure"]
                        )}
                        ref={ref => {
                            this.input = ref;
                            console.log("### add input ref", ref);
                        }}
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
                                ? { name: i18n("subNodeForm.descItemType.notIdentified") }
                                : this.renderItem
                        }
                        getItemName={item => (item ? item.value : "")}
                        onEmptySelect={this.addNewStructure}
                        footer={this.renderFooter()}
                    />
                </ItemTooltipWrapper>
            </div>
        );
    }
}

export default connect(
    (state, props: ComponentProps) => {
        const { structures } = state;
        const key = props.descItem.value;

        return {
            structureNodeForm: key && structures.stores.hasOwnProperty(key) ? structures.stores[key] : null
        };
    },
    null,
    null,
    { withRef: true }
)(DescItemStructureRef);
