package com.thoughtworks.problem1application.application.dtos.response;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class AuthenticateResponseDTO {

	private String message;
	private String id;
	private String name;
	private String email;
	private String accessToken;
}
