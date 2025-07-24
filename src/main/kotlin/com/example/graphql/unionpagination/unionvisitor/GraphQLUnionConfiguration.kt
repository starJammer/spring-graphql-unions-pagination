package com.example.graphql.unionpagination.unionvisitor

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.ScrollPosition
import org.springframework.graphql.data.pagination.CursorStrategy
import org.springframework.graphql.data.query.SliceConnectionAdapter
import org.springframework.graphql.data.query.WindowConnectionAdapter
import org.springframework.graphql.execution.GraphQlSource

@Suppress("UNCHECKED_CAST")
@ConditionalOnClass(ScrollPosition::class)
@Configuration(proxyBeanMethods = false)
class GraphQLUnionConfiguration {
    /**
     * cursorStrategyCustomizerForUnions works similarly to
     * [org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration.GraphQlDataAutoConfiguration.cursorStrategyCustomizer]
     * except that instead of registering a [org.springframework.graphql.data.pagination.ConnectionFieldTypeVisitor] it
     * registers a [ConnectionUnderUnionTypeVisitor] instead.
     */
//    @Bean
    fun cursorStrategyCustomizerForUnions(cursorStrategy: CursorStrategy<*>): GraphQlSourceBuilderCustomizer {
        if (cursorStrategy.supports(ScrollPosition::class.java)) {
            val scrollCursorStrategy = cursorStrategy as CursorStrategy<ScrollPosition>
            val connectionUnderUnionTypeVisitor = ConnectionUnderUnionTypeVisitor
                .create(
                    listOf(
                        WindowConnectionAdapter(scrollCursorStrategy),
                        SliceConnectionAdapter(scrollCursorStrategy),
                    ),
                )
            return GraphQlSourceBuilderCustomizer { builder: GraphQlSource.SchemaResourceBuilder ->
                builder.typeVisitors(listOf(connectionUnderUnionTypeVisitor))
            }
        }
        return GraphQlSourceBuilderCustomizer { _: GraphQlSource.SchemaResourceBuilder? -> }
    }
}
