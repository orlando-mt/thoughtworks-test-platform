package com.thoughtworks.problem1application.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thoughtworks.problem1application.application.dtos.request.UserRequestCreateDTO;
import com.thoughtworks.problem1application.application.dtos.response.UserResponseDTO;
import com.thoughtworks.problem1application.application.interfaces.UserApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("api/register")
public class RegisterController {

	@Autowired
	private UserApplicationService userApplicationService;

	@PostMapping
	@Operation(description = "Register a new user in the system.", summary = "Register new user.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "New user registered successfully."),
			@ApiResponse(responseCode = "500", description = "Internal server error.")
	})
	public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserRequestCreateDTO dto) {
		return ResponseEntity.status(201).body(userApplicationService.create(dto));
	}
}
