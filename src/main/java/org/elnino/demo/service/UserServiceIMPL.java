package org.elnino.demo.service;

import java.util.List;

import org.elnino.demo.dao.BasicComponent;
import org.elnino.demo.model.User;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

@Service
public class UserServiceIMPL extends BasicComponent implements UserService{

	@SuppressWarnings("unchecked")
	public List<User> findAll() {
		StringBuffer total = new StringBuffer();
		List<User> list = null;
		try {
			JSONObject jsonparam = new JSONObject();
			list = (List<User>) select(jsonparam, User.class, total);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public void save(User user) {
		
	}

	public boolean update(User user) {
		// TODO Auto-generated method stub
		return false;
	}

	public User findByGh(Object id) {
		User user = null;
		try {
			JSONObject jsonparam = new JSONObject();
			jsonparam.put("Gh", id.toString());
			user = selectOne(jsonparam, User.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return user;
	}

	public boolean delete(int id) {
		// TODO Auto-generated method stub
		return false;
	}

}
