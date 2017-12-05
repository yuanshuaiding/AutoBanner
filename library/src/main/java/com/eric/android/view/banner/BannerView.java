package com.eric.android.view.banner;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Handler;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Scroller;

import com.eric.android.view.banner.holder.BannerHolderCreator;
import com.eric.android.view.banner.holder.BannerViewHolder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class BannerView<T> extends RelativeLayout {
    private BannerViewPager mViewPager;
    private BannerPagerAdapter mAdapter;
    private List<T> mDatas;
    private boolean mIsAutoPlay = true;// 是否自动播放
    private int mCurrentItem = 0;//当前位置
    private Handler mHandler = new Handler();
    private int mDelayedTime = 5000;// Banner 切换时间间隔
    private ViewPagerScroller mViewPagerScroller;//控制ViewPager滑动速度的Scroller
    private boolean mIsCanLoop = true;// 是否轮播图片
    private LinearLayout mIndicatorContainer;//indicator容器
    private ArrayList<ImageView> mIndicators = new ArrayList<>();
    //mIndicatorRes[0] 为为选中，mIndicatorRes[1]为选中
    private int[] mIndicatorRes = new int[]{R.drawable.indicator_normal, R.drawable.indicator_selected};
    private int mIndicatorPaddingLeft = 0;// indicator 距离左边的距离
    private int mIndicatorPaddingRight = 0;//indicator 距离右边的距离
    private int mIndicatorPaddingTop = 0;//indicator 距离上边的距离
    private int mIndicatorPaddingBottom = 0;//indicator 距离下边的距离
    private int mIndicatorAlign = 1;
    private ViewPager.OnPageChangeListener mOnPageChangeListener;
    private BannerPageClickListener mBannerPageClickListener;

    public enum IndicatorAlign {
        LEFT,//做对齐
        CENTER,//居中对齐
        RIGHT //右对齐
    }

    public BannerView(@NonNull Context context) {
        this(context, null);
    }

    public BannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BannerView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readAttrs(context, attrs);
        init();
    }

    private void readAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BannerView);
        if (typedArray != null) {
            mIsCanLoop = typedArray.getBoolean(R.styleable.BannerView_canLoop, true);
            mIndicatorAlign = typedArray.getInt(R.styleable.BannerView_indicatorPosition, 1);
            mIndicatorPaddingLeft = typedArray.getDimensionPixelSize(R.styleable.BannerView_indicatorPaddingLeft, 0);
            mIndicatorPaddingRight = typedArray.getDimensionPixelSize(R.styleable.BannerView_indicatorPaddingRight, 0);
            mIndicatorPaddingTop = typedArray.getDimensionPixelSize(R.styleable.BannerView_indicatorPaddingTop, 0);
            mIndicatorPaddingBottom = typedArray.getDimensionPixelSize(R.styleable.BannerView_indicatorPaddingBottom, 0);
            typedArray.recycle();
        }
    }


    private void init() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_banner_with_indicator, this, true);
        mIndicatorContainer = view.findViewById(R.id.banner_indicator_container);
        mViewPager = view.findViewById(R.id.vp_banner);
        mViewPager.setOffscreenPageLimit(4);

        // 初始化Scroller
        initViewPagerScroll();

        if (mIndicatorAlign == 0) {
            setIndicatorAlign(IndicatorAlign.LEFT);
        } else if (mIndicatorAlign == 1) {
            setIndicatorAlign(IndicatorAlign.CENTER);
        } else {
            setIndicatorAlign(IndicatorAlign.RIGHT);
        }


    }

    /**
     * 设置ViewPager的滑动速度
     */
    private void initViewPagerScroll() {
        try {
            Field mScroller;
            mScroller = ViewPager.class.getDeclaredField("mScroller");
            mScroller.setAccessible(true);
            mViewPagerScroller = new ViewPagerScroller(mViewPager.getContext());
            mScroller.set(mViewPager, mViewPagerScroller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private final Runnable mLoopRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsAutoPlay) {
                mCurrentItem = mViewPager.getCurrentItem();
                mCurrentItem++;
                if (mCurrentItem == mAdapter.getCount() - 1) {
                    mCurrentItem = 0;
                    mViewPager.setCurrentItem(mCurrentItem, false);
                    mHandler.postDelayed(this, mDelayedTime);
                } else {
                    mViewPager.setCurrentItem(mCurrentItem);
                    mHandler.postDelayed(this, mDelayedTime);
                }
            } else {
                mHandler.postDelayed(this, mDelayedTime);
            }
        }
    };


    /**
     * 初始化指示器Indicator
     */
    private void initIndicator() {
        mIndicatorContainer.removeAllViews();
        mIndicators.clear();

        for (int i = 0; i < mDatas.size(); i++) {
            ImageView imageView = new ImageView(getContext());
            if (mIndicatorAlign == IndicatorAlign.LEFT.ordinal()) {
                if (i == 0) {
                    int paddingLeft = mIndicatorPaddingLeft;
                    imageView.setPadding(paddingLeft + 6, 0, 6, 0);
                } else {
                    imageView.setPadding(6, 0, 6, 0);
                }

            } else if (mIndicatorAlign == IndicatorAlign.RIGHT.ordinal()) {
                if (i == mDatas.size() - 1) {
                    int paddingRight = mIndicatorPaddingRight;
                    imageView.setPadding(6, 0, 6 + paddingRight, 0);
                } else {
                    imageView.setPadding(6, 0, 6, 0);
                }

            } else {
                imageView.setPadding(6, 0, 6, 0);
            }

            if (i == (mCurrentItem % mDatas.size())) {
                imageView.setImageResource(mIndicatorRes[1]);
            } else {
                imageView.setImageResource(mIndicatorRes[0]);
            }

            mIndicators.add(imageView);
            mIndicatorContainer.addView(imageView);
            if (mDatas.size() > 1) {
                mIndicatorContainer.setVisibility(VISIBLE);
            } else {
                //只有一张时无需显示指示器
                mIndicatorContainer.setVisibility(GONE);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!mIsCanLoop) {
            return super.dispatchTouchEvent(ev);
        }
        switch (ev.getAction()) {
            // 按住Banner的时候，停止自动轮播
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_DOWN:
                int paddingLeft = mViewPager.getLeft();
                float touchX = ev.getRawX();
                //判断是否在触摸在有效图片区域
                if (touchX >= paddingLeft && touchX < getScreenWidth(getContext()) - paddingLeft) {
                    mIsAutoPlay = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                mIsAutoPlay = true;
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    public static int getScreenWidth(Context context) {
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        return dm.widthPixels;
    }

    /**
     * 开始轮播
     */
    public void start() {
        // 如果Adapter为null, 说明还没有设置数据，这个时候不应该轮播Banner
        if (mAdapter == null) {
            return;
        }
        if (mIsCanLoop && mDatas.size() > 1) {
            mIsAutoPlay = true;
            mHandler.postDelayed(mLoopRunnable, mDelayedTime);
        } else {
            mIsAutoPlay = false;
            mIsCanLoop = false;
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 停止轮播
     */
    public void pause() {
        mIsAutoPlay = false;
        mHandler.removeCallbacks(mLoopRunnable);
    }

    /**
     * 设置BannerView 的切换时间间隔
     */
    public void setDelayedTime(int delayedTime) {
        mDelayedTime = delayedTime;
    }

    public void addPageChangeLisnter(ViewPager.OnPageChangeListener onPageChangeListener) {
        mOnPageChangeListener = onPageChangeListener;
    }

    /**
     * 添加Page点击事件
     */
    public void setBannerPageClickListener(BannerPageClickListener bannerPageClickListener) {
        mBannerPageClickListener = bannerPageClickListener;
    }

    /**
     * 是否显示Indicator
     *
     * @param visible true 显示Indicator，否则不显示
     */
    public void setIndicatorVisible(boolean visible) {
        if (visible) {
            mIndicatorContainer.setVisibility(VISIBLE);
        } else {
            mIndicatorContainer.setVisibility(GONE);
        }
    }

    /**
     * 返回ViewPager
     *
     * @return {@link ViewPager}
     */
    public ViewPager getViewPager() {
        return mViewPager;
    }

    /**
     * 设置indicator 图片资源
     *
     * @param unSelectRes 未选中状态资源图片
     * @param selectRes   选中状态资源图片
     */
    public void setIndicatorRes(@DrawableRes int unSelectRes, @DrawableRes int selectRes) {
        mIndicatorRes[0] = unSelectRes;
        mIndicatorRes[1] = selectRes;
    }

    /**
     * 设置数据，这是最重要的一个方法。
     * <p>其他的配置应该在这个方法之前调用</p>
     *
     * @param datas               Banner 展示的数据集合
     * @param bannerHolderCreator ViewHolder生成器
     */
    public void setPages(List<T> datas, BannerHolderCreator bannerHolderCreator) {
        if (datas == null || bannerHolderCreator == null) {
            return;
        }
        mDatas = datas;
        //如果在播放，就先让播放停止
        pause();
        // 将Indicator初始化放在Adapter的初始化之前，解决更新数据变化更新时crush.
        //初始化Indicator
        initIndicator();
        //如果只有一个图片,则不需要轮播
        if (mDatas.size() < 2) {
            mIsCanLoop = false;
        }
        mAdapter = new BannerPagerAdapter<>(datas, bannerHolderCreator, mIsCanLoop);
        mAdapter.setUpViewViewPager(mViewPager);
        mAdapter.setPageClickListener(mBannerPageClickListener);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                int realPosition = position % mIndicators.size();
                if (mOnPageChangeListener != null) {
                    mOnPageChangeListener.onPageScrolled(realPosition, positionOffset, positionOffsetPixels);
                }
            }

            @Override
            public void onPageSelected(int position) {
                mCurrentItem = position;
                // 切换indicator
                int realSelectPosition = mCurrentItem % mIndicators.size();
                for (int i = 0; i < mDatas.size(); i++) {
                    if (i == realSelectPosition) {
                        mIndicators.get(i).setImageResource(mIndicatorRes[1]);
                    } else {
                        mIndicators.get(i).setImageResource(mIndicatorRes[0]);
                    }
                }
                // 不能直接将mOnPageChangeListener 设置给ViewPager ,否则拿到的position 是原始的positon
                if (mOnPageChangeListener != null) {
                    mOnPageChangeListener.onPageSelected(realSelectPosition);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                switch (state) {
                    case ViewPager.SCROLL_STATE_DRAGGING:
                        mIsAutoPlay = false;
                        break;
                    case ViewPager.SCROLL_STATE_SETTLING:
                        mIsAutoPlay = true;
                        break;

                }
                if (mOnPageChangeListener != null) {
                    mOnPageChangeListener.onPageScrollStateChanged(state);
                }
            }
        });


    }

    /**
     * 设置Indicator 的对齐方式
     *
     * @param indicatorAlign {@link IndicatorAlign#CENTER }{@link IndicatorAlign#LEFT }{@link IndicatorAlign#RIGHT }
     */
    public void setIndicatorAlign(IndicatorAlign indicatorAlign) {
        mIndicatorAlign = indicatorAlign.ordinal();
        LayoutParams layoutParams = (LayoutParams) mIndicatorContainer.getLayoutParams();
        if (indicatorAlign == IndicatorAlign.LEFT) {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        } else if (indicatorAlign == IndicatorAlign.RIGHT) {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        } else {
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        }

        // 设置Indicator 的上下边距。
        layoutParams.setMargins(0, mIndicatorPaddingTop, 0, mIndicatorPaddingBottom);
        mIndicatorContainer.setLayoutParams(layoutParams);
    }

    /**
     * 设置ViewPager切换的速度
     *
     * @param duration 切换动画时间
     */
    public void setDuration(int duration) {
        mViewPagerScroller.setDuration(duration);
    }

    /**
     * 设置是否使用ViewPager默认是的切换速度
     *
     * @param useDefaultDuration 切换动画时间
     */
    public void setUseDefaultDuration(boolean useDefaultDuration) {
        mViewPagerScroller.setUseDefaultDuration(useDefaultDuration);
    }

    /**
     * 获取Banner页面切换动画时间
     */
    public int getDuration() {
        return mViewPagerScroller.getScrollDuration();
    }


    public static class BannerPagerAdapter<T> extends PagerAdapter {
        private List<T> mDatas;
        private BannerHolderCreator mBannerHolderCreator;
        private ViewPager mViewPager;
        private boolean canLoop;
        private BannerPageClickListener mPageClickListener;
        private final int mLooperCountFactor = 500;

        BannerPagerAdapter(List<T> datas, BannerHolderCreator BannerHolderCreator, boolean canLoop) {
            if (mDatas == null) {
                mDatas = new ArrayList<>();
            }
            if (datas != null)
                mDatas.addAll(datas);
            mBannerHolderCreator = BannerHolderCreator;
            this.canLoop = canLoop;
        }

        void setPageClickListener(BannerPageClickListener pageClickListener) {
            mPageClickListener = pageClickListener;
        }

        /**
         * 初始化Adapter和设置当前选中的Item
         */
        void setUpViewViewPager(ViewPager viewPager) {
            mViewPager = viewPager;
            mViewPager.setAdapter(this);
            mViewPager.getAdapter().notifyDataSetChanged();
            int currentItem = canLoop ? getStartSelectItem() : 0;
            //设置当前选中的Item
            mViewPager.setCurrentItem(currentItem);
        }

        private int getStartSelectItem() {
            // 我们设置当前选中的位置为Integer.MAX_VALUE / 2,这样开始就能往左滑动
            // 但是要保证这个值与getRealPosition 的 余数为0，因为要从第一页开始显示
            int currentItem = getRealCount() * mLooperCountFactor / 2;
            if (currentItem % getRealCount() == 0) {
                return currentItem;
            }
            // 直到找到从0开始的位置
            while (currentItem % getRealCount() != 0) {
                currentItem++;
            }
            return currentItem;
        }

        public void setDatas(List<T> datas) {
            mDatas = datas;
        }

        @Override
        public int getCount() {
            return canLoop ? getRealCount() * mLooperCountFactor : getRealCount();//ViewPager返回int 最大值
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            View view = getView(position, container);
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            // 轮播模式才执行
            if (canLoop) {
                int position = mViewPager.getCurrentItem();
                if (position == getCount() - 1) {
                    position = 0;
                    setCurrentItem(position);
                }
            }

        }

        private void setCurrentItem(int position) {
            try {
                mViewPager.setCurrentItem(position, false);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        /**
         * 获取真实的Count
         */
        private int getRealCount() {
            return mDatas == null ? 0 : mDatas.size();
        }

        private View getView(int position, ViewGroup container) {

            final int realPosition = position % getRealCount();
            BannerViewHolder<T> holder;
            // create holder
            holder = mBannerHolderCreator.createViewHolder();

            if (holder == null) {
                throw new RuntimeException("can not return a null holder");
            }
            // create View
            View view = holder.createView(container.getContext());

            if (mDatas != null && mDatas.size() > 0) {
                holder.onBind(container.getContext(), realPosition, mDatas.get(realPosition));
            }

            // 添加点击事件
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPageClickListener != null) {
                        mPageClickListener.onPageClick(v, realPosition);
                    }
                }
            });

            return view;
        }


    }

    /**
     * ＊由于ViewPager 默认的切换速度有点快，因此用一个Scroller 来控制切换的速度
     * <p>而实际上ViewPager 切换本来就是用的Scroller来做的，因此我们可以通过反射来</p>
     * <p>获取取到ViewPager 的 mScroller 属性，然后替换成我们自己的Scroller</p>
     */
    public static class ViewPagerScroller extends Scroller {
        private int mDuration = 800;// ViewPager默认的最大Duration 为600,我们默认稍微大一点。值越大越慢。
        private boolean mIsUseDefaultDuration = false;

        ViewPagerScroller(Context context) {
            super(context);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {
            super.startScroll(startX, startY, dx, dy, mDuration);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, mIsUseDefaultDuration ? duration : mDuration);
        }

        void setUseDefaultDuration(boolean useDefaultDuration) {
            mIsUseDefaultDuration = useDefaultDuration;
        }

        public boolean isUseDefaultDuration() {
            return mIsUseDefaultDuration;
        }

        void setDuration(int duration) {
            mDuration = duration;
        }


        int getScrollDuration() {
            return mDuration;
        }
    }

    /**
     * Banner 点击回调
     */
    public interface BannerPageClickListener {
        void onPageClick(View view, int position);
    }

    public static int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

}
