package com.research.usage_stats.ui;

import android.annotation.SuppressLint;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.transition.Fade;
import android.util.Log;
import android.view.ContextMenu;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;


import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import com.research.usage_stats.FireBase;
import com.research.usage_stats.R;
import com.research.usage_stats.data.AppItem;
import com.research.usage_stats.data.DataManager;
import com.research.usage_stats.db.DbIgnoreExecutor;
import com.research.usage_stats.service.AlarmService;
import com.research.usage_stats.service.AppService;

import com.research.usage_stats.util.AppUtil;
import com.research.usage_stats.util.PreferenceManager;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private LinearLayout mSort;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch mSwitch;
    private TextView mSwitchText;
    private RecyclerView mList;
    private MyAdapter mAdapter;
    private AlertDialog mDialog;
    private SwipeRefreshLayout mSwipe;
    private TextView mSortName;
    private long mTotal;
    private int mDay;
    private PackageManager mPackageManager;
    public static int count=0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // https://guides.codepath.com/android/Shared-Element-Activity-Transition
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(new Fade(Fade.OUT));
        setContentView(R.layout.activity_main);



        mPackageManager = getPackageManager();
        mSort = findViewById(R.id.sort_group);
        mSortName = findViewById(R.id.sort_name);
        mSwitch = findViewById(R.id.enable_switch);
        mSwitchText = findViewById(R.id.enable_text);
        mAdapter = new MyAdapter();

        mList = findViewById(R.id.list);
        mList.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mList.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.divider, getTheme()));
        mList.addItemDecoration(dividerItemDecoration);
        mList.setAdapter(mAdapter);

        initLayout();
        initEvents();
        initSpinner();
        initSort();


        SharedPreferences sharedPreferences =getSharedPreferences(getString(R.string.launch_count),MODE_PRIVATE);
        SharedPreferences.Editor editor1= sharedPreferences.edit();

        int launch_count = sharedPreferences.getInt(getString(R.string.launch_count), 0);
        boolean isOk= sharedPreferences.getBoolean("AGREED",false);
        launch_count++;

        if(!isOk) {
            AlertDialog dialog= new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Disclaimer")
                    .setCancelable(false)
                    .setMessage("This app collects your phone usage statistics only for research purposes and does not hinder your privacy in any manner")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            editor1.putBoolean("AGREED",true).apply();
                            dialog.dismiss();

                        }
                    }).create();
            dialog.show();
        }

        editor1.putInt(getString(R.string.launch_count), launch_count).apply();


        SharedPreferences preferences= getSharedPreferences(getString(R.string.unlock_count),MODE_PRIVATE);
        count=preferences.getInt("UNLOCK_COUNT",0);
        if (DataManager.getInstance().hasPermission(getApplicationContext())) {
            mAuth = FirebaseAuth.getInstance();
            mAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull  Task<AuthResult> task) {
                    if(task.isSuccessful()){

                    }else{

                    }
                }
            });
            WorkManager workManager = WorkManager.getInstance(getApplicationContext());
            PeriodicWorkRequest workRequest= new PeriodicWorkRequest.Builder(FireBase.class, 24, TimeUnit.HOURS).build();
            workManager.enqueue(workRequest);
            process();
            startService(new Intent(this, AlarmService.class));
        }
    }
    private void initLayout() {
        mSwipe = findViewById(R.id.swipe_refresh);
        if (DataManager.getInstance().hasPermission(getApplicationContext())) {
            mSwitchText.setText(R.string.enable_apps_monitoring);
            mSwitch.setVisibility(View.GONE);
            mSort.setVisibility(View.VISIBLE);
            mSwipe.setEnabled(true);
        } else {
            mSwitchText.setText(R.string.enable_apps_monitor);
            mSwitch.setVisibility(View.VISIBLE);
            mSort.setVisibility(View.GONE);
            mSwitch.setChecked(false);
            mSwipe.setEnabled(false);
        }
    }

    private void initSort() {
        if (DataManager.getInstance().hasPermission(getApplicationContext())) {
            mSort.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    triggerSort();
                }
            });
        }
    }

    private void triggerSort() {
        mDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.sort)
                .setSingleChoiceItems(R.array.sort, PreferenceManager.getInstance().getInt(PreferenceManager.PREF_LIST_SORT), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        PreferenceManager.getInstance().putInt(PreferenceManager.PREF_LIST_SORT, i);
                        process();
                        mDialog.dismiss();
                    }
                })
                .create();
        mDialog.show();
    }

    private void initSpinner() {
        if (DataManager.getInstance().hasPermission(getApplicationContext())) {
            Spinner spinner = findViewById(R.id.spinner);
            spinner.setVisibility(View.VISIBLE);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.duration, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if (mDay != i) {
                        int[] values = getResources().getIntArray(R.array.duration_int);
                        mDay = values[i];
                        process();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
        }
    }

    private void initEvents() {
        if (!DataManager.getInstance().hasPermission(getApplicationContext())) {
            mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {

                        Intent intent = new Intent(MainActivity.this, AppService.class);
                        intent.putExtra(AppService.SERVICE_ACTION, AppService.SERVICE_ACTION_CHECK);
                        startService(intent);
                    }
                }
            });
        }
        mSwipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                process();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!DataManager.getInstance().hasPermission(getApplicationContext())) {
            mSwitch.setChecked(false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (DataManager.getInstance().hasPermission(this)) {
            mSwipe.setEnabled(true);
            mSort.setVisibility(View.VISIBLE);
            mSwitch.setVisibility(View.GONE);
            initSpinner();
            initSort();
            process();
        }
    }

    public void process() {
        if (DataManager.getInstance().hasPermission(getApplicationContext())) {
            mList.setVisibility(View.INVISIBLE);
            int sortInt = PreferenceManager.getInstance().getInt(PreferenceManager.PREF_LIST_SORT);
            mSortName.setText(getSortName(sortInt));
            new MyAsyncTask().execute(sortInt, mDay);
        }
    }

    private String getSortName(int sortInt) {
//        return getResources().getStringArray(R.array.sort)[sortInt];
        return getResources().getString(R.string.sort_by);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AppItem info = mAdapter.getItemInfoByPosition(item.getOrder());
        switch (item.getItemId()) {
            case R.id.ignore:
                DbIgnoreExecutor.getInstance().insertItem(info);
                process();
                Toast.makeText(this, R.string.ignore_success, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.open:
                startActivity(mPackageManager.getLaunchIntentForPackage(info.mPackageName));
                return true;
            case R.id.more:
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + info.mPackageName));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), 1);
                return true;
            case R.id.sort:
                triggerSort();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode > 0) process();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null) mDialog.dismiss();

    }


    class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        public  List<AppItem> mData;

        MyAdapter() {
            super();
            mData = new ArrayList<>();
        }

        void updateData(List<AppItem> data) {
            mData = data;
            notifyDataSetChanged();
        }


        AppItem getItemInfoByPosition(int position) {
            if (mData.size() > position) {
                return mData.get(position);
            }
            return null;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            AppItem item = getItemInfoByPosition(position);
            holder.mName.setText(item.mName);
            holder.mUsage.setText(AppUtil.formatMilliSeconds(item.mUsageTime));
            holder.mTime.setText(String.format(Locale.getDefault(),
                    "%s · %d %s",
                    new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(new Date(item.mEventTime)),
                    item.mCount,
                    getResources().getString(R.string.times_only))
            );
            holder.mDataUsage.setText(String.format(Locale.getDefault(), "%s", AppUtil.humanReadableByteCount(item.mMobile)));
            if (mTotal > 0) {
                holder.mProgress.setProgress((int) (item.mUsageTime * 100 / mTotal));
            } else {
                holder.mProgress.setProgress(0);
            }
            Glide.with(MainActivity.this)
                    .load(AppUtil.getPackageIcon(MainActivity.this,item.mPackageName))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(holder.mIcon);

            holder.setOnClickListener(item);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

            private TextView mName;
            private TextView mUsage;
            private TextView mTime;
            private TextView mDataUsage;
            private ImageView mIcon;
            private ProgressBar mProgress;

            MyViewHolder(View itemView) {
                super(itemView);
                mName = itemView.findViewById(R.id.app_name);
                mUsage = itemView.findViewById(R.id.app_usage);
                mTime = itemView.findViewById(R.id.app_time);
                mDataUsage = itemView.findViewById(R.id.app_data_usage);
                mIcon = itemView.findViewById(R.id.app_image);
                mProgress = itemView.findViewById(R.id.progressBar);
                itemView.setOnCreateContextMenuListener(this);
            }

            @SuppressLint("RestrictedApi")
            void setOnClickListener(final AppItem item) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                        intent.putExtra(DetailActivity.EXTRA_PACKAGE_NAME, item.mPackageName);
                        intent.putExtra(DetailActivity.EXTRA_DAY, mDay);
                        ActivityOptionsCompat options = ActivityOptionsCompat.
                                makeSceneTransitionAnimation(MainActivity.this, mIcon, "profile");
                        startActivityForResult(intent, 1, options.toBundle());
                    }
                });
            }

            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                int position = getAdapterPosition();
                AppItem item = getItemInfoByPosition(position);
                contextMenu.setHeaderTitle(item.mName);
                contextMenu.add(Menu.NONE, R.id.open, position, getResources().getString(R.string.open));
                if (item.mCanOpen) {
                    contextMenu.add(Menu.NONE, R.id.more, position, getResources().getString(R.string.app_info));
                }
                contextMenu.add(Menu.NONE, R.id.ignore, position, getResources().getString(R.string.ignore));
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class MyAsyncTask extends AsyncTask<Integer, Void, List<AppItem>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSwipe.setRefreshing(true);
        }

        @Override
        protected List<AppItem> doInBackground(Integer... integers) {
            return DataManager.getInstance().getApps(getApplicationContext(), integers[0], integers[1]);
        }

        @Override
        protected void onPostExecute(List<AppItem> appItems) {
            mList.setVisibility(View.VISIBLE);
            mTotal = 0;
            for (AppItem item : appItems) {
                if (item.mUsageTime <= 0) continue;
                mTotal += item.mUsageTime;
                item.mCanOpen = mPackageManager.getLaunchIntentForPackage(item.mPackageName) != null;
            }
            mSwitchText.setText(String.format(getResources().getString(R.string.total), AppUtil.formatMilliSeconds(mTotal)));
            mSwipe.setRefreshing(false);
            mAdapter.updateData(appItems);
        }
    }
}
