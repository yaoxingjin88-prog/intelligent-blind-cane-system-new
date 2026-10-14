package com.ruoyi.service;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.Guardian;
import com.ruoyi.entity.VisuallyImpairedUser;
import com.ruoyi.mapper.GuardianMapper;
import com.ruoyi.mapper.VisuallyImpairedUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 家庭监护圈：持杖用户 + 主监护人 + 子监护人
 */
@Service
@RequiredArgsConstructor
public class GuardianFamilyService {

    private final GuardianMapper guardianMapper;
    private final VisuallyImpairedUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<Guardian> listFamilyMembers(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return guardianMapper.getGuardiansByUserId(userId);
    }

    public boolean isPrimary(Guardian guardian) {
        return guardian != null && guardian.isPrimary() && guardian.getUserId() != null;
    }

    public void requirePrimary(Guardian guardian) {
        if (!isPrimary(guardian)) {
            throw new IllegalArgumentException("仅主监护人可执行此操作");
        }
    }

    /**
     * 小程序首次注册：创建持杖用户占位档案，并把自己设为主监护人
     */
    @Transactional
    public Guardian bootstrapPrimaryFamily(String phone, String name) {
        String username = uniqueUsername("g_" + phone);
        VisuallyImpairedUser elder = new VisuallyImpairedUser();
        elder.setUsername(username);
        elder.setPassword(passwordEncoder.encode(UUID.randomUUID().toString().substring(0, 12)));
        elder.setName(name != null && !name.isBlank() ? name + "的家人" : "待完善持杖人");
        elder.setPhone(phone);
        elder.setIdCard("");
        elder.setAddress("");
        elder.setEmergencyContact(name != null ? name : "主监护人");
        elder.setEmergencyPhone(phone);
        userMapper.insert(elder);

        Guardian guardian = new Guardian();
        guardian.setUserId(elder.getId());
        guardian.setPhone(phone);
        guardian.setName(name != null && !name.isBlank() ? name : "主监护人");
        guardian.setRelation("家属");
        guardian.setRole(Guardian.ROLE_PRIMARY);
        guardianMapper.insert(guardian);
        return guardian;
    }

    /**
     * 主监护人邀请子监护人（手机号已存在则拒绝）
     */
    @Transactional
    public Guardian inviteMember(Guardian primary, String phone, String name, String relation) {
        requirePrimary(primary);
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        String normalizedPhone = phone.trim();
        Guardian existing = guardianMapper.findByPhone(normalizedPhone);
        if (existing != null) {
            throw new IllegalArgumentException("该手机号已注册监护人，无法重复邀请");
        }
        Guardian member = new Guardian();
        member.setUserId(primary.getUserId());
        member.setPhone(normalizedPhone);
        member.setName(name != null && !name.isBlank() ? name.trim() : "子监护人");
        member.setRelation(relation != null && !relation.isBlank() ? relation.trim() : "家属");
        member.setRole(Guardian.ROLE_MEMBER);
        guardianMapper.insert(member);
        return member;
    }

    /**
     * 主监护人移除子监护人（不能移除自己）
     */
    @Transactional
    public void removeMember(Guardian primary, Long memberId) {
        requirePrimary(primary);
        if (memberId == null) {
            throw new IllegalArgumentException("成员ID不能为空");
        }
        if (memberId.equals(primary.getId())) {
            throw new IllegalArgumentException("不能移除主监护人自己");
        }
        Guardian target = guardianMapper.getGuardianById(memberId);
        if (target == null || primary.getUserId() == null
                || !primary.getUserId().equals(target.getUserId())) {
            throw new IllegalArgumentException("成员不存在或不属于当前家庭");
        }
        if (target.isPrimary()) {
            throw new IllegalArgumentException("不能直接移除主监护人，请先转让");
        }
        guardianMapper.delete(memberId);
    }

    public Map<String, Object> toFamilyView(Guardian me) {
        Map<String, Object> view = new HashMap<>();
        if (me == null) {
            return view;
        }
        view.put("me", me);
        view.put("role", me.getRole() != null ? me.getRole() : Guardian.ROLE_MEMBER);
        view.put("primary", me.isPrimary());
        view.put("userId", me.getUserId());
        view.put("members", listFamilyMembers(me.getUserId()));
        return view;
    }

    private String uniqueUsername(String base) {
        String candidate = base;
        int i = 0;
        while (userMapper.findByUsername(candidate) != null) {
            i++;
            candidate = base + "_" + i;
        }
        return candidate;
    }
}
