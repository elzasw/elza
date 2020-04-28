import React, { useState } from 'react';
import { Button, Col } from 'react-bootstrap';
import Row from '../Row';
import { connect } from 'react-redux';
import * as ModalActions from '../../shared/reducers/modal/ModalActions';
import { Action, Dispatch } from 'redux';
import SharedModal, { SharedModalProps } from '../SharedModal';
import './RecordDetailSide.scss';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { IconDefinition } from '@fortawesome/fontawesome-common-types';
import {
  faEye,
  faSave,
  faCheck,
  faChevronLeft,
  faBan,
  faChevronUp,
  faChevronDown,
  faEdit,
  faReply
} from '@fortawesome/free-solid-svg-icons';
import StyledButton from "../StyledButton";

type Props = ReturnType<typeof mapDispatchToProps>;

const RecordDetailSide = (props: Props) => {
  const [editMode, setEditMode] = useState<boolean>(true);

  const onEyeClick = () => {
    setEditMode(!editMode);
  };

  const onEditClick = () => {
    props.onAdd({
      title: 'Upravit archivní entitu',
      body: 'Přejete si vytvořit novou revizi archivní entity k úpravě?',
      buttonText: 'Vytvořit',
      form: false
    });
  };

  const handleNewRecordClick = () => {
    props.onAdd({
      title: 'Nový záznam do jádra',
      body: 'Přejete si vytvořit nový záznam do jádra?',
      buttonText: 'Vytvořit',
      form: true
    });
  };

  const handleApproveRequestClick = () => {
    props.onAdd({
      title: 'Ke schválení',
      body: 'Přejete si předat archivní entitu ke schválení?',
      buttonText: 'Ke schválení',
      form: true
    });
  };

  const handleReturnRequestClick = () => {
    props.onAdd({
      title: 'Vrátit k doplňení',
      body: 'Přejete si vrátit archivní entitu k doplňení?',
      buttonText: 'K doplnění',
      form: true
    });
  };

  const handleApproveClick = () => {
    props.onAdd({
      title: 'Schválit archivní entitu',
      body: 'Přejete si schválit archivní entitu?',
      buttonText: 'Schválit',
      form: false
    });
  };

  const handleInvalidateClick = () => {
    props.onAdd({
      title: 'Zneplatnit archivní entitu',
      body: 'Přejete si zneplatnit archivní entitu?',
      buttonText: 'Zneplatnit',
      form: false
    });
  };

  return (
    <Row
      className="container"

      column={true}
      justify="space-between"
    >
      <Col>
        <div className="top">
          <StyledButton icon={faChevronLeft} onClick={() => window.history.back()}/>
        </div>
        <div className="middle">
          <StyledButton icon={faEye} onClick={onEyeClick} />
          <StyledButton icon={faEdit} title="Upravit archivní entitu" onClick={onEditClick} />
          <StyledButton icon={faSave} title="Nový záznam do jádra" onClick={handleNewRecordClick} />
          <StyledButton icon={faCheck} title="Ke schválení" onClick={handleApproveRequestClick} />
          <StyledButton icon={faReply} title="Vrátit k doplnění" onClick={handleReturnRequestClick} />
          <StyledButton icon={faCheck} title="Schválit archivní entitu" onClick={handleApproveClick} />
          <StyledButton icon={faBan} title="Zneplatnit archivní entitu" onClick={handleInvalidateClick} />
        </div>
      </Col>
      <Col className="bottom">
        <StyledButton icon={faChevronUp} />
        <StyledButton icon={faChevronDown} />
      </Col>
    </Row>
  );
};

const mapDispatchToProps = (dispatch: Dispatch<Action>) => ({
  onAdd: (modalProps: SharedModalProps) =>
    dispatch(
      ModalActions.show(SharedModal, {
        ...modalProps,
        onOk: () => dispatch(ModalActions.hide()),
        onCancel: () => dispatch(ModalActions.hide())
      })
    )
});

export default connect(
  null,
  mapDispatchToProps
)(RecordDetailSide);
