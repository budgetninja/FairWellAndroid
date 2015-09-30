package io.github.budgetninja.fairwellandroid;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

import net.simonvt.menudrawer.MenuDrawer;

import java.util.ArrayList;
import java.util.List;

import static io.github.budgetninja.fairwellandroid.Utility.isFacebookUser;
import static io.github.budgetninja.fairwellandroid.Utility.isTwitterUser;

//Do not modify this code - it is part of the side panel.

public class ContentActivity extends AppCompatActivity {

    private static final String STATE_ACTIVE_POSITION = "net.simonvt.menudrawer.samples.ContentActivity.activePosition";
    private static final String STATE_CONTENT_TEXT = "net.simonvt.menudrawer.samples.ContentActivity.contentText";
    private static final int INDEX_VIEW_STATEMENT = 1;
    private static final int INDEX_ADD_STATEMENT = 2;
    private static final int INDEX_RESOLVE_STATEMENT = 3;
    private static final int POSITION_HOME = 0;
    private static final int POSITION_FEATURES = 1;
    private static final int POSITION_FRIENDS = 2;
    private static final int POSITION_ADD_FRIEND = 3;
    private static final int POSITION_SETTING = 4;
    private static final int POSITION_ACCOUNT_SETTING = 5;
    private static final int POSITION_NOTIFICATION_SETTING = 6;
    private static final int POSITION_OTHERS = 7;
    private static final int POSITION_RATE_THIS_APP = 8;
    private static final int POSITION_CONTACT_US = 9;
    private static final int POSITION_LOGOUT = 10;
    private MenuDrawer mMenuDrawer;
    private MenuAdapter mAdapter;
    private ListView mList;
    private int mActivePosition = -1;
    private String mContentText;
    private TextView mContentTextView;

    private ParseUser user;

    @Override
    protected void onCreate(Bundle inState) {
        super.onCreate(inState);
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.nav_icon);
        user = ParseUser.getCurrentUser();

        if (inState != null) {
            mActivePosition = inState.getInt(STATE_ACTIVE_POSITION);
            mContentText = inState.getString(STATE_CONTENT_TEXT);
        }

        mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_CONTENT);
        mMenuDrawer.setContentView(R.layout.activity_content);
        mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_FULLSCREEN);

        List<Object> items = new ArrayList<Object>();
        items.add(new Item(getString(R.string.home), R.drawable.ic_action_select_all_dark));
        items.add(new Category(getString(R.string.features)));
        items.add(new Item(getString(R.string.friends), R.drawable.ic_action_select_all_dark));
        items.add(new Item(getString(R.string.add_friend), R.drawable.ic_action_select_all_dark));
        items.add(new Category(getString(R.string.setting)));
        items.add(new Item(getString(R.string.account_setting), R.drawable.ic_action_refresh_dark));
        items.add(new Item(getString(R.string.notification_setting), R.drawable.ic_action_select_all_dark));
        items.add(new Category(getString(R.string.others)));
        items.add(new Item(getString(R.string.rate_this_app), R.drawable.ic_action_refresh_dark));
        items.add(new Item(getString(R.string.contact_us), R.drawable.ic_action_select_all_dark));
        items.add(new Item(getString(R.string.logout), R.drawable.ic_action_select_all_dark));

        mList = new ListView(this);
        mAdapter = new MenuAdapter(items);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(mItemClickListener);

        mMenuDrawer.setMenuView(mList);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);          //Uncommented
        }

        mContentTextView = (TextView) findViewById(R.id.contentText);
        mContentTextView.setText(mContentText);

        mMenuDrawer.setOnInterceptMoveEventListener(new MenuDrawer.OnInterceptMoveEventListener() {
            @Override
            public boolean isViewDraggable(View v, int dx, int x, int y) {
                return v instanceof SeekBar;
            }
        });

//3 Button Functions
        Button addStatementButton = (Button) findViewById(R.id.addStatementButton);
        addStatementButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(ContentActivity.this, ContainerActivity.class);
                i.putExtra("Index", INDEX_ADD_STATEMENT);
                startActivity(i);
            }
        });

        Button resolveStatementButton = (Button) findViewById(R.id.resolveStatementButton);
        resolveStatementButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(ContentActivity.this, ContainerActivity.class);
                i.putExtra("Index", INDEX_RESOLVE_STATEMENT);
                startActivity(i);
            }
        });

        Button viewStatementButton = (Button) findViewById(R.id.viewStatementButton);
        viewStatementButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(ContentActivity.this, ContainerActivity.class);
                i.putExtra("Index", INDEX_VIEW_STATEMENT);
                startActivity(i);
            }
        });

        //Display Full Name
        TextView name = (TextView) findViewById(R.id.name);
        String nameString;
        if(isFacebookUser(user)){
            nameString = user.getString("usernameFacebook");
        }else if(isTwitterUser(user)){
            nameString = user.getString("usernameTwitter");
        }else {
            nameString = user.getString("First_Name") + " " + user.getString("Last_Name");
        }
        name.setText(nameString);

    }

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mActivePosition = position;
            mMenuDrawer.setActiveView(view, position);
            mContentTextView.setText(((TextView) view).getText());
            mMenuDrawer.closeMenu();
            //Toast.makeText(getApplicationContext(),((TextView) view).getText(), Toast.LENGTH_SHORT).show();

            //Logout Function
            //if(((Item)mAdapter.getItem(position)).mTitle.equals("Logout")){
            switch(position) {
                case POSITION_LOGOUT:
                    ParseUser.logOutInBackground(new LogOutCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Intent intent = new Intent(ContentActivity.this, MainActivity.class);
                                ContentActivity.this.finish();
                                startActivity(intent);
                            } else {
                                Toast.makeText(getApplicationContext(), getString(R.string.logout_failed), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    break;
                case POSITION_FRIENDS:
                    Intent intent = new Intent(ContentActivity.this,FriendsActivity.class);
                    startActivity(intent);
                    break;
                default:
                    break;

            }
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ACTIVE_POSITION, mActivePosition);
        outState.putString(STATE_CONTENT_TEXT, mContentText);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mMenuDrawer.toggleMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        final int drawerState = mMenuDrawer.getDrawerState();
        if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
            mMenuDrawer.closeMenu();
            return;
        }
        super.onBackPressed();
    }

    private static class Item {
        String mTitle;
        int mIconRes;

        Item(String title, int iconRes) {
            mTitle = title;
            mIconRes = iconRes;
        }
    }

    private static class Category {
        String mTitle;

        Category(String title) {
            mTitle = title;
        }
    }

    private class MenuAdapter extends BaseAdapter {
        private List<Object> mItems;

        MenuAdapter(List<Object> items) {
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position) instanceof Item ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position) instanceof Item;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            Object item = getItem(position);

            if (item instanceof Category) {
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.menu_row_category, parent, false);
                }
                ((TextView) v).setText(((Category) item).mTitle);
            } else {
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.menu_row_item, parent, false);
                }
                TextView tv = (TextView) v;
                tv.setText(((Item) item).mTitle);
                tv.setCompoundDrawablesWithIntrinsicBounds(((Item) item).mIconRes, 0, 0, 0);
            }

            v.setTag(R.id.mdActiveViewPosition, position);

            if (position == mActivePosition) {
                mMenuDrawer.setActiveView(v, position);
            }

            return v;
        }
    }
}
