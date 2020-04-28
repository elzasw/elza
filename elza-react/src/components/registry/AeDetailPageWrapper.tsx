import React, {ReactElement, useEffect, useState} from 'react';
import {ThunkDispatch} from 'redux-thunk';
import {Action} from 'redux';
//import {APPROVE_AE_DETAIL_AREA, DETAIL_VALIDATION_RESULT, EDIT_AE_DETAIL_AREA} from '../constants';
import {connect} from 'react-redux';
//import ContentSpinner from "../components/Loader";
//import PageInfoContent from "../components/PageInfoContent";
//import * as DetailActions from "../shared/reducers/detail/DetailActions";
import {
    AeDetailVO,
    AePartFormVO,
    AePartNameVO,
    AePartVO,
    AeValidationErrorsVO,
    PartType,
} from '../../api/generated/model';
import * as AePartInfo from "../../api/AePartInfo";
import {PartSectionsType} from "../../api/AePartInfo";
import * as PartTypeInfo from "../../api/PartTypeInfo";
import DetailMultiSection from "./Detail/DetailMultiSection";
//import * as ModalActions from "../shared/reducers/modal/ModalActions";
//import PartEditModal from "../components/modal/PartEditModal";
//import {sortItems} from "../itemutils";
//import {CodelistData} from "../shared/reducers/codelist/CodelistTypes";
//import {CodelistState} from "../shared/reducers/codelist/CodelistReducer";
import DetailHistory from "./Detail/DetailHistory";
import Loading from '../shared/loading/Loading';
import {MOCK_CODE_DATA} from './Detail/mock';
//import * as EntitiesClientApiCall from "./../api/call/EntitiesClientApiCall";
//import {filterPartFormForSubmit} from "../partutils";


type OwnProps = {
  id: number; // ae id
  area: string;
  sider: ReactElement;
  header: ReactElement;
  editMode: boolean;
  globalCollapsed: boolean;
  validationResult?: AeValidationErrorsVO;
  globalEntity: boolean;
}

type Props = OwnProps & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps>;

// @ts-ignore
const MOCK: AeDetailVO = {"head":{"@class":"AeDetailHeadGlobal","name":"Wallis a Futuna","aeTypeId":23,"aeState":"APS_NEW","lastChange":{"id":null,"change":"2020-01-21T14:59:17.266","user":{"id":null,"displayName":"pubal"}},"description":"souostrovĂ­ v TichĂ©m oceĂˇnu (OceĂˇnii), zĂˇmoĹ™skĂ© spoleÄŤenstvĂ­ Francie","id":231},"existsUnfinished":null,"content":[{"@class":"AePartBody","id":null,"uuid":"f5266b53-091e-441b-836f-a3980a635343","textValue":"souostrovĂ­ v TichĂ©m oceĂˇnu (OceĂˇnii), zĂˇmoĹ™skĂ© spoleÄŤenstvĂ­ Francie","parentPartId":null,"parentPartUuid":null,"items":[{"@class":"AeItemRecordRef","id":null,"uuid":"838945eb-4f7b-4b9b-a433-5e05d187364d","itemTypeId":5,"itemSpecId":null,"textValue":"Francie","value":59},{"@class":"AeItemEnum","id":null,"uuid":"193396da-cc93-4e7a-bb00-055447dbfad1","itemTypeId":4,"itemSpecId":133,"textValue":"stát"},{"@class":"AeItemString","id":null,"uuid":"e050074a-4600-414c-9a9b-68e3dbf27bd0","itemTypeId":25,"itemSpecId":null,"textValue":"souostrovĂ­ v TichĂ©m oceĂˇnu (OceĂˇnii), zĂˇmoĹ™skĂ© spoleÄŤenstvĂ­ Francie","value":"souostrovĂ­ v TichĂ©m oceĂˇnu (OceĂˇnii), zĂˇmoĹ™skĂ© spoleÄŤenstvĂ­ Francie"}]},{"@class":"AePartIdentifier","id":null,"uuid":"480f19a8-02e4-4179-afed-33f48afa042c","textValue":"ISO 3166-1 alpha-2: WF","parentPartId":null,"parentPartUuid":null,"items":[{"@class":"AeItemEnum","id":null,"uuid":"24a8c9ac-1bb7-4b1c-872b-f5c9e441915d","itemTypeId":7,"itemSpecId":152,"textValue":"ISO 3166-1 alpha-2"},{"@class":"AeItemString","id":null,"uuid":"0650bcbf-042b-4079-9fa2-5fc24bab0256","itemTypeId":10,"itemSpecId":null,"textValue":"WF","value":"WF"}]},{"@class":"AePartIdentifier","id":null,"uuid":"fdff5f13-ecbc-4d7f-9328-d11da5d3b4cf","textValue":"ISO 3166-1 alpha-3: WLF","parentPartId":null,"parentPartUuid":null,"items":[{"@class":"AeItemEnum","id":null,"uuid":"30135569-c783-4b80-ab92-769816ac1f8c","itemTypeId":7,"itemSpecId":153,"textValue":"ISO 3166-1 alpha-3"},{"@class":"AeItemString","id":null,"uuid":"76e2b0a3-3c73-41e2-a260-3b2ca55e7de1","itemTypeId":10,"itemSpecId":null,"textValue":"WLF","value":"WLF"}]},{"@class":"AePartName","id":null,"uuid":"384b10df-a375-48cb-864d-6485504cdf31","textValue":"Wallis a Futuna","parentPartId":null,"parentPartUuid":null,"items":[{"@class":"AeItemString","id":null,"uuid":"7d1ddc1a-6432-47f2-b784-e6a94a05b2dd","itemTypeId":12,"itemSpecId":null,"textValue":"Wallis a Futuna","value":"Wallis a Futuna"},{"@class":"AeItemEnum","id":null,"uuid":"2412ea3c-96c3-4bf3-9767-c14ddd2bab07","itemTypeId":19,"itemSpecId":350,"textValue":"zjednodušená podoba"},{"@class":"AeItemEnum","id":null,"uuid":"7ac765a7-b0f2-4fae-8b67-12b73d42841d","itemTypeId":22,"itemSpecId":214,"textValue":"čeština"}],"preferred":true},{"@class":"AePartName","id":null,"uuid":"242e18a4-60e4-4dc9-8d63-e94dd233bd36","textValue":"Teritorium Wallisovy ostrovy a Futuna","parentPartId":null,"parentPartUuid":null,"items":[{"@class":"AeItemString","id":null,"uuid":"0d6d5a11-ccda-4658-8ee9-df59bd6ef2c1","itemTypeId":12,"itemSpecId":null,"textValue":"Teritorium Wallisovy ostrovy a Futuna","value":"Teritorium Wallisovy ostrovy a Futuna"},{"@class":"AeItemEnum","id":null,"uuid":"d89d6f9f-6555-4634-8f06-4a836e988e82","itemTypeId":19,"itemSpecId":348,"textValue":"úřední"},{"@class":"AeItemEnum","id":null,"uuid":"56c21076-9f68-47f5-a1c0-d6f47710d6b6","itemTypeId":22,"itemSpecId":214,"textValue":"čeština"}],"preferred":false},{"@class":"AePartName","id":null,"uuid":"ec6bf58f-fc26-46b3-b146-700b669fc772","textValue":"Wallis and Futuna","parentPartId":null,"parentPartUuid":null,"items":[{"@class":"AeItemString","id":null,"uuid":"3e9e56f9-1bc7-408d-a066-ba4a311c960b","itemTypeId":12,"itemSpecId":null,"textValue":"Wallis and Futuna","value":"Wallis and Futuna"},{"@class":"AeItemEnum","id":null,"uuid":"a59884df-8323-4f10-bfd8-388e92735da0","itemTypeId":19,"itemSpecId":350,"textValue":"zjednodušená podoba"},{"@class":"AeItemEnum","id":null,"uuid":"d1b8e6e1-26a7-4a76-a0eb-6f2e8d056f99","itemTypeId":22,"itemSpecId":167,"textValue":"angličtina"}],"preferred":false},{"@class":"AePartName","id":null,"uuid":"5a0bdf07-c1f5-4168-bb33-d8c547e07706","textValue":"Wallis and Futuna Islands","parentPartId":null,"parentPartUuid":null,"items":[{"@class":"AeItemString","id":null,"uuid":"a1e103ff-05d4-4c08-bb48-064ec0338d3f","itemTypeId":12,"itemSpecId":null,"textValue":"Wallis and Futuna Islands","value":"Wallis and Futuna Islands"},{"@class":"AeItemEnum","id":null,"uuid":"54257036-a783-4f79-88ee-8352d201c4a1","itemTypeId":19,"itemSpecId":348,"textValue":"úřední"},{"@class":"AeItemEnum","id":null,"uuid":"ec0598bd-71cf-4e73-92af-e1b7e2f8e0e4","itemTypeId":22,"itemSpecId":167,"textValue":"angličtina"}],"preferred":false}]};

/**
 * Detail globální archivní entity.
 */
const AeDetailPageWrapper: React.FC<Props> = props => {
  const [partSections, setPartSections] = useState<PartSectionsType | undefined>(undefined);
  const aeTypeId = props.detail.fetched ? props.detail.data.head.aeTypeId : 0;

  useEffect(() => {
    if (props.detail.data) {
      setPartSections(AePartInfo.getPartSections(props.detail.data.content));
    } else {
      setPartSections(undefined);
    }
  }, [props.detail.data]);

  const handleSetPreferred = (part: AePartNameVO) => {
    if (part.id) {
      props.setPreferred(props.area, props.id, part.id);
      props.invalidateValidationErrors(props.id);
    }
  };

  const handleDelete = (part: AePartVO) => {
    if (part.id) {
      props.deletePart(props.area, props.id, part.id);
    }
      props.invalidateValidationErrors(props.id);
  };

  const handleEdit = (part: AePartVO) => {
    part.id && props.showPartEditModal(props.area, part, props.id, aeTypeId, props.codelist, part.parentPartId);
    props.invalidateValidationErrors(props.id);
  };

  const handleAdd = (partType: PartType) => {
    props.showPartCreateModal(props.area, partType, props.id, aeTypeId);
    props.invalidateValidationErrors(props.id);
  };

  const handleAddRelated = (part: AePartVO) => {
    part.id && props.showPartCreateModal(props.area, PartType.REL, props.id, aeTypeId, part.id);
    props.invalidateValidationErrors(props.id);
  };

  const handleDeletePart = (parts: Array<AePartVO>) => {
    props.deleteParts(props.area, props.id, parts);
    props.invalidateValidationErrors(props.id);
  };

  const renderHistory = (detailFetching: boolean) => {
    if (!detailFetching) {

        // @ts-ignore
    return <DetailHistory entityId={props.id} commentCount={props.detail.data.head.comments}/>;
    }
  };

  const getPartSections = (partType: PartType): AePartVO[] => {
    if (partSections) {
      let parts: AePartVO[] = [];
      switch (partType) {
        case PartType.BODY:
          parts = partSections.body;
          break;
        case PartType.CRE:
          parts = partSections.creation;
          break;
        case PartType.EVENT:
          parts = partSections.events;
          break;
        case PartType.EXT:
          parts = partSections.extinction;
          break;
        case PartType.IDENT:
          parts = partSections.identifiers;
          break;
        case PartType.NAME:
          parts = partSections.names;
          break;
        case PartType.REL:
          parts = partSections.relations;
          break;
      }

      return parts.filter(value => !value.parentPartUuid);
    } else {
      return [];
    }
  };

  const getRelatedPartSections = (parentPartType: PartType): AePartVO[] => {
    const parentParts = getPartSections(parentPartType);

    if (parentParts.length === 0) {
      return [];
    }

    const parentIds = parentParts.filter(value => value.uuid).map(value => value.uuid);
    const allParts = props.detail.data ? props.detail.data.content as AePartVO[] : [];
    return allParts.filter(value => value.parentPartUuid).filter(value => parentIds.includes(value.parentPartUuid));
  };

  const isFetching = !props.detail.fetched || props.detail.isFetching;

  const namePartSection = <DetailMultiSection
    key="oznaceni"
    label="Označení"
    editMode={props.editMode}
    parts={getPartSections(PartType.NAME)}
    relatedParts={getRelatedPartSections(PartType.NAME)}
    globalCollapsed={props.globalCollapsed}
    onSetPreferred={handleSetPreferred}
    onEdit={handleEdit}
    onDelete={handleDelete}
    onAdd={() => handleAdd(PartType.NAME)}
    validationResult={props.validationResult}
    globalEntity={props.globalEntity}
  />;

  const creationPartSection = <DetailMultiSection
    key="vznik"
    label="Vznik"
    singlePart
    editMode={props.editMode}
    parts={getPartSections(PartType.CRE)}
    relatedParts={getRelatedPartSections(PartType.CRE)}
    globalCollapsed={props.globalCollapsed}
    onEdit={handleEdit}
    onDelete={handleDelete}
    onAdd={() => handleAdd(PartType.CRE)}
    onAddRelated={handleAddRelated}
    onDeleteParts={handleDeletePart}
    validationResult={props.validationResult}
    globalEntity={props.globalEntity}
  />;

  const extinctionPartSection = <DetailMultiSection
    key="zanik"
    label="Zánik"
    singlePart
    editMode={props.editMode}
    parts={getPartSections(PartType.EXT)}
    relatedParts={getRelatedPartSections(PartType.EXT)}
    globalCollapsed={props.globalCollapsed}
    onEdit={handleEdit}
    onDelete={handleDelete}
    onAdd={() => handleAdd(PartType.EXT)}
    onAddRelated={handleAddRelated}
    onDeleteParts={handleDeletePart}
    validationResult={props.validationResult}
    globalEntity={props.globalEntity}
  />;

  const bodyPartSection = <DetailMultiSection
    key="telo"
    label="Tělo"
    singlePart
    editMode={props.editMode}
    parts={getPartSections(PartType.BODY)}
    relatedParts={getRelatedPartSections(PartType.BODY)}
    globalCollapsed={props.globalCollapsed}
    onEdit={handleEdit}
    onDelete={handleDelete}
    onAdd={() => handleAdd(PartType.BODY)}
    onDeleteParts={handleDeletePart}
    validationResult={props.validationResult}
    globalEntity={props.globalEntity}
  />;

  const eventPartSection = <DetailMultiSection
    key="udalosti"
    label="Události"
    editMode={props.editMode}
    parts={getPartSections(PartType.EVENT)}
    relatedParts={getRelatedPartSections(PartType.EVENT)}
    globalCollapsed={props.globalCollapsed}
    onEdit={handleEdit}
    onDelete={handleDelete}
    onAdd={() => handleAdd(PartType.EVENT)}
    onAddRelated={handleAddRelated}
    onDeleteParts={handleDeletePart}
    validationResult={props.validationResult}
    globalEntity={props.globalEntity}
  />;

  const relationPartSection = <DetailMultiSection
    key="vazby"
    label="Vazby"
    editMode={props.editMode}
    parts={getPartSections(PartType.REL)}
    relatedParts={getRelatedPartSections(PartType.REL)}
    globalCollapsed={props.globalCollapsed}
    onEdit={handleEdit}
    onDelete={handleDelete}
    onAdd={() => handleAdd(PartType.REL)}
    onDeleteParts={handleDeletePart}
    validationResult={props.validationResult}
    globalEntity={props.globalEntity}
  />;

  const identifierPartSection = <DetailMultiSection
    key="ext-identifikatory"
    label="Externí identifikátory"
    editMode={props.editMode}
    parts={getPartSections(PartType.IDENT)}
    relatedParts={getRelatedPartSections(PartType.IDENT)}
    globalCollapsed={props.globalCollapsed}
    onEdit={handleEdit}
    onDelete={handleDelete}
    onAdd={() => handleAdd(PartType.IDENT)}
    onDeleteParts={handleDeletePart}
    validationResult={props.validationResult}
    globalEntity={props.globalEntity}
  />;

  return <div>
  {isFetching && <Loading />}
  {!isFetching && <div key="1" className="layout-scroll">
    {props.header}

    {partSections && <div key="part-sections">
      {props.codelist.partTypes.map((partType, index) => {
        switch (partType) {
          case PartType.BODY:
            return bodyPartSection;
          case PartType.CRE:
            return creationPartSection;
          case PartType.EVENT:
            return eventPartSection;
          case PartType.EXT:
            return extinctionPartSection;
          case PartType.IDENT:
            return identifierPartSection;
          case PartType.NAME:
            return namePartSection;
          case PartType.REL:
            return relationPartSection;
          default:
            console.warn("Nepodporovaný typ party pro render detailu", partType);
            return <div key={index}/>
        }
      })}
    </div>}
  </div>}
    {renderHistory(isFetching)}
  </div>
};

const mapDispatchToProps = (
  dispatch: ThunkDispatch<{}, {}, Action<string>>
) => ({
  showPartEditModal: (area: string, part: AePartVO, aeId: number, aeTypeId: number, codelist: any, parentPartId?: number) => {
    const partType = PartTypeInfo.getPartType(part["@class"]);
    /*dispatch(
      ModalActions.showForm(PartEditModal, {
        createDialog: false,
        partType,
        aeTypeId,
        parentPartId,
        aeId,
        partId: part.id,
        initialValues: {
          part: partType,
          items: sortItems(partType, part.items, codelist)
        },
        onSubmit: (formData: AePartFormVO) => {
          if (part.id) {
            formData.parentPartId = parentPartId;
            return EntitiesClientApiCall.formApi
              .updatePart(aeId, part.id, filterPartFormForSubmit(formData));
          }
        },
        onSubmitSuccess: () => {
          dispatch(ModalActions.hide());
          dispatch(DetailActions.invalidate(area, aeId))
        }
      }, {
        title: PartTypeInfo.getPartEditDialogLabel(partType, false),
        width: "1200px"
      })
    )*/
  },
  showPartCreateModal: (area: string, partType: PartType, aeId: number, aeTypeId: number, parentPartId?: number) => {
    /*dispatch(
      ModalActions.showForm(PartEditModal, {
        createDialog: true,
        partType,
        aeTypeId,
        parentPartId,
        aeId,
        initialValues: {
          part: partType,
          items: []
        },
        onSubmit: (formData: AePartFormVO) => {
          formData.parentPartId = parentPartId;
          return EntitiesClientApiCall.formApi
            .createPart(aeId, filterPartFormForSubmit(formData));
        },
        onSubmitSuccess: () => {
          dispatch(ModalActions.hide());
          dispatch(DetailActions.invalidate(area, aeId))
        }
      }, {
        title: PartTypeInfo.getPartEditDialogLabel(partType, true),
        width: "1200px"
      })
    )*/
  },
  setPreferred: async (area: string, aeId: number, partId: number) => {
    /*
    await EntitiesClientApiCall.standardApi
      .setPreferName(aeId, partId);

    dispatch(DetailActions.invalidate(area, aeId))
    */
  },
  deletePart: async (area: string, aeId: number, partId: number) => {
    /*
    await EntitiesClientApiCall.standardApi
      .deletePart(aeId, partId);

    dispatch(DetailActions.invalidate(area, aeId))
    */
  },
  deleteParts: async (area: string, aeId: number, parts: Array<AePartVO>) => {
    /*
    for (let part of parts) {
      if (part.id) {
        await EntitiesClientApiCall.standardApi
          .deletePart(aeId, part.id);
      }
    }

    dispatch(DetailActions.invalidate(area, aeId))
    */
  },
  invalidateValidationErrors: (aeId: number) => {
    //dispatch(DetailActions.invalidate(DETAIL_VALIDATION_RESULT, aeId))
  },
});

const mapStateToProps = (state: any, props: OwnProps) => {
  const {codelist, app}: { codelist, app: any } = state;

  return {
    detail: {
        //app[props.area]
        isFetching: false,
        fetched: true,
        data: MOCK
    },
      //detail:{"data":{"head":{"@class":"AeDetailHeadGlobal","name":"AAAAA","aeTypeId":24,"aeState":"APS_NEW","lastChange":{"id":null,"change":"2020-02-11T10:49:47.37","user":{"id":null,"displayName":"Jaroslav PÅ¯bal"}},"description":"","id":264},"existsUnfinished":null,"content":[{"@class":"AePartName","id":null,"uuid":"c2679c41-e2f3-42f9-9d5e-851fa664a20b","textValue":"AAAAA","parentPartId":null,"parentPartUuid":null,"items":[{"@class":"AeItemString","id":null,"uuid":"3ba7ef42-a58b-4c06-a976-a6f54c12ae1d","itemTypeId":12,"itemSpecId":null,"textValue":"AAAAA","value":"AAAAA"}],"preferred":true}]},"id":264,"isFetching":false,"fetched":true,"currentDataKey":"{}_264","filter":{}},
    codelist: MOCK_CODE_DATA
  }
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AeDetailPageWrapper);
