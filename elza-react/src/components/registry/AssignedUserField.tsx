import { Button, makeStyles, tokens } from '@fluentui/react-components';
import { Api } from 'api';
import UserPicker from 'components/admin/UserPicker';
import { Participant } from 'elza-api';
import { useEffect, useState } from 'react';

const useStyles = makeStyles({
    participants: {
        display: 'flex',
        flexWrap: 'wrap',
        gap: tokens.spacingHorizontalS,
        marginTop: tokens.spacingVerticalS,
    },
});

export interface AssignedUserFieldProps {
    accessPointId: number;
    /** Id of the assigned user. */
    value?: number;
    onChange: (userId: number | undefined) => void;
    label: string;
    disabled?: boolean;
    error?: string;
    /** Id of the current user: not offered among the last participants. */
    currentUserId?: number;
    excludeUserIds?: number[];
}

/**
 * User the entity is assigned to, with the users who last worked on the entity offered for one click.
 */
export const AssignedUserField = ({
    accessPointId,
    value,
    onChange,
    label,
    disabled,
    error,
    currentUserId,
    excludeUserIds,
}: AssignedUserFieldProps) => {
    const styles = useStyles();
    const [participants, setParticipants] = useState<Participant[]>([]);

    useEffect(() => {
        let cancelled = false;
        Api.accesspoints.accessPointGetLastParticipants(accessPointId).then(({ data }) => {
            if (!cancelled) {
                const unique = new Map(data.map((participant) => [participant.userId, participant]));
                setParticipants(Array.from(unique.values()).filter(({ userId }) => userId !== currentUserId));
            }
        });
        return () => {
            cancelled = true;
        };
    }, [accessPointId, currentUserId]);

    return (
        <div>
            <UserPicker
                label={label}
                value={value}
                onChange={onChange}
                disabled={disabled}
                error={error}
                excludeUserIds={excludeUserIds}
            />
            {participants.length > 0 && (
                <div className={styles.participants}>
                    {participants.map((participant) => (
                        <Button
                            key={participant.userId}
                            size="small"
                            disabled={disabled}
                            onClick={() => onChange(participant.userId)}
                        >
                            {participant.name} ({participant.username})
                        </Button>
                    ))}
                </div>
            )}
        </div>
    );
};

export default AssignedUserField;
