import { Spinner } from '@fluentui/react-components';
import { WebApi } from 'actions';
import { StructureEdit } from 'components/arr/structure/StructureEdit';
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
      const structureData = await WebApi.createStructureData(fundVersionId, structureType.code);
      setStructureObjectId(structureData.id);
      await onCreate(structureData.id);
    })();
  }, [structureObjectId, fundVersionId, structureType.code, onCreate]);

  return (
    <div style={{
      width: '100%' ,
      border: 'var(--primary-border)',
      background: 'var(--shade-2)',
      borderRadius: '8px',
      padding: '8px',
      overflow: 'auto',
    }} className="desc-item-value desc-item-value-parts">
        {structureObjectId != undefined ? (
          <StructureEdit
            fundId={fundId}
            fundVersionId={fundVersionId}
            structureObjectId={structureObjectId}
            readMode={readOnly}
          />
        ) : (
            <Spinner />
          )}
    </div>
  );
}
