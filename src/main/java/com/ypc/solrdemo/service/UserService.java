package com.ypc.solrdemo.service;

import com.ypc.solrdemo.entity.User;

import java.util.List;
import java.util.Map;

public interface UserService {

	/**
	 * 根据id查询用户
	 * @param id
	 * @return
	 */
	User queryById(int id);

	/**
	 * 查询所有用户
	 * @return
	 */
	List<User> queryAll();

	/**
	 * 添加新的用户
	 * @param user
	 * @return
	 */
	Map<String, Object> insertAndUpdate(User user);

	/**
	 * 根据id删除用户
	 * @param id
	 * @return
	 */
	Map<String, Object> deleteUserById(int id);

	/**
	 * 根据用户描述查询
	 * @param description
	 * @return
	 */
	List<User> queryByCondition(String description);

	/**
	 * 正向或是逆向排序
	 * @param sort
	 * @param des
	 * @return
	 */
	List<User> userSortList(String sort, String des);
}
