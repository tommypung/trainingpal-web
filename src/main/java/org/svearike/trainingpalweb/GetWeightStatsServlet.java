package org.svearike.trainingpalweb;

import java.io.IOException;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.svearike.trainingpalweb.tasks.Database;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

@SuppressWarnings("serial")
public class GetWeightStatsServlet extends HttpServlet
{
	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		URLPathParameters param = new URLPathParameters("/${user}/${months}/months", req.getPathInfo());
		if (!param.isValid())
			throw new ServletException("Invalid parameters: " + param.getError());

		int months = param.getInt("months");
		long user = param.getLong("user");

		GregorianCalendar cal = new GregorianCalendar();
		List<Key> keys = new LinkedList<>();
		for(int i=0;i<=months;i++)
		{
			keys.add(0, Database.getStatsKey(user, cal.getTime().getTime()));
			cal.add(GregorianCalendar.MONTH, -1);
		}

		Map<Key, Entity> ents = datastore.get(keys);

		JSONObject root = new JSONObject();
		root.put("user", user);

		JSONArray dateArr = new JSONArray();
		JSONArray weightArr = new JSONArray();
		for(Key key : keys)
		{
			Entity e = ents.get(key);
			if (e == null)
				continue;

			@SuppressWarnings("unchecked")
			Collection<Long> date = (Collection<Long>) e.getProperty("date");
			@SuppressWarnings("unchecked")
			Collection<Double> weight = (Collection<Double>) e.getProperty("weight");

			for(Long d : date)
				dateArr.put(d);
			for(Double d : weight)
				weightArr.put(d);
		}

		root.put("dates", dateArr);
		root.put("weights", weightArr);

		resp.setContentType("application/json");
		resp.getWriter().print(root.toString());
	}
}
