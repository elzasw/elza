import React from 'react';

interface Props {
  name: string;
  description?: string,
}

/**
 * Vyrenderuje název archivní entity včetně popisu.
 * @param name
 * @param description
 * @constructor
 */
const ArchiveEntityName: React.FC<Props> = ({name, description}) => {
  let useDescription = description;
  if (useDescription) {
    if (useDescription.length > 53) {
      useDescription = useDescription.substring(0, 50) + '…';
    }
  }

  return <span>
    {name}{useDescription && <small> {useDescription}</small>}
  </span>
};

export default ArchiveEntityName;
