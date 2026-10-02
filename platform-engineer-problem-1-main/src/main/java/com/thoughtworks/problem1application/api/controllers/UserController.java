package com.thoughtworks.problem1application.api.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thoughtworks.problem1application.application.dtos.request.UserRequestUpdateDTO;
import com.thoughtworks.problem1application.application.dtos.response.UserResponseDTO;
import com.thoughtworks.problem1application.application.interfaces.UserApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("api/users")
public class UserController {

	@Autowired
	private UserApplicationService userApplicationService;

	@PutMapping
	@Operation(description = "Updates the user data registered in the system.", summary = "Update user.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "user updated successfully."),
			@ApiResponse(responseCode = "401", description = "Unauthenticated user."),
			@ApiResponse(responseCode = "403", description = "Unauthorized user."),
			@ApiResponse(responseCode = "500", description = "Internal server error.")
	})
	public ResponseEntity<UserResponseDTO> put(@Valid @RequestBody UserRequestUpdateDTO dto) {
		return ResponseEntity.ok(userApplicationService.update(dto));
	}

	@DeleteMapping("/{id}")
	@Operation(description = "Delete the user data registered in the system by Id.", summary = "Delete user by Id.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "user deleted successfully."),
			@ApiResponse(responseCode = "401", description = "Unauthenticated user."),
			@ApiResponse(responseCode = "403", description = "Unauthorized user."),
			@ApiResponse(responseCode = "404", description = "User not found."),
			@ApiResponse(responseCode = "500", description = "Internal server error.")
	})
	public ResponseEntity<UserResponseDTO> delete(
			@Parameter(description = "user identification.") @PathVariable("id") Long id) {
		return ResponseEntity.ok(userApplicationService.delete(id));
	}

	@GetMapping
	@Operation(description = "Returns a list of registered users in the system.", summary = "Users list.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "List of users returned successfully."),
			@ApiResponse(responseCode = "401", description = "Unauthenticated user."),
			@ApiResponse(responseCode = "403", description = "Unauthorized user."),
			@ApiResponse(responseCode = "500", description = "Internal server error.")
	})
	public ResponseEntity<List<UserResponseDTO>> getAll() {
		return ResponseEntity.ok(userApplicationService.getAll());

	}

	@GetMapping("/{id}")
	@Operation(description = "Returns the user registered in the system by Id.", summary = "Get user by Id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "user returned successfully."),
			@ApiResponse(responseCode = "401", description = "Unauthenticated user."),
			@ApiResponse(responseCode = "403", description = "Unauthorized user."),
			@ApiResponse(responseCode = "404", description = "User not found."),
			@ApiResponse(responseCode = "500", description = "Internal server error.")
	})
	public ResponseEntity<UserResponseDTO> getById(
			@Parameter(description = "user identification.") @PathVariable("id") Long id) {
		return ResponseEntity.ok(userApplicationService.getById(id));

	}
}
