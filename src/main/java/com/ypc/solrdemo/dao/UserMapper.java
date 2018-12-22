package com.ypc.solrdemo.dao;

import com.ypc.solrdemo.entity.User;

import java.util.List;

public interface UserMapper {

	User queryById(int id);

	List<User> queryAll();

	int insertUser(User user);

	int deleteById(int id);
}
