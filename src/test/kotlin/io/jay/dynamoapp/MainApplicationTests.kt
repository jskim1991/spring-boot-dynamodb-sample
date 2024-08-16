package io.jay.dynamoapp

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import java.net.URI

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class MainApplicationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        private const val PRIMARY_KEY_ATTRIBUTE_NAME = "id"
        private const val USER_TABLE_NAME = "UserTable"
        private const val REGION = "ap-northeast-1"
        private const val ACCESS_KEY_ID = "dummy"
        private const val SECRET_ACCESS_KEY = "dummy"


        private val dynamoDb: GenericContainer<Nothing> = GenericContainer<Nothing>(
            DockerImageName.parse("public.ecr.aws/aws-dynamodb-local/aws-dynamodb-local:latest")
                .asCompatibleSubstituteFor("amazon/dynamodb-local")
        ).apply {
            withExposedPorts(8000)
        }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            dynamoDb.start()

            val dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:${dynamoDb.getMappedPort(8000)}"))
                .region(Region.of(REGION))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY_ID, SECRET_ACCESS_KEY)
                    )
                )
                .build()

            val createTableRequest = CreateTableRequest.builder()
                .attributeDefinitions(
                    AttributeDefinition.builder()
                        .attributeName(PRIMARY_KEY_ATTRIBUTE_NAME)
                        .attributeType(ScalarAttributeType.S)
                        .build()
                )
                .keySchema(
                    KeySchemaElement.builder()
                        .attributeName(PRIMARY_KEY_ATTRIBUTE_NAME)
                        .keyType(KeyType.HASH)
                        .build()
                )
                .provisionedThroughput(
                    ProvisionedThroughput.builder()
                        .readCapacityUnits(1)
                        .writeCapacityUnits(1)
                        .build()
                )
                .tableName(USER_TABLE_NAME)
                .build()

            val createTableResponse = dynamoDbClient.createTable(createTableRequest)
            println(createTableResponse.tableDescription().tableArn())

            val tableRequest = DescribeTableRequest.builder()
                .tableName("UserTable")
                .build()

            val waiterResponse = dynamoDbClient.waiter().waitUntilTableExists(tableRequest)
            waiterResponse.matched().response().ifPresent(System.out::println)
        }


        @AfterAll
        @JvmStatic
        fun afterAll() {
            dynamoDb.stop()
        }

        @DynamicPropertySource
        @JvmStatic
        fun registerDynamoDbProperties(registry: DynamicPropertyRegistry) {
            registry.add("aws.dynamodb.endpoint") { "http://localhost:${dynamoDb.getMappedPort(8000)}" }
        }
    }

    @BeforeEach
    fun setUp() {
        val users = userRepository.findAll()
        users.forEach {
            userRepository.delete(it.id)
        }
    }

    @Test
    fun `should find all users`() {
        mockMvc.perform(
            post("/api/v1/users")
                .content(objectMapper.writeValueAsString(UserCreateRequest("Jay")))
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .response
            .contentAsString


        val actualResponse = mockMvc.perform(get("/api/v1/users"))
            .andReturn()
            .response
            .contentAsString
        val actual = objectMapper.readValue<List<User>>(actualResponse)


        val saved = userRepository.findAll()
        assertThat(actual).hasSize(saved.size)
        assertThat(actual[0].id).isEqualTo(saved[0].id)
        assertThat(actual[0].name).isEqualTo(saved[0].name)
        assertThat(actual[0].createdAt).isEqualTo(saved[0].createdAt)
    }

    @Test
    fun `should find a user (request parameter)`() {
        val postResponse = mockMvc.perform(
            post("/api/v1/users")
                .content(objectMapper.writeValueAsString(UserCreateRequest("Jay")))
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .response
            .contentAsString
        val posted = objectMapper.readValue<User>(postResponse)


        val response = mockMvc.perform(get("/api/v1/users/find?id=${posted.id}"))
            .andReturn()
            .response
            .contentAsString
        val actual = objectMapper.readValue<User>(response)


        val saved = userRepository.findBy(UserEntity.withId(posted.id))
        assertThat(actual.id).isEqualTo(saved.id)
        assertThat(actual.name).isEqualTo(saved.name)
        assertThat(actual.createdAt).isEqualTo(saved.createdAt)
    }

    @Test
    fun `should find a user with id (path)`() {
        val postResponse = mockMvc.perform(
            post("/api/v1/users")
                .content(objectMapper.writeValueAsString(UserCreateRequest("Jay")))
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .response
            .contentAsString
        val posted = objectMapper.readValue<User>(postResponse)


        val response = mockMvc.perform(get("/api/v1/users/${posted.id}"))
            .andReturn()
            .response
            .contentAsString
        val actual = objectMapper.readValue<User>(response)


        val saved = userRepository.findById(posted.id)
        assertThat(actual.id).isEqualTo(saved.id)
        assertThat(actual.name).isEqualTo(saved.name)
        assertThat(actual.createdAt).isEqualTo(saved.createdAt)
    }

    @Test
    fun `should update a user`() {
        val postResponse = mockMvc.perform(
            post("/api/v1/users")
                .content(objectMapper.writeValueAsString(UserCreateRequest("Jay")))
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .response
            .contentAsString
        val posted = objectMapper.readValue<User>(postResponse)


        val response = mockMvc.perform(put("/api/v1/users/${posted.id}")
            .content(objectMapper.writeValueAsString(UserUpdateRequest("Jai")))
            .contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .response
            .contentAsString
        val actual = objectMapper.readValue<User>(response)


        val saved = userRepository.findById(posted.id)
        assertThat(actual.id).isEqualTo(saved.id)
        assertThat(actual.name).isEqualTo(saved.name)
        assertThat(actual.createdAt).isEqualTo(saved.createdAt)
    }

    @Test
    fun `should delete a user`() {
        val postResponse = mockMvc.perform(
            post("/api/v1/users")
                .content(objectMapper.writeValueAsString(UserCreateRequest("Jay")))
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .response
            .contentAsString
        val posted = objectMapper.readValue<User>(postResponse)


        mockMvc.perform(delete("/api/v1/users/${posted.id}"))
            .andReturn()


        val saved = userRepository.findAll()
        assertThat(saved).isEmpty()
    }
}
