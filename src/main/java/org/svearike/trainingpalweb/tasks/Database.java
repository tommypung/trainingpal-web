package org.svearike.trainingpalweb.tasks;

import java.text.SimpleDateFormat;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class Database
{
	public static Key getStatsKey(long userId, long date)
	{
		return KeyFactory.createKey("weight", userId + ";" + new SimpleDateFormat("yyyy-MM").format(date));
	}
}
