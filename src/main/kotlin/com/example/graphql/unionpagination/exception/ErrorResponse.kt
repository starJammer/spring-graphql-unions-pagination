package com.example.graphql.unionpagination.exception

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpStatus

data class ErrorResponse(
    @JsonIgnore
    val status: HttpStatus,
    val code: String,
    val message: String,
)
