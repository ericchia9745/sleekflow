package com.sleekflow.todo.api.dto;

/**
 * @param todo the TODO after the status change
 * @param nextOccurrence the occurrence created because a recurring TODO was
 * completed, or null when nothing was scheduled
 */
public record StatusChangeResponse(TodoResponse todo, TodoResponse nextOccurrence) {
}
