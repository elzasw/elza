package cz.tacr.elza.service;

import cz.tacr.elza.domain.DbHibernateSequence;
import cz.tacr.elza.repository.DbHibernateSequenceRepository;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementace sekvenceru, který využívá struktury/metodiky hibernate.
 *
 * @since 25.07.2018
 */
@Service
public class SequenceServiceImpl implements SequenceService {

    private final DbHibernateSequenceRepository hibernateSequenceRepository;
    private final ApplicationContext appCtx;
    private final EntityManager em;

    /**
     * Počet id, které si alokuje sequncer při prvním vyžádání.
     */
    private static final int MOUNT_SIZE = 100;
    private static ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();
    private static ConcurrentMap<String, Queue<Integer>> cache = new ConcurrentHashMap<>();

    /**
     * Získání zámku pro konkrétní sekvenci.
     *
     * @param sequenceName název sekvence
     * @return zámek
     */
    private static Object getLock(String sequenceName) {
        Object lock = locks.get(sequenceName);
        if (lock == null) {
            Object newLock = new Object();
            lock = locks.putIfAbsent(sequenceName, newLock);
            if (lock == null) {
                lock = newLock;
            }
        }
        return lock;
    }

    @Autowired
    public SequenceServiceImpl(final DbHibernateSequenceRepository hibernateSequenceRepository,
                               final ApplicationContext appCtx,
                               final EntityManager em) {
        this.hibernateSequenceRepository = hibernateSequenceRepository;
        this.appCtx = appCtx;
        this.em = em;
    }

    @Override
    public int getNext(final String sequenceName) {
        Validate.notBlank(sequenceName);

        synchronized (getLock(sequenceName)) {

            Queue<Integer> sequenceIds = cache.computeIfAbsent(sequenceName, k -> new LinkedList<>());

            if (sequenceIds.size() == 0) {
                appCtx.getBean(SequenceServiceImpl.class).loadSequence(sequenceName, sequenceIds, MOUNT_SIZE);
            }

            return sequenceIds.poll();
        }
    }

    @Override
    public int[] getNext(final String sequenceName, final int count) {
        Validate.notBlank(sequenceName);
        Validate.isTrue(count > 0);

        synchronized (getLock(sequenceName)) {

            Queue<Integer> sequenceIds = cache.computeIfAbsent(sequenceName, k -> new LinkedList<>());

            int needLoad = count - sequenceIds.size();

            if (needLoad > 0) {
                appCtx.getBean(SequenceServiceImpl.class).loadSequence(sequenceName, sequenceIds, needLoad);
            }

            int[] result = new int[count];
            for (int i = 0; i < count; i++) {
                result[i] = sequenceIds.poll();
            }
            return result;
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void loadSequence(final String sequenceName, final Queue<Integer> sequenceIds, int count) {
        int val;
        int nextVal;
        do {
            DbHibernateSequence sequence = hibernateSequenceRepository.findById(sequenceName).orElse(null);
            if (sequence == null) {
                sequence = new DbHibernateSequence();
                sequence.setSequenceName(sequenceName);
                sequence.setNextVal(1L);
                hibernateSequenceRepository.save(sequence);
            }
            em.detach(sequence);
            val = sequence.getNextVal().intValue();
            nextVal = val + count;
        } while (hibernateSequenceRepository.setNextVal(sequenceName, nextVal, val) == 0);
        sequenceIds.addAll(IntStream.range(val, nextVal).boxed().collect(Collectors.toList()));
    }

}
