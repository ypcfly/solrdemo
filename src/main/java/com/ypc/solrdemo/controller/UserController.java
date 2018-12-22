package com.ypc.solrdemo.controller;

import com.google.gson.Gson;
import com.ypc.solrdemo.entity.User;
import com.ypc.solrdemo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @description: use solr client
 * @Author: ypcfly
 * @Date: 18-12-4 下午8:03
 */
@Controller
@RequestMapping("/user")
public class UserController {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

	@Resource
	private UserService userService;

	/**
	 * query user by user id
	 * @param id
	 * @return
	 */
	@ResponseBody
	@GetMapping("/get/{id}")
	public User getUser(@PathVariable("id") int id) {
		// 根据id查询用户
		User user = userService.queryById(id);

		return user;
	}


	/**
	 * query all user
	 * @return
	 */
	@ResponseBody
	@GetMapping("/list")
	public List<User> userList() {
		List<User> list = userService.queryAll();
		return list;
	}

	/**
	 * add new user to database, and update the solr document
	 * @param user
	 * @return
	 */
	@ResponseBody
	@PostMapping("/insert")
	public Map<String, Object> insertUser(@RequestBody User user) {

		Map<String,Object> result = userService.insertAndUpdate(user);
		return result;
	}

	/**
	 * delete user by user id
	 * @param id
	 * @return
	 */
	@ResponseBody
	@PostMapping("/delete")
	public Map<String,Object> deleteUser(@RequestParam("id") int id) {

		return userService.deleteUserById(id);
	}

	/**
	 *  query by user description
	 * @param
	 * @return
	 */
	@ResponseBody
	@PostMapping("/query/condition")
	public String queryByCondition (@RequestBody User user) {
		LOGGER.info(">>>> querty by condition start <<<<");
		List<User> list = userService.queryByCondition(user.getDescription());
		Gson gson = new Gson();
		String json = gson.toJson(list);
		return json;
	}

	/**
	 *  forward to search page
	 * @return
	 */
	@GetMapping("/page")
	public String forward() {
		LOGGER.info(">>>> forward to search page start <<<<");
		return "result";
	}

	/**
	 * sort user list by specified field
	 * @return
	 */
	@ResponseBody
	@PostMapping("/list/sort")
	public List<User> userSortList(String sort,String description) {

		List<User> list = userService.userSortList(sort,description);
		return list;
	}


}
