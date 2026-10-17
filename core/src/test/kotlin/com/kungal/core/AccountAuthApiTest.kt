package com.kungal.core

import com.kungal.core.api.AccountApiException
import com.kungal.core.api.AccountAuthApi
import com.kungal.core.oauth.Pkce
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AccountAuthApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AccountAuthApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = AccountAuthApi(
            oauthBaseUrl = server.url("/api/v1").toString().trimEnd('/'),
            forumBaseUrl = server.url("/api").toString().trimEnd('/'),
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun json(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `PKCE challenge 与 RFC 7636 示例一致`() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challengeFor("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
        val codes = Pkce.generate()
        assertTrue(codes.verifier.length in 43..128)
        assertTrue(codes.challenge.isNotBlank())
        assertTrue(codes.state.isNotBlank())
        assertEquals(codes.challenge, Pkce.challengeFor(codes.verifier))
    }

    @Test
    fun `登录与注册接口解析业务错误`() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(401)
                .addHeader("Content-Type", "application/json")
                .body("""{"code":10005,"message":"用户不存在"}""")
                .build(),
        )
        try {
            api.loginAndAuthorize("nobody", "wrong")
            fail("应抛出业务错误")
        } catch (e: AccountApiException) {
            assertEquals(10005, e.code)
            assertEquals("用户不存在", e.message)
        }
    }

    @Test
    fun `注册发送验证码与注册参数`() = runBlocking {
        server.enqueue(json("""{"code":0,"message":"成功"}"""))
        server.enqueue(
            json(
                """{"code":0,"message":"成功",
                    "data":{"access_token":"token-2","user":{"name":"newbie"}}}""",
            ),
        )
        api.sendRegisterCode("newbie", "newbie@example.com")
        val token = api.register("newbie", "newbie@example.com", "secret123", "123456")
        assertEquals("token-2", token)

        val send = server.takeRequest()
        assertEquals("/api/v1/auth/register/send-code", send.url.encodedPath)
        assertTrue(send.body!!.utf8().contains("\"email\":\"newbie@example.com\""))
        val register = server.takeRequest()
        assertEquals("/api/v1/auth/register", register.url.encodedPath)
        val body = register.body!!.utf8()
        assertTrue(body.contains("\"name\":\"newbie\""))
        assertTrue(body.contains("\"code\":\"123456\""))
    }

    @Test
    fun `完整登录链：密码到会话 cookie`() = runBlocking {
        val state = java.util.concurrent.atomic.AtomicReference<String>()
        val challenge = java.util.concurrent.atomic.AtomicReference<String>()
        val verifier = java.util.concurrent.atomic.AtomicReference<String>()

        val local = MockWebServer()
        local.start()
        local.dispatcher = object : mockwebserver3.Dispatcher() {
            override fun dispatch(request: mockwebserver3.RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                return when {
                    path == "/api/v1/auth/login" -> json(
                        """{"code":0,"message":"成功",
                            "data":{"access_token":"token-1","user":{"name":"tester"}}}""",
                    )
                    path == "/api/v1/oauth/authorize/consent" -> {
                        val body = request.body!!.utf8()
                        state.set(Regex("\"state\":\"([^\"]+)\"").find(body)?.groupValues?.get(1))
                        challenge.set(
                            Regex("\"code_challenge\":\"([^\"]+)\"").find(body)?.groupValues?.get(1),
                        )
                        assertEquals("Bearer token-1", request.headers["Authorization"])
                        json(
                            """{"code":0,"message":"成功","data":{"redirect_url":
                                "https://www.kungal.com/auth/callback?code=code-1&state=${state.get()}"}}""",
                        )
                    }
                    path == "/api/auth/oauth/callback" -> {
                        val body = request.body!!.utf8()
                        verifier.set(
                            Regex("\"code_verifier\":\"([^\"]+)\"").find(body)?.groupValues?.get(1),
                        )
                        MockResponse.Builder()
                            .code(200)
                            .addHeader("Content-Type", "application/json")
                            .addHeader(
                                "Set-Cookie",
                                "kungal_session=session-value; max-age=7776000; path=/; " +
                                    "HttpOnly; Secure; SameSite=Lax",
                            )
                            .body("""{"code":0,"message":"成功","data":{"user":{"id":1}}}""")
                            .build()
                    }
                    else -> MockResponse.Builder().code(404).build()
                }
            }
        }
        try {
            val api = AccountAuthApi(
                oauthBaseUrl = local.url("/api/v1").toString().trimEnd('/'),
                forumBaseUrl = local.url("/api").toString().trimEnd('/'),
            )
            val cookie = api.loginAndAuthorize("tester", "secret123")
            assertEquals("kungal_session=session-value", cookie)
            assertTrue(state.get().isNotBlank())
            assertEquals(Pkce.challengeFor(verifier.get()), challenge.get())
        } finally {
            local.close()
        }
    }
}
