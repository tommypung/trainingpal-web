package org.svearike.trainingpalweb.tasks;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.svearike.trainingpalweb.HttpConnection;
import org.svearike.trainingpalweb.UserSerializer;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.gson.internal.Pair;

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
	private Entity mOverrideUser;

	public SaveWeightTask(String home, String weight, long date, Entity overrideUser)
	{
		this.mHome = home;
		this.mWeight = weight;
		this.mDate = date;
		this.mOverrideUser = overrideUser;

		LOG.info("Creating task; " + this);
	}

	@Override
	public void run()
	{
		List<Entity> possibleUsers = findUsers();
		if (possibleUsers.isEmpty())
		{
			Entity user = Database.createNewUser(mHome, mWeight, mDate);
			possibleUsers.add(user);
			addStats(user, new Entity(Database.getStatsKey(user.getKey().getId(), mDate)), null);
		}
		else if (possibleUsers.size() == 1)
		{
			Entity user = possibleUsers.iterator().next();
			user = updateUserAndStats(user);
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
		Map<Long, JSONObject> mapUsers = new HashMap<>();
		map.put("possibleUsers", mapUsers);
		for(Entity e : possibleUsers) {
			mapUsers.put(e.getKey().getId(), new UserSerializer().get(e));
		}

		JSONObject obj = new JSONObject(map);
		System.out.println("obj = "+ obj.toString());
		HttpConnection.connectAndGetString(new URL("https://trainingpal-web.firebaseio.com/homes/" + mHome + "/lastWeight.json"), "PUT", obj.toString(), TIMEOUT, TIMEOUT);
	}

	private Entity updateUserAndStats(Entity user)
	{
		Transaction tx = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
		try {
			user = datastore.get(user.getKey());
			if (!user.hasProperty("lastWeightDate")
					|| (((Long) user.getProperty("lastWeightDate")) < mDate))
			{
				user.setProperty("lastWeight", Float.parseFloat(mWeight));
				user.setProperty("lastWeightDate", mDate);
			}
			datastore.put(tx, user);

			Entity stats = null;
			try {
				stats = datastore.get(tx, Database.getStatsKey(user.getKey().getId(), mDate));
			} catch(EntityNotFoundException e) {
				stats = new Entity(Database.getStatsKey(user.getKey().getId(), mDate));
			}
			addStats(user, stats, tx);

			tx.commit();
			return user;
		} catch (EntityNotFoundException e) {
			LOG.log(Level.SEVERE, "Could not find the User entity, wihch is really really weird", e);
		} finally {
			if (tx != null && tx.isActive())
				tx.rollback();
		}
		return null;
	}

	private Entity addStats(Entity user, Entity stats, Transaction tx)
	{
		stats.setProperty("user", user.getKey().getId());
		if (!stats.hasProperty("firstDate"))
			stats.setProperty("firstDate", mDate);

		stats.setProperty("lastDate", mDate);
		stats.setProperty("home", mHome);

		@SuppressWarnings("unchecked")
		List<Double> weight = (List<Double>) stats.getProperty("weight");
		if (weight == null)
			weight = new LinkedList<>();

		@SuppressWarnings("unchecked")
		List<Long> date = (List<Long>) stats.getProperty("date");
		if (date == null)
			date = new LinkedList<>();

		weight.add(Double.parseDouble(mWeight));
		date.add(mDate);

		Pair<List<Double>, List<Long>> sorted = sortWeightDate(weight, date);

		weight = sorted.first;
		date = sorted.second;
		stats.setProperty("weight", weight);
		stats.setProperty("date", date);

		datastore.put(tx, stats);
		return stats;
	}

	private Pair<List<Double>, List<Long>> sortWeightDate(List<Double> weight, List<Long> date)
	{
		List<Pair<Double, Long>> list = new LinkedList<>();
		for(int i=0;i<weight.size();i++)
			list.add(new Pair<Double, Long>(weight.get(i), date.get(i)));
		Collections.sort(list, new Comparator<Pair<Double, Long>>() {
			@Override public int compare(Pair<Double, Long> o1, Pair<Double, Long> o2) {
				return o1.second.compareTo(o2.second);
			}
		});

		List<Double> dList = new ArrayList<>(list.size());
		List<Long> lList = new ArrayList<>(list.size());
		for(Pair<Double, Long> p : list) {
			dList.add(p.first);
			lList.add(p.second);
		}

		return new Pair<>(dList, lList);
	}

	private List<Entity> findUsers()
	{
		if (mOverrideUser != null)
			return Arrays.asList(mOverrideUser);

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
