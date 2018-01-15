package org.svearike.trainingpalweb;

import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONObject;

public abstract class Serializer<T>
{
	public abstract JSONObject get(T object);
	public JSONArray get(Collection<T> objects)
	{
		JSONArray arr = new JSONArray();
		if (objects != null)
			for(T t : objects)
				arr.put(get(t));
		return arr;
	}
}
