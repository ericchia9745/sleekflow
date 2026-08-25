package com.sleekflow.scheduleNote.dto;

import jakarta.validation.constraints.NotNull;

public record AddDependencyRequest(@NotNull Long dependsOnId) {
}
