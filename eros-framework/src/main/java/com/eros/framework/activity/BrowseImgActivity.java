package com.eros.framework.activity;


import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.eros.framework.R;
import com.eros.framework.model.BroeserImgModuleBean;
import com.eros.framework.module.glide.OnProgressListener;
import com.eros.framework.module.glide.ProgressManager;
import com.eros.framework.view.ViewPagerFix;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.ThreeBounce;

import java.util.List;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;


/**
 * 预览大图的默认实现
 */

public class BrowseImgActivity extends Activity implements ViewPager.OnPageChangeListener {


	private ViewGroup mOvalViewGroup;
	private ViewPagerFix mViewPager;
	private ViewPagerAdapter mViewPagerAdapter;
	public static final String BROWSE_IMG_BEAN = "browse_img_bean";

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_browse);
		initData();
	}

	//数据初始化
	private void initData() {
		BroeserImgModuleBean imgModuleBean = (BroeserImgModuleBean) getIntent()
				.getSerializableExtra(BROWSE_IMG_BEAN);
		if (imgModuleBean == null || imgModuleBean.getImages() == null || imgModuleBean.getImages
				().size() < 1)
			return;
		initView(imgModuleBean);

	}

	//View初始化
	private void initView(BroeserImgModuleBean imgModuleBean) {
		mViewPager = (ViewPagerFix) findViewById(R.id.viewpager_browse_img);
		mOvalViewGroup = (ViewGroup) findViewById(R.id.rl_browse_prompt_oval);
		mViewPagerAdapter = new ViewPagerAdapter(imgModuleBean.getImages());
		mViewPager.setAdapter(mViewPagerAdapter);
		mViewPager.setCurrentItem(imgModuleBean.getIndex());
		mViewPager.setOnPageChangeListener(this);
		initOval(imgModuleBean.getImages().size(), imgModuleBean.getIndex());
	}

	//添加ViewPager追踪点
	private void initOval(int size, int index) {

		for (int i = 0; i < size; i++) {
			ImageView mOvalView = new ImageView(this);
			if (i == index) {
				mOvalView.setImageResource(R.drawable.browse_shape_on);
			} else {
				mOvalView.setImageResource(R.drawable.browse_shape_off);
			}
			mOvalViewGroup.addView(mOvalView);
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

	}

	@Override
	public void onPageSelected(int position) {
		changeIndicator(position);
	}

	@Override
	public void onPageScrollStateChanged(int state) {

	}

	class ViewPagerAdapter extends PagerAdapter {

		List<String> images;

		public ViewPagerAdapter(List<String> images) {
			this.images = images;
		}


		@Override
		public int getCount() {
			return images.size();
		}


		@Override
		public boolean isViewFromObject(View view, Object obj) {
			return view == obj;
		}


		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			Glide.with(BrowseImgActivity.this).clear((View) object);
			container.removeView((View) object);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {

			final ProgressBar progressBar = new ProgressBar(container.getContext());
			progressBar.setMax(100);
			Sprite sprite = new ThreeBounce();
			sprite.setColor(getResources().getColor(R.color.colorPrimary));
			progressBar.setIndeterminateDrawable(sprite);

			PhotoView imageView = new PhotoView(container.getContext());
			imageView.setScaleType(ImageView.ScaleType.CENTER);
			imageView.setBackgroundColor(Color.argb(180,0,0,0));
			imageView.setZoomable(true);
			imageView.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
				@Override
				public void onViewTap(View view, float x, float y) {
					finish();
				}
			});
			final String url = images.get(position);
			final OnProgressListener onProgressListener = new OnProgressListener() {
				@Override
				public void onProgress(String imageUrl, long bytesRead, long totalBytes, boolean isDone, Exception exception) {

				}
			};
			ProgressManager.addProgressListener(onProgressListener);
			Glide.with(BrowseImgActivity.this)
					.load(url)
					.transition(new DrawableTransitionOptions().crossFade(500))
					.apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).fitCenter())
//					.apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).fitCenter())
					.into(new DrawableImageViewTarget(imageView) {

						@Override
						public void onLoadStarted(@Nullable Drawable placeholder) {
							super.onLoadStarted(placeholder);
							progressBar.setVisibility(View.VISIBLE);
						}

						@Override
						public void onLoadFailed(@Nullable Drawable errorDrawable) {
							super.onLoadFailed(errorDrawable);
							progressBar.setVisibility(View.GONE);
							ProgressManager.removeProgressListener(onProgressListener);
						}

						@Override
						public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
							super.onResourceReady(resource, transition);
							progressBar.setVisibility(View.GONE);
							ProgressManager.removeProgressListener(onProgressListener);
						}
					});


			FrameLayout frameLayout = new FrameLayout(container.getContext());
			frameLayout.addView(imageView, FrameLayout.LayoutParams.MATCH_PARENT, LinearLayout
					.LayoutParams.MATCH_PARENT);

			FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.gravity = Gravity.CENTER;
			frameLayout.addView(progressBar, layoutParams);

			container.addView(frameLayout, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout
					.LayoutParams.MATCH_PARENT);

			frameLayout.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
				}
			});
			return frameLayout;
		}
	}

	//重置追踪点颜色
	private void changeIndicator(int position) {
		for (int i = 0; i < mOvalViewGroup.getChildCount(); i++) {
			ImageView iv = (ImageView) mOvalViewGroup.getChildAt(i);
			if (position == i) {
				iv.setImageResource(R.drawable.browse_shape_on);
			} else {
				iv.setImageResource(R.drawable.browse_shape_off);
			}
		}
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
	}
}
