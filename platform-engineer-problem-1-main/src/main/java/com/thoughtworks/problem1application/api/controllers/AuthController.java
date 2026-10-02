package com.thoughtworks.problem1application.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thoughtworks.problem1application.application.dtos.request.AuthenticateRequestDTO;
import com.thoughtworks.problem1application.application.dtos.response.AuthenticateResponseDTO;
import com.thoughtworks.problem1application.application.interfaces.UserApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("api/auth")
public class AuthController {

	@Autowired
	private UserApplicationService userApplicationService;

	@PostMapping
	@Operation(description = "Authenticates the user in the system.", summary = "Authenticates the user.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User authenticate successfully."),
			@ApiResponse(responseCode = "401", description = "Unauthenticated user."),
			@ApiResponse(responseCode = "403", description = "Unauthorized user."),
			@ApiResponse(responseCode = "500", description = "Internal server error.")
	})
	public ResponseEntity<AuthenticateResponseDTO> auth(@Valid @RequestBody AuthenticateRequestDTO dto) {
		return ResponseEntity.ok(userApplicationService.authenticate(dto));
	}
}
