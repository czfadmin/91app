package com.u91porn.ui.download;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.aitsuki.swipe.SwipeItemLayout;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.danikula.videocache.HttpProxyCacheServer;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.orhanobut.logger.Logger;
import com.sdsmdg.tastytoast.TastyToast;
import com.u91porn.MyApplication;
import com.u91porn.R;
import com.u91porn.adapter.DownloadVideoAdapter;
import com.u91porn.data.dao.GreenDaoHelper;
import com.u91porn.data.model.UnLimit91PornItem;
import com.u91porn.service.DownloadVideoService;
import com.u91porn.ui.MvpFragment;
import com.u91porn.utils.AppCacheUtils;
import com.u91porn.utils.DownloadManager;
import com.u91porn.utils.Keys;
import com.u91porn.utils.SPUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * A simple {@link Fragment} subclass.
 */
public class DownloadingFragment extends MvpFragment<DownloadView, DownloadPresenter> implements DownloadManager.DownloadStatusUpdater, DownloadView {

    private static final String TAG = DownloadingFragment.class.getSimpleName();
    @BindView(R.id.recyclerView_download)
    RecyclerView recyclerView;
    Unbinder unbinder;
    private DownloadVideoAdapter mDownloadAdapter;


    public DownloadingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DownloadManager.getImpl().addUpdater(this);
    }

    @NonNull
    @Override
    public DownloadPresenter createPresenter() {
        DownloadActivity downloadActivity = (DownloadActivity) getActivity();
        GreenDaoHelper greenDaoHelper = GreenDaoHelper.getInstance();
        HttpProxyCacheServer cacheServer = MyApplication.getInstace().getProxy();
        File videoCacheDir = AppCacheUtils.getVideoCacheDir(getContext());
        return new DownloadPresenter(greenDaoHelper, downloadActivity.provider, cacheServer, videoCacheDir);

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);
        List<UnLimit91PornItem> mUnLimit91PornItemList = new ArrayList<>();
        mDownloadAdapter = new DownloadVideoAdapter(R.layout.item_right_menu_delete_download, mUnLimit91PornItemList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mDownloadAdapter);
        mDownloadAdapter.setEmptyView(R.layout.empty_view, recyclerView);

        mDownloadAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                UnLimit91PornItem unLimit91PornItem = (UnLimit91PornItem) adapter.getItem(position);
                Logger.t(TAG).d("当前状态：" + unLimit91PornItem.getStatus());
                if (view.getId() == R.id.right_menu_delete) {
                    SwipeItemLayout swipeItemLayout = (SwipeItemLayout) view.getParent();
                    swipeItemLayout.close();
                    FileDownloader.getImpl().clear(unLimit91PornItem.getDownloadId(), unLimit91PornItem.getDownLoadPath());
                    presenter.deleteDownloadingTask(unLimit91PornItem);
                    presenter.loadDownloadingData();
                } else if (view.getId() == R.id.iv_download_control) {
                    if (FileDownloader.getImpl().isServiceConnected()) {
                        if (unLimit91PornItem.getStatus() == FileDownloadStatus.progress) {
                            FileDownloader.getImpl().pause(unLimit91PornItem.getDownloadId());
                            ((ImageView) view).setImageResource(R.drawable.start_download);
                        } else if (unLimit91PornItem.getStatus() == FileDownloadStatus.warn) {
                            startDownload(unLimit91PornItem, view, true);
                        } else if (unLimit91PornItem.getStatus() == FileDownloadStatus.paused) {
                            startDownload(unLimit91PornItem, view, false);
                        } else if (unLimit91PornItem.getStatus() == FileDownloadStatus.error) {
                            startDownload(unLimit91PornItem, view, false);
                        }
                    }
                }
            }
        });
        presenter.loadDownloadingData();
    }

    private void startDownload(UnLimit91PornItem unLimit91PornItem, View view, boolean isForceReDownload) {
        boolean isDownloadNeedWifi = (boolean) SPUtils.get(getContext(), Keys.KEY_SP_DOWNLOAD_VIDEO_NEED_WIFI, false);
        presenter.downloadVideo(unLimit91PornItem, isDownloadNeedWifi, isForceReDownload);
        ((ImageView) view).setImageResource(R.drawable.pause_download);
        Intent intent = new Intent(getContext(), DownloadVideoService.class);
        getContext().startService(intent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_download, container, false);
    }

    @Override
    public void blockComplete(BaseDownloadTask task) {
        Logger.t(TAG).d("已经下载完成了");
        presenter.loadDownloadingData();
    }

    @Override
    public void update(BaseDownloadTask task) {
        presenter.loadDownloadingData();
    }

    @Override
    public String getTitle() {
        return "正在下载";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DownloadManager.getImpl().removeUpdater(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void setDownloadingData(List<UnLimit91PornItem> unLimit91PornItems) {
        mDownloadAdapter.setNewData(unLimit91PornItems);
        if (unLimit91PornItems.size() == 0) {
            try {
                Intent intent = new Intent(getContext(), DownloadVideoService.class);
                getContext().stopService(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setFinishedData(List<UnLimit91PornItem> unLimit91PornItems) {

    }

    @Override
    public void showLoading(boolean pullToRefresh) {

    }

    @Override
    public void showContent() {

    }

    @Override
    public void showMessage(String msg, int type) {
        super.showMessage(msg, type);
    }

    @Override
    public void showError(String message) {

    }
}
