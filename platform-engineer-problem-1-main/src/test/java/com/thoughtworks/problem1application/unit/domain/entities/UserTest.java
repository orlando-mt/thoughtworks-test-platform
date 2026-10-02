package com.thoughtworks.problem1application.unit.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.thoughtworks.problem1application.domain.entities.User;

public class UserTest {

	@Test
	void shouldCreateAnInstanceOfUserByConstructor() {
		// Arrange
		long id = new Random().nextLong();
		String name = "name of user";
		String email = "email@email.com";
		String password = "123qwe";

		// Act
		User user = new User(id, name, email, password, null);

		// Assert
		assertEquals(id, user.getId());
		assertEquals(name, user.getName());
		assertEquals(email, user.getEmail());
		assertEquals(password, user.getPassword());
		assertEquals(null, user.getAccessToken());
	}
}
