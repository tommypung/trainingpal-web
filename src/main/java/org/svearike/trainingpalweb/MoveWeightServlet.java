package org.svearike.trainingpalweb;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.svearike.trainingpalweb.tasks.Datastore;
import org.svearike.trainingpalweb.tasks.SaveWeightTask;

import com.google.appengine.api.datastore.Entity;

@SuppressWarnings("serial")
public class MoveWeightServlet extends HttpServlet
{
	private Database database = new Cache(new Datastore());

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		try {
			resp.addHeader("Access-Control-Allow-Origin", "*");
			resp.setContentType("application/json");

			handle(req, resp);
		} catch(Exception e) {
			JSONObject root = new JSONObject();
			root.put("status", "error");
			root.put("error", e.getMessage());
			resp.getWriter().write(root.toString());
		}
	}

	private void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		URLPathParameters param = new URLPathParameters("/${home}/${fromUser}/${date}/${weight}/${toUser}", req.getPathInfo());
		if (!param.isValid())
			throw new ServletException("Invalid parameters: " + param.getError());

		String home = param.getString("home");
		long fromUser = param.getLong("fromUser");
		long date = param.getLong("date");
		String weight = param.getString("weight");
		long toUser = param.getLong("toUser");

		if (toUser == -1)
		{
			Entity user = database.createNewUser(home, weight, date);
			new SaveWeightTask(home, weight, date, user).run();
			database.removeWeight(fromUser, date);
		}
		else
		{
			Entity user = database.getUser(toUser);
			new SaveWeightTask(home, weight, date, user).run();
			database.removeWeight(fromUser, date);
		}

		JSONObject root = new JSONObject();
		root.put("status", "ok");
		resp.getWriter().write(root.toString());
	}
}
