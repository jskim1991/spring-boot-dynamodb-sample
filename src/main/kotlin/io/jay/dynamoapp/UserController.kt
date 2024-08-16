package io.jay.dynamoapp

import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(private val repository: UserRepository) {

    @GetMapping
    fun findAll(): List<User> {
        return repository.findAll()
    }

    @GetMapping("/find")
    fun findUser(@RequestParam id: String): User {
        return repository.findBy(UserEntity.withId(id))
    }

    @GetMapping("/{id}")
    fun findUserById(@PathVariable id: String): User {
        return repository.findById(id)
    }

    @PostMapping
    @ResponseStatus(CREATED)
    fun createUser(@RequestBody createRequest: UserCreateRequest): User {
        return repository.insert(UserEntity(name = createRequest.name))
    }

    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: String, @RequestBody updateRequest: UserUpdateRequest): User {
        return repository.update(id, updateRequest)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    fun deleteUser(@PathVariable id: String) {
        repository.delete(id)
    }
}