package com.ruoyi.service;

import com.ruoyi.entity.VisuallyImpairedUser;
import com.ruoyi.mapper.VisuallyImpairedUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VisuallyImpairedUserService {
    @Autowired
    private VisuallyImpairedUserMapper userMapper;

    public List<VisuallyImpairedUser> getAllUsers() {
        return userMapper.getAllUsers();
    }

    public VisuallyImpairedUser getUserById(Long id) {
        return userMapper.getUserById(id);
    }

    public void addUser(VisuallyImpairedUser user) {
        userMapper.insert(user);
    }

    public void deleteUser(Long id) {
        userMapper.delete(id);
    }

    public void updateUser(VisuallyImpairedUser user) {
        userMapper.update(user);
    }

    public VisuallyImpairedUser updateUserById(Long id, VisuallyImpairedUser user) {
        user.setId(id);
        userMapper.update(user);
        return userMapper.getUserById(id);
    }
}
