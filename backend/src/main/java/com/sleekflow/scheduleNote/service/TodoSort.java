package com.sleekflow.todo.service;

import java.util.Map;
import java.util.Set;

import com.sleekflow.todo.service.exception.InvalidTodoRequestException;

import org.springframework.data.domain.Sort;

/**
 * Translates the sort keys the API exposes into entity properties.
 * <p>
 * {@code priority} and {@code status} map onto generated rank columns because
 * sorting their names as text puts HIGH before LOW and ARCHIVED before
 * NOT_STARTED -- correct alphabetically, useless to a user.
 */
final class TodoSort {

	private static final Map<String, String> PROPERTY_BY_SORT_KEY = Map.of("name", "name", "dueDate", "dueDate",
			"priority", "priorityRank", "status", "statusRank", "createdAt", "createdAt", "updatedAt", "updatedAt",
			"id", "id");

	static final Set<String> SUPPORTED_KEYS = PROPERTY_BY_SORT_KEY.keySet();

	private TodoSort() {
	}

	static Sort translate(Sort requested) {
		if (requested.isUnsorted()) {
			return Sort.by(Sort.Order.asc("dueDate"), Sort.Order.desc("priorityRank"), Sort.Order.asc("id"));
		}
		Sort translated = Sort.by(requested.stream().map(TodoSort::translateOrder).toList());
		// A stable tie-breaker; without it, paging over equal values can repeat or
		// skip rows between requests.
		return translated.and(Sort.by(Sort.Order.asc("id")));
	}

	private static Sort.Order translateOrder(Sort.Order order) {
		String property = PROPERTY_BY_SORT_KEY.get(order.getProperty());
		if (property == null) {
			throw new InvalidTodoRequestException(
					"Cannot sort by '%s'. Supported: %s".formatted(order.getProperty(), String.join(", ",
							SUPPORTED_KEYS.stream().sorted().toList())));
		}
		return new Sort.Order(order.getDirection(), property);
	}
}
