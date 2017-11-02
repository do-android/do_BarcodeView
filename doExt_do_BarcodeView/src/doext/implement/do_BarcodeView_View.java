package doext.implement;

import java.util.Map;
import java.util.Vector;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import core.DoServiceContainer;
import core.helper.DoJsonHelper;
import core.helper.DoResourcesHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoBaseActivityListener;
import core.interfaces.DoIModuleTypeID;
import core.interfaces.DoIPageView;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoUIModule;
import doext.define.do_BarcodeView_IMethod;
import doext.define.do_BarcodeView_MAbstract;
import doext.zxing.barcodeview.camera.CameraManager;
import doext.zxing.barcodeview.decoding.CaptureActivityHandler;
import doext.zxing.barcodeview.view.DoViewfinderView;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,do_BarcodeView_IMethod接口；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_BarcodeView_View extends FrameLayout implements DoIUIModuleView, do_BarcodeView_IMethod, SurfaceHolder.Callback, DoIModuleTypeID, DoBaseActivityListener {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_BarcodeView_MAbstract model;

	private Context context;
	private DoIScriptEngine scriptEngine;
	private String callbackFuncName;

	private CaptureActivityHandler handler;
	private DoViewfinderView viewfinderView;
	private boolean hasSurface;
	private boolean isStart;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private SurfaceView surfaceView;

	private int defaultLeft;
	private int defaultRight;
	private int defaultTop;
	private int defaultButton;

	private double xZoom;
	private double yZoom;

	private int screenWidth;
	private int screenHeight;

	private int width;
	private int height;

	private ImageView scanLineImageView;
	private Rect rect;

	public do_BarcodeView_View(Context context) {
		super(context);
		this.context = context;
		CameraManager.init(context.getApplicationContext());
		((DoIPageView) context).setBaseActivityListener(this);
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_BarcodeView_MAbstract) _doUIModule;
		width = (int) _doUIModule.getWidth();
		height = (int) _doUIModule.getHeight();

		xZoom = _doUIModule.getXZoom();
		yZoom = _doUIModule.getYZoom();

		screenWidth = (int) _doUIModule.getRealWidth();
		screenHeight = (int) _doUIModule.getRealHeight();

		defaultLeft = width / 4;
		defaultRight = width * 3 / 4;
		defaultTop = height / 4;
		defaultButton = height * 3 / 4;

		rect = new Rect((int) (defaultLeft * xZoom), (int) (defaultTop * yZoom), (int) (defaultRight * xZoom), (int) (defaultButton * yZoom));
		CameraManager.get().setRect(rect);

		surfaceView = new SurfaceView(context);
		FrameLayout.LayoutParams surfaceViewParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		this.addView(surfaceView, surfaceViewParams);
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("scanArea")) {
			String _scanArea = _changedValues.get("scanArea");
			if (null != _scanArea && !"".equals(_scanArea)) {
				String[] _area = _scanArea.split(",");
				if (null != _area && _area.length == 4) {
					int _left = DoTextHelper.strToInt(_area[0], defaultLeft);
					int _top = DoTextHelper.strToInt(_area[1], defaultTop);

					int _width = DoTextHelper.strToInt(_area[2], defaultRight - defaultLeft);
					int _height = DoTextHelper.strToInt(_area[3], defaultButton - defaultTop);

					int _right = _left + _width;
					if (_right > width) {
						_right = width;
					}

					int _bottom = _top + _height;
					if (_bottom > height) {
						_bottom = height;
					}
					rect = new Rect((int) (_left * xZoom), (int) (_top * yZoom), (int) (_right * xZoom), (int) (_bottom * yZoom));
					CameraManager.get().setRect(rect);
				}
			}
		}

	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("flash".equals(_methodName)) {
			flash(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		if ("start".equals(_methodName)) { // 执行动画
			this.start(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		// ...do something
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	/**
	 * 启动扫描；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void start(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		this.scriptEngine = _scriptEngine;
		this.callbackFuncName = _callbackFuncName;
		if (null != handler) {
			handler.init(); // 启动线程解码
		} else {
			isStart = true;
		}

		((Activity) context).runOnUiThread(new Runnable() {
			@Override
			public void run() {

				if (viewfinderView == null) {
					viewfinderView = new DoViewfinderView(context, rect);
					do_BarcodeView_View.this.addView(viewfinderView);
				} else {
					viewfinderView.setFrame(rect);
				}

				if (scanLineImageView == null) {
					scanLineImageView = new ImageView(context);
					scanLineImageView.setImageResource(DoResourcesHelper.getIdentifier("qrcode_scan_line", "drawable", do_BarcodeView_View.this));
					do_BarcodeView_View.this.addView(scanLineImageView);
				} else {
					scanLineImageView.clearAnimation();
				}

				FrameLayout.LayoutParams scanLineImageViewParams = new FrameLayout.LayoutParams(rect.width(), rect.height());
				scanLineImageViewParams.leftMargin = rect.left;
				scanLineImageViewParams.topMargin = rect.top - (rect.height() / 2);
				scanLineImageView.setLayoutParams(scanLineImageViewParams);
				scanLineImageView.setVisibility(View.VISIBLE);

				TranslateAnimation anim = new TranslateAnimation(0, 0, 0, rect.height());
				anim.setRepeatCount(-1);
				anim.setDuration(2000);
				anim.setRepeatMode(Animation.REVERSE);
				anim.setInterpolator(new AccelerateDecelerateInterpolator());

				scanLineImageView.startAnimation(anim);
			}
		});
	}

	@Override
	public String getTypeID() {
		return model.getTypeID();
	}

	/**
	 * 处理扫描结果
	 * 
	 * @param result
	 * @param barcode
	 */
	public void handleDecode(Result result, Bitmap barcode) {
		try {
			stopBarcodeScan();
			Log.d("CaptureActivityHandler", result.getText());
			DoInvokeResult jsonResult = new DoInvokeResult(this.model.getUniqueKey());
			JSONObject _jsonNode = new JSONObject();
			_jsonNode.put("code", result.getBarcodeFormat().name());
			_jsonNode.put("value", result.getText());
			jsonResult.setResultNode(_jsonNode);
			this.scriptEngine.callback(this.callbackFuncName, jsonResult);
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("do_BardcodeView : barcode_decode_success\n", e);
		}
	}

	private void stopBarcodeScan() {
		if (scanLineImageView != null) { // 停止扫描线动画
			((Activity) context).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					scanLineImageView.clearAnimation();
					do_BarcodeView_View.this.removeView(scanLineImageView);
					scanLineImageView = null;
				}
			});
		}
		CameraManager.get().stopPreview();
	}

	public Handler getHandler() {
		return handler;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
			if (isStart) {
				if (null != handler) {
					handler.init(); // 启动线程解码
				}
			}
			isStart = false;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	@Override
	public void onResume() {
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		decodeFormats = null;
		characterSet = null;
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder, screenWidth, screenHeight);
			CameraManager.get().startPreview();
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("do_BardcodeView : initCamera\n", e);
			return;
		}
		if (handler == null) {
			handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
		}
	}

	@Override
	public void onPause() {
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
		stopBarcodeScan();
	}

	@Override
	public void onRestart() {

	}

	@Override
	public void onStop() {

	}

	/**
	 * 开关闪光灯；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void flash(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _status = DoJsonHelper.getString(_dictParas, "status", "on");
		CameraManager.get().flash("on".equals(_status));
	}
}