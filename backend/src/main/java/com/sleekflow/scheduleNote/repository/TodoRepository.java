package com.sleekflow.scheduleNote.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sleekflow.scheduleNote.domain.Todo;
import com.sleekflow.scheduleNote.dto.TodoRevisionResponse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoRepository extends JpaRepository<Todo, Long>, JpaSpecificationExecutor<Todo> {

	Optional<Todo> findByIdAndDeletedAtIsNull(Long id);

	/**
	 * Of the given TODOs, which are blocked. Answering this for a whole page in
	 * one round trip is what keeps listing 10,000 items off the N+1 path.
	 */
	@Query(value = """
			SELECT DISTINCT td.todo_id
			FROM todo_dependencies td
			JOIN todos d ON d.id = td.depends_on_id
			WHERE td.todo_id IN (:ids)
			  AND d.deleted_at IS NULL
			  AND d.status <> 'COMPLETED'
			""", nativeQuery = true)
	Set<Long> findBlockedIdsAmong(@Param("ids") Collection<Long> ids);

	/**
	 * One level of the dependency graph, used to walk it breadth-first when
	 * checking whether a proposed edge would close a cycle.
	 */
	@Query(value = "SELECT DISTINCT td.depends_on_id FROM todo_dependencies td WHERE td.todo_id IN (:ids)",
			nativeQuery = true)
	Set<Long> findDependencyIdsOf(@Param("ids") Collection<Long> ids);

	/**
	 * One aggregate row summarising the state of the list, for change polling.
	 * Two aggregates over an indexed column: far cheaper than shipping a page.
	 */
	@Query("SELECT new com.sleekflow.scheduleNote.dto.TodoRevisionResponse(MAX(t.updatedAt), COUNT(t)) FROM Todo t")
	TodoRevisionResponse loadRevision();

	/** TODOs that depend on the given one -- used to explain a blocked state. */
	@Query("SELECT t FROM Todo t JOIN t.dependencies d WHERE d.id = :id AND t.deletedAt IS NULL")
	List<Todo> findDependents(@Param("id") Long id);
}
