package org.svearike.trainingpalweb;

import java.io.IOException;
import java.security.InvalidParameterException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.svearike.trainingpalweb.tasks.Database;
import org.svearike.trainingpalweb.tasks.Database.OnLoad;

import com.google.appengine.api.datastore.Entity;

@SuppressWarnings("serial")
public class UserInfoServlet extends HttpServlet
{
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.addHeader("Access-Control-Allow-Origin", "*");
		URLPathParameters params = new URLPathParameters("/info/${user}", req.getPathInfo());
		if (!params.isValid())
			throw new ServletException("Invalid parameters: " + params.getError());

		String rawData = IOUtils.toString(req.getInputStream());
		JSONObject obj = new JSONObject(rawData);
		final String name = obj.getString("name");
		final long length = Long.parseLong(obj.getString("length"));
		final String image = obj.getString("image");

		Database.updateUser(params.getLong("user"), new OnLoad() {
			@Override
			public boolean loadedInATransaction(Entity e) throws InvalidParameterException {
				e.setProperty("newAutoCreate", false);
				e.setProperty("name", name);
				e.setProperty("length", length);
				e.setProperty("image", image);
				return true;
			}
		});
		
		
	}
}
