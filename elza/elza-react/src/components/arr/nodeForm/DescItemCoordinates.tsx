import PropTypes from 'prop-types';
import * as React from 'react';
import {AbstractReactComponent, FormInput, i18n, Icon, NoFocusButton} from 'components/shared';
import {objectFromWKT, wktFromTypeAndData, wktType} from 'components/Utils.jsx';
import {decorateValue} from './DescItemUtils.jsx';
import {Button} from '../../ui';
import DescItemLabel from './DescItemLabel.jsx';
import ItemTooltipWrapper from './ItemTooltipWrapper.jsx';

import './DescItemCoordinates.scss';
import {DescItemComponentProps} from './DescItemTypes';
import {modalDialogHide, modalDialogShow} from "../../../actions/global/modalDialog";
import ExportCoordinateModal from "../../registry/Detail/coordinate/ExportCoordinateModal";
import {Action, Dispatch} from "redux";
import {connect} from "react-redux";
import {CLS_ITEM_COORDINATES} from "../../../shared/factory/factoryConsts";
import CrossTabHelper, {CrossTabEventType, getThisLayout} from "../../CrossTabHelper";
import {PolygonShowInMap} from "../../PolygonShowInMap";

type Props = DescItemComponentProps<string> & {onUpload: Function; onDownload: Function; coordinatesUpload: null | string; itemId: number | undefined;} & ReturnType<typeof mapDispatchToProps>;
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

    handleChangeData = e => {
        const val = wktFromTypeAndData(this.state.type, e.target.value);
        if (val !== this.props.descItem.value) {
            this.props.onChange(val);
        }
    };

    handleImportChangeData = value => {
        this.props.onChange(value);
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

    render() {
        const {descItem, locked, repeatable, onUpload, readMode, cal, coordinatesUpload} = this.props;
        const {type, data} = this.state;
        let value = cal && descItem.value == null ? i18n('subNodeForm.descItemType.calculable') : descItem.value;

        if (readMode) {
            return <DescItemLabel value={value} cal={cal} notIdentified={descItem.undefined} />;
        }

        if (coordinatesUpload) {
            this.handleImportChangeData(coordinatesUpload);
        }

        return (
            <div className="desc-item-value-coordinates">
                <div className="desc-item-value" key="cords">
                    <Button variant="default" disabled>
                        {wktType(type)}
                    </Button>
                    <ItemTooltipWrapper tooltipTitle="dataType.coordinates.format">
                        <input
                            {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked)}
                            ref={this.focusEl}
                            disabled={locked || descItem.undefined}
                            onChange={this.handleChangeData}
                            value={descItem.undefined ? i18n('subNodeForm.descItemType.notIdentified') : data}
                        />
                    </ItemTooltipWrapper>
                    {!descItem.undefined && descItem.descItemObjectId && (
                        <div className="desc-item-coordinates-action" key="download-action">
                            <NoFocusButton onClick={() => this.props.showExportDialog(descItem.id)}>
                                <Icon glyph="fa-download" />
                            </NoFocusButton>
                        </div>
                    )}
                </div>
                {!repeatable && (
                    <div className="desc-item-coordinates-action" key="cord-actions">
                        <NoFocusButton
                            onClick={this.handleUploadClick}
                            title={i18n('subNodeForm.descItem.coordinates.action.add')}
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
                {descItem?.['@class'] === CLS_ITEM_COORDINATES && <PolygonShowInMap className={'mx-1'} polygon={value} />}
            </div>
        );
    }
}

const mapDispatchToProps = (dispatch: Dispatch<Action>) => ({
    showExportDialog: (
        itemId: number | undefined,
    ) =>
        dispatch(
            modalDialogShow(
                this,
                i18n('ap.coordinate.export.title'),
                <ExportCoordinateModal onClose={() => dispatch(modalDialogHide())} itemId={itemId} arrangement={true} />,
            ),
        ),
});

export default connect(null, mapDispatchToProps)(DescItemCoordinates as any);
