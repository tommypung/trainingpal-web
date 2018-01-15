package org.svearike.trainingpalweb.tasks;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;

public class Database
{
	private static final Logger LOG = Logger.getLogger(Database.class.getName());
	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	public interface OnLoad
	{
		boolean loadedInATransaction(Entity e) throws InvalidParameterException;
	}


	public static Key getStatsKey(long userId, long date)
	{
		return KeyFactory.createKey("weight", userId + ";" + new SimpleDateFormat("yyyy-MM").format(date));
	}

	public static Key createUserKey(long id) {
		return KeyFactory.createKey("user", id);
	}

	public static Entity updateUser(long id, OnLoad onLoad)
	{
		Transaction tx = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
		try {
			Entity user = datastore.get(createUserKey(id));
			if (onLoad.loadedInATransaction(user)) {
				datastore.put(tx, user);
				tx.commit();
			}
			else
				tx.rollback();

			return user;
		} catch (EntityNotFoundException e) {
			LOG.log(Level.SEVERE, "Could not find the User entity, wihch is really really weird", e);
			return null;
		} finally {
			if (tx != null && tx.isActive())
				tx.rollback();
		}
	}

	public static Entity createNewUser(String home, String lastWeight, Long lastWeightDate)
	{
		LOG.info("Creating a new User");
		Entity user = new Entity("user");
		user.setProperty("home", home);
		user.setProperty("name", "Anonym");
		if (lastWeight != null && lastWeightDate != null)
		{
			user.setProperty("lastWeight", Double.parseDouble(lastWeight));
			user.setProperty("lastWeightDate", lastWeightDate);
		}
		user.setProperty("newAutoCreate", true);

		datastore.put(user);
		return user;
	}

	public static List<Entity> getUsers(String home)
	{
		Query q = new Query("user");
		q.setFilter(new FilterPredicate("home", FilterOperator.EQUAL, home));
		return datastore.prepare(q).asList(FetchOptions.Builder.withDefaults());
	}

	public static void removeWeight(long id, long date)
	{
		Transaction tx = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
		try {
			Key statsKey = getStatsKey(id, date);
			Key userKey = createUserKey(id);
			Map<Key, Entity> ents = datastore.get(Arrays.asList(statsKey, userKey));
			if (ents.size() != 2)
				return;

			Entity stats = ents.get(statsKey);
			Entity user = ents.get(userKey);

			@SuppressWarnings("unchecked")
			List<Double> weights = (List<Double>) stats.getProperty("weight");
			if (weights == null)
				return;

			@SuppressWarnings("unchecked")
			List<Long> dates = (List<Long>) stats.getProperty("date");
			if (dates == null)
				return;

			for(int i=0;i<dates.size();i++)
				if (dates.get(i) == date)
				{
					dates.remove(i);
					weights.remove(i);
				}

			if (dates.isEmpty())
				datastore.delete(tx, statsKey);
			else
			{
				stats.setProperty("weight", weights);
				stats.setProperty("date", dates);
				stats.setProperty("lastDate", dates.get(dates.size() - 1));

				if ((Long) user.getProperty("lastWeightDate") == date)
				{
					user.setProperty("lastWeightDate", stats.getProperty("lastDate"));
					user.setProperty("lastWeight", weights.get(weights.size() - 1));
				}

				datastore.put(tx, Arrays.asList(user, stats));
			}

			tx.commit();
		} finally {
			if (tx != null && tx.isActive())
				tx.rollback();
		}
	}

	public static Entity getUser(long toUser) throws EntityNotFoundException
	{
		return datastore.get(KeyFactory.createKey("user", toUser));
	}
}
