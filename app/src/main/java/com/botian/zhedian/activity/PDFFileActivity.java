package com.botian.zhedian.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.botian.zhedian.BaseActivity;
import com.botian.zhedian.MainWebViewActivity;
import com.botian.zhedian.R;
import com.botian.zhedian.utils.ProgressDialogUtil;
import com.botian.zhedian.utils.ToastUtils;
import com.botian.zhedian.utils.downloadUtil.AbsFileProgressCallback;
import com.botian.zhedian.utils.downloadUtil.DownloadFileUtils;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.util.FitPolicy;

import java.io.File;

import butterknife.BindView;

public class PDFFileActivity extends BaseActivity {
    @BindView(R.id.mImgBack)
    ImageView mImgBack;
    @BindView(R.id.pdfView)
    PDFView   pdfView;

    @Override
    protected int setLayout() {
        return R.layout.act_show_pdf;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {

    }

    @Override
    protected void initData() {
        String pdfUrl = getIntent().getStringExtra("pdfUrl");
        if (TextUtils.isEmpty(pdfUrl)) {
            ToastUtils.showToast("pdf文件链接不能为空");
            return;
        }
        ShowPdfFile(pdfUrl);
    }

    @Override
    protected void initListener() {
        mImgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void ShowPdfFile(String pdfUrl) {
        String pdfPath = getExternalCacheDir().getPath() + "/PDFFile.pdf";
        DownloadFileUtils.with()
                .downloadPath(pdfPath)
                .url(pdfUrl)
                //.url("http://zhedian.vicp.net:38009/bjJK/sys/common/static/upFiles/image_upload/082-01-01-00_A0_20210419085434902.pdf")
                .tag(MainWebViewActivity.class)
                .execute(new AbsFileProgressCallback() {
                    @Override
                    public void onSuccess(String result) {
                        ProgressDialogUtil.hideDialog();
                        if (pdfView != null)
                            pdfView.fromFile(new File(pdfPath))
                                    .enableSwipe(true) // allows to block changing pages using swipe
                                    .swipeHorizontal(false)
                                    .enableDoubletap(true)
                                    .defaultPage(0)
                                    .enableAnnotationRendering(false) // render annotations (such as comments, colors or forms)
                                    .password(null)
                                    .scrollHandle(null)
                                    .enableAntialiasing(true) // improve rendering a little bit on low-res screens
                                    // spacing between pages in dp. To define spacing color, set view background
                                    .spacing(0)
                                    .autoSpacing(false) // add dynamic spacing to fit each page on its own on the screen
                                    .pageFitPolicy(FitPolicy.WIDTH) // mode to fit pages in the view
                                    .fitEachPage(false) // fit each page to the view, else smaller pages are scaled relative to largest page.
                                    .pageSnap(false) // snap pages to screen boundaries
                                    .pageFling(false) // make a fling change only a single page like ViewPager
                                    .nightMode(false) // toggle night mode
                                    .load();
                    }

                    @Override
                    public void onProgress(long bytesRead, long contentLength, boolean done) {

                    }

                    @Override
                    public void onFailed(String errorMsg) {
                        ProgressDialogUtil.hideDialog();
                        ToastUtils.showToast("pdf下载失败");
                    }

                    @Override
                    public void onStart() {
                        ProgressDialogUtil.startShow(PDFFileActivity.this, "正在打开pdf文件");
                    }

                    @Override
                    public void onCancle() {
                        ProgressDialogUtil.hideDialog();
                    }
                });
    }
}
