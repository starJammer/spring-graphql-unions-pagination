package com.example.graphql.unionpagination.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.KeysetScrollPosition
import org.springframework.data.domain.OffsetScrollPosition
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Sort
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.stereotype.Service

@Service
class RelayPaginationHelper {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * handleOffsetArguments primarily uses the scrollSubrange
     * to extract a PageRequest object and an OffsetScrollPosition.
     *
     * It should be used when you have chosen to use an offset-based
     * cursor.
     *
     * @param defaultCount Default count to use if first and last were null and scrollSubrange.count() ends up empty
     * @param scrollSubrange A ScrollSubrange that Spring for GraphQL normally creates
     * @param forwardSort The sort to use if moving forward through results
     * @param backwardSort The sort to use if moving backward through results
     */
    fun handleOffsetArguments(defaultCount: Int, scrollSubrange: ScrollSubrange, forwardSort: Sort, backwardSort: Sort): Pair<PageRequest, OffsetScrollPosition> {
        // count will have either first or last
        logger.debug("scrollSubrange.count=${scrollSubrange.count()}, forward=${scrollSubrange.forward()}, backward=${!scrollSubrange.forward()}")

        val position = scrollSubrange.position().orElse(ScrollPosition.offset())
        if (position !is OffsetScrollPosition) {
            throw Exception("Unsupported after or before parameter: $position")
        }
        logger.debug("position=$position, isInitial=${position.isInitial}")

        val pageSize = scrollSubrange.count().orElse(defaultCount)
        // even though we are using an offset-based cursor
        // Spring uses pages to make requests to the database,
        // Here we do some math to get the page number from the current
        // offset based on the page size
        val pageNumber = if (position.isInitial) {
            0
        } else {
            if (scrollSubrange.forward()) {
                Math.ceil((position.offset + 1).toDouble() / pageSize).toInt()
            } else {
                Math.floor((position.offset + 1).toDouble() / pageSize).toInt()
            }
        }
        logger.debug("pageSize=$pageSize, pageNumber=$pageNumber")

        val sort = if (scrollSubrange.forward()) forwardSort else backwardSort
        val pageRequest = PageRequest.of(pageNumber, pageSize).withSort(sort)
        logger.debug("PageRequest=$pageRequest")

        return Pair(pageRequest, position)
    }

    /**
     * This version of handleOffsetArguments will take in
     * the parameters first, after, last and before
     * but do the same thing as the method above that retuns
     * an OffsetScrollPosition.
     *
     * @see RelayPaginationHelper.handleOffsetArguments(scrollSubrange: ScrollSubrange): Pair<PageRequest, OffsetScrollPosition>
     *
     * The first, after, last, and before arguments are ignored but
     * are printed out to the command line primarily so you can
     * see what the PageRequest and OffsetScrollPosition look like
     * depending on what the arguments are.
     *
     * @param first Relay pagination first parameter
     * @param after Relay pagination after parameter
     * @param last Relay pagination last parameter
     * @param before Relay pagination before parameter
     * @param defaultCount Default count to use if first and last were null and scrollSubrange.count() ends up empty
     * @param scrollSubrange A ScrollSubrange that Spring for GraphQL normally creates
     * @param forwardSort The sort to use if moving forward through results
     * @param backwardSort The sort to use if moving backward through results
     */
    fun handleOffsetArguments(first: Int?, after: String?, last: Int?, before: String?, defaultCount: Int, scrollSubrange: ScrollSubrange, forwardSort: Sort, backwardSort: Sort): Pair<PageRequest, OffsetScrollPosition> {
        logger.debug("first=$first, after=$after, last=$last, before=$before")
        return handleOffsetArguments(defaultCount, scrollSubrange, forwardSort, backwardSort)
    }

    /**
     * handleOffsetArguments primarily uses the scrollSubrange
     * to extract a PageRequest object and a KeysetScrollPosition.
     *
     * It should be used when you have chosen to use a keyset-based
     * cursor.
     *
     * @param defaultCount Default count to use if first and last were null and scrollSubrange.count() ends up empty
     * @param scrollSubrange A ScrollSubrange that Spring for GraphQL normally creates
     * @param forwardSort The sort to use if moving forward through results
     * @param backwardSort The sort to use if moving backward through results
     */
    fun handleKeysetArguments(defaultCount: Int, scrollSubrange: ScrollSubrange, forwardSort: Sort, backwardSort: Sort): Pair<PageRequest, KeysetScrollPosition> {
        // count will have either first or last
        logger.debug("scrollSubrange.count=${scrollSubrange.count()}, forward=${scrollSubrange.forward()}, backward=${!scrollSubrange.forward()}")

        val position = scrollSubrange.position().orElse(ScrollPosition.keyset().forward())

        // The line of code SHOULD be better than the above, but Spring does some data re-ordering under the hood
        // that causes the sort on the PageRequest to be ignored.
        // val position = scrollSubrange.position().orElse(if (scrollSubrange.forward()) ScrollPosition.keyset().forward() else ScrollPosition.keyset().backward())
        if (position !is KeysetScrollPosition) {
            throw Exception("Unsupported after or before parameter:  $position")
        }
        logger.debug("position=$position, isInitial=${position.isInitial}")
        val pageSize = scrollSubrange.count().orElse(defaultCount)
        logger.debug("pageSize=$pageSize")

        val sort = if (scrollSubrange.forward()) forwardSort else backwardSort
        val pageRequest = PageRequest.ofSize(pageSize).withSort(sort)

        logger.debug("PageRequest=$pageRequest")

        return Pair(pageRequest, position)
    }

    /**
     * This version of handleOffsetArguments will take in
     * the parameters first, after, last and before
     * but do the same thing as the method above that returns
     * a KeysetScrollPosition.
     *
     * @see RelayPaginationHelper.handleOffsetArguments(scrollSubrange: ScrollSubrange): Pair<PageRequest, KeysetScrollPosition>
     *
     * The first, after, last, and before arguments are ignored but
     * are printed out to the command line primarily so you can
     * see what the PageRequest and KeysetScrollPosition look like
     * depending on what the arguments are.
     *
     * @param first Relay pagination first parameter
     * @param after Relay pagination after parameter
     * @param last Relay pagination last parameter
     * @param before Relay pagination before parameter
     * @param defaultCount Default count to use if first and last were null and scrollSubrange.count() ends up empty
     * @param scrollSubrange A ScrollSubrange that Spring for GraphQL normally creates
     * @param forwardSort The sort to use if moving forward through results
     * @param backwardSort The sort to use if moving backward through results
     */
    fun handleKeysetArguments(first: Int?, after: String?, last: Int?, before: String?, defaultCount: Int, scrollSubrange: ScrollSubrange, forwardSort: Sort, backwardSort: Sort): Pair<PageRequest, KeysetScrollPosition> {
        logger.debug("first=$first, after=$after, last=$last, before=$before")
        return handleKeysetArguments(defaultCount, scrollSubrange, forwardSort, backwardSort)
    }
}
