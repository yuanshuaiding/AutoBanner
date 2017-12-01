package com.eric.android.view.banner.holder;

public interface BannerHolderCreator<VH extends BannerViewHolder> {
    /**
     * 创建ViewHolder
     */
    VH createViewHolder();
}
