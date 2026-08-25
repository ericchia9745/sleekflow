package com.sleekflow.scheduleNote.dto;

import com.sleekflow.scheduleNote.domain.TodoStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull TodoStatus status, @NotNull Long version) {
}
