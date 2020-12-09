package org.svearike.trainingpalweb.tasks;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.svearike.trainingpalweb.Database;

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
import com.google.gson.internal.Pair;

public class Datastore implements Database {
  private static final Logger LOG = Logger.getLogger(Datastore.class.getName());
  private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

  public static Key getStatsKey(String cachedKey) {
    return KeyFactory.createKey("weight", cachedKey);
  }

  public static Key getStatsKey(long userId, long date) {
    return KeyFactory.createKey(
        "weight", userId + ";" + new SimpleDateFormat("yyyy-MM").format(date));
  }

  public static Key createUserKey(long id) {
    return KeyFactory.createKey("user", id);
  }

  @Override
  public Entity updateUser(long id, OnLoad onLoad) {
    Transaction tx = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
    try {
      Entity user = datastore.get(createUserKey(id));
      if (onLoad.loadedInATransaction(user)) {
        datastore.put(tx, user);
        tx.commit();
      } else tx.rollback();

      return user;
    } catch (EntityNotFoundException e) {
      LOG.log(Level.SEVERE, "Could not find the User entity, wihch is really really weird", e);
      return null;
    } finally {
      if (tx != null && tx.isActive()) tx.rollback();
    }
  }

  @Override
  public Entity createNewUser(String home, String lastWeight, Long lastWeightDate) {
    LOG.info("Creating a new User");
    Entity user = new Entity("user");
    user.setProperty("home", home);
    user.setProperty("name", "Anonym");
    if (lastWeight != null && lastWeightDate != null) {
      user.setProperty("lastWeight", Double.parseDouble(lastWeight));
      user.setProperty("lastWeightDate", lastWeightDate);
    }
    user.setProperty("newAutoCreate", true);

    datastore.put(user);
    return user;
  }

  @Override
  public List<Entity> getUsers(String home) {
    Query q = new Query("user");
    q.setFilter(new FilterPredicate("home", FilterOperator.EQUAL, home));
    return datastore.prepare(q).asList(FetchOptions.Builder.withDefaults());
  }

  @Override
  public Entity removeWeight(long id, long date) {
    Transaction tx = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
    try {
      Key statsKey = getStatsKey(id, date);
      Key userKey = createUserKey(id);
      Map<Key, Entity> ents = datastore.get(Arrays.asList(statsKey, userKey));
      if (ents.size() != 2) return null;

      Entity stats = ents.get(statsKey);
      Entity user = ents.get(userKey);

      @SuppressWarnings("unchecked")
      List<Double> weights = (List<Double>) stats.getProperty("weight");
      if (weights == null) return user;

      @SuppressWarnings("unchecked")
      List<Long> dates = (List<Long>) stats.getProperty("date");
      if (dates == null) return user;

      for (int i = 0; i < dates.size(); i++)
        if (dates.get(i) == date) {
          dates.remove(i);
          weights.remove(i);
        }

      if (dates.isEmpty()) datastore.delete(tx, statsKey);
      else {
        stats.setProperty("weight", weights);
        stats.setProperty("date", dates);
        stats.setProperty("lastDate", dates.get(dates.size() - 1));

        if ((Long) user.getProperty("lastWeightDate") == date) {
          user.setProperty("lastWeightDate", stats.getProperty("lastDate"));
          user.setProperty("lastWeight", weights.get(weights.size() - 1));
        }

        datastore.put(tx, Arrays.asList(user, stats));
      }

      tx.commit();
      return user;
    } finally {
      if (tx != null && tx.isActive()) tx.rollback();
    }
  }

  @Override
  public Entity getUser(long toUser) throws EntityNotFoundException {
    return datastore.get(KeyFactory.createKey("user", toUser));
  }

  private Pair<List<Double>, List<Long>> sortWeightDate(List<Double> weight, List<Long> date) {
    List<Pair<Double, Long>> list = new LinkedList<>();
    for (int i = 0; i < weight.size(); i++)
      list.add(new Pair<Double, Long>(weight.get(i), date.get(i)));
    Collections.sort(
        list,
        new Comparator<Pair<Double, Long>>() {
          @Override
          public int compare(Pair<Double, Long> o1, Pair<Double, Long> o2) {
            return o1.second.compareTo(o2.second);
          }
        });

    List<Double> dList = new ArrayList<>(list.size());
    List<Long> lList = new ArrayList<>(list.size());
    for (Pair<Double, Long> p : list) {
      dList.add(p.first);
      lList.add(p.second);
    }

    return new Pair<>(dList, lList);
  }

  @Override
  public Entity addStats(Entity user, Long date, Double weight, Transaction tx) {
    Entity stats = null;
    try {
      stats = datastore.get(tx, Datastore.getStatsKey(user.getKey().getId(), date));
    } catch (EntityNotFoundException e) {
      stats = new Entity(Datastore.getStatsKey(user.getKey().getId(), date));
    }

    stats.setProperty("user", user.getKey().getId());
    if (!stats.hasProperty("firstDate")) stats.setProperty("firstDate", date);

    stats.setProperty("lastDate", date);
    stats.setProperty("home", user.getProperty("home"));

    @SuppressWarnings("unchecked")
    List<Double> weights = (List<Double>) stats.getProperty("weight");
    if (weights == null) weights = new LinkedList<>();

    @SuppressWarnings("unchecked")
    List<Long> dates = (List<Long>) stats.getProperty("date");
    if (dates == null) dates = new LinkedList<>();

    weights.add(weight);
    dates.add(date);

    Pair<List<Double>, List<Long>> sorted = sortWeightDate(weights, dates);

    weights = sorted.first;
    dates = sorted.second;
    stats.setProperty("weight", weights);
    stats.setProperty("date", dates);

    datastore.put(tx, stats);
    return stats;
  }

  @Override
  public List<Entity> getStats(long userId, int numMonths) {
    List<Entity> ents = new LinkedList<>();
    GregorianCalendar cal = new GregorianCalendar();
    List<Key> keys = new LinkedList<>();
    for (int i = 0; i <= numMonths; i++) {
      keys.add(Datastore.getStatsKey(userId, cal.getTime().getTime()));
      cal.add(GregorianCalendar.MONTH, -1);
    }
    Map<Key, Entity> entMap = datastore.get(keys);
    for (Key key : keys) {
      Entity e = entMap.get(key);
      if (e == null) continue;
      ents.add(e);
    }

    return ents;
  }

  @Override
  public Map<Key, Entity> getStats(long userId, Collection<Key> keys) {
	  return datastore.get(keys);
  }
}
