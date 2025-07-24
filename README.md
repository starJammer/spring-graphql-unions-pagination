# SPRING-GRAPHQL-UNION-WITH-PAGINATION microservice

## Owners

jerry.saravia

## Purpose

The purpose of this project is to highlight how unions and relay pagination
types are not fully compatible in Spring Boot GraphQL. Additionally, I present
a workaround to make them work together but hope that the Spring Boot GraphQL
team will provide a better approach or guide this to a more final state.

## Getting Started

- Spring 3.4.1
- Java 21
- Kotlin 1.9.25

## Quick Start Commands

```shell
# Install dependencies
./scripts/bootstrap.sh

# Compile/Build with no tests
./gradlew build -x test

# Run the service on the command line, or start it from Intellij if you want
./scripts/server.sh
```

### Problem Description

Relay Pagination is supported in Spring Boot GraphQL so that in the following
schema the `integers` field will work correctly, but the `integersUnion` field will not.

See the [schema file in this project](./src/main/resources/graphql/schema.graphqls) for the schema.

You can start the service and see the resulting errors for yourself. (sample query below)

```graphql
type Query {
    """ returns a list of integers from 1 to 1000 """
    integers(first: Int, after: String): IntegerConnection!
    """ Same as above except the connection object is wrapped in a union """
    integersUnion(first: Int, after: String): IntegerResult!
}

union IntegerResult = IntegerConnection | NoIntegersFound

type IntegerConnection {
    edges: [IntegerEdge]!
    pageInfo: PageInfo
}

type NoIntegersFound {
    message: String!   
}

type IntegerEdge {
    node: Int
    cursor: String   
}

type PageInfo {
    startCursor: String
    endCursor: String
    hasNextPage: Boolean
    hasPreviousPage: Boolean
}
```

```graphql
query Integers {
	integersUnion {
		__typename
		... on IntegerConnection {
			pageInfo {
				startCursor
				endCursor
				hasNextPage
				hasPreviousPage
			}
			edges {
				node
			}
		}
		... on NoIntegersFound {
			message
		}
	}
}
```

The resulting error returned from the `integersUnion` field is:

```json
{
	"errors": [
		{
			"message": "The field at path '/integersUnion/edges' was declared as a non null type, but the code involved in retrieving data has wrongly returned a null value.  The graphql specification requires that the parent field be set to null, or if that is non nullable that it bubble up null to its parent and so on. The non-nullable type is '[IntegerEdge]' within parent type 'IntegerConnection'",
			"path": [
				"integersUnion",
				"edges"
			],
			"extensions": {
				"classification": "NullValueInNonNullableField"
			}
		}
	],
	"data": null
}
```

### Solution Description

Open [thise file](./src/main/kotlin/com/example/graphql/unionpagination/unionvisitor/GraphQLUnionConfiguration.kt) and 
go to line 23 and add uncomment it so the bean gets created. (make sure to uncomment the related import too
at the top of the file)

Run the project again and attempt the query again and notice that it works.
If you pass in a negative `first` value you will see that the `NoIntegersFound` object is returned
instead of an `IntegerConnection`.

`GraphQLUnionConfiguration` will register the [`ConnectionUnderUnionTypeVisitor`](./src/main/kotlin/com/example/graphql/unionpagination/unionvisitor/ConnectionUnderUnionTypeVisitor.kt).
Read comments on the class for more details but in short it will:

1. Look for union types being returned and in a decorator data fetcher.
2. The decorator data fecher will execute the original data fetcher and if the return value
is a `SliceWrapper` or `WindowWrapper` it will adapt the return value to a `Connection` object
the same way that `ConnectionFieldTypeVisitor` does. It also adds a type resolver for the union
so it resolves the connection object to the Connection type.

Limitations: 
1. Does not support multiple connection objects under a union.
2. Not sure if this is a good approach or if there is a better one.