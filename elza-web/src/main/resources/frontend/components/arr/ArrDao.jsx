require ('./ArrDao.less');

import React from "react";
import {connect} from "react-redux";
import {FormInput, Icon, AbstractReactComponent, i18n, ListBox} from "components/index.jsx";
import {indexById} from "stores/app/utils.jsx";
import {Form, Button} from "react-bootstrap";
import {dateToString} from "components/Utils.jsx";
import {userDetailsSaveSettings} from "actions/user/userDetail.jsx";
import {fundChangeReadMode} from "actions/arr/fund.jsx";
import {setSettings, getOneSettings} from "components/arr/ArrUtils.jsx";
import {humanFileSize} from "components/Utils.jsx";

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
        return <Form inline className="dao-files-detail">
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
    };

    render() {
        const {fund, dao} = this.props;
        const {selectedFile} = this.state;

        if (dao == null) {
            return (<div>...</div>)
        }

        // Položky souborů včetně skupin
        const filesMap = {};    // mapa id na položku
        const options = [];
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

        return (
                <div className="dao-container">
                    <div className="dao-detail">
                        <div className="title"><i>Digitalizát</i>: {dao.label}</div>
                        <div className="info">
                            <span>ID: {dao.code}</span><br />
                            {dao.url && <span>Url: <a target="_blank" href={dao.url}>{dao.url}</a></span>}
                        </div>
                    </div>
                    <div className="dao-files">
                        <select size="2" className='dao-files-list' onChange={(e) => { this.handleSelect(filesMap[e.target.value]) }}>
                            {options}
                        </select>
                        {selectedFile !== null && this.renderFileDetail(selectedFile)}
                    </div>
                </div>
        );
    }
}

export default connect()(ArrDao);
