package com.example.graphql.unionpagination.graphql

import com.example.graphql.unionpagination.unionvisitor.WindowWrapper
import org.springframework.data.domain.Window

data class IntegerConnection(override val window: Window<Int>) : WindowWrapper<Int>, IntegerResult
