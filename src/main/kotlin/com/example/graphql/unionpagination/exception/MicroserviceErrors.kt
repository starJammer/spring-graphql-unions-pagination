package com.example.graphql.unionpagination.exception

import org.springframework.http.HttpStatus

object MicroserviceErrors {
    val INTERNAL_SERVER_ERROR =
        ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "api_error", "Something went wrong.")

    val INVALID_INPUT_DATA =
        ErrorResponse(HttpStatus.BAD_REQUEST, "invalid_input_data", "Please check your input data.")

    val REQUEST_TIMEOUT_ERROR =
        ErrorResponse(HttpStatus.REQUEST_TIMEOUT, "request_timeout", "Client did not respond quickly enough.")

    val UNSUPPORTED_MEDIA_TYPE =
        ErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_media", "Part of the request was in an unsupported format.")

    val FORBIDDEN_ERROR =
        ErrorResponse(HttpStatus.FORBIDDEN, "forbidden_access", "Authenticated and provided user are different.")

    val ACCESS_DENIED_ERROR =
        ErrorResponse(HttpStatus.FORBIDDEN, "access_denied_access", "User does not have necessary permissions to access the endpoint.")

    val INVALID_EMAIL_ADDRESS =
        ErrorResponse(HttpStatus.BAD_REQUEST, "invalid_email_address", "Please enter a valid email address.")

    val MESSAGE_NOT_FOUND =
        ErrorResponse(HttpStatus.NOT_FOUND, "message_not_found", "Requested message not found.")
}
