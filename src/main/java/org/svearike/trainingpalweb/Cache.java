package org.svearike.trainingpalweb;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.svearike.trainingpalweb.tasks.Datastore;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class Cache implements Database {
  private final Database impl;
  private MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
  private static final Logger LOG = Logger.getLogger(Cache.class.getName());

  public Cache(Database impl) {
    this.impl = impl;
  }

  @Override
  public Entity updateUser(long id, OnLoad onLoad) {
    Entity n = impl.updateUser(id, onLoad);
    memcache.put("user." + id, n);
    memcache.delete("home." + n.getProperty("home"));
    return n;
  }

  @Override
  public Entity createNewUser(String home, String lastWeight, Long lastWeightDate) {
    Entity n = impl.createNewUser(home, lastWeight, lastWeightDate);
    memcache.put("user." + n.getKey().getId(), n);
    memcache.delete("home." + home);
    return n;
  }

  @Override
  public List<Entity> getUsers(String home) {
    @SuppressWarnings("unchecked")
    List<Entity> users = (List<Entity>) memcache.get("home." + home);
    if (users != null) {
      LOG.info("Got all the users from the cache");
      return users;
    }

    LOG.info("Users could not be fetched from the cache");
    users = impl.getUsers(home);
    memcache.put("home." + home, users);
    return users;
  }

  @Override
  public Entity removeWeight(long id, long date) {
    Entity e = impl.removeWeight(id, date);
    memcache.delete("user." + id);
    memcache.delete("home." + e.getProperty("home"));
    return e;
  }

  @Override
  public Entity getUser(long id) throws EntityNotFoundException {
    Entity e = (Entity) memcache.get("user." + id);
    if (e != null) {
      LOG.info("Got the user from the cache");
      return e;
    }

    LOG.info("Could not get the user from the cache");
    e = impl.getUser(id);
    memcache.put("user." + id, e);
    return e;
  }

  @Override
  public Entity addStats(Entity user, Long date, Double weight, Transaction tx) {
    Entity stats = impl.addStats(user, date, weight, tx);
    memcache.put(stats.getKey().getName(), stats);
    return stats;
  }

  private Entity storeStubToPreventCacheMiss(Key key, long userId) {
    Entity e = new Entity(key);

    e.setProperty("user", userId);
    e.setProperty("weight", new LinkedList<Double>());
    e.setProperty("date", new LinkedList<Long>());
    memcache.put(key.getName(), e);
    return e;
  }

  public List<Entity> getStats(long userId, int numMonths) {
    LOG.info("Requesting " + numMonths + " stats objects from the cache");
    GregorianCalendar cal = new GregorianCalendar();
    List<Key> keys = new LinkedList<>();
    for (int i = 0; i <= numMonths; i++) {
      keys.add(getStatsKey(userId, cal.getTime().getTime()));
      cal.add(GregorianCalendar.MONTH, -1);
    }

    Map<Key, Entity> entMap = getStats(userId, keys);
    List<Entity> ents = new LinkedList<>(entMap.values());

    Collections.sort(
        ents,
        new Comparator<Entity>() {
          @Override
          public int compare(Entity o1, Entity o2) {
            return o1.getKey().getName().compareTo(o2.getKey().getName());
          }
        });

    return ents;
  }

  private List<String> fromKey(Collection<Key> keys) {
    List<String> k = new LinkedList<>();
    for (Key key : keys) k.add(key.getName());
    return k;
  }

  @Override
  public Map<Key, Entity> getStats(long userId, Collection<Key> keys) {
    LOG.info("Requesting " + keys.size() + " stats objects from the cache");
    keys = new LinkedList<>(keys);
    Map<Key, Entity> ents = new HashMap<>();

    Map<String, Object> cache = memcache.getAll(fromKey(keys));
    LOG.info("Got " + cache.size() + " objects from the cache");
    for (Entry<String, Object> e : cache.entrySet())
      ents.put(Datastore.getStatsKey(e.getKey()), (Entity) e.getValue());

    for (String keyStr : cache.keySet()) keys.remove(Datastore.getStatsKey(keyStr));

    if (!keys.isEmpty()) {
      Map<Key, Entity> stats = impl.getStats(userId, keys);
      LOG.info("Got " + stats.size() + " objects from the database");
      storeStatsInMemcache(stats);
      for (Entry<Key, Entity> e : stats.entrySet()) ents.put(e.getKey(), e.getValue());
      for (Key k : stats.keySet()) keys.remove(k);
      if (!keys.isEmpty()) {
        LOG.info("Storing " + keys.size() + " stubs in memcache");
        for (Key key : keys) {
          Entity stub = storeStubToPreventCacheMiss(key, userId);
          ents.put(key, stub);
        }
      }
    }

    return ents;
  }

  private void storeStatsInMemcache(Map<Key, Entity> stats) {
    Map<String, Entity> s = new HashMap<>();
    for (Entry<Key, Entity> e : stats.entrySet()) s.put(e.getKey().getName(), e.getValue());

    memcache.putAll(s);
  }

  private Key getStatsKey(long userId, long date) {
    return Datastore.getStatsKey(userId, date);
  }
}
