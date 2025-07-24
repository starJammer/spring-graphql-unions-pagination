package com.example.graphql.unionpagination

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@Tag(TestTags.INTEGRATION_TEST)
@ActiveProfiles("test")
@SpringBootTest
@ContextConfiguration(initializers = [ConfigDataApplicationContextInitializer::class])
abstract class BaseIntegrationTest {
    @Test
    fun test() {
    }
}
