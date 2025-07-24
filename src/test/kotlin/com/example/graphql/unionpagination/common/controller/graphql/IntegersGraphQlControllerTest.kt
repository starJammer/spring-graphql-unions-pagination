package com.example.graphql.unionpagination.common.controller.graphql

import com.example.graphql.unionpagination.BaseGraphQlTest
import com.example.graphql.unionpagination.graphql.IntegersGraphQlController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest
import org.springframework.graphql.test.tester.GraphQlTester

@GraphQlTest(IntegersGraphQlController::class)
class IntegersGraphQlControllerTest : BaseGraphQlTest() {
    @Autowired
    lateinit var graphQlTester: GraphQlTester

    fun syntheticHealthCheckPresence() {
        val document = """
            query {
            }
        """.trimIndent()

        graphQlTester.document(document).execute()
            .path("")
            .entity(String::class.java)
            .isEqualTo("Ok")
    }
}
