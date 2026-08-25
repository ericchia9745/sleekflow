package com.sleekflow.todo.api.dto;

import jakarta.validation.constraints.NotNull;

public record AddDependencyRequest(@NotNull Long dependsOnId) {
}
