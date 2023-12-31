package dev.stratospheric.todoapp.todo;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.stratospheric.todoapp.person.Person;

import java.util.List;

/**
 * [N]:jpa - Provides CRUD capabilities by extending the {@link org.springframework.data.jpa.JpaRepository} interface.
 */
public interface TodoRepository extends JpaRepository<Todo, Long> {

  List<Todo> findAllByOwner(Person person);

  List<Todo> findAllByOwnerEmail(String email);

  List<Todo> findAllByOwnerEmailOrderByIdAsc(String email);
}
