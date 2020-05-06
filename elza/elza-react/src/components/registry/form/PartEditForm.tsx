import React from 'react';
import {Form, InjectedFormProps, reduxForm} from 'redux-form';
import {connect} from "react-redux";
import {ApPartFormVO, PartType} from "../../../api/generated/model";
import {ThunkDispatch} from "redux-thunk";
import {Action} from "redux";

type OwnProps = {
    partType: PartType;
    apTypeId: number;
    formData?: ApPartFormVO;
    parentPartId?: number;
    aeId?: number;
    partId?: number;

    handleSubmit?: () => void;
    onSubmit?: (formData: any) => void;
    initialValues?: any;
}

type Props = {
} & OwnProps & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps>;


const PartEditForm: React.FC<InjectedFormProps> = (props: any) => {
    const {handleSubmit} = props;

    return <Form onSubmit={handleSubmit}>

    </Form>;
};

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>, props: OwnProps) => ({});

const mapStateToProps = (state: any) => {
    return {}
};

export default connect(mapStateToProps, mapDispatchToProps)(reduxForm({
    // a unique name for the form
    form: 'contact'
})(PartEditForm));
