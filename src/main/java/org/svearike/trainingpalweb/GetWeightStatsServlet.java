package org.svearike.trainingpalweb;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.svearike.trainingpalweb.tasks.Datastore;

import com.google.appengine.api.datastore.Entity;

@SuppressWarnings("serial")
public class GetWeightStatsServlet extends HttpServlet
{
	private Database database = new Cache(new Datastore());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		URLPathParameters param = new URLPathParameters("/${user}/${months}/months", req.getPathInfo());
		if (!param.isValid())
			throw new ServletException("Invalid parameters: " + param.getError());

		int months = param.getInt("months");
		long user = param.getLong("user");

		List<Entity> ents = database.getStats(user, months);

		JSONObject root = new JSONObject();
		root.put("user", user);
		JSONArray dateArr = new JSONArray();
		JSONArray weightArr = new JSONArray();
		for(Entity e : ents)
		{
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
		resp.addHeader("Access-Control-Allow-Origin", "*");
		resp.getWriter().print(root.toString());
	}
}
