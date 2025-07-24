package com.example.graphql.unionpagination.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator
import org.springframework.context.annotation.*
import org.springframework.data.domain.ScrollPosition
import org.springframework.graphql.data.pagination.CursorEncoder
import org.springframework.graphql.data.pagination.CursorStrategy
import org.springframework.graphql.data.pagination.EncodingCursorStrategy
import org.springframework.graphql.data.query.JsonKeysetCursorStrategy
import org.springframework.graphql.data.query.ScrollPositionCursorStrategy
import org.springframework.http.codec.CodecConfigurer
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.util.ClassUtils
import java.util.*

/**
 * GraphQlPaginationConfig adds better pagination support.
 * This configuration is based off a Github issue and its resolution.
 *
 * @see <a href="https://github.com/spring-projects/spring-graphql/issues/1047">Spring GraphQl Github Issue 1047</a>
 */
@Configuration
class GraphQLPaginationConfig {

    /**
     * Create our own EncodingCursorStrategy bean to support objects that use
     * Long as an id when decoding or encoding a cursor. The default GraphQL
     * encoding strategy only supports certain types.
     *
     * @see org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration.GraphQlDataAutoConfiguration.cursorStrategy This sets a ScrollPositionCursorStrategy with a default JsonKeysetCursorStrategy.
     * @see ScrollPositionCursorStrategy This uses a basic JsonKeysetCursorStrategy
     * @see JsonKeysetCursorStrategy We use our own JsonKeysetCursorStrategy with additional configs
     */
    @Bean
    fun cursorStrategy(): EncodingCursorStrategy<ScrollPosition> {
        // Check to make sure jackson is present in the classpath, otherwise
        // we won't customize anything.
        val jackson2Present = ClassUtils.isPresent(
            "com.fasterxml.jackson.databind.ObjectMapper",
            JsonKeysetCursorStrategy::class.java.classLoader,
        )
        val configurer = ServerCodecConfigurer.create()
        if (jackson2Present) {
            JacksonObjectMapperCustomizer.customize(configurer)
        }
        val jsonKeysetCursorStrategy = JsonKeysetCursorStrategy(configurer)
        return CursorStrategy.withEncoder(ScrollPositionCursorStrategy(jsonKeysetCursorStrategy), CursorEncoder.base64())
    }

    /**
     * JacksonObjectMapperCustomizer helps to customize the Jackson ObjectMapper
     * so that we can properly decode and encode certain types into cursors.
     *
     * Additional classes supported by this config:
     *   1. java.lang.Long::class.java
     */
    private object JacksonObjectMapperCustomizer {
        fun customize(configurer: CodecConfigurer) {
            val validator: PolymorphicTypeValidator = BasicPolymorphicTypeValidator.builder()
                // default supported types
                .allowIfBaseType(MutableMap::class.java)
                .allowIfSubType("java.time.")
                .allowIfSubType(Calendar::class.java)
                .allowIfSubType(Date::class.java)
                // additional supported types
                .allowIfSubType(java.lang.Long::class.java)
                .build()

            val mapper = Jackson2ObjectMapperBuilder.json().build<ObjectMapper>()
            mapper.activateDefaultTyping(validator, ObjectMapper.DefaultTyping.NON_FINAL)

            configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(mapper))
            configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(mapper))
        }
    }
}
