package com.thoughtworks.problem1application.application.services;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thoughtworks.problem1application.application.dtos.request.AuthenticateRequestDTO;
import com.thoughtworks.problem1application.application.dtos.request.UserRequestCreateDTO;
import com.thoughtworks.problem1application.application.dtos.request.UserRequestUpdateDTO;
import com.thoughtworks.problem1application.application.dtos.response.AuthenticateResponseDTO;
import com.thoughtworks.problem1application.application.dtos.response.UserResponseDTO;
import com.thoughtworks.problem1application.application.interfaces.UserApplicationService;
import com.thoughtworks.problem1application.domain.entities.User;
import com.thoughtworks.problem1application.domain.interfaces.UserDomainService;

@Service
public class UserApplicationServiceImpl implements UserApplicationService {

	@Autowired
	private UserDomainService userDomainService;

	@Autowired
	private ModelMapper modelMapper;

	@Override
	public UserResponseDTO create(UserRequestCreateDTO dto) {

		User user = modelMapper.map(dto, User.class);
		userDomainService.create(user);
		return modelMapper.map(user, UserResponseDTO.class);
	}

	@Override
	public List<UserResponseDTO> getAll() {

		return modelMapper.map(userDomainService.getAll(), new TypeToken<List<UserResponseDTO>>() {
		}.getType());
	}

	@Override
	public UserResponseDTO getById(Long id) {

		User user = userDomainService.getById(id).get();
		return modelMapper.map(user, UserResponseDTO.class);
	}

	@Override
	public UserResponseDTO update(UserRequestUpdateDTO dto) {

		User user = userDomainService.getById(dto.getId()).get();
		userDomainService.update(user);
		return modelMapper.map(dto, UserResponseDTO.class);
	}

	@Override
	public UserResponseDTO delete(Long id) {

		User user = userDomainService.getById(id).get();
		userDomainService.delete(user);
		return modelMapper.map(user, UserResponseDTO.class);
	}

	@Override
	public AuthenticateResponseDTO authenticate(AuthenticateRequestDTO dto) {
		User user = userDomainService.authenticate(dto.getEmail(), dto.getPassword());
		AuthenticateResponseDTO response = modelMapper.map(user, AuthenticateResponseDTO.class);

		response.setMessage("Successfully authenticated user.");

		return response;
	}

}
