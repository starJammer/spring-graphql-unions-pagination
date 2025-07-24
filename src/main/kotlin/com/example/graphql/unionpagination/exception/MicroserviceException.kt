package com.example.graphql.unionpagination.exception

class MicroserviceException(
    val error: ErrorResponse,
) : RuntimeException(error.message)
