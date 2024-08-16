package io.jay.dynamoapp

import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import kotlin.jvm.optionals.getOrNull

@Repository
class UserRepository(val client: DynamoDbEnhancedClient) {

    private val table: DynamoDbTable<UserEntity> =
        client.table("UserTable", TableSchema.fromBean(UserEntity::class.java))

    fun insert(user: UserEntity): User {
        table.putItem(user)
        return User(user.id, user.name, user.createdAt.toString())
    }

    fun findAll(): List<User> {
        return table.scan().items().map {
            User(it.id, it.name, it.createdAt.toString())
        }
    }

    fun findBy(sample: UserEntity): User {
        val found = table.getItem(sample) ?: throw RuntimeException("User not found with sample $sample")
        return User(found.id, found.name, found.createdAt.toString())
    }

    fun findById(id: String): User {
        val userEntity = findUserById(id)
        return User(userEntity.id, userEntity.name, userEntity.createdAt.toString())
    }

    fun update(id: String, updateRequest: UserUpdateRequest): User {
        val userEntity = findUserById(id)
        userEntity.name = updateRequest.name

        table.updateItem(userEntity)
        return User(userEntity.id, userEntity.name, userEntity.createdAt.toString())
    }

    private fun findUserById(id: String): UserEntity {
        val key = Key.builder()
            .partitionValue(id)
            .build()

        val queryRequest: QueryEnhancedRequest = QueryEnhancedRequest.builder()
            .queryConditional(QueryConditional.keyEqualTo(key))
            .limit(1)
            .build()

        val userEntity = table.query(queryRequest).items().stream().findAny().getOrNull()
            ?: throw RuntimeException("User not found with id $id")
        return userEntity
    }

    fun delete(id: String) {
        val userEntity = findUserById(id)
        table.deleteItem(userEntity)
    }

}