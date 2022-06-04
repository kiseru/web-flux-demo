package com.example.webfluxdemo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableWebFlux
public class WebFluxDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebFluxDemoApplication.class, args);
    }

    @Bean
    public RouterFunction<ServerResponse> routerFunction(PersonHandler handler) {
        return RouterFunctions.route()
                .path("/person", builder -> builder
                        .GET("/{id}", RequestPredicates.accept(MediaType.APPLICATION_JSON), handler::getPerson)
                        .GET(RequestPredicates.accept(MediaType.APPLICATION_JSON), handler::listPeople)
                        .POST("/", handler::createPerson))
                .build();
    }
}

@Data
@AllArgsConstructor
@RequiredArgsConstructor
class Person {

    private int id;

    private String name;
}

@Component
@RequiredArgsConstructor
class PersonHandler {

    private final PersonRepository repository;

    public Mono<ServerResponse> listPeople(ServerRequest request) {
        Flux<Person> people = repository.allPeople();
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(people, Person.class);
    }

    public Mono<ServerResponse> createPerson(ServerRequest request) {
        Mono<Person> person = request.bodyToMono(Person.class);
        return ServerResponse.ok()
                .build(repository.savePerson(person));
    }

    public Mono<ServerResponse> getPerson(ServerRequest request) {
        int personId = Integer.parseInt(request.pathVariable("id"));
        return repository.getPerson(personId)
                .flatMap(person -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(person))
                .switchIfEmpty(ServerResponse.notFound().build());
    }
}

@Repository
class PersonRepository {

    private final Map<Integer, Person> people = new HashMap<>();

    public PersonRepository() {
        people.put(1, new Person(1, "Some cool name #1"));
        people.put(2, new Person(2, "Some cool name #2"));
        people.put(3, new Person(3, "Some cool name #3"));
    }

    public Flux<Person> allPeople() {
        return Flux.fromIterable(people.values());
    }

    public Mono<Void> savePerson(Mono<Person> person) {
        return person.doOnNext(p -> people.put(p.getId(), p))
                .then();
    }

    public Mono<Person> getPerson(int id) {
        return Mono.justOrEmpty(people.get(id));
    }
}
