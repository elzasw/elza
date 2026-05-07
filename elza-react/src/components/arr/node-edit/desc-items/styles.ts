import { makeStyles } from '@fluentui/react-components';

export const useStyles = makeStyles({
  dataPillWrapper: {
    margin: '0 -2px',
    display: 'flex',
  },
  dataPill: {
    background: 'var(--shade-4)',
    padding: '0px 4px',
    borderRadius: '4px',
    display: 'inline-block',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    maxWidth: '250px',
    margin: '2px',
  },
});
