package service;

import static org.junit.Assert.*;
import static service.UserService.MIN_LOGCOUT_FOR_SILVER;
import static service.UserService.MIN_RECCOMMEND_FOR_GOLD;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

import code.Level;
import dao.UserDao;
import entity.UserEntity;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/applicationContext.xml")
public class UserServiceTest {
	@Autowired
	UserService userService;
	@Autowired
	UserDao userDao;
	@Autowired
	DataSource dataSource;
	@Autowired
	PlatformTransactionManager transactionManager;
	@Autowired
	MailSender mailSender;

	List<UserEntity> usersFixture;

	@Before
	public void setUp() {
		usersFixture = Arrays.asList(new UserEntity("id_1", "user_1", "pwpw", Level.BASIC, MIN_LOGCOUT_FOR_SILVER - 1, 0, "mailmail@mail.com"), new UserEntity(
				"id_2", "user_2", "pwpw", Level.BASIC, MIN_LOGCOUT_FOR_SILVER, 0, "mailmail@mail.com"), new UserEntity("id_3", "user_3", "pwpw", Level.SILVER, 60,
				MIN_RECCOMMEND_FOR_GOLD - 1, "mailmail@mail.com"), new UserEntity("id_4", "user_4", "pwpw", Level.SILVER, 60, MIN_RECCOMMEND_FOR_GOLD,
				"mailmail@mail.com"), new UserEntity("id_5", "user_5", "pwpw", Level.GOLD, 100, Integer.MAX_VALUE, "mailmail@mail.com"));
	}

	@Test
	public void upgradeUserLevelTest() throws Exception {
		userDao.deleteAll();

		for (UserEntity user : usersFixture)
			userDao.add(user);

		userService.upgradeLevelOfEveryUser();

		checkUserLevel(usersFixture.get(0), false);
		checkUserLevel(usersFixture.get(1), true);
		checkUserLevel(usersFixture.get(2), false);
		checkUserLevel(usersFixture.get(3), true);
		checkUserLevel(usersFixture.get(4), false);
	}

	@Test
	public void levelHandlingUserAddTest() {
		userDao.deleteAll();

		UserEntity userWithLevel = usersFixture.get(4);
		UserEntity userWithoutLevel = usersFixture.get(0);
		userWithoutLevel.setLevel(null);

		userService.add(userWithLevel);
		userService.add(userWithoutLevel);

		UserEntity userWithLevelRetrieved = userDao.get(userWithLevel.getId());
		UserEntity userWithoutLevelRetrieved = userDao.get(userWithoutLevel.getId());

		assertEquals(Level.GOLD, userWithLevelRetrieved.getLevel());
		assertEquals(Level.BASIC, userWithoutLevelRetrieved.getLevel());
	}

	@Test
	public void upgradeAllorNothing() throws Exception {
		UserService testUserService = new TestUserService(usersFixture.get(3).getId());
		testUserService.setUserDao(this.userDao);
		testUserService.setTransactionManager(transactionManager);
		testUserService.setMailSender(mailSender);

		userDao.deleteAll();
		for (UserEntity user : usersFixture)
			userDao.add(user);

		try {
			testUserService.upgradeLevelOfEveryUser();
			fail("TestUserServiceException expected");
		} catch (TestUserServiceException e) {
		}
		checkUserLevel(usersFixture.get(1), false);
	}

	private void checkUserLevel(UserEntity user, boolean upgraded) {
		System.out.print("checking user level of user [" + user.getId() + "]: ");
		UserEntity updatedUser = userDao.get(user.getId());
		if (upgraded) {
			System.out.println("updated.");
			assertEquals(updatedUser.getLevel(), user.getLevel().getNextLevel());
		} else {
			System.out.println("not updated.");
			assertEquals(updatedUser.getLevel(), user.getLevel());
		}
	}

	static class TestUserService extends UserService {
		private String id;

		private TestUserService(String id) {
			this.id = id;
		}

		protected void upgradeLevelOfOneUser(UserEntity user) {
			if (user.getId().equals(this.id)) throw new TestUserServiceException();
			super.upgradeLevelOfOneUser(user);
		}
	}

	static class TestUserServiceException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}