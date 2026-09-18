package com.eduagent.learning.common;

import com.eduagent.common.constant.ServiceConstants;
import com.eduagent.common.result.ApiException;
import com.eduagent.common.security.AuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 角色守卫：身份解析 + 角色校验（网关只做认证，授权在本服务）。 */
class RoleGuardTest {

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void currentUserId_parsesLong() {
        AuthContext.set("1001", "ROLE_STUDENT");
        assertEquals(1001L, RoleGuard.currentUserId());
    }

    @Test
    void currentUserId_unauthorizedWhenMissing() {
        assertThrows(ApiException.class, RoleGuard::currentUserId);
    }

    @Test
    void currentUserId_rejectsGarbage() {
        AuthContext.set("abc", "ROLE_STUDENT");
        ApiException e = assertThrows(ApiException.class, RoleGuard::currentUserId);
        assertEquals(401, e.getCode());
    }

    @Test
    void requireTeacherOrAdmin_acceptsTeacher() {
        AuthContext.set("9", "ROLE_TEACHER");
        assertDoesNotThrow(RoleGuard::requireTeacherOrAdmin);
    }

    @Test
    void requireTeacherOrAdmin_acceptsCommaSeparatedRoles() {
        AuthContext.set("9", "ROLE_STUDENT, ROLE_ADMIN");
        assertDoesNotThrow(RoleGuard::requireTeacherOrAdmin);
    }

    @Test
    void requireTeacherOrAdmin_rejectsStudent() {
        AuthContext.set("1001", ServiceConstants.ROLE_STUDENT);
        ApiException e = assertThrows(ApiException.class, RoleGuard::requireTeacherOrAdmin);
        assertEquals(403, e.getCode());
    }

    @Test
    void currentUserIdOrNull_returnsNullInsteadOfThrowing() {
        assertEquals(null, RoleGuard.currentUserIdOrNull());
    }
}
