package com.linkedInProject.userService.service;

import com.linkedInProject.userService.dto.LoginRequestDto;
import com.linkedInProject.userService.dto.SignupRequestDto;
import com.linkedInProject.userService.dto.UserDto;
import com.linkedInProject.userService.entity.User;
import com.linkedInProject.userService.event.UserCreatedEvent;
import com.linkedInProject.userService.exception.BadRequestException;
import com.linkedInProject.userService.exception.ResourceNotFoundException;
import com.linkedInProject.userService.repository.UserRepository;
import com.linkedInProject.userService.utils.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository ;
    private final ModelMapper modelMapper ;
    private final JwtService jwtService ;
    private final KafkaTemplate<Long,UserCreatedEvent> kafkaTemplate ;

    public UserDto signUp(SignupRequestDto signupRequestDto) {
        log.info("Signup a user with email: {}",signupRequestDto.getEmail());

        boolean exists = userRepository.existsByEmail(signupRequestDto.getEmail()) ;
        if(exists) throw new BadRequestException("User already exists") ;

        User user = modelMapper.map(signupRequestDto,User.class);
        user.setPassword(BCrypt.hash(user.getPassword()));

        user = userRepository.save(user) ;

        UserCreatedEvent userCreatedEvent = UserCreatedEvent.builder()
                .userId(user.getId())
                .name(user.getName())
                .build();
        kafkaTemplate.send("user_created_topic",userCreatedEvent);

        return modelMapper.map(user, UserDto.class) ;
    }

    public String login(LoginRequestDto loginRequestDto) {
        log.info("Login request for user with email: {}",loginRequestDto.getEmail());
        User user = userRepository.findByEmail(loginRequestDto.getEmail()).orElseThrow(() -> new BadRequestException("Incorrect email or password")) ;
        boolean isPasswordMatch = BCrypt.match(loginRequestDto.getPassword(),user.getPassword());
        if(!isPasswordMatch){
            throw new BadRequestException("Incorrect email or password");
        }
        return jwtService.generateAccessToken(user) ;
    }
}
