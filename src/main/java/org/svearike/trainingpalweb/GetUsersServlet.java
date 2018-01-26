package org.svearike.trainingpalweb;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.svearike.trainingpalweb.tasks.Database;

import com.google.appengine.api.datastore.Entity;

@SuppressWarnings("serial")
public class GetUsersServlet extends HttpServlet
{
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		try {
			resp.addHeader("Access-Control-Allow-Origin", "*");
			handle(req, resp);
		} catch(Exception e) {
			throw new ServletException(e);
		}
	}

	public void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		URLPathParameters param = new URLPathParameters("/${home}", req.getPathInfo());
		URLPathParameters p1 = new URLPathParameters("/byId/${user}", req.getPathInfo());

		if (!param.isValid() && !p1.isValid())
			throw new ServletException("Invalid parameters: " + param.getError());

		resp.setContentType("application/json");
		JSONObject root = new JSONObject();
		if (param.isValid())
		{
			List<Entity> users = Database.getUsers(param.getString("home"));
			root.put("users", new UserSerializer().get(users));
		}
		else if (p1.isValid())
			root.put("user", new UserSerializer().get(Database.getUser(p1.getLong("user"))));

		resp.getWriter().write(root.toString());
	}
}
