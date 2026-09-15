import { AbstractReactComponent, FormInput, Icon, NoFocusButton, TooltipTrigger } from 'components/shared';
import { FormattedMessage, WrappedComponentProps, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { nodeMessages } from 'components/arr/nodeMessages';

import { CoordinatesDisplay } from 'components/shared/coordinates/CoordinatesDisplay';
import { addToastr } from 'components/shared/toastr/ToastrActions.jsx';
import { objectFromWKT, wktFromTypeAndData } from 'components/Utils.jsx';
import { copyTextToClipboard } from 'utils/clipboard';
import PropTypes from 'prop-types';
import * as React from 'react';
import { connect } from "react-redux";
import { Action } from "redux";
import { modalDialogHide, modalDialogShow } from "../../../actions/global/modalDialog";
import CrossTabHelper, { CrossTabEventType, getThisLayout } from "../../CrossTabHelper";
import { PolygonShowInMap } from "../../PolygonShowInMap";
import ExportCoordinateModal from "../../registry/Detail/coordinate/ExportCoordinateModal";
import { Button } from '../../ui';
import './DescItemCoordinates.scss';
import { DescItemComponentProps } from './DescItemTypes';
import { decorateValue } from './DescItemUtils.jsx';
import ItemTooltipWrapper from './ItemTooltipWrapper.jsx';
import DescItemLabel from './DescItemLabel';
import { AppState, ExternalSystem } from 'typings/store';
import { GisSystemType } from '../../../constants';
import { kmlExtSystemListFetchIfNeeded } from 'actions/admin/kmlExtSystemList';
import { ThunkDispatch } from 'redux-thunk';
import { editInMapEditor } from 'components/registry/part-edit/form/fields/FormCoordinates';
import { getIntl } from 'components/shared/lang/intlInstance';


type Props = DescItemComponentProps<string> & {onUpload: Function; onDownload: Function; coordinatesUpload: null | string; itemId: number | undefined;} & ReturnType<typeof mapDispatchToProps> & WrappedComponentProps;
type State = {type: null | string; data: null | string};

/**
 * Input prvek pro desc item - typ STRING.
 */
class DescItemCoordinates extends AbstractReactComponent<Props, State> {
    private readonly focusEl: React.RefObject<HTMLInputElement>;
    private readonly uploadInput: React.RefObject<React.ComponentClass<typeof FormInput>>;

    constructor(props) {
        super(props);
        this.state = objectFromWKT(props.descItem.value);
        this.focusEl = React.createRef();
        this.uploadInput = React.createRef();
    }

    static propTypes = {
        onChange: PropTypes.func.isRequired,
        onDownload: PropTypes.func.isRequired,
        onUpload: PropTypes.func,
        descItem: PropTypes.object.isRequired,
        repeatable: PropTypes.bool.isRequired,
        coordinatesUpload: PropTypes.string,
    };

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.setState(objectFromWKT(nextProps.descItem.value));
    }

    focus = () => {
        this.focusEl.current?.focus();
    };

    handleUploadClick = () => {
        if (this.uploadInput.current) {
            ((this.uploadInput.current as any) as HTMLInputElement).click();
        }
    };

    handleChangeData = (e) => {
        const val = e.target.value;
        if (val !== this.props.descItem.value) {
            this.props.onChange(val);
        }
    };

    handleImportChangeData = value => {
        this.props.onChange(value);
        setTimeout(() => this.focus(),0) // without setTimeout the field is focused with empty value
    };

    handleChangeSelect = e => {
        const val = wktFromTypeAndData(e.target.value, this.state.data);
        if (val !== this.props.descItem.value) {
            this.props.onChange(val);
        }
    };

    showInMap(polygon) {
        const thisLayout = getThisLayout();

        if (thisLayout) {
            CrossTabHelper.sendEvent(thisLayout, {type: CrossTabEventType.SHOW_IN_MAP, data: polygon});
        }
    }

    handleEditInMapSave = (value: string) => {
        const { onChange } = this.props;
        const { current } = this.focusEl;

        onChange(value);

        // artificial focus and blur to force save
        if(current){
            current.focus();
            current.blur();
        }
    }

    getLabel = (value: string) => {
        return value.split(' ')[0];
    };


    render() {
        const {descItem, locked, repeatable, onUpload, readMode, cal, coordinatesUpload, copyValueToClipboard} = this.props;
        const {type, data} = this.state;
        let value = cal && descItem.value == null ? this.props.intl.formatMessage(nodeMessages.subNodeFormDescItemTypeCalculable) : descItem.value;

        if (readMode) {
            if(descItem.undefined){
                return <DescItemLabel
                    value={value}
                    cal={cal}
                    isValueUndefined={descItem.undefined}
                    isValueInhibited={descItem.inhibited}
                />;
            }

            return <CoordinatesDisplay
                value={value}
                id={descItem.id}
                arrangement={true}
            />
        }

        if (coordinatesUpload) {
            this.handleImportChangeData(coordinatesUpload);
        }

        return (
            <div className="desc-item-value-coordinates">
                <div className="desc-item-value" key="cords">
                    <div className="desc-item-coordinates-action" key="download-action">
                        <PolygonShowInMap polygon={value} showInEditor={true} onEditorSave={this.handleEditInMapSave}/>
                    </div>
                    <ItemTooltipWrapper tooltipTitle="dataType.coordinates.format">
                        <input
                            {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked)}
                            ref={this.focusEl}
                            disabled={locked || descItem.undefined}
                            onChange={this.handleChangeData}
                            value={descItem.undefined ? this.props.intl.formatMessage(nodeMessages.subNodeFormDescItemTypeUndefinedValue) : data}
                            />
                    </ItemTooltipWrapper>
                    {!descItem.undefined && descItem.descItemObjectId && (
                        <>
                            <TooltipTrigger
                                className="desc-item-coordinates-action"
                                content={<FormattedMessage {...globalMessages.copyToClipboard} />}
                                style={{width: "auto"}}
                                placement="vertical"
                            >
                                <Button
                                    variant={'action'}
                                    size="sm"
                                    onClick={() => copyValueToClipboard(value)}
                                >
                                    <Icon glyph="fa-clone" fixedWidth className="icon" />
                                </Button>
                            </TooltipTrigger>
                            <TooltipTrigger
                                className="desc-item-coordinates-action"
                                content={<FormattedMessage {...globalMessages.export} />}
                                style={{width: "auto"}}
                                placement="vertical"
                            >
                                <NoFocusButton onClick={() => this.props.showExportDialog(descItem.id)}>
                                    <Icon glyph="fa-download" />
                                </NoFocusButton>
                            </TooltipTrigger>
                            </>
                    )}
                </div>
                {!repeatable && (
                    <div className="desc-item-coordinates-action" key="cord-actions">
                        <NoFocusButton
                            onClick={this.handleUploadClick}
                            title={<FormattedMessage {...nodeMessages.subNodeFormDescItemCoordinatesActionAdd} />}
                        >
                            <Icon glyph="fa-upload" />
                        </NoFocusButton>
                        <FormInput
                            className="d-none"
                            accept="application/vnd.google-earth.kml+xml"
                            type="file"
                            ref={this.uploadInput}
                            onChange={onUpload as any}
                            />
                    </div>
                )}
            </div>
        );
    }
}

const mapDispatchToProps = (dispatch: ThunkDispatch<AppState, void, Action>) => ({
    showExportDialog: (
        itemId: number | undefined,
    ) =>
        dispatch(
            modalDialogShow(
                this,
                getIntl().formatMessage(nodeMessages.apCoordinateExportTitle),
                <ExportCoordinateModal onClose={() => dispatch(modalDialogHide())} itemId={itemId} arrangement={true} />,
            ),
        ),
    // Při nedostupné schránce se záměrně nic nezobrazuje, jen se neukáže potvrzení.
    copyValueToClipboard: async (value: string) => {
        if (await copyTextToClipboard(value)) {
            dispatch(addToastr(getIntl().formatMessage(nodeMessages.globalActionCopyToClipboardFinished), undefined, undefined, "md", 3000));
        }
    }
});

export default connect(undefined, mapDispatchToProps)(injectIntl(DescItemCoordinates) as any);
