package com.linkedInProject.userService.controller;

import com.linkedInProject.userService.dto.LoginRequestDto;
import com.linkedInProject.userService.dto.SignupRequestDto;
import com.linkedInProject.userService.dto.UserDto;
import com.linkedInProject.userService.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService ;

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signUp(@RequestBody SignupRequestDto signupRequestDto){
        UserDto userDto = userService.signUp(signupRequestDto);
        return new ResponseEntity<>(userDto, HttpStatus.CREATED) ;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDto loginRequestDto){
        String token = userService.login(loginRequestDto) ;
        return ResponseEntity.ok(token) ;
    }
}
