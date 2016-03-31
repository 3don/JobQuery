package com.example.jobquery;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener;

import com.Upwork.api.Config;
import com.Upwork.api.OAuthClient;
import com.Upwork.api.Routers.Organization.Users;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.zip.Inflater;

import oauth.signpost.OAuth;

public class JobQueryList extends ListActivity {

    private final static String CONSUMER_KEY="";
    private final static String SECRET_KEY="";
    private final static String OAUTH_CALLBACK_SCHEME = "x-oauthflow";
    private OAuthClient client;
    SharedPreferences prefs;
    static ArrayList<JobQuery> jobQueryList;
    private final static String TAG = "myLog";
    private ListView jobQueryListView;
    String authUrl;
    private File jobQueryFile;
    private JobQueryAdapter jobQueryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onNewIntent(getIntent());

        jobQueryListView = getListView();
        setContentView(R.layout.activity_job_query_list);
        Button addQueryButton = (Button) findViewById(R.id.addQueryButton);
        addQueryButton.setOnClickListener(addNewQueryButtonListener);
        jobQueryFile = new File(getExternalFilesDir(null).getAbsolutePath()+"/JobQueryData.ser");
        new LoadJobQueryTask().execute((Object[]) null);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String token = prefs.getString("token", null);
        String tokenSecret = prefs.getString("secret", null);

        if (token != null && tokenSecret != null && client != null) {
            client.setTokenWithSecret(token, tokenSecret);
        } else {
            if (client == null) {
                //NOTE: KEY/SECRET PAIR MUST BE STORED IN A SAFE PLACE
                //THIS PART OF ASSIGNING OF CONSUMER KEY IS AN EXAMPLE
                Properties props = new Properties();
                props.setProperty("consumerKey", CONSUMER_KEY);
                props.setProperty("consumerSecret", SECRET_KEY);

                Config config = new Config(props);

                client = new OAuthClient(config);

                if (token!=null)
                Log.e(TAG, token);
            }
            // authorize
           // new ODeskAuthorizeTask().execute();
        }
    }

    private class LoadJobQueryTask extends AsyncTask<Object, Object, Object>{
        @Override
        protected Object doInBackground(Object... params) {
            if (jobQueryFile.exists()){
                try{
                    ObjectInputStream input = new ObjectInputStream(new FileInputStream(jobQueryFile));
                    jobQueryList = (ArrayList<JobQuery>) input.readObject();
                }
                catch (final Exception e){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(JobQueryList.this, "Не загружен список запросов",Toast.LENGTH_SHORT).show();
                            Log.e(TAG, e.toString());
                        }
                    });
                }
            }
            if (jobQueryList == null) jobQueryList = new ArrayList<JobQuery>();
            return (Object) null;
        }
        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            jobQueryAdapter = new JobQueryAdapter(JobQueryList.this, jobQueryList);
            jobQueryListView.setAdapter(jobQueryAdapter);

        }
    }

    public OnClickListener addNewQueryButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            openAddDialog();
        }
    };

    public void openAddDialog(){
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.new_job_query, null);

        AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
        inputDialog.setView(view);
        inputDialog.setTitle(R.string.input_dialog_title);
        final EditText jobQueryNameEditText = (EditText) view.findViewById(R.id.nameEditText);
        final EditText jobQueryKeyEditText = (EditText) view.findViewById(R.id.keyWordEditText);
        inputDialog.setPositiveButton(R.string.add_job_query_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = jobQueryNameEditText.getText().toString();
                String key = jobQueryKeyEditText.getText().toString();
                Log.e(TAG, name + " " + key);
                saveQuery(name, key);
            }
        });
        inputDialog.setNegativeButton(R.string.cancel_button, null);
        inputDialog.show();
    }

    public void openEditDialog(ViewParent v){
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.new_job_query, null);

        AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
        inputDialog.setView(view);
        inputDialog.setTitle(R.string.input_dialog_title);
        final EditText jobQueryNameEditText = (EditText) view.findViewById(R.id.nameEditText);
        final EditText jobQueryKeyEditText = (EditText) view.findViewById(R.id.keyWordEditText);
        inputDialog.setPositiveButton(R.string.add_job_query_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = jobQueryNameEditText.getText().toString();
                String key = jobQueryKeyEditText.getText().toString();
                Log.e(TAG, name + " " + key);
                saveQuery(name, key);
            }
        });
        inputDialog.setNeutralButton(R.string.delete_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                jobQueryList.remove(getJobQueryByName(jobQueryNameEditText.getText().toString()));
                //jobQueryList.remove((JobQuery) v.getTag());
                jobQueryAdapter.notifyDataSetChanged();
                new SaveJobQueryTask().execute((Object[]) null);
            }
        });
        inputDialog.setNegativeButton(R.string.cancel_button, null);
        inputDialog.show();
    }

    public void saveQuery(String name, String key) {
         if (name.length() != 0 && key.length() != 0) {
            jobQueryList.add(new JobQuery(name, key));
            jobQueryAdapter.notifyDataSetChanged();
            Log.e(TAG, "adapter was notified");
            new SaveJobQueryTask().execute((Object[]) null);
        }
    }
    
    private class SaveJobQueryTask extends AsyncTask<Object, Object, Object> {
        @Override
        protected Object doInBackground(Object... arg0){
            try{
                if (!jobQueryFile.exists())
                    jobQueryFile.createNewFile();
                
                ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(jobQueryFile));
                output.writeObject(jobQueryFile);
                output.close();
            }
            catch (final Exception e){
                runOnUiThread(new Runnable(){
                    public void run(); {
                        Toast.makeText(JobQueryList.this, "Файл сохранен", Toast.Length_LONG).show();
                    }   
                });
            }
            return (Object) null;
        }
    }

    class ODeskAuthorizeTask extends AsyncTask<Void, Void, String> {

        private Exception exception;

        @Override
        protected String doInBackground(Void... params) {

            //String authzUrl = client.getAuthorizationUrl();
            // if your api key type is 'mobile', possibly you want to use
            // oauth_callback in your application, then use
            String authzUrl = client.getAuthorizationUrl("x-oauthflow://callback");

            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authzUrl)));
            getIntent().setData(Uri.parse(authzUrl));
            Log.e(TAG, getIntent().getDataString());
            return authzUrl;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                Toast.makeText(JobQueryList.this, result, Toast.LENGTH_LONG).show();
            }
        }
    }

    class ODeskRetrieveAccessTokenTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String verifier = params[0];

            HashMap<String, String> token = client.getAccessTokenSet(verifier);
            client.setTokenWithSecret(token.get("token"), token.get("secret"));

            // Save token/secret in preferences
            prefs.edit().putString("token", token.get("token"))
                    .putString("secret", token.get("secret"))
                    .commit();

            return "ok - token is: " + prefs.getString("token", null);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                Toast.makeText(JobQueryList.this, result, Toast.LENGTH_LONG).show();
                Log.e(TAG, result);
            }
        }
    }

    class GetMyUIdTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            JSONObject json = null;
            String token = prefs.getString("token", null);
            Users users = new Users(client);

            String myId = "";
            try {
                json = users.getMyInfo();
                JSONObject user = json.getJSONObject("user");
                myId = user.getString("id");
            }
            catch (JSONException e) {
                e.printStackTrace();
            }

            return "Your UID is " + myId;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                Toast.makeText(JobQueryList.this, result, Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_job_query_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if (uri!=null)
            Log.e(TAG, uri.toString());
        else Log.e(TAG,"URI - null");

        if (uri != null && uri.getScheme().equals(OAUTH_CALLBACK_SCHEME)) {
            String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);

            new ODeskRetrieveAccessTokenTask().execute(verifier);
        }
    }

    private static class ViewHolder {
        TextView nameTextView;
        TextView newQueryTextView;
        Button editButton;
    }

    private class JobQueryAdapter extends ArrayAdapter<JobQuery>{
        private ArrayList<JobQuery> items;
        private LayoutInflater inflater;

        public JobQueryAdapter(Context context, ArrayList<JobQuery> items){
            super(context,-1,items);
            this.items = items;
            inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            Log.e(TAG, "Created adapter");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.e(TAG, "start getView");
            ViewHolder viewHolder;
            if (convertView==null){
                convertView = inflater.inflate(R.layout.job_query_item, null);
                viewHolder = new ViewHolder();
                viewHolder.nameTextView = (TextView) convertView.findViewById(R.id.jobQueryNameTextView);
                viewHolder.newQueryTextView = (TextView) convertView.findViewById(R.id.newJobOffersTextView);
                viewHolder.editButton = (Button) convertView.findViewById(R.id.editQueryButton);
                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();
            JobQuery jobQuery = items.get(position);
            viewHolder.nameTextView.setText(jobQuery.getJobQueryName());
            viewHolder.editButton.setTag(jobQuery);
            viewHolder.editButton.setOnClickListener(editButtonListener);

            return convertView;
        }
    }

    OnClickListener editButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            openEditDialog(v.getParent());
        }
    };
}
