package com.eric.android.view.bannerdemo;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.eric.android.view.banner.BannerView;
import com.eric.android.view.banner.holder.BannerHolderCreator;
import com.eric.android.view.banner.holder.BannerViewHolder;

import java.util.ArrayList;
import java.util.List;

public class BannerFragment extends Fragment {
    final int[] BANNER = new int[]{R.mipmap.banner1, R.mipmap.banner2, R.mipmap.banner3, R.mipmap.banner4, R.mipmap.banner5};
    private BannerView<ImageData> mBannerView;

    public static BannerFragment newInstance() {
        return new BannerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_banners_layout, null);
        initView(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mBannerView.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mBannerView.pause();
    }

    private void initView(View view) {
        mBannerView = view.findViewById(R.id.banner_pager);
        mBannerView.setDelayedTime(3000);
        mBannerView.setPages(mockData(), new BannerHolderCreator<ViewPagerHolder>() {
            @Override
            public ViewPagerHolder createViewHolder() {
                return new ViewPagerHolder();
            }
        });

    }


    public static final class ViewPagerHolder implements BannerViewHolder<ImageData> {
        private ImageView mImageView;

        @Override
        public View createView(Context context) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_banner_normal, null);
            mImageView = view.findViewById(R.id.img_banner);
            return view;
        }

        @Override
        public void onBind(Context context, int position, ImageData data) {
            mImageView.setImageResource(data.resId);
        }
    }

    private List<ImageData> mockData() {
        List<ImageData> list = new ArrayList<>();
        ImageData imageData;
        for (int banner : BANNER) {
            imageData = new ImageData();
            imageData.resId = banner;
            list.add(imageData);
        }

        return list;
    }
}
