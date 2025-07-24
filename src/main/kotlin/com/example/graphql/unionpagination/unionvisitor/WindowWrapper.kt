package com.example.graphql.unionpagination.unionvisitor

import org.springframework.data.domain.Window

interface WindowWrapper<T> {
    val window: Window<T>?
}
