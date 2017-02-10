require ('./ArrDao.less');

import React from "react";
import {connect} from "react-redux";
import {NoFocusButton, FormInput, Icon, AbstractReactComponent, i18n, ListBox} from "components/index.jsx";
import {indexById} from "stores/app/utils.jsx";
import {Form, Button} from "react-bootstrap";
import {dateToString} from "components/Utils.jsx";
import {userDetailsSaveSettings} from "actions/user/userDetail.jsx";
import {fundChangeReadMode} from "actions/arr/fund.jsx";
import {setSettings, getOneSettings} from "components/arr/ArrUtils.jsx";
import {humanFileSize} from "components/Utils.jsx";
import ArrRequestForm from "./ArrRequestForm";
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {WebApi} from 'actions/index.jsx';

var classNames = require('classnames');

class ArrDao extends AbstractReactComponent {

    constructor(props) {
        super(props);
        this.state = {
            selectedFile: null,
        };
    }

    static PropTypes = {
        dao: React.PropTypes.object.isRequired,
        fund: React.PropTypes.object.isRequired,
        onUnlink: React.PropTypes.func.isRequired,
        readMode: React.PropTypes.bool.isRequired
    };

    componentDidMount() {

    }

    componentWillReceiveProps(nextProps) {
        if (this.props.dao !== nextProps.dao) {
            this.setState({
                selectedFile: null,
            });
        }
    }

    handleSelect = (file) => {
        this.setState({
            selectedFile: file
        });
    }

    renderFileDetail = (item) => {

        let itemDetail;

        if (item != null) {
            itemDetail = <Form inline>
                {item.mimetype && <div><FormInput type="static" label={i18n("arr.daos.files.title.mimeType") + ":"}>{item.mimetype}</FormInput></div>}
                {item.size && <div><FormInput type="static" label={i18n("arr.daos.files.title.size") + ":"}>{humanFileSize(item.size)}</FormInput></div>}
                {item.duration && <div><FormInput type="static" label={i18n("arr.daos.files.title.duration") + ":"}>{item.duration}</FormInput></div>}
                {item.imageWidth && item.imageHeight && <div><FormInput type="static" label={i18n("arr.daos.files.title.imageWidthHeight") + ":"}>{item.imageWidth}x{item.imageHeight} px</FormInput></div>}
                {item.sourceXDimesionValue && item.sourceYDimesionValue &&
                <div>
                    <FormInput type="static" label={i18n("arr.daos.files.title.sourceXY") + ":"}>
                        {item.sourceXDimesionValue}{i18n(`arr.daos.files.title.unitOfMeasure.${item.sourceXDimesionUnit}`)}x{item.sourceYDimesionValue}{i18n(`arr.daos.files.title.unitOfMeasure.${item.sourceYDimesionUnit}`)}
                    </FormInput>
                </div>
                }
                {item.url && <div><FormInput type="static" label={i18n("arr.daos.files.title.url") + ":"}><a target="_blank" href={item.url}>{item.url}</a></FormInput></div>}
            </Form>
        }

        return <div className="dao-files-detail">{itemDetail}</div>
    };

    handleUnlink = () => {
        const {onUnlink} = this.props;
        if (confirm(i18n("arr.daos.unlink.confirm"))) {
            onUnlink();
        }
    };

    handleTrash = () => {
        const {fund, dao} = this.props;

        const form = <ArrRequestForm
            fundVersionId={fund.versionId}
            type="DAO"
            onSubmitForm={(send, data) => {
                WebApi.arrDaoRequestAddDaos(fund.versionId, data.requestId, send, data.description, [dao.id], data.daoType)
                    .then(() => {
                        this.dispatch(modalDialogHide());
                    });
            }}
        />;
        this.dispatch(modalDialogShow(this, i18n('arr.request.dao.form.title'), form));
    };

    render() {
        const {fund, dao, readMode} = this.props;
        const {selectedFile} = this.state;

        // Položky souborů včetně skupin
        const filesMap = {};    // mapa id na položku
        const options = [];

        if (dao != null) {
            if (dao.fileList.length > 0) {
                options.push(<optgroup className="global-group" label={i18n("arr.daos.files.title.withoutGroup")}></optgroup>);
                dao.fileList.forEach(f => {
                    options.push(<option selected={selectedFile && f.id === selectedFile.id} className="file" value={f.id}>{f.code}</option>);
                    filesMap[f.id] = f;
                });
            }
            if (dao.fileGroupList.length > 0) {
                options.push(<optgroup className="global-group" label={i18n("arr.daos.files.title.inGroup")}></optgroup>);
                dao.fileGroupList.forEach(fg => {
                    options.push(
                        <optgroup className="file-group" label={fg.code}>
                            {fg.fileList.map(f => {
                                filesMap[f.id] = f;
                                return <option selected={selectedFile && f.id === selectedFile.id} className="file" value={f.id}>{f.code}</option>
                            })};
                        </optgroup>
                    );
                });
            }
        }

        let daoDetail;

        if (dao) {
            daoDetail = <div><div className="title"><i>Digitalizát</i>: {dao.label}</div>
                <div className="info">
                    {!readMode && <Button disabled={!dao.daoLink} onClick={this.handleUnlink}><Icon glyph='fa-unlink'/></Button>}
                    {!readMode && <Button onClick={this.handleTrash}><Icon glyph='fa-trash'/></Button>}
                <Form inline>
            <div><FormInput type="static" label={i18n("arr.daos.title.code") + ":"}>{dao.code}</FormInput></div>
            {dao.url && <div><FormInput type="static" label={i18n("arr.daos.title.url") + ":"}><a target="_blank" href={dao.url}>{dao.url}</a></FormInput></div>}
            {dao.daoLink && <div><FormInput type="static" label={i18n("arr.daos.title.node") + ":"}>{dao.daoLink.treeNodeClient.accordionLeft ? dao.daoLink.treeNodeClient.accordionLeft : i18n('accordion.title.left.name.undefined', dao.daoLink.treeNodeClient.id)}</FormInput></div>}
                </Form>
                </div></div>
        }

        return (
            <div className="dao-container">
                <div className="dao-detail">
                    {daoDetail}
                </div>
                <div className="dao-files">
                    <div className='dao-files-list'>
                        <select size="2" onChange={(e) => { this.handleSelect(filesMap[e.target.value]) }}>
                            {options}
                        </select>
                    </div>
                    {/*selectedFile !== null &&*/ this.renderFileDetail(selectedFile)}
                </div>
            </div>
        );
    }
}

export default connect()(ArrDao);
