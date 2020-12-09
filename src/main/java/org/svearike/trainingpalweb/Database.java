package org.svearike.trainingpalweb;

import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;

public interface Database {
  public interface OnLoad {
    boolean loadedInATransaction(Entity e) throws InvalidParameterException;
  }

  Entity updateUser(long id, OnLoad onLoad);

  Entity createNewUser(String home, String lastWeight, Long lastWeightDate);

  List<Entity> getUsers(String home);

  Entity removeWeight(long id, long date);

  Entity getUser(long toUser) throws EntityNotFoundException;

  Entity addStats(Entity user, Long date, Double weight, Transaction tx);

  List<Entity> getStats(long userId, int numMonths);

  Map<Key, Entity> getStats(long userId, Collection<Key> keys);
}
