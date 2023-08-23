import NodeLabel from './NodeLabel';
import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, Icon, NoFocusButton} from 'components/shared';
import {Button} from '../ui';
import {humanFileSize} from 'components/Utils.jsx';
import ArrRequestForm from './ArrRequestForm';
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog.jsx';
import {WebApi} from 'actions/index.jsx';

import './ArrDao.scss';
import { showConfirmDialog } from 'components/shared/dialog';

class ArrDao extends AbstractReactComponent {
    static propTypes = {
        dao: PropTypes.object.isRequired,
        daoFile: PropTypes.object,
        fund: PropTypes.object.isRequired,
        onUnlink: PropTypes.func.isRequired,
        readMode: PropTypes.bool.isRequired,
    };

    componentDidMount() {}

    UNSAFE_componentWillReceiveProps(nextProps) {}

    handleUnlink = async () => {
        const {onUnlink, dispatch} = this.props;
        const response = await dispatch(showConfirmDialog(i18n('arr.daos.unlink.confirm')));
        if (response) {
            onUnlink();
        }
    };

    handleTrash = () => {
        const {fund, dao} = this.props;

        const form = (
            <ArrRequestForm
                fundVersionId={fund.versionId}
                type="DAO"
                onSubmitForm={(send, data) => {
                    return WebApi.arrDaoRequestAddDaos(
                        fund.versionId,
                        data.requestId,
                        send,
                        data.description,
                        [dao.id],
                        data.daoType,
                    );
                }}
                onSubmitSuccess={(result, dispatch) => dispatch(modalDialogHide())}
            />
        );
        this.props.dispatch(modalDialogShow(this, i18n('arr.request.dao.form.title'), form));
    };

    copyToClipboard = url => {
        const el = document.createElement('textarea');
        el.value = url;
        document.body.appendChild(el);
        el.select();
        document.execCommand('copy');
        document.body.removeChild(el);
    };

    renderDaoDetail = () => {
        const {dao, readMode} = this.props;

        return (
            <div className="dao-detail">
                <div key="actions" className="dao-actions">
                    {dao.url && (
                        <div key="left" className="left">
                            <a target="_blank" rel="noopener noreferrer" href={dao.url}>
                                <NoFocusButton>
                                    <Icon glyph="fa-external-link" />
                                    {i18n('arr.daos.title.action.url')}
                                </NoFocusButton>
                            </a>
                            <NoFocusButton onClick={() => this.copyToClipboard(dao.url)}>
                                <Icon glyph="fa-paste" />
                                {i18n('arr.daos.title.action.copy')}
                            </NoFocusButton>
                        </div>
                    )}
                    {!readMode && (
                        <div key="right" className="right">
                            <Button variant="action" disabled={!dao.daoLink} onClick={this.handleUnlink}>
                                <Icon glyph="fa-unlink" />
                            </Button>
                            <Button variant="action" onClick={this.handleTrash} disabled={dao.existInArrDaoRequest}>
                                <Icon glyph="fa-trash" />
                            </Button>
                        </div>
                    )}
                </div>
                <div key="info" className="dao-info">
                    {this.renderLabel('arr.daos.title.id', dao.id)}
                    {this.renderLabel('arr.daos.title.code', dao.code, true)}
                    {this.renderLabel('arr.daos.title.file-count', dao.fileList.length)}
                </div>
            </div>
        );
    };

    renderThumbnail = () => {
        const {daoFile} = this.props;
        const exists = daoFile.thumbnailUrl;
        const cls = exists ? 'thumbnail' : 'thumbnail empty';

        let img = exists ? (
            <>
                <img className="img-blur" src={daoFile.thumbnailUrl} alt="" />
                <img src={daoFile.thumbnailUrl} alt="" />
            </>
        ) : (
            <div className="empty-img" style={{overflow:"hidden"}}>
                <Icon glyph="fa-remove" />
            </div>
        );

        return (
            <div title={exists ? daoFile.thumbnailUrl : i18n('arr.daos.title.thumbnail.empty')} className={cls}>
                {img}
            </div>
        );
    };

    renderDaoFileDetail = () => {
        let {fund, dao, readMode, daoFile} = this.props;

        const count = dao.fileList.length;
        const curr = dao.fileList.indexOf(daoFile) + 1;
        const leftDisable = curr <= 1;
        const rightDisable = curr >= count;

        return (
            <div className="dao-file-detail">
                {daoFile.thumbnailUrl &&
                    <div className="dao-file-thumbnail">
                        <div className="navigation">
                            <div className="title">{i18n('arr.daos.title.thumbnail', curr, count)}</div>
                            <div className="arrows">
                                <NoFocusButton disabled={leftDisable} onClick={this.props.prevDaoFile}>
                                    <Icon glyph="fa-chevron-left" />
                                </NoFocusButton>
                                <NoFocusButton disabled={rightDisable} onClick={this.props.nextDaoFile}>
                                    <Icon glyph="fa-chevron-right" />
                                </NoFocusButton>
                            </div>
                        </div>
                        {this.renderThumbnail()}
                    </div>
                }
                <div className="dao-file-info">
                    <div className="dao-file-info-title">
                        <div className="title">
                            {i18n('arr.daos.title.select-file')}
                        </div>
                        <div className="spacer"/>
                        <div className="actions">
                            <a
                                title={i18n('arr.daos.title.action.url')}
                                target="_blank"
                                rel="noopener noreferrer"
                                href={daoFile.url}
                            >
                                <NoFocusButton>
                                    <Icon glyph="fa-external-link" />
                                </NoFocusButton>
                            </a>
                            <NoFocusButton
                                title={i18n('arr.daos.title.action.copy')}
                                onClick={() => this.copyToClipboard(daoFile.url)}
                            >
                                <Icon glyph="fa-paste" />
                            </NoFocusButton>
                        </div>
                    </div>
                    <div className="dao-file-info-base">
                        <span className="file">{daoFile.code}</span>
                        {daoFile.mimetype && this.renderLabel('arr.daos.files.title.mimeType', daoFile.mimetype)}
                        {daoFile.size && this.renderLabel('arr.daos.files.title.size', humanFileSize(daoFile.size))}
                        {daoFile.duration && this.renderLabel('arr.daos.files.title.duration', daoFile.duration)}
                        {daoFile.imageWidth &&
                            daoFile.imageHeight &&
                            this.renderLabel(
                                'arr.daos.files.title.imageWidthHeight',
                                daoFile.imageWidth + ' x ' + daoFile.imageHeight + ' px',
                            )}
                        {daoFile.sourceXDimesionValue &&
                            daoFile.sourceYDimesionValue &&
                            this.renderLabel(
                                'arr.daos.files.title.sourceXY',
                                daoFile.sourceXDimesionValue +
                                    i18n(`arr.daos.files.title.unitOfMeasure.${daoFile.sourceXDimesionUnit}`) +
                                    ' x ' +
                                    daoFile.sourceYDimesionValue +
                                    i18n(`arr.daos.files.title.unitOfMeasure.${daoFile.sourceYDimesionUnit}`),
                            )}
                        {daoFile.description && this.renderLabel('arr.daos.files.title.description', daoFile.description)}
                    </div>
                </div>
            </div>
        );
    };

    renderLabel = (label, value, block = false) => {
        const cls = block ? 'lbl block' : 'lbl';
        const val = <span className="lbl-value">{value}</span>;
        const valFinal = block ? <div className="scrollable">{val}</div> : val;
        return (
            <div title={i18n(label) + ': ' + value} className={cls}>
                <span className="lbl-name">{i18n(label)}</span>
                {valFinal}
            </div>
        );
    };

    render() {
        const {fund, dao, readMode, daoFile} = this.props;
        return (
            <div className="dao-container">
                {dao.daoLink && (
                    <div key="jp-panel" className="dao-jp-panel">
                        {i18n('arr.daos.title.jp')} <NodeLabel inline node={dao.daoLink.treeNodeClient} />
                    </div>
                )}
                <div className="dao-info-container">
                    {daoFile ? this.renderDaoFileDetail() : this.renderDaoDetail()}
                </div>
            </div>
        );
    }
}

export default connect()(ArrDao);
