package com.example.graphql.unionpagination.config

import graphql.scalars.ExtendedScalars
import graphql.schema.TypeResolver
import graphql.schema.idl.RuntimeWiring.Builder
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer
import org.springframework.context.annotation.*
import org.springframework.graphql.data.federation.FederationSchemaFactory
import org.springframework.graphql.execution.ClassNameTypeResolver
import org.springframework.graphql.execution.GraphQlSource.SchemaResourceBuilder
import org.springframework.graphql.execution.RuntimeWiringConfigurer

@Configuration
class GraphQLConfig {

    @Bean
    fun runtimeWiringConfigurer(): RuntimeWiringConfigurer {
        return RuntimeWiringConfigurer { wiringBuilder: Builder ->
            wiringBuilder
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.GraphQLLong)
        }
    }

    @Bean
    fun customizer(factory: FederationSchemaFactory): GraphQlSourceBuilderCustomizer? {
        return GraphQlSourceBuilderCustomizer { builder: SchemaResourceBuilder ->
            builder
                .schemaFactory(factory::createGraphQLSchema)
                .defaultTypeResolver(buildTypeResolver())
        }
    }

    @Bean
    fun schemaFactory(): FederationSchemaFactory {
        return FederationSchemaFactory()
    }

    private fun buildTypeResolver(): TypeResolver {
        val typeResolver = ClassNameTypeResolver()
        return typeResolver
    }
}
