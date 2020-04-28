import React, {ComponentProps, ComponentPropsWithoutRef, useState} from 'react';
import {Button, Col, Row} from 'react-bootstrap';
import {connect} from 'react-redux';
//import * as ModalActions from '../../shared/reducers/modal/ModalActions';
import {Action, Dispatch} from 'redux';
//import SharedModal, {SharedModalProps} from '../SharedModal';
import './RecordDetailSide.scss';
import Icon from '../../shared/icon/Icon';
//import StyledButton from "../StyledButton";

type Props = {
  onPrev?: () => void | undefined;
  onNext?: () => void | undefined;
  onBack?: () => void;
  backTitle?: string;
  hasPrev: boolean;
  hasNext: boolean;
}
type AllProps = ReturnType<typeof mapDispatchToProps> & Props & ComponentPropsWithoutRef<any>;

const AEDetailSider = ({
                         hasPrev,
                         hasNext,
                         onPrev,
                         onNext,
                         onBack,
                         backTitle,
                         children
                       }: AllProps) => {
  return (
    <Row
      className="flex-column space-between"
    >
      <Col>
        {onBack && <div className="top">
            <Icon glyph={'fa-chevron-left'} onClick={onBack} title={backTitle}/>
        </div>}
        <div className="middle">
          {children}
        </div>
      </Col>
      {(onPrev || onNext) && <Col className="bottom">
        {onPrev && <Icon disabled={!hasPrev} glyph={'fa-chevron-up'} onClick={onPrev}/>}
        {onNext && <Icon disabled={!hasNext} glyph={'fa-chevron-down'} onClick={onNext}/>}
      </Col>}
    </Row>
  );
};

const mapDispatchToProps = (dispatch: Dispatch<Action>) => ({
  onAdd: (modalProps) => null
    /*dispatch(
      ModalActions.show(SharedModal, {
        ...modalProps,
        onOk: () => dispatch(ModalActions.hide()),
        onCancel: () => dispatch(ModalActions.hide())
      })
    )*/
});

export default connect(
  null,
  mapDispatchToProps
)(AEDetailSider);
