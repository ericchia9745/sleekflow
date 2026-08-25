package com.sleekflow.scheduleNote.dto;

import java.util.List;

import com.sleekflow.scheduleNote.domain.enums.TodoStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BulkStatusRequest(@NotNull TodoStatus status,
		@NotEmpty @Valid List<BulkTodoRef> items) {
}
