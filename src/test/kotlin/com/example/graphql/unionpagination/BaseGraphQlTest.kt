package com.example.graphql.unionpagination

import com.example.graphql.unionpagination.config.GraphQLConfig
import org.junit.jupiter.api.Tag
import org.springframework.context.annotation.Import

@Tag(TestTags.GRAPHQL_TEST)
@Import(GraphQLConfig::class)
abstract class BaseGraphQlTest
