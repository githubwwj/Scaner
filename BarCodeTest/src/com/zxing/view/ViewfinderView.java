package com.zxing.view;

/**
 2015-07-31 @author chenjb 
 */

import java.util.Collection;
import java.util.HashSet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;

import com.ericssonlabs.R;
import com.google.zxing.ResultPoint;
import com.zxing.camera.CameraManager;

/**
 * 该视图是覆盖在相机的预览视图之上的一层视图。扫描区构成原理，其实是在预览视图上画四块遮罩层，
 * 中间留下的部分保持透明，并画上一条激光线，实际上该线条就是展示而已，与扫描功能没有任何关系。
 */
public final class ViewfinderView extends View {
	/**
	 * 刷新界面的时间
	 */
	private static final long ANIMATION_DELAY = 10L;
	private static final int OPAQUE = 0xFF;

	/**
	 * 中间那条线每次刷新移动的距离
	 */
	private static int SPEEN_DISTANCE;

	/**
	 * 手机的屏幕密度
	 */
	private static float density;
	/**
	 * 画笔对象的引用
	 */
	private Paint paint;

	/**
	 * 中间滑动线的最顶端位置
	 */
	private int slideTop;

	/**
	 * 将扫描的二维码拍下来，这里没有这个功能，暂时不考虑
	 */
	private Bitmap resultBitmap;
	private final int maskColor;
	private final int resultColor;
	private final int resultPointColor;
	private Collection<ResultPoint> possibleResultPoints;
	private Collection<ResultPoint> lastPossibleResultPoints;
	private int CORNER_PADDING;
	boolean isFirst;

	private Bitmap bitmapCornerTopleft;
	private Bitmap bitmapCornerTopright;
	private Bitmap bitmapCornerBottomLeft;
	private Bitmap bitmapCornerBottomRight;

	@SuppressLint("NewApi")
	public ViewfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		density = context.getResources().getDisplayMetrics().density;
		SPEEN_DISTANCE = (int) (3 * density);
		CORNER_PADDING = dip2px(context, 0.0F);
		paint = new Paint();
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.result_minor_text);
		resultColor = resources.getColor(R.color.result_view);

		resultPointColor = resources.getColor(R.color.possible_result_points);
		possibleResultPoints = new HashSet<ResultPoint>(5);
		bitmapCornerTopleft = BitmapFactory.decodeResource(resources,
				R.drawable.zxing_scan_corner_top_left);
		bitmapCornerTopright = BitmapFactory.decodeResource(resources,
				R.drawable.zxing_scan_corner_top_right);
		bitmapCornerBottomLeft = BitmapFactory.decodeResource(resources,
				R.drawable.zxing_scan_corner_bottom_left);
		bitmapCornerBottomRight = BitmapFactory.decodeResource(resources,
				R.drawable.zxing_scan_corner_bottom_right);

	}

	@SuppressLint("DrawAllocation")
	@Override
	public void onDraw(Canvas canvas) {
		try {

			// 中间的扫描框，要修改扫描框的大小，去CameraManager里面修改
			Rect frame = CameraManager.get().getFramingRect();
			if (frame == null) {
				return;
			}

			// 初始化中间线滑动的最上边和最下边
			if (!isFirst) {
				isFirst = true;
				slideTop = frame.top;
			}

			// 获取屏幕的宽和高
			int width = canvas.getWidth();
			int height = canvas.getHeight();

			paint.setColor(resultBitmap != null ? resultColor : maskColor);

			// 画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
			// 扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
			canvas.drawRect(0, 0, width, frame.top, paint);
			canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
			canvas.drawRect(frame.right + 1, frame.top, width,
					frame.bottom + 1, paint);
			canvas.drawRect(0, frame.bottom + 1, width, height, paint);
			if (resultBitmap != null) {
				// 绘制扫描结果的图
				// paint.setAlpha(OPAQUE);
				// canvas.drawBitmap(resultBitmap, frame.left, frame.top,
				// paint);
			} else {

				// 画扫描框边上的角
				drawRectEdges(canvas, frame);

				// 绘制扫描线
				drawScanningLine(canvas, frame);
				// 绘制中间的线,每次刷新界面，中间的线往下移动SPEEN_DISTANCE
				// slideTop += SPEEN_DISTANCE;
				// if(slideTop >= frame.bottom){
				// slideTop = frame.top;
				// }
				// canvas.drawRect(frame.left + MIDDLE_LINE_PADDING, slideTop -
				// MIDDLE_LINE_WIDTH/2, frame.right -
				// MIDDLE_LINE_PADDING,slideTop + MIDDLE_LINE_WIDTH/2, paint);

				// 画扫描框下面的字
				// paint.setColor(Color.WHITE);
				// paint.setTextSize(TEXT_SIZE * density);
				// paint.setAlpha(0x40);
				// paint.setTypeface(Typeface.create("System", Typeface.BOLD));
				// canvas.drawText(
				// getResources().getString(R.string.scanner_hint),
				// frame.left,
				// (float) (frame.bottom + (float) TEXT_PADDING_TOP
				// * density), paint);

				Collection<ResultPoint> currentPossible = possibleResultPoints;
				Collection<ResultPoint> currentLast = lastPossibleResultPoints;
				if (currentPossible.isEmpty()) {
					lastPossibleResultPoints = null;
				} else {
					possibleResultPoints = new HashSet<ResultPoint>(5);
					lastPossibleResultPoints = currentPossible;
					paint.setAlpha(OPAQUE);
					paint.setColor(resultPointColor);
					for (ResultPoint point : currentPossible) {
						canvas.drawCircle(frame.left + point.getX(), frame.top
								+ point.getY(), 6.0f, paint);
					}
				}
				if (currentLast != null) {
					paint.setAlpha(OPAQUE / 2);
					paint.setColor(resultPointColor);
					for (ResultPoint point : currentLast) {
						canvas.drawCircle(frame.left + point.getX(), frame.top
								+ point.getY(), 3.0f, paint);
					}
				}

				// 只刷新扫描框的内容，其他地方不刷新
				postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top,
						frame.right, frame.bottom);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 描绘方形的四个角
	 * 
	 * @param canvas
	 * @param frame
	 */
	private void drawRectEdges(Canvas canvas, Rect frame) {

		paint.setColor(Color.WHITE);
		paint.setAlpha(OPAQUE);

		Resources resources = getResources();
		/**
		 * 这些资源可以用缓存进行管理，不需要每次刷新都新建
		 */

		canvas.drawBitmap(bitmapCornerTopleft, frame.left + CORNER_PADDING,
				frame.top, paint);
		canvas.drawBitmap(bitmapCornerTopright, frame.right - CORNER_PADDING
				- bitmapCornerTopright.getWidth(), frame.top, paint);
		canvas.drawBitmap(bitmapCornerBottomLeft, frame.left + CORNER_PADDING,
				2 + (frame.bottom - CORNER_PADDING - bitmapCornerBottomLeft
						.getHeight()), paint);
		canvas.drawBitmap(bitmapCornerBottomRight, frame.right - CORNER_PADDING
				- bitmapCornerBottomRight.getWidth(), 2 + (frame.bottom
				- CORNER_PADDING - bitmapCornerBottomRight.getHeight()), paint);

		// bitmapCornerTopleft.recycle();
		// bitmapCornerTopleft = null;
		// bitmapCornerTopright.recycle();
		// bitmapCornerTopright = null;
		// bitmapCornerBottomLeft.recycle();
		// bitmapCornerBottomLeft = null;
		// bitmapCornerBottomRight.recycle();
		// bitmapCornerBottomRight = null;

	}

	/**
	 * 绘制扫描线
	 * 
	 * @param canvas
	 * @param frame
	 *            扫描框
	 */
	private void drawScanningLine(Canvas canvas, Rect frame) {

		slideTop += SPEEN_DISTANCE;
		if (slideTop >= frame.bottom) {
			slideTop = frame.top;
		}
		Rect lineRect = new Rect();
		lineRect.left = frame.left;
		lineRect.right = frame.right;

		lineRect.top = slideTop;
		lineRect.bottom = slideTop + 18;
		canvas.drawBitmap(((BitmapDrawable) (getResources()
				.getDrawable(R.drawable.zxing_scan_laser))).getBitmap(), null,
				lineRect, paint);

	}

	public void drawViewfinder() {
		resultBitmap = null;
		invalidate();
	}

	/**
	 * dp转px
	 * 
	 * @param context
	 * @param dipValue
	 * @return
	 */
	public int dip2px(Context context, float dipValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}

	/**
	 * Draw a bitmap with the result points highlighted instead of the live
	 * scanning display.
	 * 
	 * @param barcode
	 *            An image of the decoded barcode.
	 */
	public void drawResultBitmap(Bitmap barcode) {
		resultBitmap = barcode;
		invalidate();
	}

	public void addPossibleResultPoint(ResultPoint point) {
		possibleResultPoints.add(point);
	}

}
