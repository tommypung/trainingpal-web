package org.svearike.trainingpalweb.tasks;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
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
}
