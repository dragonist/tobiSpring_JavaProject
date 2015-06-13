package dao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import core.ConnectionMaker;
import core.DConnectionMaker;

@Configuration
public class DaoFactory {
	@Bean
	public UserDao userDao() {
		return new UserDao(connectionMaker());
	}

	@Bean
	public ConnectionMaker connectionMaker() {
		return new DConnectionMaker();
	}
}