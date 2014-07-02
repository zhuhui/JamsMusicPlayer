package com.jams.music.player.ListViewFragment;

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andraskindler.quickscroll.QuickScroll;
import com.jams.music.player.Helpers.PauseOnScrollHelper;
import com.jams.music.player.R;
import com.jams.music.player.DBHelpers.DBAccessHelper;
import com.jams.music.player.Helpers.TypefaceHelper;
import com.jams.music.player.Helpers.UIElementsHelper;
import com.jams.music.player.Utils.Common;

/**
 * Generic, multipurpose ListView fragment.
 * 
 * @author Saravan Pantham
 */
public class ListViewFragment extends Fragment {
	
	private Context mContext;
	private ListViewFragment mFragment;
	private Common mApp;
	private View mRootView;
	private int mFragmentId;
	
	private QuickScroll mQuickScroll;
	private ListViewCardsAdapter mListViewAdapter;
    private HashMap<Integer, String> mDBColumnsMap;
	private ListView mListView;
	private TextView mEmptyTextView;
	
	private RelativeLayout mSearchLayout;
	private EditText mSearchEditText;
	
	public Handler mHandler = new Handler();
	private Cursor mCursor;
	private String mQuerySelection = "";
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_list_view, container, false);
        mContext = getActivity().getApplicationContext();
	    mApp = (Common) mContext;
        mFragment = this;
        
        //Set the background.
        if (mApp.getSharedPreferences().getString("SELECTED_THEME", "LIGHT_CARDS_THEME").equals("LIGHT_CARDS_THEME"))
			mRootView.setBackgroundColor(0xFFEEEEEE);
		else
			mRootView.setBackgroundColor(0xFF111111);
        
        //Grab the fragment. This will determine which data to load into the cursor.
        mFragmentId = getArguments().getInt(Common.FRAGMENT_ID);
        mDBColumnsMap = new HashMap<Integer, String>();
        
	    //Init the search fields.
	    mSearchLayout = (RelativeLayout) mRootView.findViewById(R.id.search_layout);
	    mSearchEditText = (EditText) mRootView.findViewById(R.id.search_field);
	    
	    mSearchEditText.setTypeface(TypefaceHelper.getTypeface(mContext, "RobotoCondensed-Regular"));
	    mSearchEditText.setPaintFlags(mSearchEditText.getPaintFlags() | Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
	    mSearchEditText.setTextColor(UIElementsHelper.getThemeBasedTextColor(mContext));
	    mSearchEditText.setFocusable(true);
	    mSearchEditText.setCursorVisible(true);
	    
        mQuickScroll = (QuickScroll) mRootView.findViewById(R.id.quickscroll);

	    mListView = (ListView) mRootView.findViewById(R.id.generalListView);
        mListView.setVerticalScrollBarEnabled(false);
		mListView.setDivider(getResources().getDrawable(R.drawable.transparent_drawable));
		mListView.setDividerHeight(10);
		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mListView.getLayoutParams();
		layoutParams.setMargins(20, 20, 20, 20);
		mListView.setLayoutParams(layoutParams);
        
        //KitKat translucent navigation/status bar.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        	int topPadding = Common.getStatusBarHeight(mContext);
            
            //Calculate navigation bar height.
            int navigationBarHeight = 0;
            int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                navigationBarHeight = getResources().getDimensionPixelSize(resourceId);
            }
            
            mListView.setClipToPadding(false);
            mListView.setPadding(0, topPadding, 0, navigationBarHeight);
            mQuickScroll.setPadding(0, topPadding, 0, navigationBarHeight);
            
            layoutParams = (RelativeLayout.LayoutParams) mSearchLayout.getLayoutParams();
            layoutParams.setMargins(15, topPadding + 15, 15, 0);
            mSearchLayout.setLayoutParams(layoutParams);
            
        }

        //Set the empty views.
        mEmptyTextView = (TextView) mRootView.findViewById(R.id.empty_view_text);
	    mEmptyTextView.setTypeface(TypefaceHelper.getTypeface(mContext, "Roboto-Light"));
	    mEmptyTextView.setPaintFlags(mEmptyTextView.getPaintFlags() | Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        
        //Create a set of options to optimize the bitmap memory usage.
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inJustDecodeBounds = false;
        options.inPurgeable = true;
	    
        mHandler.postDelayed(queryRunnable, 250);
        return mRootView;
    }
    
    /**
     * Query runnable.
     */
    public Runnable queryRunnable = new Runnable() {

		@Override
		public void run() {
			new AsyncRunQuery().execute();
			
		}
    	
    };
    
    /**
     * Displays the search field.
     */
    private void showSearch() {
    	mSearchLayout.setVisibility(View.VISIBLE);
    	final TranslateAnimation searchAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, 
    														   		 Animation.RELATIVE_TO_SELF, 0f, 
    														   		 Animation.RELATIVE_TO_SELF, -2f, 
    														   		 Animation.RELATIVE_TO_SELF, 0f);
    	searchAnim.setDuration(500l);
    	searchAnim.setInterpolator(new AccelerateDecelerateInterpolator());
    	
    	final TranslateAnimation gridListAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, 
		   		 													   Animation.RELATIVE_TO_SELF, 0f, 
		   		 													   Animation.RELATIVE_TO_SELF, 0f, 
		   		 													   Animation.RELATIVE_TO_SELF, 2f);

    	gridListAnim.setDuration(500l);
    	gridListAnim.setInterpolator(new LinearInterpolator());
    	
    	gridListAnim.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				mListView.setAdapter(null);
				
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onAnimationStart(Animation animation) {
				mSearchLayout.startAnimation(searchAnim);
				mSearchLayout.setVisibility(View.VISIBLE);
				
			}
    		
    	});
    	
    	searchAnim.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				if (mSearchEditText.requestFocus()) {
				    mFragment.getActivity()
				    		.getWindow()
				    		.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
				
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
    	
    	mListView.startAnimation(gridListAnim);
    	
    }
    
    /**
     * Item click listener for the ListView.
     */
    private OnItemClickListener onItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int index, long id) {
			mApp.getPlaybackKickstarter()
				.initPlayback(mContext, mQuerySelection, Common.SONGS_FRAGMENT, index, true);	
			
		}
    	
    };
    
    @Override
    public void onDestroyView() {
    	super.onDestroyView();
    	mRootView = null;
    	
    	if (mCursor!=null) {
        	mCursor.close();
        	mCursor = null;
    	}
    	
    	onItemClickListener = null;
    	mListView = null;
    	mListView = null;
    	mListViewAdapter = null;
    	mContext = null;
    	mHandler = null;
    	
    }
    
    /**
     * Runs the correct DB query based on the passed in fragment id and 
     * displays the ListView.
     * 
     * @author Saravan Pantham
     */
    public class AsyncRunQuery extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
	        mCursor = mApp.getDBAccessHelper().getFragmentCursor(mContext, mQuerySelection, mFragmentId);
	        loadDBColumnNames();
	        
	        return null;
		}
		
		/**
		 * Populates the DB column names based on the specifed fragment id.
		 */
		private void loadDBColumnNames() {
			
			switch (mFragmentId) {
			case Common.ARTISTS_FRAGMENT:
				mDBColumnsMap.put(ListViewCardsAdapter.TITLE_TEXT, DBAccessHelper.SONG_ARTIST);
				mDBColumnsMap.put(ListViewCardsAdapter.SOURCE, DBAccessHelper.SONG_SOURCE);
				mDBColumnsMap.put(ListViewCardsAdapter.FILE_PATH, DBAccessHelper.SONG_FILE_PATH);
				mDBColumnsMap.put(ListViewCardsAdapter.ARTWORK_PATH, DBAccessHelper.SONG_ALBUM_ART_PATH);
				break;
			case Common.ALBUM_ARTISTS_FRAGMENT:
				mDBColumnsMap.put(ListViewCardsAdapter.TITLE_TEXT, DBAccessHelper.SONG_ALBUM_ARTIST);
				mDBColumnsMap.put(ListViewCardsAdapter.SOURCE, DBAccessHelper.SONG_SOURCE);
				mDBColumnsMap.put(ListViewCardsAdapter.FILE_PATH, DBAccessHelper.SONG_FILE_PATH);
				mDBColumnsMap.put(ListViewCardsAdapter.ARTWORK_PATH, DBAccessHelper.SONG_ALBUM_ART_PATH);
				break;
			case Common.ALBUMS_FRAGMENT:
				mDBColumnsMap.put(ListViewCardsAdapter.TITLE_TEXT, DBAccessHelper.SONG_ALBUM);
				mDBColumnsMap.put(ListViewCardsAdapter.SOURCE, DBAccessHelper.SONG_SOURCE);
				mDBColumnsMap.put(ListViewCardsAdapter.FILE_PATH, DBAccessHelper.SONG_FILE_PATH);
				mDBColumnsMap.put(ListViewCardsAdapter.ARTWORK_PATH, DBAccessHelper.SONG_ALBUM_ART_PATH);
				break;
			case Common.SONGS_FRAGMENT:
				mDBColumnsMap.put(ListViewCardsAdapter.TITLE_TEXT, DBAccessHelper.SONG_TITLE);
				mDBColumnsMap.put(ListViewCardsAdapter.SOURCE, DBAccessHelper.SONG_SOURCE);
				mDBColumnsMap.put(ListViewCardsAdapter.FILE_PATH, DBAccessHelper.SONG_FILE_PATH);
				mDBColumnsMap.put(ListViewCardsAdapter.ARTWORK_PATH, DBAccessHelper.SONG_ALBUM_ART_PATH);
				mDBColumnsMap.put(ListViewCardsAdapter.FIELD_1, DBAccessHelper.SONG_DURATION);
				mDBColumnsMap.put(ListViewCardsAdapter.FIELD_2, DBAccessHelper.SONG_ARTIST);
				break;
			case Common.PLAYLISTS_FRAGMENT:
				break;
			case Common.GENRES_FRAGMENT:
				break;
			case Common.FOLDERS_FRAGMENT:
				break;
			}
			
		}
    	
		@Override
		public void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, 
					  											  Animation.RELATIVE_TO_SELF, 0.0f, 
					  											  Animation.RELATIVE_TO_SELF, 2.0f, 
					  											  Animation.RELATIVE_TO_SELF, 0.0f);

			animation.setDuration(600);
			animation.setInterpolator(new AccelerateDecelerateInterpolator());
			
        	mListViewAdapter = new ListViewCardsAdapter(mContext, mFragment, mDBColumnsMap);
	        mListView.setAdapter(mListViewAdapter);
	        
	       /* SwingBottomInAnimationAdapter animationAdapter = new SwingBottomInAnimationAdapter(mListViewAdapter);
	        animationAdapter.setShouldAnimate(true);
	        animationAdapter.setShouldAnimateFromPosition(0);
	        animationAdapter.setAbsListView(mListView);
	        mListView.setAdapter(animationAdapter);*/

            PauseOnScrollHelper pauseOnScrollHelper = new PauseOnScrollHelper(mApp.getImageLoader(),
                                                                              true, true, 500);
            mListView.setOnScrollListener(pauseOnScrollHelper);
	        mListView.setOnItemClickListener(onItemClickListener);
	        
	        //Init the quick scroll widget.
	        mQuickScroll.init(QuickScroll.TYPE_INDICATOR_WITH_HANDLE, 
	        				  mListView,
	        				  (ListViewCardsAdapter) mListViewAdapter,
	        				  QuickScroll.STYLE_HOLO);
	        
	        int[] quickScrollColors = UIElementsHelper.getQuickScrollColors(mContext);
	        mQuickScroll.setHandlebarColor(quickScrollColors[0], quickScrollColors[0], quickScrollColors[1]);
	        mQuickScroll.setIndicatorColor(quickScrollColors[1], quickScrollColors[0], quickScrollColors[2]);
	        mQuickScroll.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 48);
	        
	        animation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationEnd(Animation arg0) {
					mQuickScroll.setVisibility(View.VISIBLE);
					
				}

				@Override
				public void onAnimationRepeat(Animation arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onAnimationStart(Animation arg0) {
					mListView.setVisibility(View.VISIBLE);
					
				}
	        	
	        });
	        
	        mListView.startAnimation(animation);
			
		}
		
    }

    /*
     * Getter methods.
     */

	public ListViewCardsAdapter getListViewAdapter() {
		return (ListViewCardsAdapter) mListViewAdapter;
	}

	public ListView getListView() {
		return mListView;
	}

	public Cursor getCursor() {
		return mCursor;
	}

	/*
	 * Setter methods.
	 */
	
	public void setCursor(Cursor cursor) {
		this.mCursor = cursor;
	}

}
