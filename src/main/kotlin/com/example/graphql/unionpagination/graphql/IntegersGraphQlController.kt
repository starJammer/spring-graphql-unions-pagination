package com.example.graphql.unionpagination.graphql

import com.example.graphql.unionpagination.service.RelayPaginationHelper
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Window
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.stereotype.Controller

@Controller
class IntegersGraphQlController(
    val relayPaginationHelper: RelayPaginationHelper,
) {
    @QueryMapping
    fun integers(
        @Argument first: Int?,
        @Argument after: String?,
        scrollSubrange: ScrollSubrange,
    ): Window<Int> {
        val result = integersWindow(scrollSubrange)
        return result
    }

    @QueryMapping
    fun integersUnion(
        @Argument first: Int?,
        @Argument after: String?,
        scrollSubrange: ScrollSubrange,
    ): IntegerResult {
        val result = integersWindow(scrollSubrange)

        if (first != null && (first < 1 || first > 1000)) {
            return NoIntegersFound("First value must be between 1 and 1000")
        }

        return IntegerConnection(result)
    }

    private fun integersWindow(
        scrollSubrange: ScrollSubrange,
    ): Window<Int> {
        val (_, scrollPosition) = relayPaginationHelper.handleKeysetArguments(
            10,
            scrollSubrange,
            Sort.unsorted(),
            Sort.unsorted(),
        )

        val (start, count) = if (scrollPosition.isInitial) {
            Pair(1, 10)
        } else {
            var tmp = (scrollPosition.keys["start"] as Int)
            val tmp2 = scrollSubrange.count().orElse(10)
            Pair(
                tmp,
                if ((tmp + tmp2) <= 1000) tmp2 else 1000 - tmp + 1,
            )
        }

        val contents = if (start >= 1 && start <= 1000) {
            start.rangeTo(start + count - 1).toList()
        } else {
            emptyList()
        }

        val positionFunction = { index: Int ->
            ScrollPosition.of(
                mapOf<String, Int>("start" to contents[index]),
                ScrollPosition.Direction.FORWARD,
            )
        }

        return Window.from(contents, positionFunction, (start + count) <= 1000)
    }
}
