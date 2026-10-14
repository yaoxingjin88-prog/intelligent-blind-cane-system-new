package com.ruoyi.service;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.Guardian;
import com.ruoyi.mapper.GuardianMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 监护人档案：查询、增删改及按手机号查找（小程序登录用）
 */
@Service
@RequiredArgsConstructor
public class GuardianService {
    private final GuardianMapper guardianMapper;

    /** 查询全部监护人 */
    public List<Guardian> getAllGuardians() {
        return guardianMapper.getAllGuardians();
    }

    /**
     * 按主键查监护人
     *
     * @param id 监护人主键
     * @return 监护人，不存在则 null
     */
    public Guardian getGuardianById(Long id) {
        return guardianMapper.getGuardianById(id);
    }

    /** 新增监护人 */
    public void addGuardian(Guardian guardian) {
        guardianMapper.insert(guardian);
    }

    /** 按主键删除监护人 */
    public void deleteGuardian(Long id) {
        guardianMapper.delete(id);
    }

    /** 更新监护人信息 */
    public void updateGuardian(Guardian guardian) {
        guardianMapper.update(guardian);
    }

    /**
     * 按手机号查找监护人（小程序短信/微信登录绑定）
     *
     * @param phone 手机号
     * @return 监护人，不存在则 null
     */
    public Guardian findByPhone(String phone) {
        return guardianMapper.findByPhone(phone);
    }
}
