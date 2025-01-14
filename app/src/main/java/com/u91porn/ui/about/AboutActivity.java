package com.u91porn.ui.about;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.gson.Gson;
import com.qmuiteam.qmui.util.QMUIPackageHelper;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.sdsmdg.tastytoast.TastyToast;
import com.u91porn.MyApplication;
import com.u91porn.R;
import com.u91porn.data.NoLimit91PornServiceApi;
import com.u91porn.data.model.UpdateVersion;
import com.u91porn.service.UpdateDownloadService;
import com.u91porn.ui.MvpActivity;
import com.u91porn.ui.main.MainActivity;
import com.u91porn.ui.update.UpdatePresenter;
import com.u91porn.utils.ApkVersionUtils;
import com.u91porn.utils.AppCacheUtils;
import com.u91porn.utils.DialogUtils;
import com.u91porn.utils.GlideApp;
import com.u91porn.utils.Keys;
import com.u91porn.utils.SPUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author flymegoc
 */
public class AboutActivity extends MvpActivity<AboutView, AboutPresenter> implements AboutView {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.version)
    TextView mVersionTextView;
    @BindView(R.id.about_list)
    QMUIGroupListView mAboutGroupListView;
    @BindView(R.id.copyright)
    TextView mCopyrightTextView;

    private AlertDialog alertDialog;
    private AlertDialog cleanCacheDialog;
    private QMUIGroupListView.Section aboutSection;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        toolbar.setContentInsetStartWithNavigation(0);

        setTitle("关于");

        mVersionTextView.setText("v" + QMUIPackageHelper.getAppVersion(this));

        aboutSection = initAboutSection();
        aboutSection.addTo(mAboutGroupListView);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy", Locale.CHINA);
        String currentYear = dateFormat.format(new java.util.Date());
        mCopyrightTextView.setText(String.format(getResources().getString(R.string.about_copyright), currentYear));

        alertDialog = DialogUtils.initLodingDialog(this, "正在检查更新，请稍后...");
    }

    private QMUIGroupListView.Section initAboutSection() {
        boolean isDownloadNeedWifi = (boolean) SPUtils.get(this, Keys.KEY_SP_DOWNLOAD_VIDEO_NEED_WIFI, false);
        QMUICommonListItemView itemWithSwitch = mAboutGroupListView.createItemView("非Wi-Fi环境下下载视频");
        itemWithSwitch.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_SWITCH);
        itemWithSwitch.getSwitch().setChecked(!isDownloadNeedWifi);
        itemWithSwitch.getSwitch().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SPUtils.put(AboutActivity.this, Keys.KEY_SP_DOWNLOAD_VIDEO_NEED_WIFI, !isChecked);
            }
        });
        return QMUIGroupListView.newSection(this)

                .addItemView(mAboutGroupListView.createItemView(getResources().getString(R.string.about_item_github)), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String url = "https://github.com/techGay/91porn";
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    }
                })
                .addItemView(mAboutGroupListView.createItemView(getResources().getString(R.string.about_item_homepage)), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String url = "https://github.com/techGay/91porn/issues";
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    }
                })
                .addItemView(itemWithSwitch, null)
                .addItemView(mAboutGroupListView.createItemView(getCleanCacheTitle()), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showChoiceCacheCleanDialog();
                    }
                })
                .addItemView(mAboutGroupListView.createItemView(getResources().getString(R.string.about_check_update)), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int versionCode = ApkVersionUtils.getVersionCode(AboutActivity.this);
                        if (versionCode == 0) {
                            showMessage("获取应用本版失败", TastyToast.ERROR);
                            return;
                        }
                        alertDialog.show();
                        presenter.checkUpdate(versionCode);
                    }
                });

    }

    private void showChoiceCacheCleanDialog() {
        final String[] items = new String[]{
                "网页缓存(" + AppCacheUtils.getRxcacheFileSizeStr(this) + ")",
                "视频缓存(" + AppCacheUtils.getVideoCacheFileSizeStr(this) + ")",
                "图片缓存(" + AppCacheUtils.getGlidecacheFileSizeStr(this) + ")"
        };
        final QMUIDialog.MultiCheckableDialogBuilder builder = new QMUIDialog.MultiCheckableDialogBuilder(this)
                .setCheckedItems(new int[]{1})
                .addItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.setTitle("请选择要清除的缓存");
        builder.addAction("取消", new QMUIDialogAction.ActionListener() {
            @Override
            public void onClick(QMUIDialog dialog, int index) {
                dialog.dismiss();
            }
        });
        builder.addAction("清除", new QMUIDialogAction.ActionListener() {
            @Override
            public void onClick(QMUIDialog dialog, int index) {
                actionCleanFile(builder);
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void actionCleanFile(QMUIDialog.MultiCheckableDialogBuilder builder) {
        int selectIndexLength = builder.getCheckedItemIndexes().length;
        List<File> fileDirList = new ArrayList<>();
        for (int i = 0; i < selectIndexLength; i++) {
            int indexCheck = builder.getCheckedItemIndexes()[i];
            switch (indexCheck) {
                case 0:
                    fileDirList.add(AppCacheUtils.getRxCacheDir(AboutActivity.this));
                    break;
                case 1:
                    fileDirList.add(AppCacheUtils.getVideoCacheDir(AboutActivity.this));
                    break;
                case 2:
                    fileDirList.add(AppCacheUtils.getGlideDiskCacheDir(AboutActivity.this));
                default:
            }
        }
        if (fileDirList.size() == 0) {
            showMessage("未选择任何条目，无法清除缓存", TastyToast.INFO);
            return;
        }
        presenter.cleanCacheFile(fileDirList, getApplicationContext());
    }

    private String getCleanCacheTitle() {
        String zeroFileSize = "0 B";
        String fileSizeStr = AppCacheUtils.getAllCacheFileSizeStr(this);
        if (zeroFileSize.equals(fileSizeStr)) {
            return getResources().getString(R.string.about_item_clean_cache);
        }
        return getResources().getString(R.string.about_item_clean_cache) + "(" + fileSizeStr + ")";
    }

    @NonNull
    @Override
    public AboutPresenter createPresenter() {
        NoLimit91PornServiceApi noLimit91PornServiceApi = MyApplication.getInstace().getNoLimit91PornService();
        return new AboutPresenter(new UpdatePresenter(noLimit91PornServiceApi, new Gson(), provider), provider);
    }

    private void showUpdateDialog(final UpdateVersion updateVersion) {
        new QMUIDialog.MessageDialogBuilder(this)
                .setTitle("发现新版本--v" + updateVersion.getVersionName())
                .setMessage(updateVersion.getUpdateMessage())
                .addAction("立即更新", new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        dialog.dismiss();
                        showMessage("开始下载", TastyToast.INFO);
                        Intent intent = new Intent(AboutActivity.this, UpdateDownloadService.class);
                        intent.putExtra("updateVersion", updateVersion);
                        startService(intent);
                    }
                })
                .addAction("稍后更新", new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    public void needUpdate(UpdateVersion updateVersion) {
        showUpdateDialog(updateVersion);
    }

    @Override
    public void noNeedUpdate() {
        showMessage("当前已是最新版本", TastyToast.SUCCESS);
    }

    @Override
    public void checkUpdateError(String message) {
        showMessage(message, TastyToast.ERROR);
    }

    @Override
    public void showLoading(boolean pullToRefresh) {
        alertDialog.show();
    }

    @Override
    public void showContent() {
        dismissDialog();
    }

    @Override
    public void showMessage(String msg, int type) {
        super.showMessage(msg, type);
    }

    @Override
    public void showError(String message) {

    }

    @Override
    public void showCleanDialog(String message) {
        cleanCacheDialog = DialogUtils.initLodingDialog(this, message);
        cleanCacheDialog.show();
    }

    @Override
    public void cleanCacheSuccess(String message) {
        dismissDialog();
        aboutSection.removeFrom(mAboutGroupListView);
        aboutSection = initAboutSection();
        aboutSection.addTo(mAboutGroupListView);
        showMessage(message, TastyToast.SUCCESS);
    }

    @Override
    public void cleanCacheFailure(String message) {
        dismissDialog();
        showMessage(message, TastyToast.ERROR);
    }

    private void dismissDialog() {
        if (alertDialog != null && alertDialog.isShowing() && !isFinishing()) {
            alertDialog.dismiss();
        } else if (cleanCacheDialog != null && cleanCacheDialog.isShowing() && !isFinishing()) {
            cleanCacheDialog.dismiss();
        }
    }
}
