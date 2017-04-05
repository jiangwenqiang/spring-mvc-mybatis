package org.elnino.demo.service;

import java.util.List;

import org.elnino.demo.model.User;

public interface UserService {

	List<User> findAll();

	void save(User user);

	boolean update(User user);

	User findByGh(Object id);

	boolean delete(int id);

}
