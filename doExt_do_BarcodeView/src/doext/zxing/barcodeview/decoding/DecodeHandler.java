/*
 * Copyright (C) 2010 ZXing authors
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

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.hxcode.HxDecode;

import doext.implement.do_BarcodeView_View;
import doext.zxing.barcodeview.camera.BracodeConstant;
import doext.zxing.barcodeview.camera.CameraManager;
import doext.zxing.barcodeview.camera.PlanarYUVLuminanceSource;

final class DecodeHandler extends Handler {

	private final do_BarcodeView_View activity;
	private final MultiFormatReader multiFormatReader;

	DecodeHandler(do_BarcodeView_View activity, Hashtable<DecodeHintType, Object> hints) {
		multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(hints);
		this.activity = activity;
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case BracodeConstant.DECODE:
			//Log.d(TAG, "Got decode message");
			decode((byte[]) message.obj, message.arg1, message.arg2);
			break;
		case BracodeConstant.QUIT:
			Looper.myLooper().quit();
			break;
		}
	}

	/**
	 * Decode the data within the viewfinder rectangle, and time how long it
	 * took. For efficiency, reuse the same reader objects from one decode to
	 * the next.
	 *
	 * @param data
	 *            The YUV preview frame.
	 * @param width
	 *            The width of the preview frame.
	 * @param height
	 *            The height of the preview frame.
	 */
	private void decode(byte[] data, int width, int height) {
		Result rawResult = null;
		//汉信码
		HxDecode hxDecode = new HxDecode();
		byte[] imgbytes = new byte[width * height];
		int iwidth = hxDecode.preprocessImg(data, width, height, imgbytes);
		if (iwidth > 0 && imgbytes != null) {
			byte[] decodebytes = new byte[4 * 1024];
			iwidth = hxDecode.DeCodeCsbyte(imgbytes, iwidth, decodebytes);
			if (iwidth > 0) {
				try {
					if (isUTF8(decodebytes, 0, iwidth)) {
						rawResult = new Result(new String(decodebytes, 0, iwidth, "UTF-8"), imgbytes, null, BarcodeFormat.HX_CODE);
					} else {
						rawResult = new Result(new String(decodebytes, 0, iwidth, "GBK"), imgbytes, null, BarcodeFormat.HX_CODE);
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		if (rawResult == null) {
			//modify here
			byte[] rotatedData = new byte[data.length];
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++)
					rotatedData[x * height + height - y - 1] = data[x + y * width];
			}
			int tmp = width; // Here we are swapping, that's the difference to #11
			width = height;
			height = tmp;

			PlanarYUVLuminanceSource source = CameraManager.get().buildLuminanceSource(rotatedData, width, height);
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
			try {
				rawResult = multiFormatReader.decodeWithState(bitmap);
			} catch (ReaderException re) {
				// continue
			} finally {
				multiFormatReader.reset();
			}
		}

		if (rawResult != null) {
			Message message = Message.obtain(activity.getHandler(), BracodeConstant.DECODE_SUCCEEDED, rawResult);
			message.sendToTarget();
		} else {
			Message message = Message.obtain(activity.getHandler(), BracodeConstant.DECODE_FAILED);
			message.sendToTarget();
		}
	}

	private boolean isUTF8(byte[] data, int start, int end) {
		boolean canBeUTF8 = true;
		for (int i = start; i < end; i++) {
			int value = data[i] & 0xFF;

			//两字节情况
			if (value >= 0xC0 && value <= 0xDF) {
				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}
			} else if (value >= 0xE0 && value <= 0xEF) { //三字节情况
				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}

				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}
			} else if (value >= 0xF0 && value <= 0xF7) { //四字节情况
				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}

				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}

				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}
			} else if (value >= 0xF8 && value <= 0xFB) { //五字节情况
				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}

				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}

				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}

				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}
			} else if (value >= 0xFC && value <= 0xFD) { //六字节情况
				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}

				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}

				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}

				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}

				i++;
				value = data[i] & 0xFF;
				if (value >= 0x80 && value <= 0xBF) {

				} else {
					return false;
				}
			}

		}
		return canBeUTF8;
	}

}
