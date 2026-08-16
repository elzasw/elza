import { Spinner } from '@fluentui/react-components';
import { WebApi } from 'actions';
import { StructureEdit } from 'components/arr/structure/StructureEdit';
import { StructureView } from 'components/arr/structure/StructureView';
import { DataStructureRef } from 'elza-api';
import { useEffect, useState } from 'react';
import { StructureType } from 'typings/store';
import { useActiveFund } from 'utils/hooks';

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
    // Read-only display must not create a structure object; there is simply nothing to show yet.
    if (structureObjectId != undefined || readOnly) {
      return;
    }
    (async function () {
      const structureData = await WebApi.createStructureData(fundVersionId, structureType.code);
      setStructureObjectId(structureData.id);
      await onCreate?.(structureData.id);
    })();
  }, [structureObjectId, fundVersionId, structureType.code, onCreate, readOnly]);

  return (
    <div
      style={{
        width: '100%',
        border: 'var(--primary-border)',
        background: 'var(--shade-2)',
        borderRadius: '8px',
        padding: readOnly ? '0px' : '8px',
        overflow: 'auto',
      }}
      className="desc-item-value desc-item-value-parts"
    >
      {structureObjectId == undefined ? (
        readOnly ? null : <Spinner />
      ) : readOnly ? (
        <StructureView fundId={fundId} fundVersionId={fundVersionId} structureObjectId={structureObjectId} />
      ) : (
        <StructureEdit fundId={fundId} fundVersionId={fundVersionId} structureObjectId={structureObjectId} confirmOnCreate={true} />
      )}
    </div>
  );
}
