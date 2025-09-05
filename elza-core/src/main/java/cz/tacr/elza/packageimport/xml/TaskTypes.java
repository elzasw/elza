package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * List of available task types
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "task-types")
@XmlType(name = "task-types")
public class TaskTypes {

    @XmlElement(name = "task-type", required = true)
    private List<TaskType> taskTypes;

	public List<TaskType> getTaskTypes() {
		return taskTypes;
	}

	public void setTaskTypes(List<TaskType> taskTypes) {
		this.taskTypes = taskTypes;
	}
}
