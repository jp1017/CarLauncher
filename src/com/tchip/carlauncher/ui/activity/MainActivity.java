package com.tchip.carlauncher.ui.activity;

import java.io.File;
import java.util.List;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.navi.BaiduMapAppNotSupportNaviException;
import com.baidu.mapapi.navi.BaiduMapNavigation;
import com.baidu.mapapi.navi.NaviPara;
import com.tchip.carlauncher.Constant;
import com.tchip.carlauncher.MyApplication;
import com.tchip.carlauncher.R;
import com.tchip.carlauncher.lib.filemanager.FolderActivity;
import com.tchip.carlauncher.model.DriveVideo;
import com.tchip.carlauncher.model.DriveVideoDbHelper;
import com.tchip.carlauncher.model.Typefaces;
import com.tchip.carlauncher.service.BrightAdjustService;
import com.tchip.carlauncher.service.ConnectWifiService;
import com.tchip.carlauncher.service.LocationService;
import com.tchip.carlauncher.service.RouteRecordService;
import com.tchip.carlauncher.service.SensorWatchService;
import com.tchip.carlauncher.service.SpeakService;
import com.tchip.carlauncher.service.WeatherService;
import com.tchip.carlauncher.ui.activity.WifiListActivity.refreshWifiThread;
import com.tchip.carlauncher.ui.dialog.WifiPswDialog;
import com.tchip.carlauncher.ui.dialog.WifiPswDialog.OnCustomDialogListener;
import com.tchip.carlauncher.util.AudioPlayUtil;
import com.tchip.carlauncher.util.DateUtil;
import com.tchip.carlauncher.util.NetworkUtil;
import com.tchip.carlauncher.util.StorageUtil;
import com.tchip.carlauncher.util.WeatherUtil;
import com.tchip.carlauncher.util.WiFiUtil;
import com.tchip.carlauncher.util.WifiAdmin;
import com.tchip.tachograph.TachographCallback;
import com.tchip.tachograph.TachographRecorder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

public class MainActivity extends Activity implements TachographCallback,
		Callback {

	private SharedPreferences sharedPreferences;
	private Editor editor;

	private DriveVideoDbHelper videoDb;

	private LocationClient mLocationClient;

	private SurfaceView surfaceCamera;
	private boolean isSurfaceLarge = false;
	private MapView mainMapView;
	private BaiduMap baiduMap;
	private com.baidu.mapapi.map.MyLocationConfiguration.LocationMode currentMode;
	boolean isFirstLoc = true;// 是否首次定位

	private int scanSpan = 1000; // 轨迹点采集间隔(ms)

	private ImageView smallVideoRecord, smallVideoLock, smallVideoCamera;
	private RelativeLayout layoutLargeButton;
	private TextView textTemp, textLocation, textTodayWeather, textRecordTime;
	private ImageView imageTodayWeather;

	private ProgressBar updateProgress;
	private ImageView imageWifiLevel; // WiFi状态图标
	private IntentFilter wifiIntentFilter; // WiFi状态监听器

	private ImageView imageShadowRight, imageShadowLeft;
	private RelativeLayout layoutMap, layoutSetting;

	private HorizontalScrollView hsvMain;

	// Record
	private ImageView largeVideoSize, largeVideoTime, largeVideoLock,
			largeVideoMute, largeVideoRecord, largeVideoCamera;
	private SurfaceHolder mHolder;
	private TachographRecorder mMyRecorder;
	private Camera mCamera;

	// 分辨率
	private static final int STATE_RESOLUTION_720P = 0;
	private static final int STATE_RESOLUTION_1080P = 1;

	// 录像状态
	private static final int STATE_RECORD_STARTED = 0;
	private static final int STATE_RECORD_STOPPED = 1;

	// 视频分段
	private static final int STATE_INTERVAL_3MIN = 0;
	private static final int STATE_INTERVAL_5MIN = 1;

	// 第二视图
	private static final int STATE_SECONDARY_ENABLE = 0;
	private static final int STATE_SECONDARY_DISABLE = 1;

	// 路径
	private static final int STATE_PATH_ZERO = 0;
	private static final int STATE_PATH_ONE = 1;
	private static final int STATE_PATH_TWO = 2;
	private static final String PATH_ZERO = "/mnt/sdcard";
	private static final String PATH_ONE = "/mnt/sdcard/path_one";
	private static final String PATH_TWO = "/mnt/sdcard/path_two";

	// 重叠
	private static final int STATE_OVERLAP_ZERO = 0;
	private static final int STATE_OVERLAP_FIVE = 1;

	// 静音
	private static final int STATE_MUTE = 0;
	private static final int STATE_UNMUTE = 1;

	private int mResolutionState;
	private int mRecordState;
	private int mIntervalState;
	private int mPathState;
	private int mSecondaryState;
	private int mOverlapState;
	private int mMuteState; // 静音

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);

		sharedPreferences = getSharedPreferences(
				Constant.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		editor = sharedPreferences.edit();

		videoDb = new DriveVideoDbHelper(getApplicationContext());

		initialLayout();
		initialCameraButton();
		initialService();

		// 录像
		setupRecordDefaults();
		setupRecordViews();
	}

	/**
	 * 初始化服务
	 */
	private void initialService() {
		// WiFi连接
		Intent intentWiFi = new Intent(this, ConnectWifiService.class);
		startService(intentWiFi);

		// 位置
		Intent intentLocation = new Intent(this, LocationService.class);
		startService(intentLocation);

		// 亮度自动调整服务
		Intent intentBrightness = new Intent(this, BrightAdjustService.class);
		startService(intentBrightness);

		// 轨迹记录服务
		Intent intentRoute = new Intent(this, RouteRecordService.class);
		startService(intentRoute);

		// 碰撞侦测服务
		Intent intentSensor = new Intent(this, SensorWatchService.class);
		startService(intentSensor);

		importOfflineMapFromSDCard();
	}

	private MKOfflineMap mOffline = null;

	/**
	 * 导入离线地图包
	 */
	public void importOfflineMapFromSDCard() {
		mOffline = new MKOfflineMap();
		mOffline.init(new MyMKOfflineMapListener());
		int num = mOffline.importOfflineData();
		if (num == 0) {
			// 没有导入离线包，可能是离线包放置位置不正确，或离线包已经导入过
		} else {
			// "成功导入 num 个离线包
		}
	}

	class MyMKOfflineMapListener implements MKOfflineMapListener {

		@Override
		public void onGetOfflineMapState(int type, int state) {
			switch (type) {
			case MKOfflineMap.TYPE_DOWNLOAD_UPDATE:
				MKOLUpdateElement update = mOffline.getUpdateInfo(state);
				// 处理下载进度更新提示
				if (update != null) {
					// stateView.setText(String.format("%s : %d%%",
					// update.cityName,
					// update.ratio));
					// updateView();
				}
				break;
			case MKOfflineMap.TYPE_NEW_OFFLINE:
				// 有新离线地图安装
				break;
			case MKOfflineMap.TYPE_VER_UPDATE:
				// 版本更新提示
				// MKOLUpdateElement e = mOffline.getUpdateInfo(state);
				break;
			}
		}
	}

	/**
	 * 初始化布局
	 */
	private void initialLayout() {
		// 录像窗口
		surfaceCamera = (SurfaceView) findViewById(R.id.surfaceCamera);
		surfaceCamera.setOnClickListener(new MyOnClickListener());
		surfaceCamera.getHolder().addCallback(this);

		textRecordTime = (TextView) findViewById(R.id.textRecordTime);

		// 天气预报和时钟,状态图标
		RelativeLayout layoutWeather = (RelativeLayout) findViewById(R.id.layoutWeather);
		layoutWeather.setOnClickListener(new MyOnClickListener());
		TextClock textClock = (TextClock) findViewById(R.id.textClock);
		textClock.setTypeface(Typefaces.get(this, Constant.FONT_PATH
				+ "Font-Helvetica-Neue-LT-Pro.otf"));

		TextClock textDate = (TextClock) findViewById(R.id.textDate);
		textDate.setTypeface(Typefaces.get(this, Constant.FONT_PATH
				+ "Font-Droid-Sans-Fallback.ttf"));

		TextClock textWeek = (TextClock) findViewById(R.id.textWeek);
		textWeek.setTypeface(Typefaces.get(this, Constant.FONT_PATH
				+ "Font-Droid-Sans-Fallback.ttf"));

		textTemp = (TextView) findViewById(R.id.textTemp);
		textTemp.setTypeface(Typefaces.get(this, Constant.FONT_PATH
				+ "Font-Droid-Sans-Fallback.ttf"));

		imageTodayWeather = (ImageView) findViewById(R.id.imageTodayWeather);
		textTodayWeather = (TextView) findViewById(R.id.textTodayWeather);
		textLocation = (TextView) findViewById(R.id.textLocation);
		updateProgress = (ProgressBar) findViewById(R.id.updateProgress);

		LinearLayout layoutWiFi = (LinearLayout) findViewById(R.id.layoutWiFi);
		layoutWiFi.setOnClickListener(new MyOnClickListener());

		// WiFi状态信息
		imageWifiLevel = (ImageView) findViewById(R.id.imageWifiLevel);

		wifiIntentFilter = new IntentFilter();
		wifiIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		updateWiFiState();

		// 更新天气与位置信息
		updateLocationAndWeather();
		// updateProgress.setVisibility(View.VISIBLE);

		// 定位地图
		mainMapView = (MapView) findViewById(R.id.mainMapView);
		// 去掉缩放控件和百度Logo
		int count = mainMapView.getChildCount();
		for (int i = 0; i < count; i++) {
			View child = mainMapView.getChildAt(i);
			if (child instanceof ImageView || child instanceof ZoomControls) {
				child.setVisibility(View.INVISIBLE);
			}
		}
		baiduMap = mainMapView.getMap();
		// 开启定位图层
		baiduMap.setMyLocationEnabled(true);

		// 自定义Maker
		BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory
				.fromResource(R.drawable.icon_arrow_up);

		// LocationMode 跟随：FOLLOWING 普通：NORMAL 罗盘：COMPASS
		currentMode = com.baidu.mapapi.map.MyLocationConfiguration.LocationMode.NORMAL;
		baiduMap.setMyLocationConfigeration(new MyLocationConfiguration(
				currentMode, true, null));
		InitLocation(
				com.baidu.location.LocationClientOption.LocationMode.Hight_Accuracy,
				"bd09ll", scanSpan, true);
		// 设置地图放大级别 0-19
		MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(15);
		baiduMap.animateMapStatus(msu);

		View mapHideView = findViewById(R.id.mapHideView);
		mapHideView.setOnClickListener(new MyOnClickListener());

		ImageView imageNavi = (ImageView) findViewById(R.id.imageNavi);
		imageNavi.setOnClickListener(new MyOnClickListener());

		// 多媒体
		ImageView imageMultimedia = (ImageView) findViewById(R.id.imageMultimedia);
		imageMultimedia.setOnClickListener(new MyOnClickListener());

		// 文件管理
		ImageView imageFileExplore = (ImageView) findViewById(R.id.imageFileExplore);
		imageFileExplore.setOnClickListener(new MyOnClickListener());

		// 周边搜索
		ImageView imageNearSearch = (ImageView) findViewById(R.id.imageNearSearch);
		imageNearSearch.setOnClickListener(new MyOnClickListener());

		// 语音助手
		ImageView imageVoiceChat = (ImageView) findViewById(R.id.imageVoiceChat);
		imageVoiceChat.setOnClickListener(new MyOnClickListener());

		// 行驶轨迹
		ImageView imageRouteTrack = (ImageView) findViewById(R.id.imageRouteTrack);
		imageRouteTrack.setOnClickListener(new MyOnClickListener());

		// 路径规划（摄像头，红绿灯）
		ImageView imageRoutePlan = (ImageView) findViewById(R.id.imageRoutePlan);
		imageRoutePlan.setOnClickListener(new MyOnClickListener());

		// 拨号
		ImageView imageDialer = (ImageView) findViewById(R.id.imageDialer);
		imageDialer.setOnClickListener(new MyOnClickListener());

		// 短信
		ImageView imageMessage = (ImageView) findViewById(R.id.imageMessage);
		imageMessage.setOnClickListener(new MyOnClickListener());

		// 设置
		ImageView imageSetting = (ImageView) findViewById(R.id.imageSetting);
		imageSetting.setOnClickListener(new MyOnClickListener());

		// HorizontalScrollView，左右两侧阴影
		imageShadowLeft = (ImageView) findViewById(R.id.imageShadowLeft);
		imageShadowRight = (ImageView) findViewById(R.id.imageShadowRight);

		layoutMap = (RelativeLayout) findViewById(R.id.layoutMap);
		layoutSetting = (RelativeLayout) findViewById(R.id.layoutSetting);
		hsvMain = (HorizontalScrollView) findViewById(R.id.hsvMain);

		hsvMain.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_MOVE:
					View childFirst = ((HorizontalScrollView) v).getChildAt(0);

					// 右侧阴影
					if (v.getScrollX() + v.getWidth() + 20 >= childFirst
							.getMeasuredWidth()) {
						imageShadowRight.setVisibility(View.INVISIBLE);
					} else {
						imageShadowRight.setVisibility(View.VISIBLE);
					}
					// 左侧阴影
					if (v.getScrollX() >= 20) {
						imageShadowLeft.setVisibility(View.VISIBLE);
					} else {
						imageShadowLeft.setVisibility(View.INVISIBLE);
					}
					break;
				default:
					break;

				}
				return false;
			}
		});

		// 更新界面线程
		new Thread(new UpdateLayoutThread()).start();
	}

	private void startSpeak(String content) {
		Intent intent = new Intent(MainActivity.this, SpeakService.class);
		intent.putExtra("content", content);
		startService(intent);
	}

	/**
	 * 初始化录像按钮
	 */
	private void initialCameraButton() {
		// ********** 小视图 **********
		// 录制
		smallVideoRecord = (ImageView) findViewById(R.id.smallVideoRecord);
		smallVideoRecord.setOnClickListener(new MyOnClickListener());

		// 锁定
		smallVideoLock = (ImageView) findViewById(R.id.smallVideoLock);
		smallVideoLock.setOnClickListener(new MyOnClickListener());

		// 拍照
		smallVideoCamera = (ImageView) findViewById(R.id.smallVideoCamera);
		smallVideoCamera.setOnClickListener(new MyOnClickListener());

		// ********** 大视图 **********
		layoutLargeButton = (RelativeLayout) findViewById(R.id.layoutLargeButton);

		// 视频尺寸
		largeVideoSize = (ImageView) findViewById(R.id.largeVideoSize);
		largeVideoSize.setOnClickListener(new MyOnClickListener());

		// 视频分段长度
		largeVideoTime = (ImageView) findViewById(R.id.largeVideoTime);
		largeVideoTime.setOnClickListener(new MyOnClickListener());

		// 锁定
		largeVideoLock = (ImageView) findViewById(R.id.largeVideoLock);
		largeVideoLock.setOnClickListener(new MyOnClickListener());

		// 静音
		largeVideoMute = (ImageView) findViewById(R.id.largeVideoMute);
		largeVideoMute.setOnClickListener(new MyOnClickListener());

		// 录制
		largeVideoRecord = (ImageView) findViewById(R.id.largeVideoRecord);
		largeVideoRecord.setOnClickListener(new MyOnClickListener());

		// 拍照
		largeVideoCamera = (ImageView) findViewById(R.id.largeVideoCamera);
		largeVideoCamera.setOnClickListener(new MyOnClickListener());

		updateButtonState(isSurfaceLarge());
	}

	/**
	 * 更新录像大小按钮显示状态
	 * 
	 * @param isSurfaceLarge
	 */
	private void updateButtonState(boolean isSurfaceLarge) {
		if (isSurfaceLarge) {
			smallVideoRecord.setVisibility(View.INVISIBLE);
			smallVideoLock.setVisibility(View.INVISIBLE);
			smallVideoCamera.setVisibility(View.INVISIBLE);
			layoutLargeButton.setVisibility(View.VISIBLE);
		} else {
			smallVideoRecord.setVisibility(View.VISIBLE);
			smallVideoLock.setVisibility(View.VISIBLE);
			smallVideoCamera.setVisibility(View.VISIBLE);
			layoutLargeButton.setVisibility(View.INVISIBLE);
		}

	}

	private boolean isSurfaceLarge() {
		return isSurfaceLarge;
	}

	/**
	 * 更新位置和天气
	 */
	private void updateLocationAndWeather() {

		if ("未定位".equals(sharedPreferences.getString("cityName", "未定位"))) {
			String cityName = sharedPreferences.getString("cityNameRealButOld",
					"未定位");
			if ("未定位".equals(cityName)) {
				String addrStr = sharedPreferences.getString("addrStr", "未定位");
				if (addrStr.contains("省") && addrStr.contains("市")) {
					cityName = addrStr.split("省")[1].split("市")[0];
				} else if ((!addrStr.contains("省")) && addrStr.contains("市")) {
					cityName = addrStr.split("市")[0];
				} else {
					cityName = addrStr;
				}
			}
			editor.putString("cityNameRealButOld", cityName);
			textLocation.setText(cityName);
		} else {
			textLocation
					.setText(sharedPreferences.getString("cityName", "未定位"));
		}

		String weatherToday = sharedPreferences.getString("day0weather", "未知");
		textTodayWeather.setText(weatherToday);
		imageTodayWeather.setImageResource(WeatherUtil
				.getWeatherDrawable(WeatherUtil.getTypeByStr(weatherToday)));
		String day0tmpLow = sharedPreferences.getString("day0tmpLow", "15℃");
		String day0tmpHigh = sharedPreferences.getString("day0tmpHigh", "25℃");
		day0tmpLow = day0tmpLow.split("℃")[0];
		textTemp.setText(day0tmpLow + "~" + day0tmpHigh);
	}

	private int secondCount = -1;

	public class updateRecordTimeThread implements Runnable {

		@Override
		public void run() {
			// 解决录像时，快速点击录像按钮两次，线程叠加跑秒过快的问题
			synchronized (updateRecordTimeHandler) {
				do {
					try {
						Thread.sleep(1000);
						Message message = new Message();
						message.what = 1;
						updateRecordTimeHandler.sendMessage(message);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} while (MyApplication.isVideoReording);
			}
		}
	}

	final Handler updateRecordTimeHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				secondCount++;
				textRecordTime.setText(DateUtil
						.getFormatTimeBySecond(secondCount));
				break;

			default:
				break;
			}
		}
	};

	/**
	 * 更新WiF状态
	 */
	private void updateWiFiState() {

		int level = ((WifiManager) getSystemService(WIFI_SERVICE))
				.getConnectionInfo().getRssi();// Math.abs()
		imageWifiLevel.setImageResource(WiFiUtil.getImageBySignal(level));
	}

	public class UpdateLayoutThread implements Runnable {

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(3000);
					Message message = new Message();
					message.what = 1;
					updateLayoutHandler.sendMessage(message);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	final Handler updateLayoutHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 1:
				// 更新WiFi状态图标
				updateWiFiState();

				// 更新位置和天气信息
				updateLocationAndWeather();

				// 电源断开保存视频
				if (MyApplication.isVideoReording
						&& (!MyApplication.isPowerConnect)) {
					if (stopRecorder() == 0) {
						secondCount = -1; // 录制时间秒钟复位
						textRecordTime.setText("00:00:00");
						textRecordTime.setVisibility(View.INVISIBLE);
						mRecordState = STATE_RECORD_STOPPED;
						MyApplication.isVideoReording = false;
						setupRecordViews();
					}
				}

				// 连接电源自动录像
				// if (MyApplication.isFirstLaunch &&
				// (!MyApplication.isVideoReording)
				// && sharedPreferences.getBoolean("autoRecord", true)
				// && MyApplication.isPowerConnect) {
				// mRecordState = STATE_RECORD_STARTED;
				// MyApplication.isVideoReording = true;
				// MyApplication.isFirstLaunch = false;
				// setupRecordViews();
				// }
				break;

			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	class MyOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.surfaceCamera:
				if (!isSurfaceLarge) {
					int widthFull = 854;
					int heightFull = 480;
					surfaceCamera
							.setLayoutParams(new RelativeLayout.LayoutParams(
									widthFull, heightFull));
					isSurfaceLarge = true;

					// 更新HorizontalScrollView阴影
					imageShadowLeft.setVisibility(View.GONE);
					imageShadowRight.setVisibility(View.GONE);

					updateButtonState(true);
				} else {
					int widthSmall = 480;
					int heightSmall = 270;
					surfaceCamera
							.setLayoutParams(new RelativeLayout.LayoutParams(
									widthSmall, heightSmall));
					isSurfaceLarge = false;

					// 更新HorizontalScrollView阴影
					hsvMain.scrollTo(0, 0);
					imageShadowLeft.setVisibility(View.GONE);
					imageShadowRight.setVisibility(View.VISIBLE);

					updateButtonState(false);
				}
				break;

			case R.id.smallVideoRecord:
			case R.id.largeVideoRecord:
				if (mRecordState == STATE_RECORD_STOPPED) {
					if (startRecorder() == 0) {
						mRecordState = STATE_RECORD_STARTED;
						MyApplication.isVideoReording = true;
					}
				} else if (mRecordState == STATE_RECORD_STARTED) {
					if (stopRecorder() == 0) {
						mRecordState = STATE_RECORD_STOPPED;
						MyApplication.isVideoReording = false;
					}
				}
				AudioPlayUtil.playAudio(getApplicationContext(),
						FILE_TYPE_VIDEO);
				setupRecordViews();
				break;

			case R.id.smallVideoLock:
			case R.id.largeVideoLock:
				if (!MyApplication.isVideoLock) {
					MyApplication.isVideoLock = true;
					startSpeak("视频加锁");
				} else {
					MyApplication.isVideoLock = false;
					startSpeak("视频解锁");
				}
				setupRecordViews();
				break;

			case R.id.largeVideoSize:
				// 切换分辨率录像停止，需要重置时间
				MyApplication.isVideoReording = false;
				secondCount = -1; // 录制时间秒钟复位
				textRecordTime.setText("00:00:00");
				textRecordTime.setVisibility(View.INVISIBLE);

				if (mResolutionState == STATE_RESOLUTION_1080P) {
					setResolution(STATE_RESOLUTION_720P);
					editor.putString("videoSize", "720");
					mRecordState = STATE_RECORD_STOPPED;
					startSpeak("分辨率七二零P");
				} else if (mResolutionState == STATE_RESOLUTION_720P) {
					setResolution(STATE_RESOLUTION_1080P);
					editor.putString("videoSize", "1080");
					mRecordState = STATE_RECORD_STOPPED;
					startSpeak("分辨率一零八零P");
				}
				editor.commit();
				setupRecordViews();
				break;

			case R.id.largeVideoTime:
				if (mIntervalState == STATE_INTERVAL_3MIN) {
					if (setInterval(5 * 60) == 0) {
						mIntervalState = STATE_INTERVAL_5MIN;
						editor.putString("videoTime", "5");
						startSpeak("视频分段五分钟");
					}
				} else if (mIntervalState == STATE_INTERVAL_5MIN) {
					if (setInterval(3 * 60) == 0) {
						mIntervalState = STATE_INTERVAL_3MIN;
						editor.putString("videoTime", "3");
						startSpeak("视频分段三分钟");
					}
				}
				editor.commit();
				setupRecordViews();
				break;

			case R.id.largeVideoMute:
				if (mMuteState == STATE_MUTE) {
					if (setMute(false) == 0) {
						mMuteState = STATE_UNMUTE;
						startSpeak("录音");
					}
				} else if (mMuteState == STATE_UNMUTE) {
					if (setMute(true) == 0) {
						mMuteState = STATE_MUTE;
						startSpeak("静音");
					}
				}
				setupRecordViews();
				break;

			case R.id.smallVideoCamera:
			case R.id.largeVideoCamera:
				takePhoto();
				AudioPlayUtil.playAudio(getApplicationContext(),
						FILE_TYPE_IMAGE);
				break;

			case R.id.layoutWeather:
				Intent intentWeather = new Intent(MainActivity.this,
						WeatherActivity.class);
				startActivity(intentWeather);
				overridePendingTransition(R.anim.zms_translate_down_out,
						R.anim.zms_translate_down_in);
				break;

			case R.id.mapHideView:
			case R.id.imageNavi:
				try {
					ComponentName componentMap = new ComponentName(
							"com.baidu.BaiduMap",
							"com.baidu.baidumaps.WelcomeScreen");
					Intent intentMap = new Intent();
					intentMap.setComponent(componentMap);
					startActivity(intentMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;

			case R.id.imageMultimedia:
				Intent intentMultimedia = new Intent(MainActivity.this,
						MultimediaActivity.class);
				startActivity(intentMultimedia);
				overridePendingTransition(R.anim.zms_translate_up_out,
						R.anim.zms_translate_up_in);
				break;

			case R.id.imageRouteTrack:
				Intent intentRouteTrack = new Intent(MainActivity.this,
						RouteListActivity.class);
				startActivity(intentRouteTrack);
				overridePendingTransition(R.anim.zms_translate_up_out,
						R.anim.zms_translate_up_in);
				break;

			case R.id.imageRoutePlan:
				Intent intentRoutePlan = new Intent(MainActivity.this,
						RoutePlanActivity.class);
				startActivity(intentRoutePlan);
				overridePendingTransition(R.anim.zms_translate_up_out,
						R.anim.zms_translate_up_in);
				break;

			case R.id.imageFileExplore:
				Intent intentFileExplore = new Intent(MainActivity.this,
						FolderActivity.class);
				// Intent intentFileExplore = new Intent(MainActivity.this,
				// VideoListActivity.class);
				startActivity(intentFileExplore);
				overridePendingTransition(R.anim.zms_translate_up_out,
						R.anim.zms_translate_up_in);
				break;

			case R.id.imageNearSearch:
				Intent intentNearSearch = new Intent(MainActivity.this,
						NearActivity.class);
				startActivity(intentNearSearch);
				overridePendingTransition(R.anim.zms_translate_up_out,
						R.anim.zms_translate_up_in);
				break;

			case R.id.imageVoiceChat:
				Intent intentVoiceChat = new Intent(MainActivity.this,
						ChatActivity.class);
				startActivity(intentVoiceChat);
				overridePendingTransition(R.anim.zms_translate_up_out,
						R.anim.zms_translate_up_in);
				break;

			case R.id.imageDialer:
				// try {
				// ComponentName componentDialer = new ComponentName(
				// "com.android.dialer",
				// "com.android.dialer.DialtactsActivity");
				// Intent intentDialer = new Intent();
				// intentDialer.setComponent(componentDialer);
				// startActivity(intentDialer);
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				Intent intentBTDialer = new Intent(MainActivity.this,
						BluetoothDialerActivity.class);
				startActivity(intentBTDialer);
				overridePendingTransition(R.anim.zms_translate_up_out,
						R.anim.zms_translate_up_in);
				break;

			case R.id.imageMessage:
				try {
					ComponentName componentMessage = new ComponentName(
							"com.android.mms",
							"com.android.mms.ui.BootActivity");
					Intent intentMessage = new Intent();
					intentMessage.setComponent(componentMessage);
					startActivity(intentMessage);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;

			case R.id.imageSetting:
				Intent intentSetting = new Intent(MainActivity.this,
						SettingActivity.class);
				startActivity(intentSetting);
				overridePendingTransition(R.anim.zms_translate_up_out,
						R.anim.zms_translate_up_in);
				break;

			case R.id.layoutWiFi:
				// startActivity(new Intent(
				// android.provider.Settings.ACTION_WIFI_SETTINGS));
				Intent intentWiFi = new Intent(MainActivity.this,
						WifiListActivity.class);
				startActivity(intentWiFi);
				break;

			default:
				break;
			}
		}
	}

	class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			// MapView 销毁后不在处理新接收的位置
			if (location == null || mainMapView == null)
				return;
			MyLocationData locData = new MyLocationData.Builder()
					.accuracy(0)
					// accuracy设为0去掉蓝色精度圈，RAW:.accuracy(location.getRadius())
					// 此处设置开发者获取到的方向信息，顺时针0-360
					.direction(100).latitude(location.getLatitude())
					.longitude(location.getLongitude()).build();
			baiduMap.setMyLocationData(locData);
			if (isFirstLoc) {
				isFirstLoc = false;
				LatLng ll = new LatLng(location.getLatitude(),
						location.getLongitude());
				MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
				baiduMap.animateMapStatus(u);
				// 更新天气
				startWeatherService();
			}

			// 存储非“未定位”的城市信息
			if (!"未定位".equals(location.getCity())) {
				editor.putString("cityNameRealButOld", location.getCity());
			}

			// 城市名发生变化，需要更新位置和天气
			if (!sharedPreferences.getString("cityName", "未定位").equals(
					location.getCity())) {
				startWeatherService();
				Editor editor = sharedPreferences.edit();
				editor.putString("cityName", location.getCity());

				editor.commit();
			}

			String cityName = location.getCity();

			if ((cityName != null) && (!cityName.equals("未定位"))) {

				// editor.putLong("cityCode", cityCode);
				editor.putString("cityName", cityName);
				editor.putString("cityNameRealButOld", cityName);
				editor.putString("latitude", "" + location.getLatitude());
				editor.putString("longitude", "" + location.getLongitude());
				editor.putString("district", location.getDistrict());
				editor.putString("floor", location.getFloor());
				editor.putString("addrStr", location.getAddrStr());
				editor.putString("street", location.getStreet());
				editor.putString("streetNum", location.getStreetNumber());
				editor.putFloat("speed", location.getSpeed());
				editor.putString("altitude", "" + location.getAltitude());
				editor.putString("lbsTime", location.getTime());
				editor.commit();

				// new Thread(networkTask).start();
			}
		}

		public void onReceivePoi(BDLocation poiLocation) {
		}
	}

	/**
	 * 
	 * @param tempMode
	 *            LocationMode.Hight_Accuracy-高精度
	 *            LocationMode.Battery_Saving-低功耗
	 *            LocationMode.Device_Sensors-仅设备
	 * @param tempCoor
	 *            gcj02-国测局加密经纬度坐标 bd09ll-百度加密经纬度坐标 bd09-百度加密墨卡托坐标
	 * @param frequence
	 *            MIN_SCAN_SPAN = 1000; MIN_SCAN_SPAN_NETWORK = 3000;
	 * @param isNeedAddress
	 *            是否需要地址
	 */
	private void InitLocation(
			com.baidu.location.LocationClientOption.LocationMode tempMode,
			String tempCoor, int frequence, boolean isNeedAddress) {

		mLocationClient = new LocationClient(this.getApplicationContext());
		mLocationClient.registerLocationListener(new MyLocationListener());
		// mGeofenceClient = new GeofenceClient(getApplicationContext());

		LocationClientOption option = new LocationClientOption();
		option.setLocationMode(tempMode);
		option.setCoorType(tempCoor);
		option.setScanSpan(frequence);
		option.setOpenGps(true);// 打开gps
		mLocationClient.setLocOption(option);

		mLocationClient.start();
	}

	/**
	 * WiFi状态Receiver
	 */
	private BroadcastReceiver wifiIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int wifi_state = intent.getIntExtra("wifi_state", 0);
			int level = ((WifiManager) getSystemService(WIFI_SERVICE))
					.getConnectionInfo().getRssi();// Math.abs()
			switch (wifi_state) {
			case WifiManager.WIFI_STATE_DISABLING:
				imageWifiLevel.setImageResource(WiFiUtil
						.getImageBySignal(level));
				break;
			case WifiManager.WIFI_STATE_DISABLED:
				imageWifiLevel.setImageResource(WiFiUtil
						.getImageBySignal(level));
				break;
			case WifiManager.WIFI_STATE_ENABLING:
				imageWifiLevel.setImageResource(WiFiUtil
						.getImageBySignal(level));
				break;
			case WifiManager.WIFI_STATE_ENABLED:
				imageWifiLevel.setImageResource(WiFiUtil
						.getImageBySignal(level));
				break;
			case WifiManager.WIFI_STATE_UNKNOWN:
				imageWifiLevel.setImageResource(WiFiUtil
						.getImageBySignal(level));
				break;
			}
		}
	};

	/**
	 * 更新天气
	 */
	private void startWeatherService() {
		Intent intent = new Intent(this, WeatherService.class);
		startService(intent);
	}

	@Override
	protected void onPause() {
		mainMapView.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		mainMapView.onResume();

		// 注册wifi消息处理器
		registerReceiver(wifiIntentReceiver, wifiIntentFilter);

		// 更新录像界面按钮状态
		refreshRecordButton();
		setupRecordViews();

		super.onResume();
	}

	@Override
	protected void onDestroy() {
		// 退出时销毁定位
		mLocationClient.stop();

		// 关闭定位图层
		baiduMap.setMyLocationEnabled(false);
		mainMapView.onDestroy();
		mainMapView = null;

		// 取消注册wifi消息处理器
		unregisterReceiver(wifiIntentReceiver);
		super.onDestroy();

		// 录像区域
		release();
	}

	// *********** Record ***********

	/**
	 * 设置录制初始值
	 */
	private void setupRecordDefaults() {
		refreshRecordButton();

		mRecordState = STATE_RECORD_STOPPED;

		mPathState = STATE_PATH_ZERO;
		mSecondaryState = STATE_SECONDARY_DISABLE;
		mOverlapState = STATE_OVERLAP_FIVE;

		mMuteState = STATE_UNMUTE;

	}

	private void refreshRecordButton() {
		// 视频尺寸
		String videoSizeStr = sharedPreferences.getString("videoSize", "720");
		if ("1080".equals(videoSizeStr)) {
			mResolutionState = STATE_RESOLUTION_1080P;
		} else {
			mResolutionState = STATE_RESOLUTION_720P;
		}

		// 视频分段
		String videoTimeStr = sharedPreferences.getString("videoTime", "5");
		if ("3".equals(videoTimeStr)) {
			mIntervalState = STATE_INTERVAL_3MIN;
		} else {
			mIntervalState = STATE_INTERVAL_5MIN;
		}
	}

	private void setupRecordViews() {
		// 视频分辨率
		if (mResolutionState == STATE_RESOLUTION_720P) {
			largeVideoSize.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_size_720));
		} else if (mResolutionState == STATE_RESOLUTION_1080P) {
			largeVideoSize.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_size_1080));
		}

		// 录像按钮
		if (mRecordState == STATE_RECORD_STOPPED) {
			largeVideoRecord.setBackground(getResources().getDrawable(
					R.drawable.ui_main_video_record));
			smallVideoRecord.setBackground(getResources().getDrawable(
					R.drawable.ui_main_video_record));
		} else if (mRecordState == STATE_RECORD_STARTED) {
			largeVideoRecord.setBackground(getResources().getDrawable(
					R.drawable.ui_main_video_pause));
			smallVideoRecord.setBackground(getResources().getDrawable(
					R.drawable.ui_main_video_pause));
		}

		// 视频分段
		if (mIntervalState == STATE_INTERVAL_3MIN) {
			largeVideoTime.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_time_3));
		} else if (mIntervalState == STATE_INTERVAL_5MIN) {
			largeVideoTime.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_time_5));
		}

		// 视频加锁
		if (MyApplication.isVideoLock) {
			smallVideoLock.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_lock));
			largeVideoLock.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_lock));
		} else {
			smallVideoLock.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_unlock));
			largeVideoLock.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_unlock));
		}

		// 静音
		if (mMuteState == STATE_MUTE) {
			// TODO:更新图片
			largeVideoMute.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_mute_off));
		} else if (mMuteState == STATE_UNMUTE) {
			largeVideoMute.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_mute_off));
		}

		// 路径
		// if (mPathState == STATE_PATH_ZERO) {
		// mPathBtn.setText(R.string.path_zero);
		// } else if (mPathState == STATE_PATH_ONE) {
		// mPathBtn.setText(R.string.path_one);
		// } else if (mPathState == STATE_PATH_TWO) {
		// mPathBtn.setText(R.string.path_two);
		// }

		// if (mSecondaryState == STATE_SECONDARY_DISABLE) {
		// mSecondaryBtn.setText(R.string.secondary_disable);
		// } else if (mSecondaryState == STATE_SECONDARY_ENABLE) {
		// mSecondaryBtn.setText(R.string.secondary_enable);
		// }

		// if (mOverlapState == STATE_OVERLAP_ZERO) {
		// mOverlapBtn.setText(R.string.nooverlap);
		// } else if (mOverlapState == STATE_OVERLAP_FIVE) {
		// mOverlapBtn.setText(R.string.overlap_5s);
		// }

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// mHolder = holder;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

		if (mCamera == null) {
			mHolder = holder;
			setup();
		} else {
			try {
				mCamera.lock();
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();
				mCamera.unlock();
			} catch (Exception e) {
				// e.printStackTrace();
			}

		}

		// mHolder = holder;
		// if (!MyApplication.isVideoReording) {
		// setup();
		// } else { // 正在录像中，回到主界面
		// try {
		// // mCamera = Camera.open(0);
		// // mCamera.lock();
		// // mCamera.stopPreview();
		// // mCamera.setPreviewDisplay(null);
		//
		// mCamera.setPreviewDisplay(mHolder);
		// mCamera.startPreview();
		// // mCamera.unlock();
		//
		// surfaceCamera = (SurfaceView) findViewById(R.id.surfaceCamera);
		// surfaceCamera.setOnClickListener(new MyOnClickListener());
		// surfaceCamera.getHolder().addCallback(this);
		// } catch (Exception e) {
		// Log.e(Constant.TAG, "Back to main when record, Err:" + e);
		// }
		// }
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// if (!MyApplication.isVideoReording) {
		// release();
		// }
		// mHolder = null;
	}

	private boolean openCamera() {
		if (mCamera != null) {
			closeCamera();
		}
		try {
			mCamera = Camera.open(0);
			mCamera.lock();
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();
			mCamera.unlock();
			return true;
		} catch (Exception ex) {
			closeCamera();
			return false;
		}
	}

	private boolean closeCamera() {
		if (mCamera == null)
			return true;
		try {
			mCamera.lock();
			mCamera.stopPreview();
			mCamera.setPreviewDisplay(null);
			mCamera.release();
			mCamera.unlock();
			mCamera = null;
			return true;
		} catch (Exception ex) {
			mCamera = null;
			return false;
		}
	}

	/**
	 * 删除最旧视频
	 */
	private boolean deleteOldestUnlockVideo() {
		try {
			String sdcardPath = sharedPreferences.getString("sdcardPath",
					"/mnt/sdcard/");
			float sdFree = StorageUtil.getSDAvailableSize(sdcardPath);
			float sdTotal = StorageUtil.getSDTotalSize(sdcardPath);
			while (sdFree < sdTotal * Constant.SD_MIN_FREE_PERCENT) {
				int oldestUnlockVideoId = videoDb.getOldestUnlockVideoId();
				// 删除视频文件
				if (oldestUnlockVideoId != -1) {
					String oldestUnlockVideoName = videoDb
							.getVideNameById(oldestUnlockVideoId);
					File f = new File(sdcardPath + "tachograph/"
							+ oldestUnlockVideoName.split("_")[0]
							+ File.separator + oldestUnlockVideoName);
					if (f.exists() && f.isFile()) {
						f.delete();
						Log.d(Constant.TAG, "Delete Old Video:" + f.getName());
					}

					// 删除数据库记录
					videoDb.deleteDriveVideoById(oldestUnlockVideoId);
					// 更新剩余空间
					sdFree = StorageUtil.getSDAvailableSize(sdcardPath);
				}
			}
			return true;
		} catch (Exception e) {
			/*
			 * 异常原因：1.所有未加锁文件都已删除，空间还是不足 2.文件由用户手动删除
			 */
			e.printStackTrace();
			return false;
		}
	}

	public int startRecorder() {

		if (mMyRecorder != null) {
			deleteOldestUnlockVideo();
			textRecordTime.setVisibility(View.VISIBLE);
			new Thread(new updateRecordTimeThread()).start(); // 更新录制时间
			Log.d(Constant.TAG, "Record Start");
			return mMyRecorder.start();
		}
		return -1;
	}

	public int stopRecorder() {
		if (mMyRecorder != null) {
			secondCount = -1; // 录制时间秒钟复位
			textRecordTime.setText("00:00:00");
			textRecordTime.setVisibility(View.INVISIBLE);
			Log.d(Constant.TAG, "Record Stop");
			return mMyRecorder.stop();
		}
		return -1;
	}

	public int setInterval(int seconds) {
		if (mMyRecorder != null) {
			return mMyRecorder.setVideoSeconds(seconds);
		}
		return -1;
	}

	public int setOverlap(int seconds) {
		if (mMyRecorder != null) {
			return mMyRecorder.setVideoOverlap(seconds);
		}
		return -1;
	}

	public int takePhoto() {
		if (mMyRecorder != null) {
			return mMyRecorder.takePicture();
		}
		return -1;
	}

	public int setDirectory(String dir) {
		if (mMyRecorder != null) {
			return mMyRecorder.setDirectory(dir);
		}
		return -1;
	}

	private int setMute(boolean mute) {
		if (mMyRecorder != null) {
			return mMyRecorder.setMute(mute);
		}
		return -1;
	}

	public int setResolution(int state) {
		if (state != mResolutionState) {
			mResolutionState = state;
			release();
			if (openCamera()) {
				setupRecorder();
			}
		}
		return -1;
	}

	public int setSecondary(int state) {
		if (state == STATE_SECONDARY_DISABLE) {
			if (mMyRecorder != null) {
				return mMyRecorder.setSecondaryVideoEnable(false);
			}
		} else if (state == STATE_SECONDARY_ENABLE) {
			if (mMyRecorder != null) {
				mMyRecorder.setSecondaryVideoSize(320, 240);
				mMyRecorder.setSecondaryVideoFrameRate(30);
				mMyRecorder.setSecondaryVideoBiteRate(120000);
				return mMyRecorder.setSecondaryVideoEnable(true);
			}
		}
		return -1;
	}

	private void setupRecorder() {
		releaseRecorder();
		mMyRecorder = new TachographRecorder();
		mMyRecorder.setTachographCallback(this);
		mMyRecorder.setCamera(mCamera);
		mMyRecorder.setClientName(this.getPackageName());
		if (mResolutionState == STATE_RESOLUTION_1080P) {
			mMyRecorder.setVideoSize(1920, 1080);
			mMyRecorder.setVideoFrameRate(30);
			mMyRecorder.setVideoBiteRate(8500000);
		} else {
			mMyRecorder.setVideoSize(1280, 720);
			mMyRecorder.setVideoFrameRate(30);
			mMyRecorder.setVideoBiteRate(3500000);
		}
		if (mSecondaryState == STATE_SECONDARY_ENABLE) {
			mMyRecorder.setSecondaryVideoEnable(true);
			mMyRecorder.setSecondaryVideoSize(320, 240);
			mMyRecorder.setSecondaryVideoFrameRate(30);
			mMyRecorder.setSecondaryVideoBiteRate(120000);
		} else {
			mMyRecorder.setSecondaryVideoEnable(false);
		}
		if (mIntervalState == STATE_INTERVAL_5MIN) {
			mMyRecorder.setVideoSeconds(5 * 60);
		} else {
			mMyRecorder.setVideoSeconds(3 * 60);
		}
		if (mOverlapState == STATE_OVERLAP_FIVE) {
			mMyRecorder.setVideoOverlap(5);
		}
		mMyRecorder.prepare();
	}

	private void releaseRecorder() {
		if (mMyRecorder != null) {
			mMyRecorder.stop();
			mMyRecorder.close();
			mMyRecorder.release();
			mMyRecorder = null;
			Log.d(Constant.TAG, "Record Release");
		}
	}

	@Override
	public void onError(int err) {
		Log.e(Constant.TAG, "Error : " + err);
	}

	@Override
	public void onFileSave(int type, String path) {
		/**
		 * [Type] 0-图片 1-视频
		 * 
		 * [Path] 视频：/mnt/sdcard/tachograph/2015-07-01/2015-07-01_105536.mp4
		 * 图片:/mnt/sdcard/tachograph/camera_shot/2015-07-01_105536.jpg
		 */
		deleteOldestUnlockVideo();
		if (type == 1) {
			String videoName = path.split("/")[5];
			editor.putString("sdcardPath", "/mnt/" + path.split("/")[2] + "/");
			editor.commit();
			int videoResolution = 720;
			int videoLock = 0;

			if (mResolutionState == STATE_RESOLUTION_1080P) {
				videoResolution = 1080;
			}
			if (MyApplication.isVideoLock) {
				videoLock = 1;
				MyApplication.isVideoLock = false; // 还原
				setupRecordViews(); // 更新录制按钮状态
			}
			DriveVideo driveVideo = new DriveVideo(videoName, videoLock,
					videoResolution);
			videoDb.addDriveVideo(driveVideo);
		} else {
			Toast.makeText(getApplicationContext(), "照片已保存", Toast.LENGTH_SHORT)
					.show();
		}

		// 更新Media Database
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
				Uri.parse("file://" + path)));
		Log.d(Constant.TAG, "File Save, Type=" + type);
	}

	public void setup() {
		release();
		if (openCamera()) {
			setupRecorder();
		}
	}

	public void release() {
		releaseRecorder();
		closeCamera();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// Do Nothing
			return true;
		} else
			return super.onKeyDown(keyCode, event);
	}

}
