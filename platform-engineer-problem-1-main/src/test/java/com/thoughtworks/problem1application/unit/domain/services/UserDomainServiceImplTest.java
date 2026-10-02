package com.thoughtworks.problem1application.unit.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.thoughtworks.problem1application.domain.entities.User;
import com.thoughtworks.problem1application.domain.exceptions.EmailAlreadyRegisteredException;
import com.thoughtworks.problem1application.domain.exceptions.InvalidCredentialsException;
import com.thoughtworks.problem1application.domain.services.UserDomainServiceImpl;
import com.thoughtworks.problem1application.helpers.UserBuilder;
import com.thoughtworks.problem1application.infrastructure.repository.repositories.UserRepository;
import com.thoughtworks.problem1application.infrastructure.security.TokenCreator;

@ExtendWith(MockitoExtension.class)
class UserDomainServiceImplTest {

	@InjectMocks
	private UserDomainServiceImpl userDomainService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private TokenCreator tokenCreator;

	@Test
	void shouldCreateUserWithHashedPassword() {
		User user = new UserBuilder().builder();
		String rawPassword = user.getPassword();
		when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
		when(passwordEncoder.encode(rawPassword)).thenReturn("hashed");
		when(userRepository.save(user)).thenReturn(user);

		User created = userDomainService.create(user);

		assertEquals("hashed", created.getPassword());
		verify(userRepository).save(user);
	}

	@Test
	void shouldRejectAlreadyRegisteredEmail() {
		User user = new UserBuilder().builder();
		when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

		assertThrows(EmailAlreadyRegisteredException.class, () -> userDomainService.create(user));

		verify(passwordEncoder, never()).encode(anyString());
		verify(userRepository, never()).save(any());
	}

	@Test
	void shouldUpdateUser() {
		User user = new UserBuilder().builder();
		user.setName("new name user");

		userDomainService.update(user);

		verify(userRepository).save(user);
	}

	@Test
	void shouldDeleteUser() {
		User user = new UserBuilder().builder();

		userDomainService.delete(user);

		verify(userRepository).delete(user);
	}

	@Test
	void shouldGetUserById() {
		User user = new UserBuilder().builder();
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

		Optional<User> found = userDomainService.getById(user.getId());

		assertEquals(user, found.orElseThrow());
	}

	@Test
	void shouldGetAllUsers() {
		when(userRepository.findAll()).thenReturn(List.of(
				new UserBuilder().builder(), new UserBuilder().builder(),
				new UserBuilder().builder(), new UserBuilder().builder()));

		assertEquals(4, userDomainService.getAll().size());
	}

	@Test
	void shouldAuthenticateWithCorrectPassword() {
		User user = new UserBuilder().builder();
		user.setPassword("hashed");
		when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("q1w2e3", "hashed")).thenReturn(true);
		when(tokenCreator.generateToken(user.getEmail())).thenReturn("token");

		User authenticated = userDomainService.authenticate(user.getEmail(), "q1w2e3");

		assertEquals("token", authenticated.getAccessToken());
	}

	@Test
	void shouldRejectWrongPassword() {
		User user = new UserBuilder().builder();
		user.setPassword("hashed");
		when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

		assertThrows(InvalidCredentialsException.class,
				() -> userDomainService.authenticate(user.getEmail(), "wrong"));

		verify(tokenCreator, never()).generateToken(anyString());
	}

	@Test
	void shouldRejectUnknownEmail() {
		when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThrows(InvalidCredentialsException.class,
				() -> userDomainService.authenticate("nobody@example.com", "q1w2e3"));

		verify(passwordEncoder, never()).matches(any(), any());
	}

	@Test
	void shouldProduceSameToStringForEqualUsers() {
		User user1 = new User();
		user1.setId(1L);
		user1.setEmail("email@email");
		User user2 = new User();
		user2.setId(1L);
		user2.setEmail("email@email");

		assertEquals(user1.toString(), user2.toString());
	}
}