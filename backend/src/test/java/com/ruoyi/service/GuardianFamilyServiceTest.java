package com.ruoyi.service;

import com.ruoyi.entity.Guardian;
import com.ruoyi.mapper.GuardianMapper;
import com.ruoyi.mapper.VisuallyImpairedUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 家庭监护圈：主监护人邀请与权限校验
 */
@ExtendWith(MockitoExtension.class)
class GuardianFamilyServiceTest {

    @Mock
    private GuardianMapper guardianMapper;
    @Mock
    private VisuallyImpairedUserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private GuardianFamilyService guardianFamilyService;

    @Test
    void memberCannotInvite() {
        Guardian member = new Guardian();
        member.setId(2L);
        member.setUserId(1L);
        member.setRole(Guardian.ROLE_MEMBER);
        assertThrows(IllegalArgumentException.class,
                () -> guardianFamilyService.inviteMember(member, "13900001111", "张三", "子女"));
    }

    @Test
    void primaryCanInviteMember() {
        Guardian primary = new Guardian();
        primary.setId(1L);
        primary.setUserId(10L);
        primary.setRole(Guardian.ROLE_PRIMARY);
        when(guardianMapper.findByPhone("13900001111")).thenReturn(null);

        Guardian created = guardianFamilyService.inviteMember(primary, "13900001111", "李四", "配偶");
        assertEquals(Guardian.ROLE_MEMBER, created.getRole());
        assertEquals(10L, created.getUserId());
        verify(guardianMapper).insert(any(Guardian.class));
    }
}
