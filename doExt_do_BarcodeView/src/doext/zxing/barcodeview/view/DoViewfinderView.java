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

package doext.zxing.barcodeview.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the laser scanner
 * animation and result points.
 * 
 */
public final class DoViewfinderView extends View {
	private Paint paint;
	private Rect frame;

	public DoViewfinderView(Context context, Rect frame) {
		super(context);
		this.frame = frame;
		paint = new Paint();
	}

	public void setFrame(Rect frame) {
		this.frame = frame;
		this.invalidate();
	}

	@Override
	public void onDraw(Canvas canvas) {
		// 获取屏幕的宽和高
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		paint.setColor(Color.parseColor("#60000000"));
		// 画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
		// 扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
		canvas.drawRect(0, 0, width, frame.top, paint); // 上
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint); // 左
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint); // 右
		canvas.drawRect(0, frame.bottom + 1, width, height, paint); // 下
	}
}
