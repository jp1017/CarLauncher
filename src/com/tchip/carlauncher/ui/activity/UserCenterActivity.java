package com.tchip.carlauncher.ui.activity;

import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.tchip.carlauncher.Constant;
import com.tchip.carlauncher.R;
import com.tchip.carlauncher.util.MyLog;
import com.tchip.carlauncher.util.NetworkUtil;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class UserCenterActivity extends Activity {

	private RelativeLayout layoutContentSign, layoutContentLogin,
			layoutContentDefault, layoutContentMessage, layoutSign,
			layoutLogin, layoutLogout;
	private EditText textSignUsername, textSignPassOne, textSignPassTwo,
			textLoginUsername, textLoginPass;
	private Button btnSign, btnSignReset, btnLogin, btnLoginReset;

	private SharedPreferences preferences;
	private Editor editor;

	private boolean isUserLogin = false;

	private String userName = "", userPass = "", strResult = "";

	public static enum PannelState {
		DEFAULT, SIGN, LOGIN, MESSAGE
	}

	private PannelState pannelState = PannelState.DEFAULT;

	private static final String URL_PREFIX = "http://t-chip.com.cn/wechat/index.php/Home";
	private static final String SUFFIX_USER_SIGN = "/Cllogin/register_user";
	private static final String SUFFIX_USER_LOGIN = "/Cllogin/index";
	private static final String SUFFIX_USER_QUERY = "/Cllogin/query";
	private static final String SUFFIX_DEVICE_SIGN = "/Cllogin/register_device";
	private static final String SUFFIX_DEVICE_BIND = "/Cllogin/user_band_device";
	private static final String SUFFIX_WECHAT_QR = "/Client/qrcodeCreate";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_user_center);

		preferences = getSharedPreferences(Constant.SHARED_PREFERENCES_NAME,
				Context.MODE_PRIVATE);
		editor = preferences.edit();

		initialLayout();
		PannelState state = initialPannelState(isUserLogin());
		initialPannelLayout(state);

	}

	private void initialLayout() {
		RelativeLayout layoutBack = (RelativeLayout) findViewById(R.id.layoutBack);
		layoutBack.setOnClickListener(new MyOnClickListener());

		layoutSign = (RelativeLayout) findViewById(R.id.layoutSign);
		layoutLogin = (RelativeLayout) findViewById(R.id.layoutLogin);
		layoutLogout = (RelativeLayout) findViewById(R.id.layoutLogout);

		layoutContentDefault = (RelativeLayout) findViewById(R.id.layoutContentDefault);
		layoutContentSign = (RelativeLayout) findViewById(R.id.layoutContentSign);
		layoutContentLogin = (RelativeLayout) findViewById(R.id.layoutContentLogin);
		layoutContentMessage = (RelativeLayout) findViewById(R.id.layoutContentMessage);

		textSignUsername = (EditText) findViewById(R.id.textSignUsername);
		textSignPassOne = (EditText) findViewById(R.id.textSignPassOne);
		textSignPassTwo = (EditText) findViewById(R.id.textSignPassTwo);
		textLoginUsername = (EditText) findViewById(R.id.textLoginUsername);
		textLoginPass = (EditText) findViewById(R.id.textLoginPass);

		btnSign = (Button) findViewById(R.id.btnSign);
		btnSignReset = (Button) findViewById(R.id.btnSignReset);
		btnLogin = (Button) findViewById(R.id.btnLogin);
		btnLoginReset = (Button) findViewById(R.id.btnLoginReset);

		layoutSign.setOnClickListener(new MyOnClickListener());
		layoutLogin.setOnClickListener(new MyOnClickListener());
		layoutLogout.setOnClickListener(new MyOnClickListener());
		btnSign.setOnClickListener(new MyOnClickListener());
		btnSignReset.setOnClickListener(new MyOnClickListener());
		btnLogin.setOnClickListener(new MyOnClickListener());
		btnLoginReset.setOnClickListener(new MyOnClickListener());
	}

	private boolean isUserLogin() {
		isUserLogin = preferences.getBoolean("isUserLogin", false);
		return isUserLogin;
	}

	private PannelState initialPannelState(boolean isUserLogin) {
		if (isUserLogin) {
			return PannelState.MESSAGE;
		} else {
			return PannelState.DEFAULT;
		}
	}

	private void initialPannelLayout(PannelState state) {
		switch (state) {
		case SIGN: // 注册界面
			layoutLogout.setVisibility(View.GONE);

			layoutContentDefault.setVisibility(View.GONE);
			layoutContentSign.setVisibility(View.VISIBLE);
			layoutContentLogin.setVisibility(View.GONE);
			layoutContentMessage.setVisibility(View.GONE);
			break;

		case LOGIN: // 登录界面
			layoutLogout.setVisibility(View.GONE);

			layoutContentDefault.setVisibility(View.GONE);
			layoutContentSign.setVisibility(View.GONE);
			layoutContentLogin.setVisibility(View.VISIBLE);
			layoutContentMessage.setVisibility(View.GONE);
			break;

		case MESSAGE: // 登录成功后消息界面
			layoutLogout.setVisibility(View.VISIBLE);

			layoutContentDefault.setVisibility(View.GONE);
			layoutContentSign.setVisibility(View.GONE);
			layoutContentLogin.setVisibility(View.GONE);
			layoutContentMessage.setVisibility(View.VISIBLE);
			break;

		default: // 默认界面，提示
			layoutLogout.setVisibility(View.GONE);

			layoutContentDefault.setVisibility(View.VISIBLE);
			layoutContentSign.setVisibility(View.GONE);
			layoutContentLogin.setVisibility(View.GONE);
			layoutContentMessage.setVisibility(View.GONE);
			break;
		}
	}

	class MyOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.layoutBack:
				finish();
				break;

			case R.id.layoutSign:
				initialPannelLayout(PannelState.SIGN);
				break;

			case R.id.layoutLogin:
				initialPannelLayout(PannelState.LOGIN);
				break;

			case R.id.layoutLogout:
				userLogout();
				break;

			case R.id.btnSign:
				String userName = textSignUsername.getText().toString();
				String userPassOne = textSignPassOne.getText().toString();
				String userPassTwo = textSignPassTwo.getText().toString();
				if (userName == null || userName.trim().length() < 1) {
					Toast.makeText(getApplicationContext(), "请输入用户名",
							Toast.LENGTH_SHORT).show();
				} else if (userPassOne == null
						|| userPassOne.trim().length() < 5
						|| userPassOne.trim().length() > 15) {
					Toast.makeText(getApplicationContext(), "请输入5-15位密码",
							Toast.LENGTH_SHORT).show();
				} else if (userPassTwo == null
						|| userPassTwo.trim().length() < 1) {
					Toast.makeText(getApplicationContext(), "请确认密码",
							Toast.LENGTH_SHORT).show();
				} else if (!userPassOne.equals(userPassTwo)) {
					Toast.makeText(getApplicationContext(), "两次输入密码不一致",
							Toast.LENGTH_SHORT).show();
				} else {
					userSign(userName, userPassOne);
				}
				break;

			case R.id.btnSignReset:
				break;

			case R.id.btnLogin:
				break;

			case R.id.btnLoginReset:
				break;

			default:
				initialPannelLayout(PannelState.DEFAULT);
				break;
			}
		}
	}

	/**
	 * 用户注册
	 * 
	 * @param userName
	 * @param userPass
	 */
	private void userSign(String userName, String userPass) {
		if (NetworkUtil.isNetworkConnected(getApplicationContext())) {
			this.userName = userName;
			this.userPass = userPass;
			new Thread(new UserSignThread()).start();
		} else {
			NetworkUtil.noNetworkHint(getApplicationContext());
		}
	}

	class UserSignThread implements Runnable {

		@Override
		public void run() {
			String urlUserSign = URL_PREFIX + SUFFIX_USER_SIGN;
			HttpPost httpPost = new HttpPost(urlUserSign);

			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("username", Base64
					.encodeToString(userName.getBytes(), Base64.DEFAULT)));
			params.add(new BasicNameValuePair("password", Base64
					.encodeToString(userPass.getBytes(), Base64.DEFAULT)));
			try {
				httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
				HttpResponse httpResponse = new DefaultHttpClient()
						.execute(httpPost);

				int statusCode = httpResponse.getStatusLine().getStatusCode();
				if (statusCode == HttpURLConnection.HTTP_OK) {
					strResult = EntityUtils.toString(httpResponse.getEntity());
					MyLog.v("[UserCenter]userSign:" + strResult);
					// {" status":0," msg":"\u53c2\u6570\u9519\u8bef"} 参数错误

					Message message = new Message();
					message.what = 1;
					userSignHandler.sendMessage(message);
				} else {
					MyLog.e("[UserCenter]Err,StatusCode:" + statusCode);
				}
			} catch (Exception e) {
				e.printStackTrace();
				MyLog.e("[UserCenter]Exception:" + e.toString());
			}
		}

	}

	final Handler userSignHandler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				if (strResult != null && strResult.trim().length() > 0) {
					textSignUsername.setText(strResult);
				}
				break;

			default:
				break;
			}
		}
	};

	/**
	 * 用户登录
	 * 
	 * @param userName
	 * @param userPass
	 */
	private void userLogin(String userName, String userPass) {
		if (NetworkUtil.isNetworkConnected(getApplicationContext())) {

		} else {
			NetworkUtil.noNetworkHint(getApplicationContext());
		}
	}

	/**
	 * 退出登录
	 */
	private void userLogout() {
		// TODO:对话框确认
		editor.putBoolean("isUserLogin", false);
		editor.putString("userName", "");
		editor.putString("UserPass", "");
		editor.commit();

		initialPannelLayout(PannelState.DEFAULT);
	}

}
