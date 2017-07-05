package org.svearike.trainingpalweb.tasks;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.svearike.trainingpalweb.HttpConnection;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.api.taskqueue.DeferredTask;

@SuppressWarnings("serial")
public class SaveWeightTask implements DeferredTask
{
	private static final Logger LOG = Logger.getLogger(SaveWeightTask.class.getName());
	private static final float ALLOWED_PERCENTAGE_DELTA = 0.03f;
	private static final int TIMEOUT = 30000;
	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	private String mHome;
	private String mWeight;
	private long mDate;

	public SaveWeightTask(String home, String weight, long date)
	{
		this.mHome = home;
		this.mWeight = weight;
		this.mDate = date;

		LOG.info("Creating task; " + this);
	}

	@Override
	public void run()
	{
		List<Entity> possibleUsers = findUsers();
		if (possibleUsers.isEmpty())
		{
			Entity user = createNewUser();
			possibleUsers.add(user);
			addStats(user, new Entity(Database.getStatsKey(user.getKey().getId(), mDate)), null);
		}
		else if (possibleUsers.size() == 1)
		{
			Entity user = possibleUsers.iterator().next();
			updateUserAndStats(user);
		}

		try {
			notifyFirebase(possibleUsers);
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Could not update the information in firebase", e);
		}
	}

	private void notifyFirebase(List<Entity> possibleUsers) throws IOException
	{
		LOG.info("Notifying firebase");

		Map<String, Object> map = new HashMap<>();
		map.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(mDate)));
		map.put("kg", Float.parseFloat(mWeight));
		Map<Long, Map<String, Object>> mapUsers = new HashMap<>();
		map.put("possibleUsers", mapUsers);
		for(Entity e : possibleUsers) {
			Map<String, Object> userMap = new HashMap<>();
			userMap.put("id", e.getKey().getId());
			userMap.put("name", e.getProperty("name"));
			mapUsers.put(e.getKey().getId(), userMap);
		}

		JSONObject obj = new JSONObject(map);
		System.out.println("obj = "+ obj.toString());
		HttpConnection.connectAndGetString(new URL("https://trainingpal-web.firebaseio.com/homes/" + mHome + "/lastWeight.json"), "PUT", obj.toString(), TIMEOUT, TIMEOUT);
	}

	private void updateUserAndStats(Entity user)
	{
		Transaction tx = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
		try {
			user = datastore.get(user.getKey());
			user.setProperty("lastWeight", Float.parseFloat(mWeight));
			user.setProperty("lastWeightDate", mDate);
			datastore.put(tx, user);

			Entity stats = null;
			try {
				stats = datastore.get(tx, Database.getStatsKey(user.getKey().getId(), mDate));
			} catch(EntityNotFoundException e) {
				stats = new Entity(Database.getStatsKey(user.getKey().getId(), mDate));
			}
			addStats(user, stats, tx);

			tx.commit();
		} catch (EntityNotFoundException e) {
			LOG.log(Level.SEVERE, "Could not find the User entity, wihch is really really weird", e);
		} finally {
			if (tx != null && tx.isActive())
				tx.rollback();
		}
	}

	private Entity createNewUser()
	{
		LOG.info("Creating a new User");
		Entity user = new Entity("user");
		user.setProperty("home", mHome);
		user.setProperty("name", "Anonym");
		user.setProperty("lastWeight", Double.parseDouble(mWeight));
		user.setProperty("lastWeightDate", mDate);
		user.setProperty("newAutoCreate", true);

		datastore.put(user);
		return user;
	}
	
	private Entity addStats(Entity user, Entity stats, Transaction tx)
	{
		stats.setProperty("user", user.getKey().getId());
		if (!stats.hasProperty("firstDate"))
			stats.setProperty("firstDate", mDate);

		stats.setProperty("lastDate", mDate);
		stats.setProperty("home", mHome);

		@SuppressWarnings("unchecked")
		Collection<Double> weight = (Collection<Double>) stats.getProperty("weight");
		if (weight == null)
			weight = new LinkedList<>();

		@SuppressWarnings("unchecked")
		Collection<Long> date = (Collection<Long>) stats.getProperty("date");
		if (date == null)
			date = new LinkedList<>();

		weight.add(Double.parseDouble(mWeight));
		date.add(mDate);

		stats.setProperty("weight", weight);
		stats.setProperty("date", date);

		datastore.put(tx, stats);
		return stats;
	}

	private List<Entity> findUsers()
	{
		LOG.info("Attempting to find user in db; " + this);
		Query query = new Query("user");
		query.setFilter(new Query.FilterPredicate("home", FilterOperator.EQUAL, mHome));
		int numFoundUsers = 0;
		List<Entity> possibleUsers = new LinkedList<>();
		float weightFloat = Float.parseFloat(mWeight);
		for(Entity e : datastore.prepare(query).asIterable())
		{
			numFoundUsers++;
			if (!e.hasProperty("lastWeight"))
				continue;

			float lastWeight = (float) (double) e.getProperty("lastWeight");
			int delta = (int) Math.abs(weightFloat - lastWeight);
			if ((delta / lastWeight) < ALLOWED_PERCENTAGE_DELTA)
			{
				possibleUsers.add(e);
			}
		}
		LOG.info("Found " + numFoundUsers + " users, and a total of " + possibleUsers.size() + " matched with a small enough delta");
		return possibleUsers;
	}

	@Override
	public String toString() {
		return "home=" + mHome + ",weight=" + mWeight+",date=" + mDate;
	}
}
