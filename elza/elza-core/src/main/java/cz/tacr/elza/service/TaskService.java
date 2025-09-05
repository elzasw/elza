package cz.tacr.elza.service;

import static cz.tacr.elza.domain.WfTaskType.AP_CONFIRM;
import static cz.tacr.elza.domain.WfTaskType.AP_REV_CONFIRM;
import static cz.tacr.elza.domain.WfTaskType.AP_REV_UPDATE;
import static cz.tacr.elza.domain.WfTaskType.AP_UPDATE;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.WfTaskApRevStateRepository;
import cz.tacr.elza.repository.WfTaskApStateRepository;
import cz.tacr.elza.repository.WfTaskRepository;
import cz.tacr.elza.repository.WfTaskTypeRepository;

@Service
public class TaskService {

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
	private ApStateRepository apStateRepository;

	/**
	 * Získání seznamu zpracovatelů entity 
	 * 
	 * @param apState
	 * @return
	 */
	public List<Participant> GetLastParticipants(ApState apState) {
		List<Participant> result = new ArrayList<>();
		List<UsrUser> users = new ArrayList<>();

		List<WfTaskApState> wfTaskApStates = wfTaskApStateRepository.findAllByState(apState);
		wfTaskApStates.forEach(i -> {
			WfTask wfTask = i.getTask();
			UsrUser creator = wfTask.getCreator();
			UsrUser assignee = wfTask.getAssignee();
			UsrUser closedBy = wfTask.getClosedBy();
			if (creator != null) {
				users.add(creator);
			}
			if (assignee != null) {
				users.add(assignee);
			}
			if (closedBy != null) {
				users.add(closedBy);
			}
		});

		List<Integer> apIds = users.stream().map(u -> u.getAccessPointId()).toList();
        Map<Integer, ApIndex> apIndexMap = accessPointService.findPreferredPartIndexMapByIds(apIds); 

        List<ApState> apStates = apStateRepository.findLastByAccessPointIds(apIds);
        Map<Integer, Integer> apIdScopeIdMap = apStates.stream().collect(Collectors.toMap(s -> s.getStateId(), s -> s.getScopeId()));

		List<UsrPermission> permisions = permissionRepository.findAllByUserIn(users);
		Map<Integer, List<UsrPermission>> permissionMap = permisions.stream().collect(Collectors.groupingBy(UsrPermission::getPermissionId));

		users.forEach(u -> {
			Integer apId = u.getAccessPointId();
			ApIndex apIndex = apIndexMap.get(apId);
			String apName = apIndex != null ? apIndex.getIndexValue() : null; 
			Integer scopeId = apIdScopeIdMap.get(apId);
			List<UsrPermission> permissions = permissionMap.get(apId);

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
	 * Získání seznamu úkolů pro daného uživatele
	 * 
	 * @return seznam úkolů
	 */
	public List<TasksViewDetail> getMyTasks() {
		List<TasksViewDetail> result = new ArrayList<>();
        UsrUser loggedUser = userService.getLoggedUser();

        List<WfTask> wfTasks = wfTaskRepository.findAllByAssigneeId(loggedUser.getUserId());
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

        wfTasks.forEach(t -> {
        	UsrUser creator = t.getCreator();
        	UsrUser closedBy = t.getClosedBy();
        	ApState apState = Objects.requireNonNull(taskIdApStateMap.get(t.getTaskId()));
        	ApIndex apIndex = indexMap.get(apState.getAccessPointId());
        	String apName = apIndex != null ? apIndex.getIndexValue() : null;
        	TasksEntityType entityType = taskApStateIds.contains(t.getTaskId()) ? TasksEntityType.AP : TasksEntityType.AP_REV; 
        	
        	TasksViewDetail tvd = new TasksViewDetail();
        	tvd.setAssigneeId(t.getAssignee().getUserId());
        	tvd.setAssigneeName(t.getAssignee().getUsername());
        	tvd.setClosed(t.getTimeClosed());
        	if (closedBy != null) {
        		tvd.setClosedById(closedBy.getUserId());
        		tvd.setClosedByName(closedBy.getUsername());
        	}
        	tvd.setCreated(tvd.getCreated());
        	if (creator != null) {
        		tvd.setCreatorId(creator.getUserId());
        		tvd.setCreatorName(creator.getUsername());
        	}
        	tvd.setDescription(t.getDescription());
        	tvd.setPrimaryEntityId(apState.getAccessPointId());
        	tvd.setPrimaryEntityName(apName);
        	tvd.setPrimaryEntityType(entityType);
        	tvd.setStatus(TasksStatus.fromValue(t.getStatus().name()));
        	tvd.setTaskId(t.getTaskId());
        	tvd.setTaskTypeCode(t.getTaskType().getCode());
        	tvd.setTaskTypeId(t.getTaskType().getTaskTypeId());
        	tvd.setTaskTypeName(t.getTaskType().getName());
        	result.add(tvd);
        });

		return result;
	}

	/**
	 * Vytvoření nového WfTask.
	 * 
	 * @param taskTypeCode
	 * @param assignTo
	 * @return
	 */
	private WfTask createWfTask(String taskTypeCode, Integer assignTo) {
    	UsrUser assignToUser = userService.getUser(assignTo);
        UsrUser leggedUser = userService.getLoggedUser();

        WfTaskType taskType = wfTaskTypeRepository.findByCode(taskTypeCode);

    	WfTask wfTask = new WfTask();
    	wfTask.setTimeCreated(OffsetDateTime.now());
    	wfTask.setAssignee(assignToUser);
    	wfTask.setCreator(leggedUser);
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
	public void createTaskApState(ApState apState, Integer assignTo) {
        // create new WfTask
        String taskTypeCode = apState.getStateApproval() == StateApproval.APPROVED ? AP_CONFIRM : AP_UPDATE;
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
}