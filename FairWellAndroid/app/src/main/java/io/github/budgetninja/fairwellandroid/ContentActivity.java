package io.github.budgetninja.fairwellandroid;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import net.simonvt.menudrawer.MenuDrawer;

import java.util.ArrayList;
import java.util.List;

public class ContentActivity extends AppCompatActivity {

    private static final String STATE_ACTIVE_POSITION = "net.simonvt.menudrawer.samples.ContentActivity.activePosition";
    private static final String STATE_CONTENT_TEXT = "net.simonvt.menudrawer.samples.ContentActivity.contentText";
    public static final int POSITION_HOME = 0;
    private static final int POSITION_FRIENDS = 2;
    private static final int POSITION_SMART_SOLVE = 3;
    private static final int POSITION_ACCOUNT_SETTING = 5;
    private static final int POSITION_NOTIFICATION_SETTING = 6;
    private static final int POSITION_RATE_THIS_APP = 8;
    private static final int POSITION_ABOUT_US = 9;
    private static final int POSITION_LOGOUT = 10;
    public static final int INDEX_VIEW_STATEMENT = 11;
    public static final int INDEX_ADD_STATEMENT = 12;
    public static final int INDEX_RESOLVE_STATEMENT = 13;

    protected MenuDrawer mMenuDrawer;
    private MenuAdapter mAdapter;
    private ListView mList;
    private int mActivePosition = -1;
    private String mContentText;
    boolean doubleBackToExitPressedOnce = false;

    protected ConnectivityManager connectMgr;
    protected FragmentManager fragMgr;
    protected FragmentTransaction fragTrans;
    private ParseUser user;

    @Override
    protected void onCreate(Bundle inState) {
        super.onCreate(inState);
        connectMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        fragMgr = getSupportFragmentManager();
        user = ParseUser.getCurrentUser();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(isNetworkConnected()) {
                    Utility.generateRawFriendList(user);
                    Utility.generateFriendArray();
                }
                else { Utility.generateFriendArrayOffline();}
            }
        }).start();

        //ActionBar
        if(getSupportActionBar() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            getSupportActionBar().setElevation(0);
        }

        //SideMenu
        if (inState != null) {
            mActivePosition = inState.getInt(STATE_ACTIVE_POSITION);
            mContentText = inState.getString(STATE_CONTENT_TEXT);
        }
        mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_CONTENT);
        mMenuDrawer.setContentView(R.layout.activity_container);
        mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_FULLSCREEN);

        List<Object> items = new ArrayList<>();
        items.add(new Item(getString(R.string.home), R.drawable.ic_action_select_all_dark));
        items.add(new Category(getString(R.string.features)));
        items.add(new Item(getString(R.string.friends), R.drawable.ic_action_select_all_dark));
        items.add(new Item(getString(R.string.smart_solve), R.drawable.ic_action_select_all_dark));
        items.add(new Category(getString(R.string.setting)));
        items.add(new Item(getString(R.string.account_setting), R.drawable.ic_action_select_all_dark));
        items.add(new Item(getString(R.string.notification_setting), R.drawable.ic_action_select_all_dark));
        items.add(new Category(getString(R.string.others)));
        items.add(new Item(getString(R.string.rate_this_app), R.drawable.ic_action_select_all_dark));
        items.add(new Item(getString(R.string.about_us), R.drawable.ic_action_select_all_dark));
        items.add(new Item(getString(R.string.logout), R.drawable.ic_action_select_all_dark));

        mList = new ListView(this);
        mAdapter = new MenuAdapter(items);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(mItemClickListener);
        mMenuDrawer.setMenuView(mList);
        mMenuDrawer.setOnInterceptMoveEventListener(new MenuDrawer.OnInterceptMoveEventListener() {
            @Override
            public boolean isViewDraggable(View v, int dx, int x, int y) {
                return v instanceof SeekBar;
            }
        });

        //Prompt Facebook and Twitter User to setup email
        if(isNetworkConnected()) {
            if (user != null) {
                if (user.getEmail() == null) {
                    setEmailFacebookTwitterUser();
                }
            }
        }

        layoutManage(POSITION_HOME);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ACTIVE_POSITION, mActivePosition);
        outState.putString(STATE_CONTENT_TEXT, mContentText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    //MUST not put anything function for id equal to android.R.id.home or R.id.action_add_friend,
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onBackPressed() {
        final int drawerState = mMenuDrawer.getDrawerState();
        if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
            mMenuDrawer.closeMenu();
            return;
        }
        if (doubleBackToExitPressedOnce) {
            // this is to close the app entirely, but it will still be in the stack
            Intent a = new Intent(Intent.ACTION_MAIN);
            a.addCategory(Intent.CATEGORY_HOME);
            a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(a);
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    public void iconClick(View view){
        String clicked = "";
        if(view == findViewById(R.id.icon_1)){ clicked = "Food"; }
        else if(view == findViewById(R.id.icon_2)){ clicked = "Movie"; }
        else if(view == findViewById(R.id.icon_3)){ clicked = "Travel"; }
        else if(view == findViewById(R.id.icon_4)){ clicked = "Grocery"; }
        else if(view == findViewById(R.id.icon_5)){ clicked = "Utility"; }
        else if(view == findViewById(R.id.icon_6)){ clicked = "Entertainment"; }
        else if(view == findViewById(R.id.icon_7)){ clicked = "Other"; }
        ((AddStatementFragment)getSupportFragmentManager().findFragmentByTag("Add")).setClickedIconText(clicked);
    }

    protected void layoutManage(int index){
        fragTrans = fragMgr.beginTransaction();
        checkForUpdate();
        Fragment fragment;
        switch (index) {
            case POSITION_HOME:
                fragment = fragMgr.findFragmentByTag("Home");
                if(fragment != null){
                    if(fragment.isVisible()) { break; }
                }
                fragMgr.popBackStack("Home", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                fragTrans.replace(R.id.container, new HomepageFragment(), "Home").addToBackStack("Home");
                break;

            case POSITION_FRIENDS:
                fragment = fragMgr.findFragmentByTag("Friend");
                if(fragment != null){
                    if(fragment.isVisible()) { break; }
                }
                fragTrans.replace(R.id.container, new FriendsFragment(), "Friend").addToBackStack("Friend");
                break;

            case POSITION_SMART_SOLVE:
                fragment = fragMgr.findFragmentByTag("Solve");
                if(fragment != null){
                    if(fragment.isVisible()) { break; }
                }
                fragTrans.replace(R.id.container, new SmartSolveFragment(), "Solve").addToBackStack("Solve");
                break;

            case POSITION_ACCOUNT_SETTING:
                fragment = fragMgr.findFragmentByTag("Account");
                if(fragment != null){
                    if(fragment.isVisible()) { break; }
                }
                fragTrans.replace(R.id.container, new AccountSettingFragment(), "Account").addToBackStack("Account");
                break;

            case POSITION_NOTIFICATION_SETTING:
                fragment = fragMgr.findFragmentByTag("Notification");
                if(fragment != null){
                    if(fragment.isVisible()) { break; }
                }
                fragTrans.replace(R.id.container, new NotificationSettingFragment(), "Notification").addToBackStack("Notification");
                break;

            case INDEX_VIEW_STATEMENT:
                fragTrans.replace(R.id.container, new ViewStatementsFragment(), "View").addToBackStack("View");
                break;

            case INDEX_ADD_STATEMENT:
                fragTrans.replace(R.id.container, new AddStatementFragment(), "Add").addToBackStack("Add");
                break;

            case INDEX_RESOLVE_STATEMENT:
                fragTrans.replace(R.id.container, new ResolveStatementsFragment(), "Resolve").addToBackStack("Resolve");
                break;
        }
        fragTrans.commit();
    }

    protected boolean isNetworkConnected(){
        NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    protected void checkForUpdate(){        //Run by other Thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(isNetworkConnected()){
                    if(Utility.checkNewEntryField()){
                        Utility.generateRawFriendList(user);
                        Utility.setChangedRecord();
                    }
                }
            }
        }).start();
    }

    private void setEmailFacebookTwitterUser(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(ContentActivity.this);
        final LinearLayout layout = new LinearLayout(ContentActivity.this);
        final TextView message = new TextView(ContentActivity.this);
        final EditText userInput = new EditText(ContentActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        para.setMargins(20, 20, 20, 0);
        message.setText("An email address is required for some functionality. Please link your email address to the account.");
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        message.setLayoutParams(para);
        userInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        userInput.setLayoutParams(para);
        layout.addView(message);
        layout.addView(userInput);
        builder.setTitle("Link your Email");
        builder.setView(layout);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String email = userInput.getText().toString();
                user.setEmail(email);
                user.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e == null){
                            Toast.makeText(getApplicationContext(), "Success. A verification email was sent to " + email,
                                    Toast.LENGTH_SHORT).show();
                            Utility.setNewEntryFieldForAllFriend();
                            return;
                        }
                        Toast.makeText(getApplicationContext(), "Invalid Email Address", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Do it Later", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mActivePosition = position;
            mMenuDrawer.setActiveView(view, position);

            switch(position) {
                case POSITION_HOME:
                    layoutManage(position);
                    break;

                case POSITION_FRIENDS:
                    layoutManage(position);
                    break;

                case POSITION_SMART_SOLVE:
                    layoutManage(position);
                    break;

                case POSITION_ACCOUNT_SETTING:
                    layoutManage(position);
                    break;

                case POSITION_NOTIFICATION_SETTING:
                    layoutManage(position);
                    break;

                case POSITION_RATE_THIS_APP:
                    Toast.makeText(getApplicationContext(), "Coming soon!", Toast.LENGTH_SHORT).show();
                    break;

                case POSITION_ABOUT_US:
                    if(!isNetworkConnected()) {
                        Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    Intent i = new Intent();
                    i.setAction(Intent.ACTION_VIEW);
                    i.addCategory(Intent.CATEGORY_BROWSABLE);
                    i.setData(Uri.parse("http://budgetninja.github.io"));
                    startActivity(i);
                    break;

                case POSITION_LOGOUT:
                    if(!isNetworkConnected()) {
                        Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    ParseUser.logOutInBackground(new LogOutCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Intent intent = new Intent(ContentActivity.this, MainActivity.class);
                                ContentActivity.this.finish();
                                Utility.resetExistingFriendList();
                                Utility.setChangedRecord();
                                startActivity(intent);
                            } else {
                                Toast.makeText(getApplicationContext(), getString(R.string.logout_failed), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    break;
            }
            mMenuDrawer.closeMenu(false);
        }
    };

    //Related to Side-Menu
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
        public int getItemViewType(int position) { return getItem(position) instanceof Item ? 0 : 1; }

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
