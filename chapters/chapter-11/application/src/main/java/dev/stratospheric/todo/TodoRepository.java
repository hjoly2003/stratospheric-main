package dev.stratospheric.todo;

import dev.stratospheric.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * [N]:jpa - Provides CRUD capabilities by extending the {@link org.springframework.data.jpa.JpaRepository} interface.
 */
public interface TodoRepository extends JpaRepository<Todo, Long> {

  List<Todo> findAllByOwner(Person person);

  List<Todo> findAllByOwnerEmail(String email);

  List<Todo> findAllByOwnerEmailOrderByIdAsc(String email);
}
