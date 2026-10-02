package com.thoughtworks.problem1application.domain.interfaces;

import java.util.List;
import java.util.Optional;

import com.thoughtworks.problem1application.domain.entities.User;

public interface UserDomainService {
	User create(User user);

	List<User> getAll();

	Optional<User> getById(Long id);

	void update(User user);

	void delete(User user);

	User authenticate(String email, String password);
}
