package com.ruoyi.service;

import com.ruoyi.entity.Guardian;
import com.ruoyi.mapper.GuardianMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuardianService {
    @Autowired
    private GuardianMapper guardianMapper;

    public List<Guardian> getAllGuardians() {
        return guardianMapper.getAllGuardians();
    }

    public Guardian getGuardianById(Long id) {
        return guardianMapper.getGuardianById(id);
    }

    public void addGuardian(Guardian guardian) {
        guardianMapper.insert(guardian);
    }

    public void deleteGuardian(Long id) {
        guardianMapper.delete(id);
    }

    public void updateGuardian(Guardian guardian) {
        guardianMapper.update(guardian);
    }
}
