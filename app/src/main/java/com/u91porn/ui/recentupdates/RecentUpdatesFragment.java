package com.u91porn.ui.recentupdates;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aitsuki.swipe.SwipeMenuRecyclerView;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.helper.loadviewhelper.help.OnLoadViewListener;
import com.helper.loadviewhelper.load.LoadViewHelper;
import com.sdsmdg.tastytoast.TastyToast;
import com.u91porn.MyApplication;
import com.u91porn.R;
import com.u91porn.adapter.UnLimit91Adapter;
import com.u91porn.data.NoLimit91PornServiceApi;
import com.u91porn.data.cache.CacheProviders;
import com.u91porn.data.model.UnLimit91PornItem;
import com.u91porn.ui.MvpFragment;
import com.u91porn.utils.HeaderUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * @author flymegoc
 */
public class RecentUpdatesFragment extends MvpFragment<RecentUpdatesView, RecentUpdatesPresenter> implements RecentUpdatesView, SwipeRefreshLayout.OnRefreshListener {
    @BindView(R.id.recyclerView_recent_updates)
    SwipeMenuRecyclerView recyclerViewRecentUpdates;
    @BindView(R.id.contentView)
    SwipeRefreshLayout contentView;
    Unbinder unbinder;
    private String next;
    private UnLimit91Adapter mUnLimit91Adapter;
    private LoadViewHelper helper;

    public RecentUpdatesFragment() {
        // Required empty public constructor
    }

    public static RecentUpdatesFragment newInstance(String next) {
        RecentUpdatesFragment fragment = new RecentUpdatesFragment();
        Bundle args = new Bundle();
        args.putString("next", next);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public RecentUpdatesPresenter createPresenter() {
        NoLimit91PornServiceApi noLimit91PornServiceApi = MyApplication.getInstace().getNoLimit91PornService();
        CacheProviders cacheProviders = MyApplication.getInstace().getCacheProviders();
        return new RecentUpdatesPresenter(noLimit91PornServiceApi, cacheProviders, next,provider);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        next = getArguments().getString("next");
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);
        contentView.setOnRefreshListener(this);

        ArrayList<UnLimit91PornItem> mUnLimit91PornItemList = new ArrayList<>();
        mUnLimit91Adapter = new UnLimit91Adapter(R.layout.item_unlimit_91porn, mUnLimit91PornItemList);
        recyclerViewRecentUpdates.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewRecentUpdates.setAdapter(mUnLimit91Adapter);
        mUnLimit91Adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                UnLimit91PornItem unLimit91PornItems = (UnLimit91PornItem) adapter.getData().get(position);
                goToPlayVideo(unLimit91PornItems);
            }
        });
        mUnLimit91Adapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                presenter.loadRecentUpdatesData(false, next, HeaderUtils.getIndexHeader());
            }
        }, recyclerViewRecentUpdates);
        helper = new LoadViewHelper(recyclerViewRecentUpdates);
        helper.setListener(new OnLoadViewListener() {
            @Override
            public void onRetryClick() {
                loadData(false);
            }
        });
        loadData(false);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_recent_updates, container, false);
    }

    @Override
    public void loadMoreDataComplete() {
        mUnLimit91Adapter.loadMoreComplete();
    }

    @Override
    public void loadMoreFailed() {
        showMessage("加载更多失败",TastyToast.ERROR);
        mUnLimit91Adapter.loadMoreFail();
    }

    @Override
    public void noMoreData() {
        mUnLimit91Adapter.loadMoreEnd(true);
        showMessage("没有更多数据了",TastyToast.INFO);
    }

    @Override
    public void setMoreData(List<UnLimit91PornItem> unLimit91PornItemList) {
        mUnLimit91Adapter.addData(unLimit91PornItemList);
    }

    @Override
    public void loadData(boolean pullToRefresh) {
        presenter.loadRecentUpdatesData(pullToRefresh, next, HeaderUtils.getIndexHeader());
    }

    @Override
    public void setData(List<UnLimit91PornItem> data) {
        mUnLimit91Adapter.setNewData(data);
    }

    @Override
    public void showError(String message) {
        contentView.setRefreshing(false);
        helper.showError();
        showMessage(message, TastyToast.ERROR);
    }

    @Override
    public void showLoading(boolean pullToRefresh) {
        helper.showLoading();
        contentView.setEnabled(false);
    }

    @Override
    public void showContent() {
        helper.showContent();
        contentView.setEnabled(true);
        contentView.setRefreshing(false);
    }

    @Override
    public void showMessage(String msg,int type) {
        super.showMessage(msg,type);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onRefresh() {
        loadData(false);
    }
}
