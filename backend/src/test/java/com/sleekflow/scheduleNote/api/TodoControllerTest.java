package com.sleekflow.scheduleNote.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.sleekflow.scheduleNote.dto.RecurrenceResponse;
import com.sleekflow.scheduleNote.config.AppProperties;
import com.sleekflow.scheduleNote.dto.StatusChangeResponse;
import com.sleekflow.scheduleNote.dto.TodoResponse;
import com.sleekflow.scheduleNote.domain.RecurrenceType;
import com.sleekflow.scheduleNote.domain.TodoPriority;
import com.sleekflow.scheduleNote.domain.TodoStatus;
import com.sleekflow.scheduleNote.service.TodoService;
import com.sleekflow.scheduleNote.exception.CircularDependencyException;
import com.sleekflow.scheduleNote.exception.DependenciesNotSatisfiedException;
import com.sleekflow.scheduleNote.exception.StaleTodoException;
import com.sleekflow.scheduleNote.exception.TodoNotFoundException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the HTTP contract: which status code each failure maps to, and that
 * failures carry enough structure for a client to act on.
 */
@WebMvcTest(TodoController.class)
// WebConfig is part of the web slice and needs the bound properties; the slice
// does not run @ConfigurationPropertiesScan on its own.
@EnableConfigurationProperties(AppProperties.class)
class TodoControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private TodoService service;

	private static TodoResponse sampleTodo() {
		return new TodoResponse(1L, "bake bread", null, LocalDate.of(2026, 3, 10), TodoStatus.NOT_STARTED,
				TodoPriority.MEDIUM, new RecurrenceResponse(RecurrenceType.NONE, null), null, List.of(), false, null,
				null, Instant.EPOCH, Instant.EPOCH, 0L);
	}

	@Test
	void createReturns201WithALocationHeader() throws Exception {
		given(this.service.create(any())).willReturn(sampleTodo());

		this.mvc.perform(post("/api/todos").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name":"bake bread","priority":"MEDIUM","dueDate":"2026-03-10"}"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/todos/1"))
			.andExpect(jsonPath("$.name").value("bake bread"));
	}

	@Test
	void rejectsABlankNameWithFieldLevelDetail() throws Exception {
		this.mvc.perform(post("/api/todos").contentType(MediaType.APPLICATION_JSON).content("""
				{"name":"   ","priority":"MEDIUM"}"""))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("name"));
	}

	@Test
	void rejectsARecurringTodoWithNoDueDate() throws Exception {
		this.mvc.perform(post("/api/todos").contentType(MediaType.APPLICATION_JSON).content("""
				{"name":"water plants","priority":"LOW","recurrence":{"type":"WEEKLY"}}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].message").value(
					org.hamcrest.Matchers.containsString("needs a due date")));
	}

	@Test
	void rejectsAnUnknownEnumValue() throws Exception {
		this.mvc.perform(post("/api/todos").contentType(MediaType.APPLICATION_JSON).content("""
				{"name":"x","priority":"URGENT"}""")).andExpect(status().isBadRequest());
	}

	@Test
	void missingTodoIs404() throws Exception {
		given(this.service.get(99L)).willThrow(new TodoNotFoundException(99L));

		this.mvc.perform(get("/api/todos/99"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.type").value("https://sleekflow.example/problems/todo-not-found"))
			.andExpect(jsonPath("$.todoId").value(99));
	}

	@Test
	void startingABlockedTodoIs409AndNamesTheBlockers() throws Exception {
		willThrow(new DependenciesNotSatisfiedException(1L, List.of("buy flour"))).given(this.service)
			.changeStatus(eq(1L), any());

		this.mvc.perform(patch("/api/todos/1/status").contentType(MediaType.APPLICATION_JSON).content("""
				{"status":"IN_PROGRESS","version":0}"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.type").value("https://sleekflow.example/problems/dependencies-not-satisfied"))
			.andExpect(jsonPath("$.outstandingDependencies[0]").value("buy flour"));
	}

	@Test
	void aStaleVersionIs409AndReportsBothVersions() throws Exception {
		willThrow(new StaleTodoException(1L, 0L, 3L)).given(this.service).changeStatus(eq(1L), any());

		this.mvc.perform(patch("/api/todos/1/status").contentType(MediaType.APPLICATION_JSON).content("""
				{"status":"COMPLETED","version":0}"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.expectedVersion").value(0))
			.andExpect(jsonPath("$.actualVersion").value(3));
	}

	@Test
	void aCyclicDependencyIs409() throws Exception {
		given(this.service.addDependency(1L, 2L)).willThrow(new CircularDependencyException(1L, 2L));

		this.mvc.perform(post("/api/todos/1/dependencies").contentType(MediaType.APPLICATION_JSON).content("""
				{"dependsOnId":2}"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.type").value("https://sleekflow.example/problems/circular-dependency"));
	}

	@Test
	void deleteReturns204() throws Exception {
		this.mvc.perform(delete("/api/todos/1")).andExpect(status().isNoContent());
	}

	@Test
	void statusChangeRequiresAVersion() throws Exception {
		this.mvc.perform(patch("/api/todos/1/status").contentType(MediaType.APPLICATION_JSON).content("""
				{"status":"COMPLETED"}""")).andExpect(status().isBadRequest());
	}
}
