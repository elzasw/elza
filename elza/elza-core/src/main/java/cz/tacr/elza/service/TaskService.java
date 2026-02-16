package cz.tacr.elza.service;

import static cz.tacr.elza.domain.WfTaskType.AP_CONFIRM;
import static cz.tacr.elza.domain.WfTaskType.AP_REV_CONFIRM;
import static cz.tacr.elza.domain.WfTaskType.AP_REV_UPDATE;
import static cz.tacr.elza.domain.WfTaskType.AP_UPDATE;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.controller.vo.Participant;
import cz.tacr.elza.controller.vo.TasksEntityType;
import cz.tacr.elza.controller.vo.TasksStatus;
import cz.tacr.elza.controller.vo.TasksViewDetail;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApRevState;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.RevStateApproval;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.WfTask;
import cz.tacr.elza.domain.WfTaskApRevState;
import cz.tacr.elza.domain.WfTask.Status;
import cz.tacr.elza.domain.WfTaskApState;
import cz.tacr.elza.domain.WfTaskType;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.WfTaskApRevStateRepository;
import cz.tacr.elza.repository.WfTaskApStateRepository;
import cz.tacr.elza.repository.WfTaskRepository;
import cz.tacr.elza.repository.WfTaskTypeRepository;
import jakarta.transaction.Transactional;

@Service
public class TaskService {

	public static final String ENTITY_DEFAULT_PARTICIPANTS = "ENTITY_DEFAULT_PARTICIPANTS";

	@Autowired
	private UserService userService;

	@Autowired
	private WfTaskRepository wfTaskRepository;

	@Autowired
	private WfTaskTypeRepository wfTaskTypeRepository;

	@Autowired
	private WfTaskApStateRepository wfTaskApStateRepository;

	@Autowired
	private WfTaskApRevStateRepository wfTaskApRevStateRepository;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private AccessPointService accessPointService;

	@Autowired
	private ApStateRepository stateRepository;

	@Autowired
	private ApChangeRepository changeRepository;

	/**
	 * Získání seznamu zpracovatelů entity + users from ENTITY_DEFAULT_PARTICIPANTS
	 * 
	 * @param state
	 * @return
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public List<Participant> getLastParticipants(ApAccessPoint accessPoint) {
		List<Participant> result = new ArrayList<>();

		List<UsrUser> users = changeRepository.findUsersByAccessPoint(accessPoint);

		// přidáváme uživatele ze skupiny ENTITY_DEFAULT_PARTICIPANTS
		List<UsrUser> groupUsers = userService.findUsersByGroupCode(ENTITY_DEFAULT_PARTICIPANTS);
		groupUsers.forEach(user -> {
			if (!users.contains(user)) {
				users.add(user);
			}
		});

		List<Integer> apIds = users.stream().map(u -> u.getAccessPointId()).toList();
        Map<Integer, ApIndex> apIndexMap = accessPointService.findPreferredPartIndexMapByIds(apIds); 

        List<ApState> apStates = stateRepository.findLastByAccessPointIds(apIds);
        Map<Integer, Integer> apIdScopeIdMap = apStates.stream().collect(Collectors.toMap(s -> s.getStateId(), s -> s.getScopeId()));

        // Permissions by user
		List<UsrPermission> permisions = permissionRepository.findAllByUserIn(users);
		Map<Integer, List<UsrPermission>> permissionMap = permisions.stream().collect(Collectors.groupingBy(UsrPermission::getUserId));

		users.forEach(u -> {
			Integer apId = u.getAccessPointId();
			ApIndex apIndex = apIndexMap.get(apId);
			String apName = apIndex != null ? apIndex.getIndexValue() : null; 
			Integer scopeId = apIdScopeIdMap.get(apId);
			List<UsrPermission> permissions = permissionMap.get(u.getUserId());

			Participant p = new Participant();
			p.setUserId(u.getUserId());
			p.setUsername(u.getUsername());
			p.setName(apName);
			p.setCanWrite(hasPermission(Permission.AP_SCOPE_WR_ALL, scopeId, permissions));
			p.setCanConfirm(hasPermission(Permission.AP_CONFIRM_ALL, scopeId, permissions));
			p.setCanEditConfirmed(hasPermission(Permission.AP_EDIT_CONFIRMED_ALL, scopeId, permissions));
			result.add(p);
		});

		return result;
	}

	private boolean hasPermission(final Permission permission, final Integer scopeId, final List<UsrPermission> permissions) {
		if(permissions == null) {
			return false;
		}
		for (UsrPermission up : permissions) {
			if (up.getPermission().equals(permission)
					|| (up.getPermission().equals(permission) && Objects.equals(scopeId, up.getScopeId()))
					|| up.getPermission().equals(Permission.ADMIN)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Create view detail for task
	 * @param task
	 * @param apState
	 * @param entityType
	 * @param apName
	 * @return
	 */
	static private TasksViewDetail createTaskViewDetail(WfTask task, ApState apState, TasksEntityType entityType, String apName) {
    	UsrUser creator = task.getCreator();
    	UsrUser closedBy = task.getClosedBy();
    	Objects.requireNonNull(apState);
    	
    	TasksViewDetail tvd = new TasksViewDetail();
    	tvd.setAssigneeId(task.getAssignee().getUserId());
    	tvd.setAssigneeName(task.getAssignee().getUsername());
    	tvd.setClosed(task.getTimeClosed());
    	if (closedBy != null) {
    		tvd.setClosedById(closedBy.getUserId());
    		tvd.setClosedByName(closedBy.getUsername());
    	}
    	tvd.setCreated(tvd.getCreated());
    	if (creator != null) {
    		tvd.setCreatorId(creator.getUserId());
    		tvd.setCreatorName(creator.getUsername());
    	}
    	tvd.setDescription(task.getDescription());
    	tvd.setPrimaryEntityId(apState.getAccessPointId());
    	tvd.setPrimaryEntityName(apName);
    	tvd.setPrimaryEntityType(entityType);
    	tvd.setStatus(TasksStatus.fromValue(task.getStatus().name()));
    	tvd.setTaskId(task.getTaskId());
    	tvd.setTaskTypeCode(task.getTaskType().getCode());
    	tvd.setTaskTypeId(task.getTaskType().getTaskTypeId());
    	tvd.setTaskTypeName(task.getTaskType().getName());
    	
    	return tvd;
	}

	/**
	 * Získání seznamu úkolů pro daného uživatele
	 * 
	 * @return seznam úkolů
	 */
	@Transactional()
	public List<TasksViewDetail> getMyTasks() {
		var userDetail = userService.getLoggedUserDetail();
		if(userDetail==null) {
			throw new AccessDeniedException("User is not logged in", Collections.emptyList());
		}
		// check if user has ID
		if(userDetail.getId() == null) {
			return Collections.emptyList();
		}

        List<WfTask> wfTasks = wfTaskRepository.findAllByAssigneeId(userDetail.getId());
        List<WfTaskApState> wfTaskApStates = wfTaskApStateRepository.findAllByTaskIn(wfTasks);
        List<WfTaskApRevState> wfTaskApRevStates = wfTaskApRevStateRepository.findAllByTaskIn(wfTasks);

        // create Map<taskId, ApState>
        Map<Integer, ApState> taskIdApStateMap = wfTaskApStates.stream()
        		.collect(Collectors.toMap(t -> t.getTaskId(), t -> t.getState()));
        wfTaskApRevStates.forEach(t -> {
        	taskIdApStateMap.put(t.getTaskId(), t.getState().getRevision().getState());
        });

        // list of taskId from WfTaskApState(s)
        List<Integer> taskApStateIds = wfTaskApStates.stream().map(t -> t.getTaskApStateId()).toList();

        // create Map<accessPointId, ApIndex> 
        List<Integer> apIds = taskIdApStateMap.values().stream().map(s -> s.getAccessPointId()).toList();
        Map<Integer, ApIndex> indexMap = accessPointService.findPreferredPartIndexMapByIds(apIds); 

        return wfTasks.stream().map(t -> {
        	TasksEntityType entityType = taskApStateIds.contains(t.getTaskId()) ? TasksEntityType.AP : TasksEntityType.AP_REV;
        	ApState apState = taskIdApStateMap.get(t.getTaskId());
        	ApIndex apIndex = indexMap.get(apState.getAccessPointId());
        	String apName = apIndex != null ? apIndex.getIndexValue() : null;
        	return createTaskViewDetail(t, apState, entityType, apName); 
        }
        ).collect(Collectors.toList());        
	}

	/**
	 * Vytvoření nového WfTask.
	 * 
	 * @param taskTypeCode
	 * @param assignTo
	 * @return
	 */
	@Transactional()
	private WfTask createWfTask(String taskTypeCode, Integer assignTo) {
    	UsrUser assignToUser = userService.getUser(assignTo);
        UsrUser loggedUser = userService.getLoggedUser();
        if (loggedUser == null) {
			throw new AccessDeniedException("User is not logged in", Collections.emptyList());
		}

        WfTaskType taskType = wfTaskTypeRepository.findByCode(taskTypeCode);
        if (taskType == null) {
        	throw new SystemException("Task type not found", BaseCode.DB_INTEGRITY_PROBLEM).set("code", taskTypeCode);
        }

    	WfTask wfTask = new WfTask();
    	wfTask.setTimeCreated(OffsetDateTime.now());
    	wfTask.setAssignee(assignToUser);
    	wfTask.setCreator(loggedUser);
    	wfTask.setTaskType(taskType);
    	wfTask.setStatus(Status.NEW);
    	return wfTaskRepository.save(wfTask);
	}

	/**
	 * Vytvoření nového WfTaskApState.
	 * 
	 * @param apState
	 * @param assignTo
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void createTaskApState(ApState apState, Integer assignTo) {
		// Vytvoření úkolu APPROVED není dostupné
		if (apState.getStateApproval() == StateApproval.APPROVED) {
            throw new BusinessException("Vytvoření úkolu [Approve/Schválit] není dostupné", BaseCode.INVALID_STATE);
		} 

        // create new WfTask
        String taskTypeCode = apState.getStateApproval() == StateApproval.TO_APPROVE ? AP_CONFIRM : AP_UPDATE;
    	WfTask wfTask = createWfTask(taskTypeCode, assignTo);

    	// create new WfTaskApState
    	WfTaskApState wfTaskApState = new WfTaskApState();
    	wfTaskApState.setTask(wfTask);
    	wfTaskApState.setState(apState);
    	wfTaskApStateRepository.save(wfTaskApState);
	}

	/**
	 * Vytvoření nového WfTaskApRevState.
	 * 
	 * @param revState
	 * @param assignTo
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void createTaskApRevState(ApRevState revState, Integer assignTo) {
        // create new WfTask
        String taskTypeCode = revState.getStateApproval() == RevStateApproval.TO_APPROVE ? AP_REV_CONFIRM : AP_REV_UPDATE;
    	WfTask wfTask = createWfTask(taskTypeCode, assignTo);

    	// create new WfTaskApRevState
    	WfTaskApRevState taskApRevState = new WfTaskApRevState();
    	taskApRevState.setTask(wfTask);
    	taskApRevState.setState(revState);
    	wfTaskApRevStateRepository.save(taskApRevState);
	}

    /**
     * Zavřít úkol podle ApState.
     * 
     * @param apState
     * @param status
     */
	@Transactional(Transactional.TxType.MANDATORY)
    public void closeWfTask(ApState apState, Status status) {
    	WfTaskApState wfTaskApState = wfTaskApStateRepository.findByStateAndTimeClosedIsNull(apState);
    	if (wfTaskApState != null) {
            UsrUser leggedUser = userService.getLoggedUser();
    		WfTask wfTask = wfTaskApState.getTask();
    		wfTask.setClosedBy(leggedUser);
    		wfTask.setTimeClosed(OffsetDateTime.now());
    		wfTask.setStatus(status);
    		wfTaskRepository.save(wfTask);
    	}
    }

    /**
     * Zavřít úkol podle ApRevState.
     * 
     * @param revState
     * @param status
     */
	@Transactional(Transactional.TxType.MANDATORY)
    public void closeWfTask(ApRevState revState, Status status) {
    	WfTaskApRevState wfTaskApRevState = wfTaskApRevStateRepository.findByStateAndTimeClosedIsNull(revState);
    	if (wfTaskApRevState != null) {
            UsrUser leggedUser = userService.getLoggedUser();
    		WfTask wfTask = wfTaskApRevState.getTask();
    		wfTask.setClosedBy(leggedUser);
    		wfTask.setTimeClosed(OffsetDateTime.now());
    		wfTask.setStatus(status);
    		wfTaskRepository.save(wfTask);
    	}
    }
	
	/**
	 * Return task by apState
	 * @param apState
	 * @return
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public WfTaskApState getTask(ApState apState) {
    	return wfTaskApStateRepository.findByStateAndTimeClosedIsNull(apState);
	}

	/**
	 * Return task by revState
	 * @param revState
	 * @return
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public WfTaskApRevState getTask(ApRevState revState) {
		return wfTaskApRevStateRepository.findByStateAndTimeClosedIsNull(revState);
	}
}
