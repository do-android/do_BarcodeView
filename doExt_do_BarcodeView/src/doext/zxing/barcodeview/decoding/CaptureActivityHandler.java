/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package doext.zxing.barcodeview.decoding;

import java.util.Vector;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import doext.implement.do_BarcodeView_View;
import doext.zxing.barcodeview.camera.BracodeConstant;
import doext.zxing.barcodeview.camera.CameraManager;
import doext.zxing.barcodeview.view.ViewfinderResultPointCallback;

/**
 * 处理二维码的回调handler
 */
public final class CaptureActivityHandler extends Handler {

	private static final String TAG = CaptureActivityHandler.class.getSimpleName();

	private final do_BarcodeView_View activity;
	private DecodeThread decodeThread;// 检测线程
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private State state;

	private enum State {
		PREVIEW, SUCCESS, DONE
	}

	public CaptureActivityHandler(do_BarcodeView_View activity, Vector<BarcodeFormat> decodeFormats, String characterSet) {
		this.activity = activity;
		this.decodeFormats = decodeFormats;
		this.characterSet = characterSet;
	}

	public void init() {
		decodeThread = new DecodeThread(activity, decodeFormats, characterSet, new ViewfinderResultPointCallback());
		decodeThread.start();
		state = State.SUCCESS;
		// Start ourselves capturing previews and decoding.
		CameraManager.get().startPreview();
		restartPreviewAndDecode();

	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case BracodeConstant.AUTO_FOCUS:
			Log.d(TAG, "Got restart AUTO_FOCUS");
			if (state == State.PREVIEW) {
				CameraManager.get().requestAutoFocus(this, BracodeConstant.AUTO_FOCUS);
			}
			break;
		case BracodeConstant.RESTART_PREVIEW:
			Log.d(TAG, "Got restart preview message");
			restartPreviewAndDecode();
			break;
		case BracodeConstant.DECODE_SUCCEEDED: {
			// 成功取得二维码

			Log.d(TAG, "Got decode succeeded message");
			state = State.SUCCESS;
			Bundle bundle = message.getData();
			Bitmap barcode = bundle == null ? null : (Bitmap) bundle.getParcelable(DecodeThread.BARCODE_BITMAP);
			activity.handleDecode((Result) message.obj, barcode);
			break;

		}
		case BracodeConstant.DECODE_FAILED:
			// We're decoding as fast as possible, so when one decode
			// fails,start another.
			state = State.PREVIEW;
			CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), BracodeConstant.DECODE);
			break;
		case BracodeConstant.RETURN_SCAN_RESULT:
			Log.d(TAG, "Got return scan result message");
//			activity.setResult(Activity.RESULT_OK, (Intent) message.obj);
//			activity.finish();
			break;
		case BracodeConstant.LAUNCH_PRODUCT_QUERY:
			Log.d(TAG, "Got product query message");
//			String url = (String) message.obj;
//			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//			activity.startActivity(intent);
			break;
		}
	}

	public void quitSynchronously() {
		state = State.DONE;
		CameraManager.get().stopPreview();
		try {
			if (null != decodeThread) {
				Message quit = Message.obtain(decodeThread.getHandler(), BracodeConstant.QUIT);
				quit.sendToTarget();
				decodeThread.join();
			}
		} catch (InterruptedException e) {
			// continue
		}

		// Be absolutely sure we don't send any queued up messages
		removeMessages(BracodeConstant.DECODE_SUCCEEDED);
		removeMessages(BracodeConstant.DECODE_FAILED);
	}

	private void restartPreviewAndDecode() {
		if (state == State.SUCCESS) {
			state = State.PREVIEW;
			CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), BracodeConstant.DECODE);
			CameraManager.get().requestAutoFocus(this, BracodeConstant.AUTO_FOCUS);
//			activity.drawViewfinder();
		}
	}

}
