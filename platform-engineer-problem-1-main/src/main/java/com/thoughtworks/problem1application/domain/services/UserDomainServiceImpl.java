package com.thoughtworks.problem1application.domain.services;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.thoughtworks.problem1application.domain.entities.User;
import com.thoughtworks.problem1application.domain.exceptions.EmailAlreadyRegisteredException;
import com.thoughtworks.problem1application.domain.exceptions.InvalidCredentialsException;
import com.thoughtworks.problem1application.domain.interfaces.UserDomainService;
import com.thoughtworks.problem1application.infrastructure.repository.repositories.UserRepository;
import com.thoughtworks.problem1application.infrastructure.security.TokenCreator;

@Service
public class UserDomainServiceImpl implements UserDomainService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenCreator tokenCreator;

	public UserDomainServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
								 TokenCreator tokenCreator) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenCreator = tokenCreator;
	}

	@Override
	public User create(User user) {
		if (userRepository.findByEmail(user.getEmail()).isPresent()) {
			throw new EmailAlreadyRegisteredException();
		}
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		return userRepository.save(user);
	}

	@Override
	public List<User> getAll() {
		return userRepository.findAll();
	}

	@Override
	public Optional<User> getById(Long id) {
		return userRepository.findById(id);
	}

	@Override
	public void update(User user) {
		userRepository.save(user);
	}

	@Override
	public void delete(User user) {
		userRepository.delete(user);
	}

	@Override
	public User authenticate(String email, String password) {
		// bcrypt no permite buscar por hash: se busca por email y se compara con matches()
		User user = userRepository.findByEmail(email)
				.filter(found -> passwordEncoder.matches(password, found.getPassword()))
				.orElseThrow(InvalidCredentialsException::new);

		user.setAccessToken(tokenCreator.generateToken(user.getEmail()));
		return user;
	}
}