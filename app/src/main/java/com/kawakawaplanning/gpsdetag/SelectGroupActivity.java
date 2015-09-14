package com.kawakawaplanning.gpsdetag;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.kawakawaplanning.gpsdetag.http.HttpConnector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SelectGroupActivity extends AppCompatActivity {


    private String myId;
    private ListView lv;
    private ProgressDialog waitDialog;
    private Handler mHandler;
    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        pref = getSharedPreferences("loginpref", Activity.MODE_MULTI_PROCESS );
        myId = pref.getString("loginid", "");
        mHandler = new Handler();
        setScreenContent(R.layout.activity_select_group);
        keyEventTimer = new CountDownTimer(1000, 100) {

            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {

                pressed = false;
            }
        };


    }

    public void logout(View v){
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("username", "");
        editor.putString("password", "");
        editor.putBoolean("AutoLogin", false);
        editor.apply();

        finish();
    }

    List<Map<String, String>> list;

    private void listLoad() {
        Wait("グループ読み込み");
        list = new ArrayList();

        HttpConnector httpConnector = new HttpConnector("getgroup", "{\"user_id\":\"" + myId + "\"}");
        httpConnector.setOnHttpResponseListener((String message) -> {
            waitDialog.dismiss();
            try {
                if (!message.equals("notfound")) {
                    JSONObject json = new JSONObject(message);
                    JSONArray data = json.getJSONArray("data");

                    for (int i = 0; i != data.length(); i++) {
                        JSONObject object = data.getJSONObject(i);//ノットファウンド
                        Map<String, String> conMap = new HashMap<>();
                        conMap.put("Name", object.getString("group_name"));
                        conMap.put("Member", "グループID:" + object.getString("group_id"));
                        list.add(conMap);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mHandler.post(() -> {
                SimpleAdapter adapter = new SimpleAdapter(SelectGroupActivity.this, list, android.R.layout.simple_list_item_2, new String[]{"Name", "Member"}, new int[]{android.R.id.text1, android.R.id.text2});
                lv.setAdapter(adapter);
                lv.setOnItemClickListener(onItem);
                lv.setOnItemLongClickListener(onItemLong);
            });
        });
        httpConnector.setOnHttpErrorListener((int error) -> {
            waitDialog.dismiss();
            android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
            adb.setTitle("接続エラー");
            adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
            adb.setPositiveButton("OK", null);
            adb.setCancelable(true);
            adb.show();
        });
        httpConnector.post();
    }

    private CountDownTimer keyEventTimer;
    private boolean pressed = false;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {

            switch(mScreenId){
                case R.layout.activity_select_group:
                    if(!pressed) {
                        keyEventTimer.cancel();
                        keyEventTimer.start();
                        Toast.makeText(this, "終了する場合は、もう一度バックボタンを押してください", Toast.LENGTH_SHORT).show();
                        pressed = true;
                        return false;
                    }
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("username", "");
                    editor.putString("password", "");
                    editor.putBoolean("AutoLogin", false);
                    editor.apply();
                    break;
                case R.layout.activity_member_wait:
                    setScreenContent(R.layout.activity_select_group);
                    break;
            }



            return super.dispatchKeyEvent(event);
        }
        return super.dispatchKeyEvent(event);
    }



    private AdapterView.OnItemClickListener onItem = (AdapterView<?> parent, View view, int position, long id) -> {
            Map<String, String> map = (Map<String, String>) parent.getAdapter().getItem(position);

            SharedPreferences pref = getSharedPreferences("loginpref", Activity.MODE_MULTI_PROCESS);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("groupId", map.get("Member").substring(7));
            editor.apply();

        setScreenContent(R.layout.activity_member_wait);
    };

    private AdapterView.OnItemLongClickListener onItemLong = new AdapterView.OnItemLongClickListener(){

        @Override
        public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {
            AlertDialog.Builder adb = new AlertDialog.Builder(SelectGroupActivity.this);
            adb.setCancelable(true);
            adb.setTitle("確認");
            adb.setMessage("このグループを削除しますか？");
            adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Wait("処理");
                    Map<String, String> map = (Map<String, String>) parent.getAdapter().getItem(position);
                    HttpConnector httpConnector = new HttpConnector("outgroup","{\"user_id\":\""+myId+"\",\"group_id\":\""+map.get("Member").substring(7)+"\"}");
                    httpConnector.setOnHttpResponseListener((String message) -> {
                        Log.v("tag", message);
                        waitDialog.dismiss();
                        if (Integer.parseInt(message) == 0) {
                            Toast.makeText(getApplicationContext(),"削除しました",Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(),"エラーが発生しました。時間を開けて試してください。それでもダメな場合はサポートに連絡してください。",Toast.LENGTH_SHORT).show();
                        }
                        listLoad();
                    });
                    httpConnector.setOnHttpErrorListener((int error) -> {
                        waitDialog.dismiss();
                        android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                        adb.setTitle("接続エラー");
                        adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
                        adb.setPositiveButton("OK", null);
                        adb.setCancelable(true);
                        adb.show();
                    });
                    httpConnector.post();
                }
            });
            adb.setNegativeButton("Cancel", null);
            AlertDialog ad = adb.create();
            ad.show();
            return true;
        }
    };

    public void makeGroup(View v){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(
                LAYOUT_INFLATER_SERVICE);
        View view =  inflater.inflate(R.layout.dialog_et1,
                (ViewGroup)findViewById(R.id.dialog_layout));

        final EditText et1 = (EditText)view.findViewById(R.id.editText1);
        final TextView tv1 = (TextView)view.findViewById(R.id.dig_tv1);

        tv1.setText("作成するグループ名を入力してください");

        alertDialogBuilder.setTitle("グループ作成");
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setPositiveButton("OK",(DialogInterface dialog, int which) -> {
            final String str = et1.getEditableText().toString();
            if (!str.isEmpty()) {
                HttpConnector httpConnector = new HttpConnector("makegroup","{\"user_id\":\""+myId+"\",\"group_name\":\""+str+"\"}");
                httpConnector.setOnHttpResponseListener((String message) -> {
                    AlertDialog.Builder adb = new AlertDialog.Builder(SelectGroupActivity.this);
                    adb.setCancelable(true);
                    adb.setTitle("グループ作成完了");
                    adb.setMessage("グループの作成が完了しました。友達を早速誘おう！グループIDは「" + message + "」です。");
                    adb.setPositiveButton("OK", null);
                    AlertDialog ad = adb.create();
                    ad.show();
                    listLoad();
                });
                httpConnector.setOnHttpErrorListener((int error) -> {
                    android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                    adb.setTitle("接続エラー");
                    adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
                    adb.setPositiveButton("OK", null);
                    adb.setCancelable(true);
                    adb.show();
                });
                httpConnector.post();
            }
        });
        alertDialogBuilder.setCancelable(true);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void inGroup(View v){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(
                LAYOUT_INFLATER_SERVICE);
        View view =  inflater.inflate(R.layout.dialog_et1,
                (ViewGroup)findViewById(R.id.dialog_layout));

        final EditText et1 = (EditText)view.findViewById(R.id.editText1);
        final TextView tv1 = (TextView)view.findViewById(R.id.dig_tv1);

        tv1.setText("ログインしたいグループIDを入力してください");

        alertDialogBuilder.setTitle("グループログイン");
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setPositiveButton("OK", null);
        alertDialogBuilder.setCancelable(true);

        alertDialogBuilder.setPositiveButton("OK", (DialogInterface dialog, int which) -> {
            final String str = et1.getEditableText().toString();
            if (!str.isEmpty()) {
                Wait("グループ検索");

                HttpConnector httpConnector = new HttpConnector("getgroup", "{\"user_id\":\"" + myId + "\"}");
                httpConnector.setOnHttpResponseListener((String jsonData) -> {

                    if (!jsonData.equals("notfound")) {
                        waitDialog.dismiss();
                        try {
                            JSONObject json = new JSONObject(jsonData);
                            JSONArray data = json.getJSONArray("data");
                            boolean flag = true;
                            for (int i = 0; i != data.length(); i++) {

                                JSONObject object = data.getJSONObject(i);
                                if (object.getString("group_id").equals(str)) {
                                    flag = false;
                                }
                            }
                            if (flag) {
                                HttpConnector httpCon = new HttpConnector("ingroup", "{\"user_id\":\"" + myId + "\",\"group_id\":\"" + str + "\"}");
                                httpCon.setOnHttpResponseListener((String message) -> {
                                    if (Integer.parseInt(message) == 0) {
                                        AlertDialog.Builder adb = new AlertDialog.Builder(SelectGroupActivity.this);
                                        adb.setCancelable(true);
                                        adb.setTitle("グループ加入完了");
                                        adb.setMessage("グループへの加入が完了しました。さっそくグループを選択して遊ぼう！");
                                        adb.setPositiveButton("OK", null);
                                        AlertDialog ad = adb.create();
                                        ad.show();
                                        listLoad();
                                    } else {
                                        AlertDialog.Builder adb = new AlertDialog.Builder(SelectGroupActivity.this);
                                        adb.setCancelable(true);
                                        adb.setTitle("エラー");
                                        adb.setMessage("グループが見つかりませんでした。グループIDを確認してもう一度お試しください。");
                                        adb.setPositiveButton("OK", null);
                                        AlertDialog ad = adb.create();
                                        ad.show();
                                    }
                                });
                                httpCon.setOnHttpErrorListener((int error) -> {
                                    android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                                    adb.setTitle("接続エラー");
                                    adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
                                    adb.setPositiveButton("OK", null);
                                    adb.setCancelable(true);
                                    adb.show();
                                });
                                httpCon.post();
                            } else {
                                AlertDialog.Builder adb = new AlertDialog.Builder(SelectGroupActivity.this);
                                adb.setCancelable(true);
                                adb.setTitle("エラー");
                                adb.setMessage("すでに加入しています。グループIDを確認してもう一度お試しください。");
                                adb.setPositiveButton("OK", null);
                                AlertDialog ad = adb.create();
                                ad.show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        waitDialog.dismiss();
                        HttpConnector httpCon = new HttpConnector("ingroup", "{\"user_id\":\"" + myId + "\",\"group_id\":\"" + str + "\"}");
                        httpCon.setOnHttpResponseListener((String message) -> {
                            if (Integer.parseInt(message) == 0) {
                                AlertDialog.Builder adb = new AlertDialog.Builder(SelectGroupActivity.this);
                                adb.setCancelable(true);
                                adb.setTitle("グループ加入完了");
                                adb.setMessage("グループへの加入が完了しました。さっそくグループを選択して遊ぼう！");
                                adb.setPositiveButton("OK", null);
                                AlertDialog ad = adb.create();
                                ad.show();
                                listLoad();
                            } else {
                                AlertDialog.Builder adb = new AlertDialog.Builder(SelectGroupActivity.this);
                                adb.setCancelable(true);
                                adb.setTitle("エラー");
                                adb.setMessage("グループが見つかりませんでした。グループIDを確認してもう一度お試しください。");
                                adb.setPositiveButton("OK", null);
                                AlertDialog ad = adb.create();
                                ad.show();
                            }
                        });
                        httpCon.setOnHttpErrorListener((int error) -> {
                            android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                            adb.setTitle("接続エラー");
                            adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
                            adb.setPositiveButton("OK", null);
                            adb.setCancelable(true);
                            adb.show();
                        });
                        httpCon.post();
                    }


                });
                httpConnector.setOnHttpErrorListener((int error) -> {
                    android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                    adb.setTitle("接続エラー");
                    adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
                    adb.setPositiveButton("OK", null);
                    adb.setCancelable(true);
                    adb.show();
                });
                httpConnector.post();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Wait(String what){
        waitDialog = new ProgressDialog(this);
        waitDialog.setMessage(what + "中...");
        waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        waitDialog.setCanceledOnTouchOutside(false);
        waitDialog.show();
    }

    int mScreenId = 0;
    private void setScreenContent(int screenId) {
        mScreenId = screenId;
        setContentView(screenId);

        switch (screenId) {
            case R.layout.activity_select_group: {
                setSelectScreenContent();
                break;
            }
            case R.layout.activity_member_wait: {
                setWaitScreenContent();
                break;
            }
        }
    }

    private void setSelectScreenContent() {
        lv = (ListView)findViewById(R.id.listView2);
        listLoad();
    }

    private void setWaitScreenContent() {

    }

}
