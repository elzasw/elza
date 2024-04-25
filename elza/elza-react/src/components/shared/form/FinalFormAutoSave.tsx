import { useEffect, useState } from 'react';
import { useForm, FormSpy, FormSpyRenderProps } from 'react-final-form';

/**
 * Component for submitting form on blur event of any field
 */
function DirtyBlurSaveSpyComponent({ active, dirtyFields }: FormSpyRenderProps) {
  const form = useForm();
  const [lastActive, setLastActive] = useState(active);

  useEffect(() => {
    // submit form when last active field has been changed
    // and blur event has been fired
    if (lastActive !== active && dirtyFields[lastActive]) {
      form.submit();
    }

    setLastActive(active)
  }, [active])

  return <></>
}

export function AutoSave() {
  return <FormSpy
    subscription={{ active: true, dirtyFields: true }}
    component={DirtyBlurSaveSpyComponent}
  />
}
