package com.redhat.hotrodclient.serviceImpl;

import java.util.List;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.redhat.hotrodclient.model.User;
import com.redhat.hotrodclient.services.UserService;

@Service
public class UserServiceImpl implements UserService {

	private RemoteCacheManager remoteCacheManager;

	@Autowired
	public UserServiceImpl(RemoteCacheManager remoteCacheManager) {
		this.remoteCacheManager = remoteCacheManager;
	}

	@Override
	public void createUser(User user) {
		remoteCacheManager.getCache(RemoteCacheManager.DEFAULT_CACHE_NAME).put(user.getUserName(), user);
	}

	@Override
	public User getUser(String userName) {
		return (User) remoteCacheManager.getCache(RemoteCacheManager.DEFAULT_CACHE_NAME).get(userName);
	}

	@Override
	public List<User> getUsersMatch(String userName)  {

	    QueryFactory qf = Search.getQueryFactory(remoteCacheManager.getCache());
	    Query query = qf.from(User.class)
	            .having("userName").like("%"+userName+"%").build();
	      
	    List<User> results = query.list();
	    System.out.println("Found " + results.size() + " matches:");
	    for (User p : results) {
	       System.out.println(">> " + p);
	    }
	    return results;  
	}

}
