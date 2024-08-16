package io.jay.dynamoapp

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import java.time.Instant
import java.util.*

@DynamoDbBean
data class UserEntity(
    @get:DynamoDbPartitionKey
    @get:DynamoDbAttribute("id")
    var id: String,
    @get:DynamoDbAttribute("name")
    var name: String,
    @get:DynamoDbAttribute("createdAt")
    var createdAt: Instant? = null,
) {
    // for dynamo db
    constructor() : this(UUID.randomUUID().toString(), "", Instant.now())

    constructor(name: String) : this(UUID.randomUUID().toString(), name, Instant.now())

    companion object {
        fun withId(id: String): UserEntity {
            return UserEntity(id, "")
        }
    }
}