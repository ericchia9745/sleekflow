package com.sleekflow.scheduleNote.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sleekflow.scheduleNote.domain.TodoPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param version the version the client last saw; a mismatch means someone else
 * changed this TODO in the meantime and the request is rejected
 */
public record UpdateTodoRequest(@NotBlank @Size(max = 200) String name, @Size(max = 2000) String description,
		LocalDate dueDate, @NotNull TodoPriority priority, @Valid RecurrenceRequest recurrence,
		@NotNull Long version) {

	@JsonIgnore
	@AssertTrue(message = "A recurring TODO needs a due date, otherwise the next occurrence cannot be scheduled")
	public boolean isRecurrenceSchedulable() {
		return this.recurrence == null || this.recurrence.type() == null || !this.recurrence.type().recurs()
				|| this.dueDate != null;
	}
}
