/*
 * Copyright 2009 Rally Software Development
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
  
package com.rallydev.rallydroid;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.rallydev.rallydroid.dto.Artifact;
import com.rallydev.rallydroid.dto.Story;

public class IterationStatus extends RallyListActivity {
	private final int MENU_OPEN = 1;
    private final int MENU_COMPLETED = 2;
    private final int MENU_ALL = 4;
    private final int MENU_REFRESH = 10;
    private int filterSelected = MENU_OPEN;

    private List<Story> stories;
    
	protected List<Artifact> loadData()
	{
		if (stories == null)
    	{
			stories = getHelper().getRallyConnection().getStoriesForCurrentIteration();
    	}
    	
    	List<Artifact> ret = new ArrayList<Artifact>();
    	for (Artifact story: stories)
    	{
    		String state = ((Story)story).getStatus();
    		if ((filterSelected == MENU_COMPLETED && !state.equals("Completed") && !state.equals("Accepted"))
    			|| (filterSelected == MENU_OPEN && (state.equals("Completed") || state.equals("Accepted"))))
        			continue;
        		
    		ret.add(story);
    	}
    	
    	return ret;
	}
	
	protected String getActivityTitle()
	{
		String title = "All Stories";
    	if (filterSelected == MENU_OPEN)
    		title = "Defined/In-Progress Stories";
    	else if (filterSelected == MENU_COMPLETED)
    		title = "Completed/Accepted Stories";
    	
    	return title;
	}
	
	protected String getLine1(Artifact artifact)
    {
    	return artifact.getName();
    }
    
    protected String getLine2(Artifact artifact)
    {
    	return artifact.getFormattedID() + " (" + ((Story)artifact).getStatus() + ")";
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_OPEN, 0, "Open");//.setIcon(android.R.drawable.ic_menu_revert);
        menu.add(0, MENU_COMPLETED, 1, "Completed");//.setIcon(android.R.drawable.ic_menu_search);
        menu.add(0, MENU_ALL, 2, "All");//;.setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, MENU_REFRESH, 3, "Refresh");//.setIcon(android.R.drawable.);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) 
        {
	        case MENU_OPEN:
	        case MENU_COMPLETED:
	        case MENU_ALL:
	        	filterSelected = item.getItemId(); // remember the filter
	        	break;
	        case MENU_REFRESH:
	        	stories = null; // force refresh
		        break;
        }
        refreshData();
        return super.onOptionsItemSelected(item);
    }

    @Override
	protected int getDetailViewResId()
    {
    	return R.layout.view_story;
    }

	@Override
	protected void PrepareDetailDialog(Dialog dialog, Artifact selectedItem) {
		Story selectedStory = (Story)selectedItem;
		dialog.setTitle(selectedStory.getFormattedID());
		
    	String description = selectedStory.getString("Description");
    	String planEstimate = selectedStory.getString("PlanEstimate");
    	String estimate=selectedStory.getString("TaskEstimateTotal");
    	String todo=selectedStory.getString("TaskRemainingTotal");
    	String actuals=selectedStory.getString("TaskActualTotal");
    	boolean blocked=selectedStory.getBoolean("Blocked");
    	String state = selectedStory.getStatus() + " " + (blocked ? "(BLOCKED)" : "(Not blocked)");
    	
		((TextView)dialog.findViewById(R.id.story_nameView)).setText(selectedStory.getName());
		((TextView)dialog.findViewById(R.id.story_descriptionView)).setText(description);
    	((TextView)dialog.findViewById(R.id.story_stateView)).setText(state);
    	((TextView)dialog.findViewById(R.id.plan_estimateView)).setText(planEstimate);
    	((TextView)dialog.findViewById(R.id.task_estimateView)).setText(estimate);
    	((TextView)dialog.findViewById(R.id.task_todoView)).setText(todo);
    	((TextView)dialog.findViewById(R.id.task_actualView)).setText(actuals);
	}

}
