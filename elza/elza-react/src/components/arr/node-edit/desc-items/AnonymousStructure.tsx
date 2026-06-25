import { Spinner } from '@fluentui/react-components';
import { Api } from 'api/api';
import { StructureEdit } from 'components/arr/structure/StructureEdit';
import { StructureView } from 'components/arr/structure/StructureView';
import { DataStructureRef } from 'elza-api';
import { useEffect, useState } from 'react';
import { StructureType } from 'typings/store';
import { useActiveFund } from '../hooks';

interface Props {
  data: DataStructureRef;
  structureType: StructureType;
  readOnly?: boolean;
  onCreate?: (structureObjectId: number) => Promise<void>;
}

export function AnonymousStructure({ structureType, data, onCreate, readOnly }: Props) {
  const { id: fundId, versionId: fundVersionId } = useActiveFund();
  const [structureObjectId, setStructureObjectId] = useState<number>(data.structuredObjectId);

  useEffect(() => {
    if (structureObjectId != undefined) {
      return;
    }
    (async function () {
      const { data: structureData } = await Api.structure.sdoCreateObject(fundId, structureType.code);
      setStructureObjectId(structureData.id);
      await onCreate(structureData.id);
    })();
  }, [structureObjectId, fundVersionId, structureType.code, onCreate]);

  return (
    <div
      style={{
        width: '100%',
        border: 'var(--primary-border)',
        background: 'var(--shade-2)',
        borderRadius: '8px',
        padding: readOnly ? '0px' : '8px',
        overflow: 'auto',
        margin: '2px',
      }}
      className="desc-item-value desc-item-value-parts"
    >
      {structureObjectId == undefined ? (
        <Spinner />
      ) : readOnly ? (
        <StructureView fundId={fundId} fundVersionId={fundVersionId} structureObjectId={structureObjectId} />
      ) : (
        <StructureEdit fundId={fundId} fundVersionId={fundVersionId} structureObjectId={structureObjectId} confirmOnCreate={true} />
      )}
    </div>
  );
}
