package xyz.monkeytong.hongbao.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import xyz.monkeytong.hongbao.BuildConfig;
import xyz.monkeytong.hongbao.R;

/**
 * Created by Zhongyi on 1/20/16.
 * Util for app update task.
 */
public class UpdateTask extends AsyncTask<String, String, UpdateTask.ReleaseApp> {

    public static final String updateUrl = "https://api.github.com/repos/kingwang666/WeChatLuckyMoney/releases/latest";

    public static boolean sHomeRequest = false;

    private final boolean showDialog;
    private final WeakReference<Context> context;


    public UpdateTask(Context context, boolean showDialog) {
        super();
        this.showDialog = showDialog;
        this.context = new WeakReference<>(context);
        if (this.showDialog) {
            Toast.makeText(context, context.getString(R.string.checking_new_version), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected ReleaseApp doInBackground(String... uri) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(uri[0])
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (isCancelled()) {
                    return null;
                }
                ResponseBody body = response.body();
                if (response.code() == 200 && body != null) {
                    String json = body.string();
                    JSONObject release = new JSONObject(json);
                    ReleaseApp app = new ReleaseApp();
                    if (release.isNull("tag_name")) {
                        return null;
                    }
                    app.latestVersion = release.getString("tag_name");
                    if (!release.isNull("prerelease")) {
                        app.isPreRelease = release.getBoolean("prerelease");
                    }
                    if (!release.isNull("body")) {
                        app.body = HtmlCompat.fromHtml(release.getString("body").replace("\n", "<p>"), HtmlCompat.FROM_HTML_MODE_COMPACT);
                    }
                    JSONArray assets;
                    if (!release.isNull("assets") && (assets = release.getJSONArray("assets")).length() > 0) {
                        JSONObject asset = assets.getJSONObject(0);
                        if (asset != null && !asset.isNull("browser_download_url")) {
                            app.downloadUrl = asset.getString("browser_download_url");
                        }
                    }
                    return app;
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(ReleaseApp app) {
        Context context = this.context.get();
        if (context == null) {
            return;
        }
        if (app == null) {
            if (this.showDialog) {
                Toast.makeText(context, R.string.update_error, Toast.LENGTH_LONG).show();
            }
            return;
        }
        sHomeRequest = true;
        if (BuildConfig.VERSION_NAME.compareToIgnoreCase(app.latestVersion) >= 0) {
            // Your version is ahead of or same as the latest.
            if (this.showDialog)
                Toast.makeText(context, R.string.update_already_latest, Toast.LENGTH_SHORT).show();
        } else {
            if (!showDialog) {
                Toast.makeText(context, context.getString(R.string.update_new_seg1) + app.latestVersion + context.getString(R.string.update_new_seg3), Toast.LENGTH_LONG).show();
                return;
            }
            if (TextUtils.isEmpty(app.downloadUrl)) {
                Toast.makeText(context, R.string.update_error, Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(app.isPreRelease ? R.string.new_version : R.string.new_version_prerelease, app.latestVersion))
                    .setMessage(app.body)
                    .setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(app.downloadUrl));
                            browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(browserIntent);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }

    }

    public void update() {
        super.execute(updateUrl);
    }

    static class ReleaseApp {
        public String latestVersion;
        public boolean isPreRelease;
        public CharSequence body;
        public String downloadUrl;
    }
}