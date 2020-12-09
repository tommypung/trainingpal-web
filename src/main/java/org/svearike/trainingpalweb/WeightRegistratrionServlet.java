package org.svearike.trainingpalweb;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.svearike.trainingpalweb.tasks.Datastore;
import org.svearike.trainingpalweb.tasks.SaveWeightTask;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

@SuppressWarnings("serial")
public class WeightRegistratrionServlet extends HttpServlet
{
	private Database database = new Cache(new Datastore());

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		URLPathParameters onlyWeight = new URLPathParameters("/${home}/register/${weight}", req.getPathInfo());
		URLPathParameters withDate = new URLPathParameters("/${home}/register/${timestamp}/${weight}", req.getPathInfo());
		URLPathParameters withDateUser = new URLPathParameters("/${home}/register/${timestamp}/${weight}/${user}", req.getPathInfo());
		URLPathParameters withDateSynchronized  = new URLPathParameters("/${home}/register/${timestamp}/${weight}/synchronized", req.getPathInfo());
		if (!onlyWeight.isValid() && !withDate.isValid() && !withDateSynchronized.isValid() && !withDateUser.isValid())
			throw new ServletException("Invalid parameters: " + onlyWeight.getError());

		Queue queue = QueueFactory.getDefaultQueue();

		SaveWeightTask wt = null;
		try {
			if (onlyWeight.isValid())
				wt = new SaveWeightTask(onlyWeight.getString("home"), onlyWeight.getString("weight"), System.currentTimeMillis(), null);
			else if (withDate.isValid())
				wt = new SaveWeightTask(withDate.getString("home"), withDate.getString("weight"), withDate.getLong("timestamp"), null);
			else if (withDateSynchronized.isValid())
				new SaveWeightTask(withDateSynchronized.getString("home"), withDateSynchronized.getString("weight"), withDateSynchronized.getLong("timestamp"), null).run();
			else if (withDateUser.isValid())
			{
				long id = withDateUser.getLong("user");
				Entity user = null;
				if (id != -1)
					user = database.getUser(id);
				else
					user = database.createNewUser(withDateUser.getString("home"), withDateUser.getString("weight"), withDateUser.getLong("timestamp"));

				wt = new SaveWeightTask(withDateUser.getString("home"), withDateUser.getString("weight"), withDateUser.getLong("timestamp"), user);
			}
		} catch(Exception e) {
			throw new ServletException(e);
		}

		if (wt != null)
			queue.add(TaskOptions.Builder.withPayload(wt));

		resp.setContentType("application/json");
		resp.getWriter().write("{\"status\":\"ok\"}");
	}
}
