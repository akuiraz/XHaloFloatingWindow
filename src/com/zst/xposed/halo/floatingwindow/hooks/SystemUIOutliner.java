package com.zst.xposed.halo.floatingwindow.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.zst.xposed.halo.floatingwindow.Common;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SystemUIOutliner {
	
	private static Context mContext;
	private static View mOutline;
	private static WindowManager mWm;
	
	static final int HIDE = -1;
	
	public static void handleLoadPackage(LoadPackageParam lpp) {
		if (!lpp.packageName.equals("com.android.systemui")) return;
		
		try {
			focusChangeContextFinder(lpp);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "Movable / SystemUIOutliner");
			XposedBridge.log(e);
		}
	}
	
	private static void focusChangeContextFinder(LoadPackageParam l) throws Throwable {
		Class<?> hookClass = findClass("com.android.systemui.SystemUIService", l.classLoader);
		XposedBridge.hookAllMethods(hookClass, "onCreate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Service thiz = (Service) param.thisObject;
				mContext = thiz.getApplicationContext();
				mContext.registerReceiver(mIntentReceiver, new IntentFilter(Common.SHOW_OUTLINE));
				mWm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
				createOutlineView(mContext);
			}
		});
	}
	
	final static BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			int[] array = intent.getIntArrayExtra(Common.INTENT_APP_PARAMS);
			if (array != null) {
				refreshOutlineView(ctx, array[0], array[1], array[2], array[3]);
			} else {
				refreshOutlineView(ctx, HIDE, HIDE, HIDE, HIDE);
			}
		}
	};
	
	private static void createOutlineView(Context ctx) {
		WindowManager.LayoutParams layOutParams = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
				PixelFormat.TRANSLUCENT);
		layOutParams.gravity = Gravity.TOP | Gravity.LEFT;
		mOutline = getOutlineView(ctx, 0xFF33b5e5);
		mOutline.setFocusable(false);
		mOutline.setClickable(false);
		mOutline.setVisibility(View.GONE);
		
		mWm.addView(mOutline, layOutParams);
	}
	
	private static void refreshOutlineView(Context ctx, int x, int y, int height, int width) {
		if (x == HIDE || y == HIDE || height == HIDE || width == HIDE) {
			mOutline.setVisibility(View.GONE);
			return;
		}
		if (mOutline == null) {
			createOutlineView(ctx);
		}
		WindowManager.LayoutParams param = (WindowManager.LayoutParams) mOutline.getLayoutParams();
		param.x = x;
		param.y = y;
		param.height = height;
		param.width = width;		
		mWm.updateViewLayout(mOutline, param);
		mOutline.setVisibility(View.VISIBLE);
	}
	
	private static View getOutlineView(Context ctx, int color) {
		FrameLayout outline = new FrameLayout(ctx);
		
		ShapeDrawable rectShapeDrawable = new ShapeDrawable(new RectShape());
		Paint paint = rectShapeDrawable.getPaint();
		paint.setColor(color);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(dp(4, ctx));
		outline.setBackgroundDrawable(rectShapeDrawable);
		
		View filling = new View(ctx);
		filling.setBackgroundColor(color);
		filling.setAlpha(0.5f);
		outline.addView(filling);
		
		return outline;
	}
	
	public static int dp(int dp, Context c) { // convert dp to px
		float scale = c.getResources().getDisplayMetrics().density;
		int pixel = (int) (dp * scale + 0.5f);
		return pixel;
	}
}
