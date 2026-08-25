package com.sleekflow.todo.api.dto;

import com.sleekflow.todo.domain.TodoStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull TodoStatus status, @NotNull Long version) {
}
