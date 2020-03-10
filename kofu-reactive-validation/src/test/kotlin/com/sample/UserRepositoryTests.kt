package com.sample

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.WebApplicationType
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.fu.kofu.application
import reactor.core.publisher.Mono

class UserRepositoryTests {

    private val dataApp = application(WebApplicationType.NONE) {
        enable(dataConfig)
    }

    private lateinit var context: ConfigurableApplicationContext
    private lateinit var repository: UserRepository

    @BeforeAll
    fun beforeAll() {
        context = dataApp.run(profiles = "test")
        repository = context.getBean()
    }

    @Test
    fun count() {
        assertEquals(3, repository.count().block())
    }

    @Test
    fun userLoginValid() {
        assertTrue(repository.findFirstUserWith("smaldini").block() == 1)
    }

    @Test
    fun userLoginInValid() {
        val flatMap = repository.findFirstUserWith("gakshintala")
                .defaultIfEmpty(0)
        assertTrue(flatMap.block() == 0)
    }

    @AfterAll
    fun afterAll() {
        context.close()
    }
}