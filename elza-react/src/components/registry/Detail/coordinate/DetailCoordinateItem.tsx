import React from 'react';
import {Button} from 'react-bootstrap';
//import {faFileExport} from "@fortawesome/free-solid-svg-icons";
import classNames from 'classnames';
//import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {connect} from 'react-redux';
import {Action, Dispatch} from 'redux';
//import * as ModalActions from "../../../shared/reducers/modal/ModalActions";
// import CoordinateModal from './CoordinateModal';
import ExportCoordinateModal from './ExportCoordinateModal';
import Icon from '../../../shared/icon/Icon';
import {ApItemCoordinatesVO} from '../../../../api/ApItemCoordinatesVO';
import {modalDialogHide, modalDialogShow} from '../../../../actions/global/modalDialog';
import i18n from '../../../i18n';

interface Props extends ReturnType<typeof mapDispatchToProps> {
    item: ApItemCoordinatesVO;
}

const DetailCoordinateItem: React.FC<Props> = props => {
    const getLabel = (value: string) => {
        return value.split(' ')[0];
    };

    return (
        <>
            <Button variant="link" onClick={() => props.showCoordinateDetail(props.item)}>
                {getLabel(props.item.value)}
            </Button>
            <Button
                variant={'action' as any}
                className={classNames('side-container-button', 'mb-1')}
                title={i18n('global.action.export')}
                size="sm"
                onClick={() => props.showExportDialog(props.item)}
            >
                <Icon glyph="fa-download" fixedWidth className="icon" />
            </Button>
        </>
    );
};

const mapDispatchToProps = (dispatch: Dispatch<Action>) => ({
    showCoordinateDetail: (item: ApItemCoordinatesVO) => null,
    /*dispatch(
      ModalActions.showForm(CoordinateModal, {
          onCancel: () => dispatch(ModalActions.hide()),
          item
        },
        {
          title: 'Detail souřadnice',
        })
    )*/ showExportDialog: (
        item: ApItemCoordinatesVO,
    ) =>
        dispatch(
            modalDialogShow(
                this,
                i18n('ap.coordinate.export.title'),
                <ExportCoordinateModal onClose={() => dispatch(modalDialogHide())} item={item} />,
            ),
        ),
});
export default connect(null, mapDispatchToProps)(DetailCoordinateItem);
