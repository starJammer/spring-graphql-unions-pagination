package com.example.graphql.unionpagination.unionvisitor

import org.springframework.data.domain.Slice

interface SliceWrapper<T> {
    val slice: Slice<T>?
}
