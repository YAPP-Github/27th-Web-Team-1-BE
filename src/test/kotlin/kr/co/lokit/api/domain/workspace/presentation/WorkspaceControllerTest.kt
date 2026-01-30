package kr.co.lokit.api.domain.workspace.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.config.security.CompositeAuthenticationResolver
import kr.co.lokit.api.domain.workspace.application.WorkspaceService
import kr.co.lokit.api.fixture.createJoinWorkspaceRequest
import kr.co.lokit.api.fixture.createWorkspace
import kr.co.lokit.api.fixture.createWorkspaceRequest
import kr.co.lokit.api.fixture.userAuth
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(WorkspaceController::class)
class WorkspaceControllerTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T = org.mockito.ArgumentMatchers.any<T>() as T

    @Autowired
    lateinit var mockMvc: MockMvc

    val objectMapper: ObjectMapper = ObjectMapper()

    @MockitoBean
    lateinit var compositeAuthenticationResolver: CompositeAuthenticationResolver

    @MockitoBean
    lateinit var workspaceService: WorkspaceService

    @Test
    fun `워크스페이스 생성 성공`() {
        val savedWorkspace = createWorkspace(id = 1L, name = "우리 가족", inviteCode = "12345678")
        doReturn(savedWorkspace).`when`(workspaceService).create(anyObject(), anyLong())

        mockMvc.perform(
            post("/workspaces")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createWorkspaceRequest(name = "우리 가족"))),
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `워크스페이스 생성 실패 - 이름이 비어있음`() {
        mockMvc.perform(
            post("/workspaces")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createWorkspaceRequest(name = ""))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `초대 코드로 워크스페이스 합류 성공`() {
        val workspace = createWorkspace(id = 1L, name = "팀", inviteCode = "12345678", userIds = listOf(1L, 2L))
        doReturn(workspace).`when`(workspaceService).joinByInviteCode(anyString(), anyLong())

        mockMvc.perform(
            post("/workspaces/join")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createJoinWorkspaceRequest())),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `초대 코드로 워크스페이스 합류 실패 - 잘못된 초대 코드`() {
        doThrow(BusinessException.ResourceNotFoundException("유효하지 않은 초대 코드입니다"))
            .`when`(workspaceService).joinByInviteCode(anyString(), anyLong())

        mockMvc.perform(
            post("/workspaces/join")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createJoinWorkspaceRequest(inviteCode = "invalid1"))),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `초대 코드로 워크스페이스 합류 실패 - 초대 코드가 8자가 아님`() {
        mockMvc.perform(
            post("/workspaces/join")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createJoinWorkspaceRequest(inviteCode = "1234"))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `인증되지 않은 사용자는 접근할 수 없다`() {
        mockMvc.perform(
            post("/workspaces")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createWorkspaceRequest())),
        )
            .andExpect(status().isUnauthorized)
    }
}
