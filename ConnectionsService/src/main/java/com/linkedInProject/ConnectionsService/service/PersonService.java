package com.linkedInProject.ConnectionsService.service;

import com.linkedInProject.ConnectionsService.entity.Person;
import com.linkedInProject.ConnectionsService.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PersonService {

    public final PersonRepository personRepository ;

    public void createPerson(Long userId, String name){
        Person person = Person.builder()
                .userId(userId)
                .name(name)
                .build() ;
        personRepository.save(person);
    }
}
