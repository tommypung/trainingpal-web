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
		URLPathParameters onlyWeight = new URLPathParameters("/${home}/register/${weight}", req.getPathInfo());
		URLPathParameters withDate = new URLPathParameters("/${home}/register/${timestamp}/${weight}", req.getPathInfo());
		URLPathParameters withDateSynchronized  = new URLPathParameters("/${home}/register/${timestamp}/${weight}/synchronized", req.getPathInfo());
		if (!onlyWeight.isValid() && !withDate.isValid() && !withDateSynchronized.isValid())
			throw new ServletException("Invalid parameters: " + onlyWeight.getError());

		Queue queue = QueueFactory.getDefaultQueue();

		SaveWeightTask wt = null;
		if (onlyWeight.isValid())
			wt = new SaveWeightTask(onlyWeight.getString("home"), onlyWeight.getString("weight"), System.currentTimeMillis());
		else if (withDate.isValid())
			wt = new SaveWeightTask(withDate.getString("home"), withDate.getString("weight"), withDate.getLong("timestamp"));
		else if (withDateSynchronized.isValid())
			new SaveWeightTask(withDateSynchronized.getString("home"), withDateSynchronized.getString("weight"), withDateSynchronized.getLong("timestamp")).run();

		if (wt != null)
			queue.add(TaskOptions.Builder.withPayload(wt));
	}
}
