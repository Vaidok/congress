package com.sunlightlabs.android.congress;

import java.util.List;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import winterwell.jtwitter.Twitter.Status;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.format.Time;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.sunlightlabs.android.congress.utils.Utils;

public class LegislatorTwitter extends ListActivity {
	private static final int MENU_REPLY = 0;
	private static final int MENU_COPY = 1;
	
	private String username;
	private List<Status> tweets;
	
	private LoadTweetsTask loadTweetsTask = null;
	
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.list);
    	
    	username = getIntent().getStringExtra("username");
    
    	LegislatorTwitterHolder holder = (LegislatorTwitterHolder) getLastNonConfigurationInstance();
    	if (holder != null) {
    		tweets = holder.tweets;
    		loadTweetsTask = holder.loadTweetsTask;
    		if (loadTweetsTask != null)
    			loadTweetsTask.onScreenLoad(this);
    	}
    	
    	setupControls();
    	if (loadTweetsTask == null)
    		loadTweets();
	}
	
	@Override
    public Object onRetainNonConfigurationInstance() {
		LegislatorTwitterHolder holder = new LegislatorTwitterHolder();
		holder.tweets = tweets;
		holder.loadTweetsTask = loadTweetsTask;
    	return holder;
    }
	
	private void setupControls() {
		Utils.setLoading(this, R.string.twitter_loading);
		((Button) findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				tweets = null;
				Utils.showLoading(LegislatorTwitter.this);
				loadTweets();
			}
		});
    	registerForContextMenu(getListView());
	}
    
	protected void loadTweets() {	    
	    if (tweets == null)
    		loadTweetsTask = (LoadTweetsTask) new LoadTweetsTask(this).execute(username);
    	else
    		displayTweets();
	}
	
	public void displayTweets() {
    	if (tweets != null && tweets.size() > 0) {
	    	setListAdapter(new TweetAdapter(this, tweets));
	    	firstToast();
    	} else
	    	Utils.showRefresh(this, R.string.twitter_empty);
    }
	
	public void firstToast() {
		if (!Preferences.getBoolean(this, "already_twittered", false)) {
    		Toast.makeText(this, R.string.first_time_twitter, Toast.LENGTH_LONG).show();
    		Preferences.setBoolean(this, "already_twittered", true);
    	}
	}
	
	public void onListItemClick(ListView parent, View v, int position, long id) {
		Status tweet = (Status) parent.getItemAtPosition(position);
		launchReplyForTweet(tweet);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.add(0, MENU_REPLY, 0, "Reply");
		menu.add(0, MENU_COPY, 1, "Copy tweet text");
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Status tweet = (Status) getListView().getItemAtPosition(info.position);
		
		switch (item.getItemId()) {
		case MENU_REPLY:
			launchReplyForTweet(tweet);
			return true;
		case MENU_COPY:
			ClipboardManager cm = (ClipboardManager) getSystemService(Activity.CLIPBOARD_SERVICE);
			cm.setText(tweet.text);
		}
		
		return super.onContextItemSelected(item);
	}
	
	private void launchReplyForTweet(Status tweet) {
		Intent intent = new Intent(this, TwitterReply.class);
		intent.putExtra("tweet_text", tweet.text);
		intent.putExtra("tweet_username", tweet.user.screenName);
		intent.putExtra("tweet_in_reply_to_id", tweet.id);
		startActivity(intent);
	}
	
	@Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
	    super.onCreateOptionsMenu(menu); 
	    getMenuInflater().inflate(R.menu.twitter, menu);
	    return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) { 
    	case R.id.settings: 
    		startActivity(new Intent(this, Preferences.class));
    		break;
    	}
    	return true;
    }
    
    protected class TweetAdapter extends ArrayAdapter<Status> {
    	LayoutInflater inflater;
    	Activity context;

        public TweetAdapter(Activity context, List<Status> tweets) {
        	super(context, 0, tweets);
        	this.context=context;
            inflater = LayoutInflater.from(context);
        }

		public View getView(final int position, View convertView, ViewGroup parent) {
			final LinearLayout view;
			if (convertView == null)
				view = (LinearLayout) inflater.inflate(R.layout.tweet, null); 
			else
				view = (LinearLayout) convertView;
			
			Status tweet = getItem(position);
			((TextView) view.findViewById(R.id.tweet_text)).setText(tweet.text);
			if(Linkify.addLinks(((TextView) view.findViewById(R.id.tweet_text)),Linkify.ALL)){
				((TextView) view.findViewById(R.id.tweet_text)).setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						((LegislatorTwitter) context).getListView().performItemClick(v,position,1);
					}
				});
				((TextView) view.findViewById(R.id.tweet_byline)).setText("posted " + timeAgoInWords(tweet.createdAt.getTime()) + " by @" + tweet.user.screenName);
				((TextView) view.findViewById(R.id.tweet_byline)).setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						LegislatorTwitter lT=(LegislatorTwitter) context;
						lT.getListView().performItemClick(view,position,1);
					}
						
				});
				}
				else{
					((TextView) view.findViewById(R.id.tweet_byline))
					.setText("posted " + timeAgoInWords(tweet.createdAt.getTime()) + " by @" + tweet.user.screenName);
				}
			return view;
		}
		
		private String timeAgoInWords(long olderTime) {
			long now = System.currentTimeMillis();
			long diff = now - olderTime; 
			if (diff < 2000) // 2 seconds
				return "just now";
			else if (diff < 50000) // 50 seconds
				return (diff / 1000) + " seconds ago";
			else if (diff < 65000) // 1 minute, 5 seconds
				return "a minute ago";
			else if (diff < 3300000) // 55 minutes
				return (diff / 60000) + " minutes ago";
			else if (diff < 3900000) // 65 minutes
				return "an hour ago";
			else if (diff < 82800000) // 23 hours
				return (diff / 3600000) + " hours ago";
			else if (diff < 90000000) // 25 hours
				return "a day ago";
			else if (diff < 1123200000) // 13 days
				return (diff / 86400000) + " days ago";
			else {
				Time old = new Time();
				old.set(olderTime);
				return old.format("%b %d");
			}
		}

    }
    
    private class LoadTweetsTask extends AsyncTask<String,Void,List<Twitter.Status>> {
    	public LegislatorTwitter context;
    	
    	public LoadTweetsTask(LegislatorTwitter context) {
    		super();
    		this.context = context;
    	}
    	
    	public void onScreenLoad(LegislatorTwitter context) {
    		this.context = context;
    	}
    	
    	@Override
    	protected List<Twitter.Status> doInBackground(String... username) {
    		try {
        		return new Twitter().getUserTimeline(username[0]);
        	} catch(TwitterException e) {
        		return null;
        	}
    	}
    	
    	@Override
    	protected void onPostExecute(List<Twitter.Status> tweets) {
    		context.tweets = tweets;
    		context.displayTweets();
    		context.loadTweetsTask = null;
    	}
    }

    static class LegislatorTwitterHolder {
    	List<Twitter.Status> tweets;
    	LoadTweetsTask loadTweetsTask;
    }
}