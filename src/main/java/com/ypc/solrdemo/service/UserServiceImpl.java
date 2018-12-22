package com.ypc.solrdemo.service;

import com.fasterxml.jackson.databind.util.JSONWrappedObject;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.ypc.solrdemo.dao.UserMapper;
import com.ypc.solrdemo.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

/**
 * @description:
 * @Author: ypcfly
 * @Date: 18-12-4 下午8:22
 */
@Service
public class UserServiceImpl implements UserService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

	@Resource
	private UserMapper userMapper;

	@Resource
	private SolrClient solrClient;

//	@Override
//	public User queryById(int id) {
//		// 先通过solr查询，查询不到查数据库
//		SolrQuery query = new SolrQuery();
//		query.setQuery("id:" + id);
//		User user = null;
//		try {
//			QueryResponse response = solrClient.query(query);
//			SolrDocumentList documentList = response.getResults();
//			if (!documentList.isEmpty()) {
//				for (SolrDocument document:documentList) {
//					user = new User();
//					user.setId(id);
//					user.setAddress((String) document.get("address"));
//					user.setMobile((String) document.get("mobile"));
//					user.setUserName((String) document.get("userName"));
//					user.setAge((Integer) document.get("age"));
//					user.setDescription((String) document.get("description"));
//					LOGGER.info(">>>> query user from solr success <<<<");
//				}
//			} else {
//				// 从数据库查询
//				user = userMapper.queryById(id);
//				if (user != null) {
//					solrClient.addBean(user,1000);
//				}
//				LOGGER.info(">>>> query user from database <<<<");
//			}
//
//		} catch (SolrServerException e) {
//			LOGGER.error(e.getMessage(),e);
//		} catch (IOException e) {
//			LOGGER.error(e.getMessage(),e);
//		}
//
//		return user;
//	}


	@Override
	public User queryById(int id) {
		User user = null;
		try {
			SolrDocument solrDocument = solrClient.getById(String.valueOf(id));
			Gson gson = new Gson();
			String solrString = gson.toJson(solrDocument);
			user = gson.fromJson(solrString,User.class);

			Map<String,Object> map = solrDocument.getFieldValueMap();
			user = gson.fromJson(map.toString(),User.class);

			if (null == user) {
				user = userMapper.queryById(id);
				solrClient.addBean(user,1000);
			}

		} catch (SolrServerException e) {
			LOGGER.error(e.getMessage(),e);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);
		}
		return user;
	}

	@Override
	public List<User> queryAll() {
		List<User> list = null;
		SolrQuery query = new SolrQuery();
		query.setQuery("*:*");
		query.setStart(0);
		query.setRows(20);
		try {
			QueryResponse response = solrClient.query(query);
			SolrDocumentList documentList = response.getResults();
			if (!documentList.isEmpty()) {
//				list = new ArrayList<>();
				Gson gson = new Gson();
				String listString = gson.toJson(documentList);
				list = gson.fromJson(listString, new TypeToken<List<User>>() {}.getType());

//				for (SolrDocument document:documentList) {
//					User user = gson.fromJson(document.getFieldValueMap().toString(),User.class);
//					list.add(user);
//				}
//				list = convertToModel(documentList);
				LOGGER.info(">>>> query user from solr success <<<<");
			} else {
				list = userMapper.queryAll();
				solrClient.addBeans(list);
				LOGGER.info(">>>> query user from database <<<<");
			}

		} catch (SolrServerException e) {
			LOGGER.error(e.getMessage(),e);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);
		}
		return list;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> insertAndUpdate(User user) {
		Map<String,Object> result = new HashMap<>();
		result.put("success",false);

		// 返回结果表示受影响的数据条数，而不是id值
		int insert = userMapper.insertUser(user);
		if (insert != 1) {
			throw new RuntimeException(" >>>> insert user to database failed,the return value should be 1,but result is:" + insert + " <<<<");
		}

		// 插入或者更新solr数据
//		SolrInputDocument document = new SolrInputDocument();
//		document.setField("id",user.getId());
//		document.setField("userName",user.getUserName());
//		document.setField("age",user.getAge());
//		document.setField("sex",user.getSex());
//		document.setField("mobile",user.getMobile());
//		document.setField("description",user.getDescription());
		try {
			UpdateResponse response = solrClient.addBean(user,1000);
			// 更新到solr，并在1秒内提交事务
//			UpdateResponse response = solrClient.add(document,1000);
			int staus = response.getStatus();
			if (staus != 0) {
				LOGGER.error(">>>> update solr document failed <<<<");
				solrClient.rollback();
				result.put("message","insert user to solr failed");
				return result;
			}
		} catch (SolrServerException e) {
			LOGGER.error(e.getMessage(),e);
			result.put("message",e.getMessage());
			return result;
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);
			result.put("message",e.getMessage());
			return result;
		}

		result.put("message","insert user to solr success");
		result.put("success",true);
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> deleteUserById(int id) {
		Map<String,Object> result = new HashMap<>();
		result.put("success",false);
		// 先删除数据库，再更新solr
		int delete = userMapper.deleteById(id);
		if (delete != 1) {
			throw new RuntimeException(">>>> delete user failed ,user id=" + id + " <<<<");
		}

		try {
			UpdateResponse response = solrClient.deleteById(String.valueOf(id),1000);
			int status = response.getStatus();
			if (status != 0) {
				LOGGER.error(">>>> delete user from solr failed ,user id=" + id + " <<<<");
				solrClient.rollback();
				result.put("message","delete user to solr failed");
				return result;
			}
		} catch (SolrServerException e) {
			LOGGER.error(e.getMessage(),e);
			result.put("message",e.getMessage());
			return result;
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);
			result.put("message",e.getMessage());
			return result;
		}

		result.put("success",true);
		result.put("message","delete user success");
		return result;
	}

	@Override
	public List<User> queryByCondition(String de) {
		List<User> list = null;
		// 关键字模糊查询
		SolrQuery query = new SolrQuery();
		String nameLike = "userName:*" + de +  "*";
		String desLike = " OR description:*" + de+  "*";
		String sexLike = " OR sex:*" + de +  "*";
		String addLike = " OR address:*" + de +  "*";
		query.set("q",nameLike + desLike + sexLike + addLike);

		query.setStart(0);
		query.setRows(20);
		try {
			QueryResponse response = solrClient.query(query);
			SolrDocumentList documentList = response.getResults();
			if (!documentList.isEmpty()) {
				Gson gson = new Gson();
				String listString = gson.toJson(documentList);
				list = gson.fromJson(listString, new TypeToken<List<User>>() {}.getType());
			} else {
				LOGGER.info(">>>> no result returned by the filter query word: " + de + " <<<<");
			}

		} catch (SolrServerException e) {
			LOGGER.error(e.getMessage(),e);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);
		}
		return list;
	}

	@Override
	public List<User> userSortList(String sort, String des) {
		List<User> list = null;
		// 根据年龄排序
		SolrQuery query = new SolrQuery();
		if (StringUtils.equals(sort,"asc"))
			query.setSort("age ", SolrQuery.ORDER.asc);
		else if (StringUtils.equals(sort,"desc"))
			query.setSort("age ", SolrQuery.ORDER.desc);
		else
			return null;
		String nameLike = "userName:*" + des +  "*";
		String desLike = " OR description:*" + des +  "*";
		String sexLike = " OR sex:*" + des +  "*";
		String addLike = " OR address:*" + des +  "*";
		query.set("q",nameLike + desLike + sexLike + addLike);
		query.setStart(0);
		query.setRows(20);
		try {
			QueryResponse response = solrClient.query(query);
			SolrDocumentList documentList = response.getResults();
			if (!documentList.isEmpty()) {
				list = convertToModel(documentList);
			} else {
				LOGGER.info(">>>> no result returned <<<<");
			}

		} catch (SolrServerException e) {
			LOGGER.error(e.getMessage(),e);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);
		}
		return list;
	}

	private List<User> convertToModel(SolrDocumentList documentList) {
		if (documentList.isEmpty())
			return null;
		List<User> list = new ArrayList<>();
		for (SolrDocument document:documentList) {
			User user = new User();
			user = new User();
			user.setId(Integer.parseInt(String.valueOf(document.get("id"))));
			user.setAddress((String) document.get("address"));
			user.setMobile((String) document.get("mobile"));
			user.setUserName((String) document.get("userName"));
			user.setSex((String) document.get("sex"));
			user.setAge((Integer) document.get("age"));
			user.setDescription((String) document.get("description"));

			list.add(user);
		}
		return list;
	}
}
