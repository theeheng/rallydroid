package com.rallydev.rallydroid;

import android.util.Base64;
import android.util.Log;

import com.rallydev.rallydroid.dto.Activity;
import com.rallydev.rallydroid.dto.Artifact;
import com.rallydev.rallydroid.dto.DomainObject;
import com.rallydev.rallydroid.dto.Iteration;
import com.rallydev.rallydroid.dto.Story;
import com.rallydev.rallydroid.dto.User;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RallyConnection {
	private String userName;
	private String password;
	private User user;
	private Iteration iteration;
	public final String domain = "rally1.rallydev.com";
	public final String apiVersion = "v2.0";//"1.17";

	public RallyConnection(String username, String password) {
		this.userName = username;
		this.password = password;
	}

	public User getCurrentUser() {
		if (user == null) {
			try {
				Log.i("user", "Retrieving current user");

                String query = "(Username = " + this.userName + ")";
                String url = getApiUrlBase() + "/user?query=" + URLEncoder.encode(query)+"&fetch=true&pretty=true";;

                try {
                    JSONObject queryResult = getResult(url)
                            .getJSONObject("QueryResult");
                    JSONArray results = queryResult.getJSONArray("Results");
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject userObject = (JSONObject) results.get(i);

                        user = new User(userObject.getString("DisplayName"),
                                userObject.getString("SubscriptionPermission"));

                    }
                } catch (Exception e) {
                    Log.e("task", "Error retrieving tasks " + e.toString());
                }


			} catch (Exception e) {
				Log.e("user", e.toString());
			}
		}

		return user;
	}

	public Iteration getCurrentIteration() {
		if (user == null) {
			Log
					.e("iteration",
							"Tried to get current iteration but user was null (probably not logged in).");
			return null;
		}

		if (iteration == null) {
			try {

                String url = getApiUrlBase() + "/iteration:current";;


                JSONObject iterationObject = getResult(url)
						.getJSONObject("Iteration");

				String name = iterationObject.getString("Name");
				Long oid = iterationObject.getLong("ObjectID");

				iteration = new Iteration(name, oid);
			} catch (Exception e) {
				Log.e("iteration", e.toString());
			}
		}

		return iteration;
	}

	public List<Story> getStoriesForCurrentIteration() {

		try {

            Iteration currentIteration = getCurrentIteration();

            String url = getApiUrlBase() + "/hierarchicalrequirement?query="+
                    URLEncoder.encode("(Iteration.ObjectID = "+currentIteration.getOid()+")")+"&fetch=true&pretty=true";


            JSONObject storiesObject = getResult(url).getJSONObject("QueryResult");
			JSONArray resultsArray = storiesObject.getJSONArray("Results");

			List<Story> stories = new ArrayList<Story>();

			Log.d("stories", "found " + resultsArray.length() + " stories");

			for (int i = 0; i < resultsArray.length(); i++) {
				JSONObject object = (JSONObject) resultsArray.get(i);
				Story story = new Story(object);
				stories.add(story);
			}

			return stories;

		} catch (Exception e) {

			Log.e("stories", e.getMessage());
			return Collections.emptyList();
		}
	}

	public List<Activity> getRecentActivities() {
		try {

			// should include workspace in this query? Need scoping:
			// workspace=https://rally1.rallydev.com/slm/webservice/1.17/workspace/41529001

            String url = getApiUrlBase() + "/conversationpost?order=CreationDate+DESC&start=1&pagesize=20&fetch=true&pretty=true";

            JSONObject recentActivitiesObject = getResult(url).getJSONObject("QueryResult");

            JSONArray resultsArray = recentActivitiesObject
					.getJSONArray("Results");

			List<Activity> recentActivities = new ArrayList<Activity>();

			Log.d("Recent Activities", "found " + resultsArray.length()
					+ " activities");

			for (int i = 0; i < resultsArray.length(); i++) {
				JSONObject object = (JSONObject) resultsArray.get(i);
				Activity activity = new Activity(object);
				recentActivities.add(activity);
			}

			return recentActivities;

		} catch (Exception e) {

			Log.e("Recent Activities", e.getMessage());
			return Collections.emptyList();
		}
	}

	public List<DomainObject> listTasksByStory(int storyOid) {
		return listTasks("(WorkProduct.Oid = " + storyOid + ")");
	}

	public List<DomainObject> listAllMyTasks() {
		String query = "(Owner = " + this.userName + ")";
		Iteration it = getCurrentIteration();
		if (it != null)
			query = "(" + query + " and (Iteration.ObjectID = " + it.getOid() + "))";
		return listTasks(query);
	}

	public List<DomainObject> listTasks(String query) {

		String url = getApiUrlBase() + "/task?query="
				+ URLEncoder.encode(query) + "&fetch=true&pretty=true";
		List<DomainObject> ret = new ArrayList<DomainObject>();
		try {
			JSONObject queryResult = getResult(url)
					.getJSONObject("QueryResult");
			JSONArray results = queryResult.getJSONArray("Results");
			for (int i = 0; i < results.length(); i++) {
				JSONObject object = (JSONObject) results.get(i);
				ret.add(new Artifact(object));
			}
		} catch (Exception e) {
			Log.e("task", "Error retrieving tasks " + e.toString());
		}
		return ret;
	}

	private String getApiUrlBase() {
		return "https://" + domain + "/slm/webservice/" + apiVersion;
	}

	/*Deprecated for API v2.0
	private JSONObject getAdHocResult(JSONObject adHocQuery) throws Exception {
		String url = getApiUrlBase() + "/adhoc.js?_method=POST&adHocQuery="
				+ URLEncoder.encode(adHocQuery.toString()) + "&pretty=true";
		return getResult(url);
	}
	*/

	protected JSONObject getResult(String uri) throws Exception {
		DefaultHttpClient httpclient = getClient();
		try {
			HttpGet httpget = new HttpGet(uri);
            String authKey = Base64.encodeToString((userName + ":" + password).getBytes(), Base64.DEFAULT).replaceAll("\\n", "");
            httpget.addHeader("Authorization", "Basic "+ authKey) ;
			HttpResponse response = httpclient.execute(httpget);
			StatusLine status = response.getStatusLine();
			int statusCode = status.getStatusCode();
			if (statusCode > 201) {
				Log.e("http", "Got unexpected http status "
						+ status.getStatusCode() + ": "
						+ status.getReasonPhrase());
				return null;
			}
			HttpEntity entity = response.getEntity();

			InputStream is = entity.getContent();
			JSONObject obj = new JSONObject(Util.slurp(is));

			return obj;
		}catch(Exception ex)
        {
            String exmsg = ex.getMessage();
        }
        finally {
			httpclient.getConnectionManager().shutdown();
		}

        return null;
	}

	private DefaultHttpClient getClient() {
		DefaultHttpClient httpclient = new DefaultHttpClient();

		httpclient.getCredentialsProvider().setCredentials(
				new AuthScope(null, 443),
				new UsernamePasswordCredentials(this.userName, this.password));
		return httpclient;
	}
}
