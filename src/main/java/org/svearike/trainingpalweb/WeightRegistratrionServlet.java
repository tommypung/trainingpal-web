package org.svearike.trainingpalweb;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.svearike.trainingpalweb.tasks.SaveWeightTask;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

@SuppressWarnings("serial")
public class WeightRegistratrionServlet extends HttpServlet
{
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		URLPathParameters param = new URLPathParameters("/${home}/register/${weight}", req.getPathInfo());
		URLPathParameters fake = new URLPathParameters("/${home}/register/${timestamp}/${weight}", req.getPathInfo());
		if (!param.isValid() && !fake.isValid())
			throw new ServletException("Invalid parameters: " + param.getError());

		Queue queue = QueueFactory.getDefaultQueue();

		SaveWeightTask wt = null;
		if (param.isValid())
		{
			wt = new SaveWeightTask(param.getString("home"), param.getString("weight"), System.currentTimeMillis());
			queue.add(TaskOptions.Builder.withPayload(wt));
		}
		else
			new SaveWeightTask(fake.getString("home"), fake.getString("weight"), fake.getLong("timestamp")).run();
	}
}
