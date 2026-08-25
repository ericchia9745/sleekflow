package com.sleekflow.scheduleNote.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sleekflow.scheduleNote.domain.TodoPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTodoRequest(@NotBlank @Size(max = 200) String name, @Size(max = 2000) String description,
		LocalDate dueDate, TodoPriority priority, @Valid RecurrenceRequest recurrence, List<Long> dependencyIds) {

	@JsonIgnore
	@AssertTrue(message = "A recurring TODO needs a due date, otherwise the next occurrence cannot be scheduled")
	public boolean isRecurrenceSchedulable() {
		return this.recurrence == null || this.recurrence.type() == null || !this.recurrence.type().recurs()
				|| this.dueDate != null;
	}

	public List<Long> dependencyIdsOrEmpty() {
		return (this.dependencyIds != null) ? this.dependencyIds : List.of();
	}
}
