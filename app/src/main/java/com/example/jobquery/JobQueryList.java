package com.example.jobquery;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.Upwork.api.Config;
import com.Upwork.api.OAuthClient;
import com.Upwork.api.Routers.Organization.Users;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Properties;
import oauth.signpost.OAuth;

public class JobQueryList extends Activity {

    private final static String CONSUMER_KEY="";
    private final static String SECRET_KEY="";
    private final static String OAUTH_CALLBACK_SCHEME = "x-oauthflow";
    private OAuthClient client;
    SharedPreferences prefs;
    private final static String TAG = "myLog";
    String authUrl;
    private Button addQueryButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onNewIntent(getIntent());
        setContentView(R.layout.activity_job_query_list);
        if (savedInstanceState == null) {
        //    getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
        }
        addQueryButton = (Button) findViewById(R.id.add_query_button);
        addQueryButton.setOnClickListener(addQueryButtonListener);

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
            new ODeskAuthorizeTask().execute();
        }
    }

    View.OnClickListener addQueryButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openAddDialog();
        }
    };

    private void openAddDialog(){
    }

  /*  public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_my, container, false);
            return rootView;
        }
    }
*/
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
}
