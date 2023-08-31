package dev.stratospheric.todoapp.todo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * [N]:jpa - Provides CRUD capabilities by extending the {@link org.springframework.data.jpa.JpaRepository} interface.
 */
public interface TodoRepository extends JpaRepository<Todo, Long> {
  List<Todo> findAllByOwnerEmailOrderByIdAsc(String email);

  List<Todo> findAllByCollaboratorsEmailOrderByIdAsc(String email);

  Optional<Todo> findByIdAndOwnerEmail(Long todoId, String todoOwnerEmail);
}
