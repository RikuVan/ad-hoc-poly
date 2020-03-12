package com.sample

import arrow.core.Either
import arrow.core.fix
import arrow.core.left
import arrow.core.right
import arrow.fx.ForIO
import arrow.fx.fix
import com.validation.*
import com.validation.ValidationError.UserLoginExits
import org.springframework.http.MediaType
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.ServerResponse.badRequest
import org.springframework.web.servlet.function.ServerResponse.ok
import org.springframework.web.servlet.function.body

class Handlers(private val userRepository: UserRepository,
               private val cityRepository: CityRepository,
               private val blockingRepo: RepoTC<ForIO>
) {
    fun listApi(request: ServerRequest): ServerResponse {
        return ok().contentType(MediaType.APPLICATION_JSON).body(userRepository.findAll())
    }
    
    fun upsert(request: ServerRequest): ServerResponse { // 👎🏼 This is struck with using FailFast strategy
        val user = request.body<User>()
        val isEmailValid = validateEmail(user.email)
        return isEmailValid.fold(
                { badRequest().body("$user email validation error: $it") },
                {   
                    if (cityRepository.findFirstCityWith(user.city)) {
                        if (userRepository.findFirstUserWith(user.login)) {
                            userRepository.update(user)
                            ok().body("Updated!! $user")
                        } else {
                            userRepository.insert(user)
                            ok().body("Inserted!! $user")
                        }
                    } else {
                        badRequest().body("City is invalid!! : $user")
                    }
                }
        )
    }

    companion object Utils {
        private fun validateEmail(email: String): Either<ValidationError, String> =
                if (email.contains("@", false)) {
                    if (email.length <= 250) {
                        email.right()
                    } else {
                        ValidationError.MaxLength(250).left()
                    }
                } else {
                    ValidationError.DoesNotContain("@").left()
                }
    }

    fun upsertX(request: ServerRequest): ServerResponse {
        val user = request.body<User>()
        return blockingRepo.run {
            RulesRunnerStrategy.failFast<ValidationError>().run {
                userRuleRunner(user).fix().unsafeRunSync()
            }
        }.fix().fold(
                { reasons ->
                    when (reasons.head) {
                        UserLoginExits(user.login) -> {
                            userRepository.update(user)
                            ok().body("Updated!! $user")
                        }
                        else -> badRequest().body("Cannot Upsert!!, reasons: $reasons")
                    }
                },
                {
                    userRepository.insert(user)
                    ok().body("Inserted!! $user")
                }
        )
    }
}
