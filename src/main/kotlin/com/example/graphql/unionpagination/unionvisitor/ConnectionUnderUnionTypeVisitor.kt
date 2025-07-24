package com.example.graphql.unionpagination.unionvisitor

import graphql.TrivialDataFetcher
import graphql.execution.DataFetcherResult
import graphql.relay.Connection
import graphql.relay.DefaultConnection
import graphql.relay.DefaultConnectionCursor
import graphql.relay.DefaultEdge
import graphql.relay.DefaultPageInfo
import graphql.relay.Edge
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLCodeRegistry
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchemaElement
import graphql.schema.GraphQLType
import graphql.schema.GraphQLTypeVisitorStub
import graphql.schema.GraphQLUnionType
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.pagination.ConnectionAdapter
import org.springframework.graphql.execution.ClassNameTypeResolver
import org.springframework.graphql.execution.TypeVisitorHelper
import org.springframework.lang.Nullable
import org.springframework.util.Assert
import reactor.core.publisher.Mono
import java.util.concurrent.CompletionStage

/**
 * [ConnectionUnderUnionTypeVisitor] is a [graphql.schema.GraphQLTypeVisitor] implementation
 * that is based off of [org.springframework.graphql.data.pagination.ConnectionFieldTypeVisitor]
 * but is modified to look for union types with Connection objects underneath.
 *
 * [org.springframework.graphql.data.pagination.ConnectionFieldTypeVisitor] looks for `field: SomeConnection`
 * while [ConnectionUnderUnionTypeVisitor] looks for `field: SomeUnion`
 * where the union is defined as `union SomeUnion: SomeConnection | OtherResultTypes`
 * and only has one connection type under it.
 *
 * If it finds any unions like that, [ConnectionUnderUnionTypeVisitor] decorates their
 * registered {@link DataFetcher} in order to adapt the return values to {@link Connection}
 * but only for the Connection types. All other types are resolved using a [ClassNameTypeResolver].
 */
@Suppress("DEPRECATION")
class ConnectionUnderUnionTypeVisitor : GraphQLTypeVisitorStub {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val adapter: ConnectionAdapter

    private constructor(adapter: ConnectionAdapter) {
        Assert.notNull(adapter, "ConnectionAdapter is required")
        this.adapter = adapter
    }

    override fun visitGraphQLFieldDefinition(
        fieldDefinition: GraphQLFieldDefinition,
        context: TraverserContext<GraphQLSchemaElement?>,
    ): TraversalControl {
        val visitorHelper = context.getVarFromParents(TypeVisitorHelper::class.java)
        val codeRegistry = context.getVarFromParents(GraphQLCodeRegistry.Builder::class.java)

        val parent = context.parentNode as GraphQLFieldsContainer
        var dataFetcher = codeRegistry.getDataFetcher(parent, fieldDefinition)

        if (visitorHelper != null && visitorHelper.isSubscriptionType(parent)) {
            return TraversalControl.ABORT
        }

        if (isUnionWithConnectionUnderneath(fieldDefinition)) {
            if (dataFetcher is TrivialDataFetcher<*>) {
                if (logger.isDebugEnabled) {
                    logger.debug(
                        "Skipping connection field " +
                            "'" + parent.name + ":" + fieldDefinition.name + "' " +
                            "because it is mapped to trivial data fetcher: " + dataFetcher.javaClass.getName(),
                    )
                }
            } else {
                dataFetcher = ConnectionDataFetcher(dataFetcher, this.adapter)
                codeRegistry.dataFetcher(parent, fieldDefinition, dataFetcher)
                assignTypeResolverToUnion(fieldDefinition, codeRegistry)
            }
        }

        return TraversalControl.CONTINUE
    }

    private fun assignTypeResolverToUnion(
        fieldDefinition: GraphQLFieldDefinition,
        codeRegistry: GraphQLCodeRegistry.Builder,
    ) {
        val unionType = getFieldType(fieldDefinition) as GraphQLUnionType
        val typeResolver = ClassNameTypeResolver()
        unionType.types.forEach {
            if (isConnectionField(it as GraphQLObjectType)) {
                typeResolver.addMapping(Connection::class.java, it.name)
            }
        }
        codeRegistry.typeResolver(unionType, typeResolver)
    }

    private fun isUnionWithConnectionUnderneath(field: GraphQLFieldDefinition): Boolean {
        val type = getFieldType(field)

        if (type !is GraphQLUnionType) {
            return false
        }

        return type.types.count { isConnectionField(it as GraphQLObjectType) } == 1
    }

    private fun isConnectionField(type: GraphQLObjectType?): Boolean {
        if (type == null ||
            !type.name.endsWith("Connection")
        ) {
            return false
        }

        val edgeType = getEdgeType(type.getField("edges"))
        if (edgeType == null ||
            !edgeType.name.endsWith("Edge")
        ) {
            return false
        }
        if (edgeType.getField("node") == null ||
            edgeType.getField("cursor") == null
        ) {
            return false
        }

        val pageInfoType = getAsObjectType(type.getField("pageInfo"))
        if (pageInfoType == null ||
            pageInfoType.name != "PageInfo"
        ) {
            return false
        }
        if (pageInfoType.getField("hasPreviousPage") == null ||
            pageInfoType.getField("hasNextPage") == null ||
            pageInfoType.getField("startCursor") == null ||
            pageInfoType.getField("endCursor") == null
        ) {
            return false
        }

        return true
    }

    @Nullable
    private fun getEdgeType(@Nullable field: GraphQLFieldDefinition?): GraphQLObjectType? {
        val fieldType = getFieldType(field)
        if (fieldType is GraphQLList) {
            val wrapperType = fieldType.wrappedType
            if (wrapperType is GraphQLObjectType) {
                return wrapperType
            }
        }
        return null
    }

    @Nullable
    private fun getAsObjectType(@Nullable field: GraphQLFieldDefinition?): GraphQLObjectType? {
        val type = getFieldType(field)
        return (type as? GraphQLObjectType)
    }

    @Nullable
    private fun getFieldType(@Nullable field: GraphQLFieldDefinition?): GraphQLType? {
        if (field == null) {
            return null
        }
        val type = field.type
        return (if (type is GraphQLNonNull) type.wrappedType else type)
    }

    companion object {
        /**
         * Create a `ConnectionUnderUnionTypeVisitor` instance that delegates to the
         * given adapters to adapt return values to [Connection].
         * @param adapters the adapters to use
         * @return the type visitor
         */
        @JvmStatic
        fun create(adapters: List<ConnectionAdapter>): ConnectionUnderUnionTypeVisitor {
            Assert.notEmpty(adapters, "Expected at least one ConnectionAdapter")
            return ConnectionUnderUnionTypeVisitor(ConnectionAdapter.from(adapters))
        }
    }

    /**
     * `DataFetcher` decorator that adapts return values with an adapter.
     */
    @JvmRecord
    private data class ConnectionDataFetcher(
        val delegate: DataFetcher<*>,
        val adapter: ConnectionAdapter,
    ) : DataFetcher<Any?> {
        @Throws(Exception::class)
        override fun get(environment: DataFetchingEnvironment): Any {
            val result: Any? = this.delegate.get(environment)?.let {
                if (it is WindowWrapper<*>) {
                    it.window
                } else if (it is SliceWrapper<*>) {
                    it.slice
                } else {
                    return it
                }
            }

            if (result is Mono<*>) {
                return result.map { value: Any? -> this.adaptDataFetcherResult(value) }
            } else if (result is CompletionStage<*>) {
                return result.thenApply { value: Any? -> this.adaptDataFetcherResult(value) }
            } else {
                return adaptDataFetcherResult(result)
            }
        }

        fun adaptDataFetcherResult(@Nullable value: Any?): Any {
            if (value is DataFetcherResult<*>) {
                val adapted = adaptDataContainer<Any?>(value.data)
                return DataFetcherResult.newResult<Any?>()
                    .data(adapted)
                    .errors(value.errors)
                    .localContext(value.localContext).build()
            } else {
                return adaptDataContainer<Any?>(value)
            }
        }

        fun <T> adaptDataContainer(@Nullable container: Any?): Any {
            if (container == null) {
                return EMPTY_CONNECTION
            }

            if (container is Connection<*>) {
                return container
            }

            if (!this.adapter.supports(container.javaClass)) {
                if (container.javaClass.getName().endsWith("Connection")) {
                    return container
                }
                throw IllegalStateException("No ConnectionAdapter for: " + container.javaClass.getName())
            }

            val nodes = this.adapter.getContent<T?>(container)
            if (nodes.isEmpty()) {
                return EMPTY_CONNECTION
            }

            var index = 0
            val edges: MutableList<Edge<T?>?> = ArrayList(nodes.size)
            for (node in nodes) {
                val cursor = this.adapter.cursorAt(container, index++)
                edges.add(DefaultEdge<T?>(node, DefaultConnectionCursor(cursor)))
            }

            val pageInfo = DefaultPageInfo(
                edges[0]!!.cursor,
                edges[edges.size - 1]!!.cursor,
                this.adapter.hasPrevious(container),
                this.adapter.hasNext(container),
            )

            return DefaultConnection<T?>(edges, pageInfo)
        }

        init {
            Assert.notNull(delegate, "DataFetcher delegate is required")
            Assert.notNull(adapter, "ConnectionAdapter is required")
        }

        companion object {
            private val EMPTY_CONNECTION: Connection<*> =
                DefaultConnection<Any?>(mutableListOf<Edge<Any?>?>(), DefaultPageInfo(null, null, false, false))
        }
    }
}
