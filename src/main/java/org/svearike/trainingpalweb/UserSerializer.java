package org.svearike.trainingpalweb;

import org.json.JSONObject;

import com.google.appengine.api.datastore.Entity;

public class UserSerializer extends Serializer<Entity>
{
	@Override
	public JSONObject get(Entity e)
	{
		JSONObject obj = new JSONObject();
		obj.put("id", e.getKey().getId());
		obj.put("name", e.getProperty("name"));
		obj.put("home", e.getProperty("home"));
		obj.put("length", e.getProperty("length"));
		obj.put("newAutoCreate", e.getProperty("newAutoCreate"));
		obj.put("lastWeight", e.getProperty("lastWeight"));
		obj.put("lastWeightDate", e.getProperty("lastWeightDate"));
		obj.put("image", e.getProperty("image"));
		return obj;
	}
}
