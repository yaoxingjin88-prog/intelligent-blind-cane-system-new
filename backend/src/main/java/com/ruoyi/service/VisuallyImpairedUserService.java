package com.ruoyi.service;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.VisuallyImpairedUser;
import com.ruoyi.mapper.CaneDeviceMapper;
import com.ruoyi.mapper.VisuallyImpairedUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 视障用户档案业务（含敏感字段脱敏与密码加密）
 */
@Service
@RequiredArgsConstructor
public class VisuallyImpairedUserService {
    private final VisuallyImpairedUserMapper userMapper;
    private final CaneDeviceMapper caneDeviceMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户列表（列表场景隐藏病历）
     *
     * @return 脱敏后的用户列表
     */
    public List<VisuallyImpairedUser> getAllUsers() {
        return userMapper.getAllUsers().stream()
                .map(user -> maskSensitive(user, true))
                .collect(Collectors.toList());
    }

    /**
     * 用户详情（保留病历供管理端编辑；密码永不回传）
     *
     * @param id 用户主键
     * @return 脱敏后的用户
     */
    public VisuallyImpairedUser getUserById(Long id) {
        return maskSensitive(userMapper.getUserById(id), false);
    }

    /**
     * 新增用户
     * 业务步骤：
     * 1. 密码非空则 BCrypt 加密
     * 2. 插入数据库
     *
     * @param user 用户实体
     */
    public void addUser(VisuallyImpairedUser user) {
        // 步骤1：密码加密（不要明文存）
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(encodeIfNeeded(user.getPassword()));
        }
        // 步骤2：落库
        userMapper.insert(user);
    }

    /**
     * 删除用户：已绑定设备时禁止删除，需先解绑
     */
    public void deleteUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (caneDeviceMapper.countByUserId(id) > 0) {
            throw new IllegalArgumentException("该用户已绑定设备，请先解绑后再删除");
        }
        userMapper.delete(id);
    }

    /**
     * 更新用户
     * 业务步骤：
     * 1. 校验用户存在
     * 2. 密码空则保留原哈希，否则重新加密
     * 3. 脱敏占位字段回填真实值（身份证/病历）
     * 4. 更新数据库
     *
     * @param user 待更新实体（需含 id）
     */
    public void updateUser(VisuallyImpairedUser user) {
        // 步骤1：存在性校验
        if (user.getId() == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        VisuallyImpairedUser existing = userMapper.getUserById(user.getId());
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 步骤2：密码处理
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(existing.getPassword());
        } else {
            user.setPassword(encodeIfNeeded(user.getPassword()));
        }

        // 步骤3：前端脱敏占位不覆盖库中真值
        if (user.getIdCard() != null && user.getIdCard().contains("*") && existing.getIdCard() != null) {
            user.setIdCard(existing.getIdCard());
        }
        if ("[已隐藏]".equals(user.getMedicalHistory()) && existing.getMedicalHistory() != null) {
            user.setMedicalHistory(existing.getMedicalHistory());
        }

        // 步骤4：更新
        userMapper.update(user);
    }

    public VisuallyImpairedUser updateUserById(Long id, VisuallyImpairedUser user) {
        user.setId(id);
        updateUser(user);
        return getUserById(id);
    }

    private String encodeIfNeeded(String rawOrHash) {
        if (rawOrHash.startsWith("$2a$") || rawOrHash.startsWith("$2b$") || rawOrHash.startsWith("$2y$")) {
            return rawOrHash;
        }
        return passwordEncoder.encode(rawOrHash);
    }

    private VisuallyImpairedUser maskSensitive(VisuallyImpairedUser user, boolean hideMedical) {
        if (user == null) {
            return null;
        }
        user.setPassword(null);
        user.setIdCard(maskIdCard(user.getIdCard()));
        if (hideMedical && user.getMedicalHistory() != null && !user.getMedicalHistory().isBlank()) {
            user.setMedicalHistory("[已隐藏]");
        }
        return user;
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        int keep = 4;
        String prefix = idCard.substring(0, keep);
        String suffix = idCard.substring(idCard.length() - keep);
        return prefix + "********" + suffix;
    }
}
