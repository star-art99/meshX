package io.meshcore.api

import io.meshcore.api.routes.configureRoutes
import io.meshcore.api.server.configureMeshApiPlugins
import io.meshcore.core.config.MeshConfig
import io.meshcore.storage.db.SqliteDatabase
import io.meshcore.storage.repository.PeerRecord
import io.meshcore.storage.repository.PeerRepository
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiTests {

    private lateinit var db: SqliteDatabase
    private lateinit var peerRepo: PeerRepository
    private val config = MeshConfig.load(null).copy(nodeId = "test-node-api")

    @BeforeEach
    fun setUp() {
        val tmpDir = Files.createTempDirectory("meshcore-api-test")
        db = SqliteDatabase(tmpDir.resolve("test.db"))
        db.open()
        peerRepo = PeerRepository(db)
    }

    @AfterEach
    fun tearDown() {
        db.close()
    }

    @Test
    fun `GET health returns 200`() = testApplication {
        application {
            configureMeshApiPlugins()
            configureRoutes(config, peerRepo)
        }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("ok"))
    }

    @Test
    fun `GET node info returns nodeId`() = testApplication {
        application {
            configureMeshApiPlugins()
            configureRoutes(config, peerRepo)
        }
        val response = client.get("/api/v1/node")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("test-node-api"))
    }

    @Test
    fun `GET peers returns empty list initially`() = testApplication {
        application {
            configureMeshApiPlugins()
            configureRoutes(config, peerRepo)
        }
        val response = client.get("/api/v1/peers")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[ ]", response.bodyAsText().trim())
    }

    @Test
    fun `GET peers returns saved peers`() = testApplication {
        application {
            configureMeshApiPlugins()
            configureRoutes(config, peerRepo)
        }
        peerRepo.save(PeerRecord("p1", "pubkey123", "Alice", null, null, false, Instant.now()))
        val response = client.get("/api/v1/peers")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("p1"))
    }

    @Test
    fun `POST send message returns 202`() = testApplication {
        application {
            configureMeshApiPlugins()
            configureRoutes(config, peerRepo)
        }
        val response = client.post("/api/v1/messages/send") {
            contentType(ContentType.Application.Json)
            setBody("""{"toPeerId":"peer1","content":"Hello!"}""")
        }
        assertEquals(HttpStatusCode.Accepted, response.status)
        assertTrue(response.bodyAsText().contains("queued"))
    }

    @Test
    fun `POST send message with blank content returns 400`() = testApplication {
        application {
            configureMeshApiPlugins()
            configureRoutes(config, peerRepo)
        }
        val response = client.post("/api/v1/messages/send") {
            contentType(ContentType.Application.Json)
            setBody("""{"toPeerId":"peer1","content":""}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET unknown peer returns 404`() = testApplication {
        application {
            configureMeshApiPlugins()
            configureRoutes(config, peerRepo)
        }
        val response = client.get("/api/v1/peers/unknown-peer")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
