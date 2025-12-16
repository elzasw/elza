import { Spinner } from "@fluentui/react-components";
import { WebApi } from "actions";
import {
  structureNodeFormFetchIfNeeded,
  structureNodeFormSelectId,
} from "actions/arr/structureNodeForm";
import DescItemFactory from "components/arr/nodeForm/DescItemFactory";
import StructureSubNodeForm from "components/arr/structure/StructureSubNodeForm";
import { DataStructureRef } from "elza-api";
import { useEffect, useState } from "react";
import { StructureType } from "typings/store";
import { useAppThunkDispatch } from "utils/hooks";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useActiveFund } from "../hooks";

interface Props {
  data: DataStructureRef;
  structureType: StructureType;
  readOnly?: boolean;
  onCreate?: (structureObjectId: number) => Promise<void>;
}

export function AnonymousStructure({
  structureType,
  data,
  onCreate,
  readOnly,
}: Props) {
  const { id: fundId, versionId: fundVersionId } = useActiveFund();
  const [structureObjectId, setStructureObjectId] = useState<number>(
    data.structuredObjectId,
  );

  const dispatch = useAppThunkDispatch();

  const structureNodeForm = useAppSelector(
    ({ structures }) =>
      (structures as any).stores?.[structureObjectId] || undefined,
  );

  // function initAnonym() {
  //   if (data.structuredObjectId) {
  //     dispatch(structureNodeFormSelectId(fundVersionId, data.structuredObjectId));
  //     dispatch(structureNodeFormFetchIfNeeded(fundVersionId, data.structuredObjectId));
  //   }
  //   // } else if (props.cal) {
  //   //   // skip init - calc
  //   //   WebApi.createStructureData(fundVersionId, structureTypeCode, this.findValue()).then(structureData => {
  //   //     this.props.onChange({id: structureData.id});
  //   //     props.dispatch(structureNodeFormSelectId(props.versionId, structureData.id));
  //   //   });
  //   // }
  // }

  useEffect(() => {
    if (structureObjectId == undefined) {
      // console.log("#distr - create temp structure", data);
      (async function () {
        const structureData = await WebApi.createStructureData(
          fundVersionId,
          structureType.code,
          // this.findValue(),
        );
        // console.log("#distr - created temp structure", structureData);
        setStructureObjectId(structureData.id);
        await onCreate(structureData.id);
        dispatch(structureNodeFormSelectId(fundVersionId, structureData.id));
        dispatch(
          structureNodeFormFetchIfNeeded(fundVersionId, structureData.id),
        );
      })();
    } else if (!structureNodeForm) {
      dispatch(structureNodeFormSelectId(fundVersionId, structureObjectId));
      dispatch(
        structureNodeFormFetchIfNeeded(fundVersionId, structureObjectId),
      );
    }
  }, [
    structureObjectId,
    fundVersionId,
    dispatch,
    structureNodeForm,
    structureType.code,
    onCreate,
  ]);

  // console.log("#astr", data.structuredObjectId, structureNodeForm);

  return (
    <div className="desc-item-value desc-item-value-parts">
      {structureNodeForm && structureNodeForm.fetched ? (
        <StructureSubNodeForm
          id={structureNodeForm.id}
          versionId={fundVersionId}
          readMode={readOnly}
          fundId={fundId}
          selectedSubNodeId={structureNodeForm.id}
          descItemFactory={DescItemFactory}
        />
      ) : (
        <Spinner />
      )}
    </div>
  );
}
